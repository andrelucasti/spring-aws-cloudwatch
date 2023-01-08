package com.andrelucas;

import com.andrelucas.ecr.ECRStack;
import com.andrelucas.ecs.ECSStack;
import com.andrelucas.loadbalancer.LoadBalancerStack;
import com.andrelucas.vpc.VPCStack;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class InfrastructureStackDev extends Stack {
    private static final String SAND_ENV = "sandbox";
    private static final String REPOSITORY_NAME = "start-project-cloudwatch-logging";
    private static final String CLUSTER_NAME = "springboot-cloudwatch-cluster";

    public InfrastructureStackDev(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        new ECRStack(this, "ecr-repository-stack", props, REPOSITORY_NAME)
                .execute();


        VPCStack vpcStack = new VPCStack(this, "vpc-stack", props, SAND_ENV, "spring-boot-cloudwatch-vpc");
        Vpc vpc = vpcStack.create();

        LoadBalancerStack loadBalancerStack = new LoadBalancerStack(this, "lb-stack", props, vpc, "spring-boot-cloudwatch-lb", SAND_ENV);
        loadBalancerStack.create();

        new ECSStack(this, "ecs-stack", props, vpc, loadBalancerStack.getApplicationLoadBalancerSecurityGroupId(), loadBalancerStack.getHttpListener(), REPOSITORY_NAME, CLUSTER_NAME, SAND_ENV)
                .execute();

    }
}
