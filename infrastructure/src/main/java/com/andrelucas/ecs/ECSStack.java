package com.andrelucas.ecs;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.EcrImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.constructs.Construct;

public class ECSStack extends Stack {

    private final String repositoryName;
    public ECSStack(final Construct scope,
                    final String id,
                    final StackProps props,
                    final String repositoryName) {

        super(scope, id, props);
        this.repositoryName = repositoryName;
    }

    public void execute() {
        IRepository repository = Repository.fromRepositoryName(this, "ecr-ecs-stack", repositoryName);
        EcrImage ecrImage = ContainerImage.fromEcrRepository(repository);
        ApplicationLoadBalancedTaskImageOptions loadBalancedTaskImageOptions = ApplicationLoadBalancedTaskImageOptions
                .builder()
                .image(ecrImage)
                .enableLogging(true)
                .containerName("springboot-cloudwatch")
                .containerPort(8929)
                .build();


        ApplicationLoadBalancedFargateService
                .Builder.create(this, "fargate-ecs-stack")
                .taskImageOptions(loadBalancedTaskImageOptions)
                .publicLoadBalancer(true)
                .serviceName("springboot-cloudwatch")
                .build();
    }
}
