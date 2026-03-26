package ai.koog.agents.snapshot

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.AIAgentPlannerContext
import ai.koog.agents.planner.AIAgentPlanner
import ai.koog.agents.planner.AIAgentPlannerStrategy
import ai.koog.agents.planner.PlannerAgentExecutionPoint
import ai.koog.agents.snapshot.feature.AgentCheckpointData
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.agents.snapshot.providers.InMemoryPersistenceStorageProvider
import ai.koog.agents.testing.tools.getMockExecutor
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.serialization.JSONObject
import ai.koog.serialization.JSONPrimitive
import ai.koog.serialization.kotlinx.KotlinxSerializer
import ai.koog.serialization.typeToken
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock

class PlannerPersistenceTests {
    enum class PlannerExecutionPoint {
        BUILD_PLAN,
        EXECUTE_STEP,
        IS_PLAN_COMPLETED
    }

    class TestPlanner(
        var failAt: PlannerExecutionPoint? = null
    ) : AIAgentPlanner<Int, Int>(
        stateType = typeToken<Int>(),
        planType = typeToken<Int>()
    ) {
        var buildPlanCalls = 0
        var executeStepCalls = 0
        var isPlanCompletedCalls = 0

        override suspend fun buildPlan(context: AIAgentPlannerContext, state: Int, plan: Int?): Int {
            buildPlanCalls++
            if (plan == 1 && failAt == PlannerExecutionPoint.BUILD_PLAN) throw IllegalStateException("Simulated failure at buildPlan")
            return (plan ?: 0) + 1
        }

        override suspend fun executeStep(context: AIAgentPlannerContext, state: Int, plan: Int): Int {
            executeStepCalls++
            if (plan == 2 && failAt == PlannerExecutionPoint.EXECUTE_STEP) throw IllegalStateException("Simulated failure at executeStep")
            return state + plan
        }

        override suspend fun isPlanCompleted(context: AIAgentPlannerContext, state: Int, plan: Int): Boolean {
            isPlanCompletedCalls++
            if (plan == 2 && failAt == PlannerExecutionPoint.IS_PLAN_COMPLETED) throw IllegalStateException("Simulated failure at isPlanCompleted")
            return plan >= 2 // plan is counting 1, 2, ..., so the planner will make two iterations
        }
    }

    val testStorage = InMemoryPersistenceStorageProvider()

    fun createTestPlannerAgent(): Pair<AIAgent<Int, Int>, TestPlanner> {
        val planner = TestPlanner()

        val agent = AIAgent(
            promptExecutor = getMockExecutor { },
            strategy = AIAgentPlannerStrategy("test", planner),
            agentConfig = AIAgentConfig(
                prompt = Prompt.Empty,
                model = OllamaModels.Meta.LLAMA_3_2,
                maxAgentIterations = 10
            )
        ) {
            install(Persistence) {
                this.storage = testStorage
            }
        }

        return Pair(agent, planner)
    }

    @ParameterizedTest
    @EnumSource(PlannerExecutionPoint::class)
    fun testPlannerResumesAfterFailureAtBuildPlan(failAt: PlannerExecutionPoint) = runTest {
        val (agent, planner) = createTestPlannerAgent()

        val runId = "test-run-fail-at-${failAt.name}"
        planner.failAt = failAt

        val message = assertThrows<IllegalStateException> {
            agent.run(0, runId)
        }.message

        assertNotNull(message)
        assertContains(message, "Simulated failure")

        planner.failAt = null

        val result = agent.run(0, runId)
        assertEquals(3, result)

        val expectedBuildPlanCalls = if (failAt == PlannerExecutionPoint.BUILD_PLAN) 3 else 2
        val expectedExecuteStepCalls = if (failAt == PlannerExecutionPoint.EXECUTE_STEP) 3 else 2
        val expectedIsPlanCompletedCalls = if (failAt == PlannerExecutionPoint.IS_PLAN_COMPLETED) 3 else 2

        assertEquals(expectedBuildPlanCalls, planner.buildPlanCalls)
        assertEquals(expectedExecuteStepCalls, planner.executeStepCalls)
        assertEquals(expectedIsPlanCompletedCalls, planner.isPlanCompletedCalls)
    }

    @ParameterizedTest
    @EnumSource(PlannerExecutionPoint::class)
    fun testPlannerResumesFromCheckpoint(resumeAfter: PlannerExecutionPoint) = runTest {
        val (agent, _) = createTestPlannerAgent()

        val runId = "test-run-resume-after-${resumeAfter.name}"

        val state = 42
        val plan = 88

        val executionPoint = when (resumeAfter) {
            PlannerExecutionPoint.BUILD_PLAN -> PlannerAgentExecutionPoint.PlanCreated
            PlannerExecutionPoint.EXECUTE_STEP -> PlannerAgentExecutionPoint.StepExecuted
            PlannerExecutionPoint.IS_PLAN_COMPLETED -> PlannerAgentExecutionPoint.PlanCompletionEvaluated(false)
        }

        val expectedResult = when (resumeAfter) {
            PlannerExecutionPoint.BUILD_PLAN -> state + plan // executeStep will be called, the state will be updated
            PlannerExecutionPoint.EXECUTE_STEP -> state // executeStep will not be called, the state will remain the same
            PlannerExecutionPoint.IS_PLAN_COMPLETED -> state + plan + 1 // buildPlan and executeStep will be called, the plan will be incremented, the state will be updated
        }

        val checkpoint = AgentCheckpointData(
            checkpointId = "id123",
            createdAt = Clock.System.now(),
            messageHistory = emptyList(),
            version = 0,
            properties = JSONObject(
                mapOf(
                    "executionPoint" to KotlinxSerializer().encodeToJSONElement(executionPoint, typeToken<PlannerAgentExecutionPoint>()),
                    "state" to JSONPrimitive(state),
                    "plan" to JSONPrimitive(plan)
                )
            ),
        )

        testStorage.saveCheckpoint(runId, checkpoint)

        val result = agent.run(state, runId)
        assertEquals(expectedResult, result)
    }
}
