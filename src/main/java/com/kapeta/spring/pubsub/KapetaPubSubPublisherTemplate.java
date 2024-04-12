/*
 * Copyright 2024 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.pubsub;

import com.google.cloud.spring.pubsub.core.publisher.PubSubPublisherTemplate;
import com.kapeta.spring.config.providers.KapetaConfigurationProvider;
import com.kapeta.spring.config.providers.types.BlockInstanceDetails;
import com.kapeta.spring.pubsub.types.PubSubBlockDefinition;
import com.kapeta.spring.pubsub.types.PubSubProviderConsumer;
import lombok.Getter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class KapetaPubSubPublisherTemplate<T> {

    @Getter
    private final String topic;
    private final PubSubPublisherTemplate googlePublisherTemplate;

    public KapetaPubSubPublisherTemplate(PubSubPublisherTemplate googlePublisherTemplate, KapetaConfigurationProvider kapetaConfigurationProvider,
                                         String resourceName) throws IOException {
        this.googlePublisherTemplate = googlePublisherTemplate;

        Optional<BlockInstanceDetails<PubSubBlockDefinition>> blockInstanceDetails = kapetaConfigurationProvider.getInstancesForProvider(resourceName, PubSubBlockDefinition.class).stream().findFirst();
        if (blockInstanceDetails.isEmpty()) {
            throw new RuntimeException("Provider for " + resourceName + " not connected");
        }

        Optional<PubSubProviderConsumer> consumer = blockInstanceDetails.get().getBlock().getSpec().getConsumers().stream()
                .filter(s -> s.getMetadata().getName().equals(resourceName))
                .findFirst();
        if (consumer.isEmpty()) {
            throw new RuntimeException("Consumer for " + resourceName + " not found");
        }

        this.topic = consumer.map(s -> s.getSpec().getTopic()).orElseThrow();
    }

    public CompletableFuture<String> publish(T message) {
        return publish(message, null);
    }

    public CompletableFuture<String> publish(T message, Map<String, String> headers) {
        return googlePublisherTemplate.publish(topic, message, headers);
    }
}
