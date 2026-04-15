package com.enterprise.engine.core.config;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfig extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        // 1. Define the Global Dead Letter Channel
        // If an AI-generated route fails, it ends up here.
        errorHandler(deadLetterChannel("kafka:enterprise.dlq.topic?brokers={{spring.kafka.bootstrap-servers}}")
            .useOriginalMessage() // Ensure the DLQ gets the exact incoming Kafka event, not a half-transformed one
            .maximumRedeliveries(3)
            .redeliveryDelay(2000)
            .backOffMultiplier(2) // Exponential backoff (2s, 4s, 8s)
            .retryAttemptedLogLevel(org.apache.camel.LoggingLevel.WARN)
            .retriesExhaustedLogLevel(org.apache.camel.LoggingLevel.ERROR)
            // Add a header so ops knows exactly which AI route failed
            .onPrepareFailure(exchange -> {
                Exception cause = exchange.getProperty(org.apache.camel.Exchange.EXCEPTION_CAUGHT, Exception.class);
                exchange.getIn().setHeader("FailedRouteId", exchange.getFromRouteId());
                exchange.getIn().setHeader("FailureReason", cause != null ? cause.getMessage() : "Unknown");
            })
        );

        // Note: You do NOT put business routes here. 
        // This class is strictly for global framework policies.
    }
}