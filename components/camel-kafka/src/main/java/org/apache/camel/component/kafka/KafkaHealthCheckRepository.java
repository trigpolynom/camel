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
package org.apache.camel.component.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.DeferredContextBinding;
import org.apache.camel.NonManagedService;
import org.apache.camel.StaticService;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckRepository;
import org.apache.camel.support.service.ServiceSupport;

/**
 * Repository for camel-kafka {@link HealthCheck}s.
 */
@org.apache.camel.spi.annotations.HealthCheck("camel-kafka-repository")
@DeferredContextBinding
public class KafkaHealthCheckRepository extends ServiceSupport
        implements CamelContextAware, HealthCheckRepository, StaticService, NonManagedService {

    private final List<HealthCheck> checks = new ArrayList<>();
    private volatile CamelContext context;
    private boolean enabled = true;

    public KafkaHealthCheckRepository() {
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.context = camelContext;
    }

    @Override
    public String getId() {
        return "camel-kafka";
    }

    @Override
    public CamelContext getCamelContext() {
        return context;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Stream<HealthCheck> stream() {
        return this.context != null && enabled
                ? checks.stream()
                : Stream.empty();
    }

    public void addHealthCheck(HealthCheck healthCheck) {
        CamelContextAware.trySetCamelContext(healthCheck, getCamelContext());
        this.checks.add(healthCheck);
    }

    public void removeHealthCheck(HealthCheck healthCheck) {
        this.checks.remove(healthCheck);
    }

}
