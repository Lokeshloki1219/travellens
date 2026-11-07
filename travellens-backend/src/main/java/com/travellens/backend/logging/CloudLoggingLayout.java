package com.travellens.backend.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.LayoutBase;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Milestone 6: emits every log line as a single JSON object in the shape Google
 * Cloud Logging expects.
 *
 * <h2>Why this exists rather than a built-in format</h2>
 *
 * Spring Boot 4 ships three structured logging formats &mdash; {@code ecs},
 * {@code gelf} and {@code logstash} &mdash; and none of them is Google's. ECS
 * is close, but it writes the level as {@code log.level} and the timestamp as
 * {@code @timestamp}, while Cloud Logging promotes a field named
 * {@code severity} to the entry's real severity and a field named
 * {@code timestamp} to its real timestamp. Without those two exact names, every
 * entry arrives as severity DEFAULT, and a log-based metric filtered on
 * {@code severity="ERROR"} silently counts nothing.
 *
 * <p>This is also deliberately a plain Logback {@link LayoutBase} rather than a
 * Spring Boot {@code StructuredLogFormatter}. Logback's layout API has been
 * stable for a decade; Spring Boot 4 reorganised its auto-configuration into
 * per-technology modules, and this project has already lost an evening to that
 * (Flyway was on the classpath but its auto-configuration was in a separate
 * artifact, so migrations silently never ran). A class wired up explicitly in
 * {@code logback-spring.xml} cannot fail that way.
 *
 * <h2>How the GKE logging agent picks this up</h2>
 *
 * Containers write to stdout. The agent running on every node reads each line,
 * and if the line parses as JSON it becomes the entry's {@code jsonPayload}
 * instead of a flat {@code textPayload}. So {@code httpStatus} below is
 * queryable as {@code jsonPayload.httpStatus}, which is exactly what the
 * log-based metric in {@code terraform/observability.tf} filters on.
 *
 * <h2>Numbers stay numbers</h2>
 *
 * MDC values are always Strings, but {@code jsonPayload.httpStatus >= 500} only
 * works as a numeric comparison if the field is written unquoted. The keys in
 * {@link #NUMERIC_FIELDS} are therefore emitted without quotes.
 *
 * <p>The JSON is written by hand instead of through Jackson, on purpose: Spring
 * Boot 4 moved to Jackson 3 and the package name changed, and a logging layout
 * that fails to load takes the whole application with it.
 */
public class CloudLoggingLayout extends LayoutBase<ILoggingEvent> {

    /** MDC keys written as JSON numbers rather than strings. */
    private static final Set<String> NUMERIC_FIELDS = Set.of("httpStatus", "durationMs");

    @Override
    public String doLayout(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');

        // Cloud Logging reads these three by name and promotes them out of
        // jsonPayload onto the LogEntry itself.
        field(sb, "timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString(), true);
        sb.append(',');
        field(sb, "severity", severityOf(event.getLevel()), true);
        sb.append(',');
        field(sb, "message", event.getFormattedMessage(), true);

        sb.append(',');
        field(sb, "logger", event.getLoggerName(), true);
        sb.append(',');
        field(sb, "thread", event.getThreadName(), true);

        // Per-request context, put there by RequestLoggingFilter.
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            for (Map.Entry<String, String> e : mdc.entrySet()) {
                if (e.getValue() == null) {
                    continue;
                }
                sb.append(',');
                field(sb, e.getKey(), e.getValue(), !NUMERIC_FIELDS.contains(e.getKey()));
            }
        }

        // Stack traces go in a field of their own so the message stays readable
        // in the console and the trace is still searchable.
        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable != null) {
            sb.append(',');
            field(sb, "exceptionClass", throwable.getClassName(), true);
            sb.append(',');
            field(sb, "stackTrace", ThrowableProxyUtil.asString(throwable), true);
        }

        sb.append('}').append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * Logback levels do not all exist in Cloud Logging. TRACE collapses into
     * DEBUG and WARN becomes WARNING; anything unrecognised falls back to INFO
     * rather than being dropped.
     */
    private static String severityOf(Level level) {
        if (level == null) {
            return "INFO";
        }
        return switch (level.toInt()) {
            case Level.TRACE_INT, Level.DEBUG_INT -> "DEBUG";
            case Level.WARN_INT -> "WARNING";
            case Level.ERROR_INT -> "ERROR";
            default -> "INFO";
        };
    }

    private static void field(StringBuilder sb, String key, String value, boolean quoted) {
        sb.append('"');
        escape(sb, key);
        sb.append("\":");
        if (quoted) {
            sb.append('"');
            escape(sb, value);
            sb.append('"');
        } else {
            // Numeric fields. If the value is not actually a number the line
            // would become invalid JSON and the agent would fall back to a flat
            // textPayload, so fall back to a quoted string instead.
            if (isNumeric(value)) {
                sb.append(value);
            } else {
                sb.append('"');
                escape(sb, value);
                sb.append('"');
            }
        }
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                if (!(i == 0 && c == '-')) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Minimal RFC 8259 string escaping. */
    private static void escape(StringBuilder sb, String s) {
        if (s == null) {
            return;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
    }
}
