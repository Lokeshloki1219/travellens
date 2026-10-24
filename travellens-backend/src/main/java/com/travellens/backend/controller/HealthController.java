package com.travellens.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Milestone 5: the two health endpoints that Kubernetes probes.
 *
 * <p>They exist as a pair on purpose, and the difference between them is the
 * whole point. A single "is it working?" endpoint forces one answer to serve
 * two unrelated decisions, and those decisions have opposite failure
 * behaviour.
 *
 * <h2>/healthz/live &mdash; should this container be restarted?</h2>
 *
 * Returns 200 as long as the JVM is running and the servlet container is
 * accepting and dispatching requests. It deliberately touches nothing else:
 * no database, no Cloud Storage, no Secret Manager. The fact that this handler
 * ran at all is the entire signal.
 *
 * <p>It has to be that narrow, because failing it is destructive &mdash; the
 * kubelet kills the container. The only faults a restart actually repairs are
 * faults inside this process: a deadlocked thread pool, a heap so exhausted
 * that nothing completes, a request dispatcher that has stopped dispatching.
 * Restarting fixes none of the things outside it.
 *
 * <h2>/healthz/ready &mdash; should this pod receive traffic?</h2>
 *
 * Borrows a connection from the HikariCP pool and runs {@code SELECT 1}
 * against Cloud SQL. Returns 200 if the round trip succeeds, 503 if it does
 * not. Failing it removes the pod from the Service endpoints and from the load
 * balancer's network endpoint group, but leaves the container running, so the
 * pod resumes serving the moment the database answers again.
 *
 * <p>This is also a real test of the connection pool and not just of the
 * network: if Hikari has no free connection within its timeout, this fails,
 * which is correct &mdash; a pod that cannot get a connection cannot serve a
 * request either.
 *
 * <h2>Why not Spring Boot Actuator</h2>
 *
 * Actuator can do this with health groups, roughly:
 *
 * <pre>
 *   management.endpoint.health.group.liveness.include=livenessState
 *   management.endpoint.health.group.readiness.include=readinessState,db
 * </pre>
 *
 * We chose not to, for one specific reason. Spring Boot 4 splits
 * auto-configuration into per-technology modules, and this project has already
 * been bitten by that once: {@code flyway-core} put Flyway on the classpath in
 * Milestone 4 while the auto-configuration that actually runs it lived in a
 * separate artifact, so migrations silently never executed and the only
 * symptom was Hibernate complaining about a missing table. The {@code db}
 * contributor that an Actuator readiness group depends on is auto-configured
 * the same way, which means a readiness probe could come back 200 while
 * quietly checking nothing at all &mdash; the most dangerous possible outcome
 * for a health check, and invisible unless you go looking.
 *
 * <p>Thirty lines of explicit JDBC cannot fail that way. The query is right
 * here in the file.
 */
@RestController
@RequestMapping("/healthz")
public class HealthController {

    /** Seconds the readiness query may take before it is abandoned. */
    private static final int QUERY_TIMEOUT_SECONDS = 3;

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Liveness. No dependencies, no I/O, no allocation worth mentioning.
     * Reaching this line is the proof.
     */
    @GetMapping(value = "/live", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> live() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("check", "liveness");
        body.put("detail", "process is accepting and dispatching HTTP requests");
        return ResponseEntity.ok(body);
    }

    /**
     * Readiness. Proves this pod can reach the database and get an answer
     * back, which is the minimum needed to serve a real request.
     *
     * <p>The query timeout is shorter than the probe's timeoutSeconds, so the
     * decision is made here rather than by the kubelet giving up. That
     * distinction matters: a probe that times out at the kubelet is reported as
     * a failure with no detail, while this path returns a 503 whose body names
     * the SQL state and reaches the pod's logs.
     */
    @GetMapping(value = "/ready", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("check", "readiness");

        long startedAt = System.nanoTime();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

            try (ResultSet rs = statement.executeQuery("SELECT 1")) {
                if (!rs.next() || rs.getInt(1) != 1) {
                    body.put("status", "DOWN");
                    body.put("database", "query returned an unexpected result");
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
                }
            }

            long millis = (System.nanoTime() - startedAt) / 1_000_000L;
            body.put("status", "UP");
            body.put("database", "reachable");
            body.put("queryMillis", millis);
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            // The message is useful in the pod log and in kubectl describe, and
            // it cannot leak a credential: the datasource password is never
            // part of a JDBC exception message, only the URL and SQL state.
            body.put("status", "DOWN");
            body.put("database", "unreachable");
            body.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }
}
