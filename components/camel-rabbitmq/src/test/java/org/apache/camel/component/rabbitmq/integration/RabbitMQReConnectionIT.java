/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.rabbitmq.integration;

import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

import com.rabbitmq.client.AlreadyClosedException;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
import org.apache.camel.test.infra.rabbitmq.services.ConnectionProperties;
import org.junit.jupiter.api.Test;

/**
 * Integration test to check that RabbitMQ Endpoint is able to reconnect to broker when broker is not available.
 * <ul>
 * <li>Stop the broker</li>
 * <li>Run the test: the producer complains it can not send messages, the consumer is silent</li>
 * <li>Start the broker: the producer sends messages, and the consumer receives messages</li>
 * <li>Stop the broker: the producer complains it can not send messages, the consumer is silent</li>
 * <li>Start the broker: the producer sends messages, and the consumer receives messages</li>
 * <li>Kill all connections from the broker: the producer sends messages, and the consumer receives messages</li>
 * </ul>
 */
public class RabbitMQReConnectionIT extends AbstractRabbitMQIT {
    private static final String EXCHANGE = "ex3";

    @Produce("direct:rabbitMQ")
    protected ProducerTemplate directProducer;

    @EndpointInject("mock:producing")
    private MockEndpoint producingMockEndpoint;

    @EndpointInject("mock:consuming")
    private MockEndpoint consumingMockEndpoint;

    @Override
    protected RouteBuilder createRouteBuilder() {
        ConnectionProperties connectionProperties = service.connectionProperties();
        String rabbitMQEndpoint = String.format("rabbitmq:localhost:%d/%s?username=%s&password=%s"
                                                + "&queue=q3&routingKey=rk3&automaticRecoveryEnabled=true&requestedHeartbeat=1000&connectionTimeout=5000",
                connectionProperties.port(), EXCHANGE, connectionProperties.username(), connectionProperties.password());

        return new RouteBuilder() {

            @Override
            @SuppressWarnings("unchecked")
            public void configure() {
                from("direct:rabbitMQ").id("producingRoute").onException(AlreadyClosedException.class, ConnectException.class)
                        .maximumRedeliveries(10).redeliveryDelay(500L).end()
                        .log("Sending message").to(ExchangePattern.InOnly, rabbitMQEndpoint).to(producingMockEndpoint);
                from(rabbitMQEndpoint).id("consumingRoute").log("Receiving message").to(consumingMockEndpoint);
            }
        };
    }

    @Test
    public void testSendEndReceive() throws Exception {
        int nbMessages = 50;
        int failedMessages = 0;
        for (int i = 0; i < nbMessages; i++) {
            try {
                directProducer.sendBodyAndHeader("Message #" + i, RabbitMQConstants.ROUTING_KEY, "rk3");
            } catch (CamelExecutionException e) {
                log.debug("Can not send message", e);
                failedMessages++;
            }
            Thread.sleep(500L);
        }
        producingMockEndpoint.expectedMessageCount(nbMessages - failedMessages);
        consumingMockEndpoint.expectedMessageCount(nbMessages - failedMessages);
        assertMockEndpointsSatisfied(5, TimeUnit.SECONDS);
    }
}
