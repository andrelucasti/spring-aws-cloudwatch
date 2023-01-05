package com.andrelucas;

import com.andrelucas.ecr.ECRStack;
import com.andrelucas.ecs.ECSStack;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class InfrastructureStack extends Stack {

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        new ECRStack(this, "ecr-repository-stack", props, "start-project-cloudwatch-logging")
                .execute();

        new ECSStack(this, "ecs-stack", props, "start-project-cloudwatch-logging")
                .execute();
    }
}