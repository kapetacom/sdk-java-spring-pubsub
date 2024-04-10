/*
 * Copyright 2024 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.pubsub;

public interface KapetaMessageConsumer<T> {
    void acceptMessage(T message) throws Exception;
}
