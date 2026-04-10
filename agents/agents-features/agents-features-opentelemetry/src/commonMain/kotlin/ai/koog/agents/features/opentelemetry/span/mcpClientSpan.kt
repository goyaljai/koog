package ai.koog.agents.features.opentelemetry.span

import ai.koog.agents.features.opentelemetry.attribute.CommonAttributes
import ai.koog.agents.features.opentelemetry.attribute.McpAttributes
import ai.koog.agents.features.opentelemetry.attribute.SpanAttributes

/**
 * Enriches an Execute Tool Span with MCP-specific attributes.
 *
 * @param toolName The name of the tool being called.
 * @param methodName The MCP method name string (e.g., "tools/call").
 * @param serverAddress The server address for client spans (recommended).
 * @param serverPort The server port for client spans (recommended).
 * @param sessionId The MCP session identifier (recommended when part of a session).
 * @param mcpProtocolVersion The MCP protocol version in use (recommended).
 * @param mcpTransportType The transport type used for communication (recommended).
 * @return This span with MCP attributes added.
 */
internal fun GenAIAgentSpan.enrichExecuteToolSpanWithMcpAttrs(
    toolName: String,
    methodName: String,
    serverAddress: String? = null,
    serverPort: Int? = null,
    sessionId: String? = null,
    mcpProtocolVersion: String,
    mcpTransportType: String,
): GenAIAgentSpan {
    // mcp.method.name (REQUIRED)
    addAttribute(McpAttributes.Mcp.Method.Name(methodName))

    // gen_ai.operation.name (RECOMMENDED for tool calls)
    addAttribute(SpanAttributes.Operation.Name(SpanAttributes.Operation.OperationNameType.EXECUTE_TOOL))

    // gen_ai.tool.name (CONDITIONALLY REQUIRED)
    addAttribute(SpanAttributes.Tool.Name(toolName))

    // mcp.session.id (RECOMMENDED)
    sessionId?.let { session ->
        addAttribute(McpAttributes.Mcp.Session.Id(session))
    }

    // mcp.protocol.version (RECOMMENDED)
    addAttribute(McpAttributes.Mcp.Protocol.Version(mcpProtocolVersion))
    // network.transport (RECOMMENDED)
    addAttribute(McpAttributes.Network.Transport(mcpTransportType))

    // server.address (RECOMMENDED)
    serverAddress?.let { address ->
        addAttribute(CommonAttributes.Server.Address(address))
    }

    // server.port (RECOMMENDED)
    serverPort?.let { port ->
        addAttribute(CommonAttributes.Server.Port(port))
    }

    return this
}
