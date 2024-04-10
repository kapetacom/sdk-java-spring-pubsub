/*
 * Copyright 2024 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.pubsub.types;

import lombok.Data;

@Data
public class PubSubTopicSubscriptionSpec {
    private String topic;
    private String subscription;
}
