package ai.koog.cli.transport

/**
 * Represents the availability of a Cli tool.
 */
public sealed interface CliAvailability

/**
 * Indicates that the tool is available.
 */
public object CliAvailable : CliAvailability

/**
 * Indicates that the tool is unavailable.
 */
public class CliUnavailable(
    public val reason: String? = null,
    public val cause: Throwable? = null,
) : CliAvailability
