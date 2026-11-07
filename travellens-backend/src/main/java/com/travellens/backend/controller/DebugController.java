package com.travellens.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Milestone 6: a deliberate, harmless way to produce a server error.
 *
 * <p>The rubric asks for a <em>controlled</em> failure, and the emphasis
 * matters. The alternative ways to make this application return a 500 all
 * involve breaking something real &mdash; stopping Cloud SQL, revoking an IAM
 * role, deleting a secret &mdash; and every one of those risks data or takes
 * minutes to undo. This endpoint throws an exception and changes nothing: no
 * write, no state, no side effect beyond one log line and one increment of the
 * metric.
 *
 * <h2>What happens when you call it</h2>
 *
 * <ol>
 *   <li>This method throws {@link IllegalStateException}.</li>
 *   <li>{@code GlobalExceptionHandler} catches it and returns HTTP 500.</li>
 *   <li>{@code RequestLoggingFilter} sees status 500 on the way out and logs
 *       at ERROR with {@code httpStatus: 500} as a JSON number.</li>
 *   <li>The GKE logging agent parses that line into {@code jsonPayload}.</li>
 *   <li>The log-based metric {@code travellens_http_5xx_count} matches it and
 *       increments.</li>
 *   <li>If enough arrive inside the alert window, the alert policy fires and
 *       emails the team.</li>
 * </ol>
 *
 * <p>So one request exercises the entire observability chain end to end, which
 * is the point.
 *
 * <h2>On leaving it reachable</h2>
 *
 * It is unauthenticated, because the demo needs it to be callable from a
 * browser address bar on camera. That is a defensible choice here and would not
 * be in a real deployment: it returns no data, touches no state and reveals
 * nothing about the schema or the environment, but it is still a free way for
 * anyone to write to our logs. In a production system this would be gated
 * behind an admin role, bound to an internal-only path, or compiled out of
 * release builds entirely. It is called out as a known exception in the written
 * analysis rather than quietly left in.
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    /**
     * Returns HTTP 500 every time, by design.
     *
     * <p>Browse to {@code https://<host>/api/debug/boom} during the demo.
     */
    @GetMapping(value = "/boom", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> boom() {
        throw new IllegalStateException(
                "Controlled failure triggered from /api/debug/boom for the Milestone 6 observability demo");
    }

    /**
     * A 200 for comparison, so the demo can show that the metric counts only
     * the errors and not every request through the same code path.
     */
    @GetMapping(value = "/ok", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> ok() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "OK");
        body.put("detail", "This endpoint succeeds. It should not appear in the 5xx metric.");
        return body;
    }
}
