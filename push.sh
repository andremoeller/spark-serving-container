`aws ecr get-login --no-include-email`
docker tag com.amazonaws/sagemaker-spark-serving 038453126632.dkr.ecr.us-east-1.amazonaws.com/spark-serving:$1
docker push 038453126632.dkr.ecr.us-east-1.amazonaws.com/spark-serving:$1
