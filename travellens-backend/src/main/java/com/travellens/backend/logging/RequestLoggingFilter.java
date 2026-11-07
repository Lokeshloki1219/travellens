package com.travellens.backend.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Milestone 6: writes exactly one structured log line per HTTP request.
 *
 * <p>That line is what the log-based metric counts. Each one carries
 * {@code httpStatus} as a JSON number, so the metric's filter can say
 * {@code jsonPayload.httpStatus >= 500} and get every server error regardless
 * of which endpoint produced it or whether the application caught the
 * exception.
 *
 * <h2>Why this runs before Spring Security</h2>
 *
 * A filter registered as a plain {@code @Component} is ordered last by default,
 * which would put it <em>inside</em> Spring Security's chain. Security
 * short-circuits: a request rejected with 401 or 403 never reaches the end of
 * the chain, so those responses would never be logged &mdash; and those are
 * precisely the ones worth seeing. Spring Security's chain sits at order -100,
 * so {@code HIGHEST_PRECEDENCE + 50} puts this filter outside it and lets it
 * observe the real status of every response.
 *
 * <p>This project has already been bitten by exactly that blind spot: a 403
 * from the CORS filter in Milestone 5 produced no application log at all, and
 * the only way to see it was the browser's network tab. After this change it
 * would have been one line in Cloud Logging.
 *
 * <h2>Probe traffic is excluded</h2>
 *
 * The kubelet hits {@code /healthz/live} and {@code /healthz/ready} on every
 * pod every ten seconds, and the load balancer adds its own checks. Logging
 * those would be roughly 25,000 entries a day of pure noise, would bury real
 * traffic, and would cost money in log storage. They are skipped.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class RequestLoggingFilter extends OncePerRequestFilter {

    /**
     * A dedicated logger name, so access logs can be filtered apart from
     * application logs with {@code jsonPayload.logger="http.access"}.
     */
    private static final Logger log = LoggerFactory.getLogger("http.access");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/healthz");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        long startedAt = System.nanoTime();

        try {
            chain.doFilter(request, response);
        } finally {
            // In a finally block so the request is still logged when something
            // downstream throws. Without this, the errors most worth counting
            // would be the ones that never got counted.
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            int status = response.getStatus();

            MDC.put("httpMethod", request.getMethod());
            MDC.put("httpPath", request.getRequestURI());
            MDC.put("httpStatus", Integer.toString(status));
            MDC.put("durationMs", Long.toString(durationMs));

            try {
                String line = "{} {} -> {} ({} ms)";
                if (status >= 500) {
                    log.error(line, request.getMethod(), request.getRequestURI(), status, durationMs);
                } else if (status >= 400) {
                    log.warn(line, request.getMethod(), request.getRequestURI(), status, durationMs);
                } else {
                    log.info(line, request.getMethod(), request.getRequestURI(), status, durationMs);
                }
            } finally {
                // Tomcat reuses worker threads. Leaving MDC populated would
                // stamp the next request on this thread with the previous
                // request's path and status.
                MDC.remove("httpMethod");
                MDC.remove("httpPath");
                MDC.remove("httpStatus");
                MDC.remove("durationMs");
            }
        }
    }
}
