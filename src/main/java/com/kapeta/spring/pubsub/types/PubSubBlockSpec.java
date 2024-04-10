/*
 * Copyright 2024 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.pubsub.types;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PubSubBlockSpec {
    private List<PubSubProviderConsumer> providers = new ArrayList<>();
    private List<PubSubProviderConsumer> consumers = new ArrayList<>();
}
