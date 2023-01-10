package com.andrelucas.loadbalancer;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.CfnSecurityGroupIngress;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetGroupsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.TargetType;
import software.constructs.Construct;

import java.util.Collections;

public class LoadBalancerStack extends Stack {

    private final Vpc vpc;
    private final String loadBalancerName;
    private final String environmentName;

    private SecurityGroup applicationLoadBalancerSecurityGroupId;
    private IApplicationListener httpListener;

    public LoadBalancerStack(final Construct scope,
                             final String id,
                             final StackProps props,
                             final Vpc vpc,
                             final String loadBalancerName,
                             final String environmentName) {
        super(scope, id, props);
        this.vpc = vpc;
        this.loadBalancerName = loadBalancerName;
        this.environmentName = environmentName;
    }

    public void create(){
        SecurityGroup securityGroup = SecurityGroup.Builder.create(this, "loadbalancerSecGroup")
                .securityGroupName(environmentName.concat("-").concat("loadbalancerSecGroup"))
                .description("Public access to the load balancer")
                .vpc(vpc)
                .build();

        CfnSecurityGroupIngress.Builder.create(this, "ingressToLoadBalancer")
                .groupId(securityGroup.getSecurityGroupId())
                .cidrIp("0.0.0.0/0")
                .ipProtocol("-1")
                .build();

        ApplicationLoadBalancer applicationLoadBalancer = ApplicationLoadBalancer.Builder.create(this, "loadBalancer")
                .loadBalancerName(environmentName.concat("-").concat(loadBalancerName))
                .vpc(vpc)
                .internetFacing(true)
                .securityGroup(securityGroup)
                .build();

        IApplicationTargetGroup defaultAppTargetGroup = ApplicationTargetGroup.Builder.create(this, "defaultTargetGroup")
                .vpc(vpc)
                .port(8929)
                .protocol(ApplicationProtocol.HTTP)
                .targetGroupName(environmentName.concat("-").concat("defaultTargetGroup"))
                .targetType(TargetType.IP)
                .deregistrationDelay(Duration.seconds(5))
                .healthCheck(HealthCheck.builder()
                        .healthyThresholdCount(2)
                        .interval(Duration.seconds(10))
                        .timeout(Duration.seconds(5))
                        .build())
                .build();

        ApplicationListener httpListener = applicationLoadBalancer.addListener("httpListener",
                BaseApplicationListenerProps.builder()
                        .port(80)
                        .protocol(ApplicationProtocol.HTTP)
                        .open(true)
                        .build());

        httpListener.addTargetGroups("http-default-target-group", AddApplicationTargetGroupsProps.builder()
                        .targetGroups(Collections.singletonList(defaultAppTargetGroup))
                .build());

        this.applicationLoadBalancerSecurityGroupId = securityGroup;
        this.httpListener = httpListener;
    }

    public SecurityGroup getApplicationLoadBalancerSecurityGroupId() {
        return applicationLoadBalancerSecurityGroupId;
    }

    public IApplicationListener getHttpListener() {
        return httpListener;
    }
}
