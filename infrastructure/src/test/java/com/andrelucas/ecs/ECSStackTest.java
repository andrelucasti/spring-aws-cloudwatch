package com.andrelucas.ecs;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;

import java.util.Map;

class ECSStackTest {

    @Test
    void shouldCreateECS() {
        var app = new App();
        var stackProps = StackProps.builder().env(getEnv("000000000000", "us-east-1"))
                .build();

        var ecsStack = new ECSStack(app, "ecs-stack", stackProps, "start-project-cloudwatch-logging");
        ecsStack.execute();

        var template = Template.fromStack(ecsStack);
        System.out.println(template.toJSON());
    }

    public static Environment getEnv(final String accountId,
                                     final String region){

        return Environment.builder()
                .account(accountId)
                .region(region)
                .build();
    }
}