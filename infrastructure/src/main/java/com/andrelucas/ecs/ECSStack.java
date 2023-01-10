package com.andrelucas.ecs;

import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.CfnSecurityGroup;
import software.amazon.awscdk.services.ec2.CfnSecurityGroupIngress;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.CfnService;
import software.amazon.awscdk.services.ecs.CfnTaskDefinition;
import software.amazon.awscdk.services.elasticloadbalancingv2.CfnListenerRule;
import software.amazon.awscdk.services.elasticloadbalancingv2.CfnTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationListener;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ECSStack extends Stack {

    private static final String APP_NAME = "springbootcloudwatch";
    private static final String DATE_FORMAT = "%Y-%m-%dT%H:%M:%S.%f%z";
    private static final Number TASK_DEF_CPU = 256;
    private static final Number TASK_DEF_MEMORY = 1024;
    private static final Number CONTAINER_PORT = 8929;

    private final IVpc vpc;
    private final SecurityGroup lbSecurityGroup;
    private final IApplicationListener httpListener;
    private final String repositoryName;
    private final String imageTag;
    private final String clusterName;
    private final String environmentName;
    public ECSStack(final Construct scope,
                    final String id,
                    final StackProps props,
                    final IVpc vpc,
                    final SecurityGroup lbSecurityGroup,
                    final IApplicationListener httpListener,
                    final String repositoryName,
                    final String clusterName,
                    final String environmentName) {

        super(scope, id, props);
        this.vpc = vpc;
        this.lbSecurityGroup = lbSecurityGroup;
        this.httpListener = httpListener;
        this.repositoryName = repositoryName;
        this.clusterName = clusterName;
        this.environmentName = environmentName;

        this.imageTag = getImageTag(scope);
    }


    private static String getImageTag(Construct scope) {
        return scope.getNode().tryGetContext("imageTag") == null ? "latest" : (String) scope.getNode().tryGetContext("imageTag");
    }

    public void create() {
        IRepository repository = Repository.fromRepositoryName(this, "ecr-ecs-stack", repositoryName);


        CfnTargetGroup cfnTargetGroup = CfnTargetGroup.Builder.create(this, "targetGroup")
                .healthCheckIntervalSeconds(30)
                .healthCheckPath("/actuator/health")
                .healthCheckPort(String.valueOf(CONTAINER_PORT))
                .healthCheckProtocol("HTTP")
                .healthCheckTimeoutSeconds(5)
                .healthyThresholdCount(2)
                .unhealthyThresholdCount(8)
                .targetGroupAttributes(List.of(
                        CfnTargetGroup.TargetGroupAttributeProperty.builder().key("stickiness.enabled").value("true").build(),
                        CfnTargetGroup.TargetGroupAttributeProperty.builder().key("stickiness.type").value("lb_cookie").build(),
                        CfnTargetGroup.TargetGroupAttributeProperty.builder().key("stickiness.lb_cookie.duration_seconds").value("3600").build()
                ))
                .targetType("ip")
                .port(CONTAINER_PORT)
                .protocol("HTTP")
                .vpcId(vpc.getVpcId())
                .build();

        CfnListenerRule.ActionProperty actionProperty = CfnListenerRule.ActionProperty.builder()
                .targetGroupArn(cfnTargetGroup.getRef())
                .type("forward")
                .build();
        CfnListenerRule.RuleConditionProperty ruleConditionProperty = CfnListenerRule.RuleConditionProperty.builder()
                .field("path-pattern")
                .values(Collections.singletonList("*"))
                .build();

        CfnListenerRule httpListenerRule = CfnListenerRule.Builder.create(this, "httpListenerRule")
                .actions(Collections.singletonList(actionProperty))
                .conditions(Collections.singletonList(ruleConditionProperty))
                .listenerArn(httpListener.getListenerArn())
                .priority(2)
                .build();

        LogGroup logGroup = LogGroup.Builder.create(this, "ecsLogGroup")
                .logGroupName(environmentName.concat("-").concat(clusterName.concat("-logs")))
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        Role ecsTaskExecutionRole = Role.Builder.create(this, "ecsTaskExecutionRole")
                .assumedBy(ServicePrincipal.Builder.create("ecs-tasks.amazonaws.com").build())
                .path("/")
                .inlinePolicies(Map.of(
                        environmentName.concat("-").concat("ecsTaskExecutionRolePolicy"),
                        PolicyDocument.Builder.create()
                                .statements(Collections.singletonList(PolicyStatement.Builder.create()
                                        .effect(Effect.ALLOW)
                                        .resources(Collections.singletonList("*"))
                                        .actions(List.of(
                                                "ecr:GetAuthorizationToken",
                                                "ecr:BatchCheckLayerAvailability",
                                                "ecr:GetDownloadUrlForLayer",
                                                "ecr:BatchGetImage",
                                                "logs:CreateLogStream",
                                                "logs:PutLogEvents"))
                                        .build()))
                                .build()))
                .build();

        Role ecsTaskRole = Role.Builder.create(this, "ecsTaskRole")
                .assumedBy(ServicePrincipal.Builder.create("ecs-tasks.amazonaws.com").build())
                .path("/")
                .build();

        repository.grantPull(ecsTaskExecutionRole);

        CfnTaskDefinition.ContainerDefinitionProperty container = CfnTaskDefinition.ContainerDefinitionProperty.builder()
                .name(environmentName.concat("-").concat(APP_NAME))
                .cpu(TASK_DEF_CPU)
                .memory(TASK_DEF_MEMORY)
                .image(repository.repositoryUriForTagOrDigest(imageTag))
                //.image("https://040335195619.dkr.ecr.us-east-1.amazonaws.com/start-project-cloudwatch-logging:a262e3201053da6bcee9541b424884166a1db493")
                .logConfiguration(CfnTaskDefinition.LogConfigurationProperty.builder()
                        .logDriver("awslogs")
                        .options(Map.of(
                                "awslogs-group", logGroup.getLogGroupName(),
                                "awslogs-region", "us-east-1",
                                "awslogs-stream-prefix", "stream-".concat(environmentName).concat(APP_NAME),
                                "awslogs-datetime-format", DATE_FORMAT
                        ))
                        .build())
                .portMappings(Collections.singletonList(CfnTaskDefinition.PortMappingProperty.builder()
                        .containerPort(CONTAINER_PORT)
                        .build()))
                .stopTimeout(2)
                .build();

        CfnTaskDefinition taskDefinition = CfnTaskDefinition.Builder.create(this, "taskDefinition")
                .cpu(String.valueOf(TASK_DEF_CPU))
                .memory(String.valueOf(TASK_DEF_MEMORY))
                .networkMode("awsvpc")
                .requiresCompatibilities(Collections.singletonList("FARGATE"))
                .executionRoleArn(ecsTaskExecutionRole.getRoleArn())
                .taskRoleArn(ecsTaskRole.getRoleArn())
                .containerDefinitions(Collections.singletonList(container))
                .build();

        CfnSecurityGroup ecsSecurityGroup = CfnSecurityGroup.Builder.create(this, "ecsSecurityGroup")
                .groupName("ecsSecurityGroup")
                .vpcId(vpc.getVpcId())
                .groupDescription("SecurityGroup for the ECS containers")
                .build();

        CfnSecurityGroupIngress.Builder.create(this, "ecsIngressFromSelf")

                .sourceSecurityGroupId(ecsSecurityGroup.getAttrGroupId())
                .groupId(ecsSecurityGroup.getAttrGroupId())
                .ipProtocol("-1")
                .build();

            CfnSecurityGroupIngress.Builder.create(this, "ecsIngressFromLoadbalancer")
                .ipProtocol("-1")
                .sourceSecurityGroupId(lbSecurityGroup.getSecurityGroupId())
                .groupId(ecsSecurityGroup.getAttrGroupId())
                .build();

        CfnService ecsService = CfnService.Builder.create(this, "ecsService")
                .cluster(environmentName.concat("-").concat(clusterName))
                .launchType("FARGATE")
                .deploymentConfiguration(CfnService.DeploymentConfigurationProperty.builder()
                        .maximumPercent(100)
                        .minimumHealthyPercent(50)
                        .build())
                .desiredCount(2)
                .taskDefinition(taskDefinition.getRef())
                .serviceName(environmentName.concat("-").concat(APP_NAME))
                .loadBalancers(Collections.singletonList(CfnService.LoadBalancerProperty.builder()
                                .containerName(environmentName.concat("-").concat(APP_NAME))
                                .containerPort(CONTAINER_PORT)
                                .targetGroupArn(cfnTargetGroup.getRef())
                        .build()))
                .networkConfiguration(CfnService.NetworkConfigurationProperty.builder()
                        .awsvpcConfiguration(CfnService.AwsVpcConfigurationProperty.builder()
                                .assignPublicIp("ENABLED")
                                .securityGroups(Collections.singletonList(ecsSecurityGroup.getAttrGroupId()))
                                .subnets(this.vpc.getPublicSubnets().stream().map(ISubnet::getSubnetId).toList())
                                .build())
                        .build())
                .build();

        ecsService.addDependsOn(httpListenerRule);
    }
}
