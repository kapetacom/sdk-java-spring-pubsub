/*
 * Copyright 2024 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.pubsub;

import com.google.cloud.spring.pubsub.core.publisher.PubSubPublisherTemplate;
import com.kapeta.spring.config.providers.KapetaConfigurationProvider;
import com.kapeta.spring.config.providers.types.BlockInstanceDetails;
import com.kapeta.spring.pubsub.types.PubSubBlockDefinition;
import lombok.Getter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class KapetaPubSubPublisherTemplate<T> {

    @Getter
    private final Set<String> topics;

    private final PubSubPublisherTemplate googlePublisherTemplate;

    public KapetaPubSubPublisherTemplate(PubSubPublisherTemplate googlePublisherTemplate, KapetaConfigurationProvider kapetaConfigurationProvider,
                                         String resourceName) throws IOException {
        this.googlePublisherTemplate = googlePublisherTemplate;

        Stream<BlockInstanceDetails<PubSubBlockDefinition>> blockInstanceDetails = kapetaConfigurationProvider.getInstancesForProvider(resourceName, PubSubBlockDefinition.class)
                .stream();

        this.topics = new HashSet<>();

        // Find all topics for connected pub sub instances
        blockInstanceDetails.forEach(s -> {
            s.getConnections().stream()
                    .filter(c -> c.getProvider().getResourceName().equals(resourceName))
                    .map(c -> c.getConsumer().getResourceName())
                    .forEach(pubSubResourceName -> s.getBlock().getSpec().getConsumers()
                            .stream()
                            .filter(c -> c.getMetadata().getName().equals(pubSubResourceName))
                            .findFirst()
                            .map(c -> c.getSpec().getTopic())
                            .ifPresent(topics::add));
        });

        if (topics.isEmpty()) {
            throw new RuntimeException("No topics found for " + resourceName);
        }
    }

    public CompletableFuture<Void> publish(T message) {
        return publish(message, null);
    }

    public CompletableFuture<Void> publish(T message, Map<String, String> headers) {
        List<CompletableFuture<String>> messages = topics.stream()
                .map(topic -> googlePublisherTemplate.publish(topic, message, headers))
                .toList();

        var out = new CompletableFuture<List<String>>();

        return CompletableFuture.allOf(messages.toArray(CompletableFuture[]::new));
    }
}
