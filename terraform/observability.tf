# ---------------------------------------------------------------------------
# Observability: the log-based metric, the notification channel, the alert
# policy and the dashboard.
#
# All four are Terraform rather than console clicks. That is a deliberate
# choice and worth defending: a dashboard assembled by hand exists only in one
# project, cannot be reviewed in a pull request, and is gone the moment someone
# deletes it. Declared here, the alerting configuration is versioned alongside
# the code that produces the logs it watches.
# ---------------------------------------------------------------------------

# ---------------------------------------------------------------------------
# The log-based metric.
#
# Counts one unit for every request that returned a 5xx. The filter depends on
# three things lining up, and all three are in this repository:
#
#   1. CloudLoggingLayout writes one JSON object per line, so the GKE logging
#      agent parses it into jsonPayload rather than a flat textPayload.
#   2. RequestLoggingFilter puts httpStatus in the MDC for every request.
#   3. The layout writes httpStatus unquoted, so >= is a numeric comparison
#      and not a string one.
#
# Break any of those and this metric reports zero for ever, with no error
# anywhere - which is the failure mode worth being paranoid about.
#
# The httpPath label means the metric is not just a count but a breakdown: the
# dashboard can show which endpoint is failing, not merely that something is.
# ---------------------------------------------------------------------------
resource "google_logging_metric" "http_5xx" {
  name        = "travellens_http_5xx_count"
  description = "Count of HTTP 5xx responses from the TravelLens backend, taken from structured application logs."

  filter = <<-EOT
    resource.type="k8s_container"
    resource.labels.cluster_name="${var.cluster_name}"
    resource.labels.container_name="backend"
    jsonPayload.httpStatus>=500
  EOT

  metric_descriptor {
    metric_kind = "DELTA"
    value_type  = "INT64"
    unit        = "1"
    display_name = "TravelLens HTTP 5xx"

    labels {
      key         = "httpPath"
      value_type  = "STRING"
      description = "Request path that produced the error"
    }
  }

  label_extractors = {
    "httpPath" = "EXTRACT(jsonPayload.httpPath)"
  }

  depends_on = [google_project_service.required]
}

# ---------------------------------------------------------------------------
# Where alerts go.
#
# Email, because it is the one channel every team member already has and the
# only one that needs no extra account to prove on camera. The address is a
# variable so it is not hard-coded into a public repository.
# ---------------------------------------------------------------------------
resource "google_monitoring_notification_channel" "email" {
  display_name = "TravelLens team email"
  type         = "email"

  labels = {
    email_address = var.alert_email
  }

  depends_on = [google_project_service.required]
}

# ---------------------------------------------------------------------------
# The alert policy.
#
# Fires when more than zero 5xx responses are recorded in a one-minute window.
#
# A threshold of zero is unusually aggressive and that is intentional for this
# application. TravelLens serves a handful of users; a single server error is
# genuinely abnormal and worth an email. A busy service would set this to a
# rate or a ratio instead - five percent of requests over five minutes, say -
# because at scale a threshold of zero is just a way of training people to
# ignore the alert.
#
# auto_close after 30 minutes stops a one-off error from leaving an incident
# open indefinitely.
# ---------------------------------------------------------------------------
resource "google_monitoring_alert_policy" "http_5xx" {
  display_name = "TravelLens: HTTP 5xx errors detected"
  combiner     = "OR"
  enabled      = true

  conditions {
    display_name = "5xx responses in the last minute"

    condition_threshold {
      filter = join(" AND ", [
        "metric.type=\"logging.googleapis.com/user/${google_logging_metric.http_5xx.name}\"",
        "resource.type=\"k8s_container\"",
      ])

      comparison      = "COMPARISON_GT"
      threshold_value = 0
      duration        = "0s"

      aggregations {
        alignment_period = "60s"
        # The metric is DELTA - each point is a count for its interval - so the
        # right aligner is a sum, not a mean. ALIGN_MEAN here would divide the
        # count by the number of series and quietly under-report.
        per_series_aligner   = "ALIGN_SUM"
        cross_series_reducer = "REDUCE_SUM"
      }

      trigger {
        count = 1
      }
    }
  }

  notification_channels = [google_monitoring_notification_channel.email.id]

  alert_strategy {
    auto_close = "1800s"
  }

  documentation {
    mime_type = "text/markdown"
    content   = <<-EOT
      The TravelLens backend returned at least one HTTP 5xx response.

      **Where to look**

      1. Logs Explorer, filtered to
         `resource.type="k8s_container" jsonPayload.httpStatus>=500`.
         Every matching entry carries `httpPath`, `httpMethod` and a stack
         trace when one was thrown.
      2. `kubectl get pods` - a crash-looping replica usually shows here first.
      3. The service health dashboard, for whether CPU or memory moved at the
         same time.

      **Expected false positive:** `/api/debug/boom` returns 500 by design and
      exists to exercise this alert. If `httpPath` is that endpoint, someone is
      testing.
    EOT
  }
}

# ---------------------------------------------------------------------------
# The dashboard: errors on top, resource usage underneath.
#
# Those two together are the point. An error count alone tells you something is
# wrong; CPU and memory on the same timeline tell you whether the cause was
# load, a memory leak, or something unrelated to resources entirely.
# ---------------------------------------------------------------------------
resource "google_monitoring_dashboard" "travellens" {
  dashboard_json = jsonencode({
    displayName = "TravelLens - Service Health"

    mosaicLayout = {
      columns = 12
      tiles = [
        {
          xPos = 0, yPos = 0, width = 8, height = 4
          widget = {
            title = "HTTP 5xx responses per minute"
            xyChart = {
              dataSets = [{
                plotType   = "STACKED_BAR"
                targetAxis = "Y1"
                timeSeriesQuery = {
                  timeSeriesFilter = {
                    filter = "metric.type=\"logging.googleapis.com/user/${google_logging_metric.http_5xx.name}\" resource.type=\"k8s_container\""
                    aggregation = {
                      alignmentPeriod    = "60s"
                      perSeriesAligner   = "ALIGN_SUM"
                      crossSeriesReducer = "REDUCE_SUM"
                      groupByFields      = ["metric.label.\"httpPath\""]
                    }
                  }
                }
              }]
              yAxis = { label = "errors", scale = "LINEAR" }
            }
          }
        },
        {
          xPos = 8, yPos = 0, width = 4, height = 4
          widget = {
            title = "5xx in the last hour"
            scorecard = {
              timeSeriesQuery = {
                timeSeriesFilter = {
                  filter = "metric.type=\"logging.googleapis.com/user/${google_logging_metric.http_5xx.name}\" resource.type=\"k8s_container\""
                  aggregation = {
                    alignmentPeriod    = "3600s"
                    perSeriesAligner   = "ALIGN_SUM"
                    crossSeriesReducer = "REDUCE_SUM"
                  }
                }
              }
              sparkChartView = { sparkChartType = "SPARK_LINE" }
            }
          }
        },
        {
          xPos = 0, yPos = 4, width = 6, height = 4
          widget = {
            title = "CPU utilisation vs request, by pod"
            xyChart = {
              dataSets = [{
                plotType   = "LINE"
                targetAxis = "Y1"
                timeSeriesQuery = {
                  timeSeriesFilter = {
                    filter = "metric.type=\"kubernetes.io/container/cpu/request_utilization\" resource.type=\"k8s_container\" resource.label.\"cluster_name\"=\"${var.cluster_name}\""
                    aggregation = {
                      alignmentPeriod    = "60s"
                      perSeriesAligner   = "ALIGN_MEAN"
                      crossSeriesReducer = "REDUCE_MEAN"
                      groupByFields      = ["resource.label.\"pod_name\""]
                    }
                  }
                }
              }]
              yAxis = { label = "fraction of request", scale = "LINEAR" }
            }
          }
        },
        {
          xPos = 6, yPos = 4, width = 6, height = 4
          widget = {
            title = "Memory used, by pod"
            xyChart = {
              dataSets = [{
                plotType   = "LINE"
                targetAxis = "Y1"
                timeSeriesQuery = {
                  timeSeriesFilter = {
                    filter = "metric.type=\"kubernetes.io/container/memory/used_bytes\" resource.type=\"k8s_container\" resource.label.\"cluster_name\"=\"${var.cluster_name}\""
                    aggregation = {
                      alignmentPeriod    = "60s"
                      perSeriesAligner   = "ALIGN_MEAN"
                      crossSeriesReducer = "REDUCE_MEAN"
                      groupByFields      = ["resource.label.\"pod_name\""]
                    }
                  }
                }
              }]
              yAxis = { label = "bytes", scale = "LINEAR" }
            }
          }
        },
      ]
    }
  })

  depends_on = [google_project_service.required]
}
