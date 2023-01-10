package com.andrelucas.ecr;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;

import java.util.Map;

class ECRStackTest {

    @Test
    void shouldCreateContainerRepository() {
        var app = new App();
        var stackProps = StackProps.builder().env(getEnv("000000000000", "us-east-1"))
                .build();
        var ecrStack = new ECRStack(app, "stackName", stackProps, "start-project-cloudwatch-logging");
        ecrStack.create();

        var template = Template.fromStack(ecrStack);
        template.hasResourceProperties("AWS::ECR::Repository", Map.of(
                "RepositoryName", Match.exact("start-project-cloudwatch-logging"),
                "ImageTagMutability", "MUTABLE")

        );
    }

    public static Environment getEnv(final String accountId,
                                     final String region){

        return Environment.builder()
                .account(accountId)
                .region(region)
                .build();
    }
}