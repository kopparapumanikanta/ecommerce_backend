#!/bin/bash
set -euo pipefail
SECRET_ID="${SECRET_ID:-ecommerce/app}"
REGION="${AWS_REGION:-us-east-1}"
aws secretsmanager get-secret-value --secret-id "$SECRET_ID" --region "$REGION" --query SecretString --output text \
  | python3 -c 'import sys, json; [print(f"{k}={v}") for k, v in json.load(sys.stdin).items()]' > .env
chmod 600 .env
echo "Wrote .env from Secrets Manager secret $SECRET_ID"
