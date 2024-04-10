/*
 * Copyright 2024 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */

package com.kapeta.spring.annotation;

import com.kapeta.spring.pubsub.KapetaGooglePubSubConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(KapetaGooglePubSubConfiguration.class)
public @interface KapetaEnableGooglePubSub {
}
