variable "alert_email" {
  type    = string
  default = ""
}

data "aws_iam_policy_document" "ec2_logs" {
  statement {
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.backend.arn}:*"]
  }
}

resource "aws_iam_role_policy" "ec2_logs" {
  name   = "write-app-logs"
  role   = aws_iam_role.ec2.id
  policy = data.aws_iam_policy_document.ec2_logs.json
}

resource "aws_sns_topic" "alerts" {
  name = "ecommerce-alerts"
}

resource "aws_sns_topic_subscription" "email" {
  count     = var.alert_email == "" ? 0 : 1
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

resource "aws_cloudwatch_log_metric_filter" "app_errors" {
  name           = "app-errors"
  log_group_name = aws_cloudwatch_log_group.backend.name
  pattern        = "ERROR"

  metric_transformation {
    name          = "AppErrors"
    namespace     = "Ecommerce"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_metric_alarm" "app_errors" {
  alarm_name          = "ecommerce-app-errors"
  alarm_description   = "At least one ERROR log line in 5 minutes"
  namespace           = "Ecommerce"
  metric_name         = "AppErrors"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  depends_on          = [aws_cloudwatch_log_metric_filter.app_errors]
}

resource "aws_cloudwatch_metric_alarm" "server_status" {
  alarm_name          = "ecommerce-server-status-check"
  alarm_description   = "EC2 status check failed"
  namespace           = "AWS/EC2"
  metric_name         = "StatusCheckFailed"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 3
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  dimensions = {
    InstanceId = aws_instance.backend.id
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}
