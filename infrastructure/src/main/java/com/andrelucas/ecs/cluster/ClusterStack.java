package com.andrelucas.ecs.cluster;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.constructs.Construct;

public class ClusterStack extends Stack {

    private final String environmentName;
    private final IVpc vpc;
    private final String clusterName;

    public ClusterStack(final Construct scope,
                        final String id,
                        final StackProps props,
                        final String environmentName,
                        final IVpc vpc,
                        final String clusterName) {

        super(scope, id, props);
        this.environmentName = environmentName;
        this.vpc = vpc;
        this.clusterName = clusterName;
    }

    public void create(){
        Cluster.Builder.create(this, "cluster")
                .vpc(vpc)
                .clusterName(environmentName.concat("-").concat(clusterName))
                .build();

    }
}
