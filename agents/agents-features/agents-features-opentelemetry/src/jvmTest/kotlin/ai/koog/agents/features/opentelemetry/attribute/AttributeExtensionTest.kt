package ai.koog.agents.features.opentelemetry.attribute

import ai.koog.agents.features.opentelemetry.mock.MockAttributesMutator
import ai.koog.agents.features.opentelemetry.mock.UnsupportedType
import ai.koog.agents.utils.HiddenString
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class AttributeExtensionTest {

    @Test
    fun `test convert STRING attribute`() =
        testAttributeType(
            key = "stringKey",
            value = "stringValue",
            expectedValue = "stringValue",
            verbose = true
        )

    @Test
    fun `test convert INTEGER attribute`() =
        testAttributeType(
            key = "intKey",
            value = 123,
            expectedValue = 123L,
            verbose = true
        )

    @Test
    fun `test convert BOOLEAN attribute`() =
        testAttributeType(
            key = "booleanKey",
            value = false,
            expectedValue = false,
            verbose = true
        )

    @Test
    fun `test convert LONG attribute`() =
        testAttributeType(
            key = "longKey",
            value = 123L,
            expectedValue = 123L,
            verbose = true
        )

    @Test
    fun `test convert DOUBLE attribute`() =
        testAttributeType(
            key = "doubleKey",
            value = 123.45,
            expectedValue = 123.45,
            verbose = true
        )

    @Test
    fun `test convert FLOAT attribute with verbose true`() =
        testAttributeType(
            key = "floatKey",
            value = 12.34f,
            expectedValue = 12.34f.toDouble(),
            verbose = true
        )

    @Test
    fun `test convert HIDDEN STRING attribute with verbose true`() =
        testAttributeType(
            key = "hiddenStringKey",
            value = HiddenString("secret"),
            expectedValue = "secret",
            verbose = true
        )

    @Test
    fun `test convert ARRAY OF STRING attribute`() =
        testAttributeType(
            key = "stringArrayKey",
            value = listOf("stringValue1", "stringValue2"),
            expectedValue = listOf("stringValue1", "stringValue2"),
            verbose = true
        )

    @Test
    fun `test convert ARRAY OF INTEGER attribute`() =
        testAttributeType(
            key = "intArrayKey",
            value = listOf(123, 456),
            expectedValue = listOf(123L, 456L),
            verbose = true
        )

    @Test
    fun `test convert ARRAY OF BOOLEAN attribute`() =
        testAttributeType(
            key = "booleanArrayKey",
            value = listOf(true, false),
            expectedValue = listOf(true, false),
            verbose = true
        )

    @Test
    fun `test convert ARRAY OF LONG attribute`() =
        testAttributeType(
            key = "longArrayKey",
            value = listOf(123L, 456L),
            expectedValue = listOf(123L, 456L),
            verbose = true
        )

    @Test
    fun `test convert ARRAY OF DOUBLE attribute`() =
        testAttributeType(
            key = "doubleArrayKey",
            value = listOf(123.45, 678.90),
            expectedValue = listOf(123.45, 678.90),
            verbose = true
        )

    @Test
    fun `test convert ARRAY OF HIDDEN STRING attribute with verbose true`() =
        testAttributeType(
            key = "hiddenStringArrayKey",
            value = listOf(
                HiddenString("one"),
                HiddenString("two")
            ),
            expectedValue = listOf("one", "two"),
            verbose = true
        )

    @Test
    fun `test convert UNSUPPORTED type attribute`() {
        val unsupportedTypeObj = UnsupportedType("unsupportedType")
        val testCustomAttribute = CustomAttribute("unsupportedKey", unsupportedTypeObj)

        val attrs = mutableMapOf<String, Any>()
        val mutator = MockAttributesMutator(attrs)

        val throwable = assertThrows<IllegalStateException> {
            mutator.applyAttributes(listOf(testCustomAttribute), verbose = true)
        }

        assertEquals(
            "Attribute 'unsupportedKey' has unsupported type: ${UnsupportedType::class.simpleName}",
            throwable.message
        )
    }

    @Test
    fun `test convert LIST OF UNSUPPORTED type attribute`() {
        val unsupportedTypeObj1 = UnsupportedType("unsupportedType1")
        val unsupportedTypeObj2 = UnsupportedType("unsupportedType2")
        val unsupportedTypeList = listOf(unsupportedTypeObj1, unsupportedTypeObj2)

        val testCustomAttribute = CustomAttribute("unsupportedKey", unsupportedTypeList)

        val attrs = mutableMapOf<String, Any>()
        val mutator = MockAttributesMutator(attrs)

        val throwable = assertThrows<IllegalStateException> {
            mutator.applyAttributes(listOf(testCustomAttribute), verbose = true)
        }

        assertEquals(
            "Attribute 'unsupportedKey' has unsupported type for List values: ${UnsupportedType::class.simpleName}",
            throwable.message
        )
    }

    @Test
    fun `test converting list of attributes with different value types`() {
        val testCustomAttribute1 = CustomAttribute("stringKey", "stringValue")
        val testCustomAttribute2 = CustomAttribute("intKey", 123)

        val attrs = mutableMapOf<String, Any>()
        val mutator = MockAttributesMutator(attrs)
        mutator.applyAttributes(listOf(testCustomAttribute1, testCustomAttribute2), verbose = true)

        assertEquals(2, attrs.size)
        assertEquals("stringValue", attrs["stringKey"])
        assertEquals(123L, attrs["intKey"])
    }

    @Test
    fun `test converting list of attributes include unsupported type`() {
        val unsupportedTypeObj = UnsupportedType("unsupportedType")
        val testCustomAttribute = CustomAttribute("stringKey", "stringValue")
        val unsupportedAttribute = CustomAttribute("unsupportedKey", unsupportedTypeObj)

        val attrs = mutableMapOf<String, Any>()
        val mutator = MockAttributesMutator(attrs)

        val throwable = assertThrows<IllegalStateException> {
            mutator.applyAttributes(listOf(testCustomAttribute, unsupportedAttribute), verbose = true)
        }

        assertEquals(
            throwable.message,
            "Attribute 'unsupportedKey' has unsupported type: ${UnsupportedType::class.simpleName}"
        )
    }

    @Test
    fun `test convert HIDDEN STRING attribute with verbose false`() {
        testAttributeType(
            key = "hiddenStringKey",
            value = HiddenString("secret"),
            expectedValue = HiddenString.HIDDEN_STRING_PLACEHOLDER,
            verbose = false
        )
    }

    @Test
    fun `test convert ARRAY OF HIDDEN STRING attribute with verbose false`() {
        testAttributeType(
            key = "hiddenStringArrayKey",
            value = listOf(
                HiddenString("one"),
                HiddenString("two")
            ),
            expectedValue = listOf(
                HiddenString.HIDDEN_STRING_PLACEHOLDER,
                HiddenString.HIDDEN_STRING_PLACEHOLDER
            ),
            verbose = false
        )
    }

    //region Private Methods

    private fun <TActual, TExpected> testAttributeType(
        key: String,
        value: TActual,
        expectedValue: TExpected,
        verbose: Boolean
    ) where TActual : Any, TExpected : Any {
        val testCustomAttribute = CustomAttribute(key, value)

        val attrs = mutableMapOf<String, Any>()
        val mutator = MockAttributesMutator(attrs)
        mutator.applyAttributes(listOf(testCustomAttribute), verbose = verbose)

        assertEquals(1, attrs.size, "Expected exactly 1 attribute for key '$key'")
        assertEquals(
            expectedValue,
            attrs[key],
            "Check key: '$key'. Expected value: $expectedValue, but got: ${attrs[key]}"
        )
    }

    //endregion Private Methods
}
