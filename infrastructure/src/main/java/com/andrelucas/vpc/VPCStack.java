package com.andrelucas.vpc;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

import java.util.List;

public class VPCStack extends Stack {

    private final String environmentName;
    private final String vpcName;

    public VPCStack(final Construct scope,
                    final String id,
                    final StackProps props,
                    final String environmentName,
                    final String vpcName) {
        super(scope, id, props);
        this.environmentName = environmentName;
        this.vpcName = vpcName;
    }

    public Vpc create(){
        SubnetConfiguration publicSubnet = SubnetConfiguration.builder()
                .subnetType(SubnetType.PUBLIC)
                .name(environmentName.concat("-").concat("publicSubnet"))
                .build();

        SubnetConfiguration isolateSubnet = SubnetConfiguration.builder()
                .subnetType(SubnetType.PRIVATE_ISOLATED)
                .name(environmentName.concat("-").concat("isolateSubnet"))
                .build();

        return Vpc.Builder.create(this, "vpc")
                .vpcName(vpcName)
                .natGateways(0)
                .maxAzs(2)
                .subnetConfiguration(List.of(publicSubnet, isolateSubnet))
                .build();
    }
}
