terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

variable "region" {
  type    = string
  default = "us-east-1"
}

resource "aws_ecr_repository" "backend" {
  name                 = "ecommerce-backend"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/ecommerce-backend"
  retention_in_days = 14
}

resource "aws_sqs_queue" "order_events_dlq" {
  name = "order-events-dlq"
}

resource "aws_sqs_queue" "order_events" {
  name = "order-events"

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.order_events_dlq.arn
    maxReceiveCount     = 3
  })
}

resource "aws_dynamodb_table" "event_records" {
  name         = "event_records"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "eventId"

  attribute {
    name = "eventId"
    type = "S"
  }

  attribute {
    name = "entityId"
    type = "S"
  }

  global_secondary_index {
    name            = "entityId-index"
    hash_key        = "entityId"
    projection_type = "ALL"
  }
}

output "ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}
