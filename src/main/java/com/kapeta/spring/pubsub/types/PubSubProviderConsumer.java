/*
 * Copyright 2024 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.pubsub.types;

import com.kapeta.schemas.entity.ResourceMetadata;
import lombok.Data;

@Data
public class PubSubProviderConsumer {
    private ResourceMetadata metadata;
    private PubSubTopicSubscriptionSpec spec;
}
