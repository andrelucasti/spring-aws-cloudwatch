package com.andrelucas.ecr;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.TagMutability;
import software.amazon.awscdk.services.ecr.TagStatus;
import software.constructs.Construct;

import java.util.Collections;

public class ECRStack extends Stack {
    private final String repositoryName;

    public ECRStack(final Construct scope,
                    final String id,
                    final StackProps props,
                    final String repositoryName) {

        super(scope, id, props);
        this.repositoryName = repositoryName;
    }

    public void create(){
        LifecycleRule lifecycleRule = LifecycleRule.builder()
                .tagStatus(TagStatus.UNTAGGED)
                .description("Repository to medium post")
                .maxImageCount(3)
                .rulePriority(1)
                .build();

        Repository.Builder.create(this, "ecr-repository-stack")
                .repositoryName(repositoryName)
                .imageTagMutability(TagMutability.IMMUTABLE)
                .lifecycleRules(Collections.singletonList(lifecycleRule))
                .build();
    }
}
