resource "aws_secretsmanager_secret" "app" {
  name                    = "ecommerce/app"
  recovery_window_in_days = 0
}

data "aws_iam_policy_document" "ec2_secrets" {
  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_secretsmanager_secret.app.arn]
  }
}

resource "aws_iam_role_policy" "ec2_secrets" {
  name   = "read-app-secret"
  role   = aws_iam_role.ec2.id
  policy = data.aws_iam_policy_document.ec2_secrets.json
}

output "secret_name" {
  value = aws_secretsmanager_secret.app.name
}
