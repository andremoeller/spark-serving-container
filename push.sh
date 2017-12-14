#!/usr/bin/env bash

set -x
set -e

`aws ecr get-login --no-include-email`
# Replace this with your own account and region.
ACCOUNT=038453126632
REGION=us-east-1
ECR_IMAGE=$ACCOUNT.dkr.ecr.$REGION.amazonaws.com/spark-serving
docker tag com.amazonaws/sagemaker-spark-serving $ECR_IMAGE
docker push $ECR_IMAGE
