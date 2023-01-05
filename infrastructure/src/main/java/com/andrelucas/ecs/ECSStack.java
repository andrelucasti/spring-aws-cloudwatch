package com.andrelucas.ecs;

import software.amazon.awscdk.CfnResource;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.IMachineImage;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AddCapacityOptions;
import software.amazon.awscdk.services.ecs.CfnCluster;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.EcrImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.constructs.Construct;

public class ECSStack extends Stack {

    private static final String APP_NAME = "springbootcloudwatch";
    private static final String CLUSTER_NAME = "springboot-cloudwatch-cluster";
    private static final String KEY_NAME = "springboot-cloudwatch-cluster";

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

        AddCapacityOptions addCapacityOptions = AddCapacityOptions.builder()
                .desiredCapacity(2)
                .instanceType(InstanceType.of(InstanceClass.T2, InstanceSize.MICRO))
                .machineImage(MachineImage.latestAmazonLinux())
                .keyName(KEY_NAME)
                .allowAllOutbound(true)
                .build();


        ApplicationLoadBalancedTaskImageOptions loadBalancedTaskImageOptions = ApplicationLoadBalancedTaskImageOptions
                .builder()
                .image(ecrImage)
                .enableLogging(true)
                .containerName(APP_NAME)
                .containerPort(8929)
                .build();


        Cluster cluster = Cluster.Builder.create(this, "ecs-cluster-stack")
                .clusterName(CLUSTER_NAME)
                .containerInsights(true)
                .capacity(addCapacityOptions)
                .enableFargateCapacityProviders(false)
                .build();

        ApplicationLoadBalancedFargateService
                .Builder.create(this, "fargate-ecs-stack")
                .cluster(cluster)
                .taskImageOptions(loadBalancedTaskImageOptions)
                .publicLoadBalancer(true)
                .serviceName(APP_NAME)
                .build();
    }
}
