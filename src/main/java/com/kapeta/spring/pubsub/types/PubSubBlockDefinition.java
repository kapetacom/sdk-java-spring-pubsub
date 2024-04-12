/*
 * Copyright 2024 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.pubsub.types;

import com.kapeta.schemas.entity.Metadata;
import lombok.Data;

@Data
public class PubSubBlockDefinition {
    private String kind;
    private Metadata metadata;
    private PubSubBlockSpec spec;
}
