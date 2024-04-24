/*
 * Copyright 2024 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.converter.JacksonPubSubMessageConverter;
import com.google.cloud.spring.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Collection;

@Configuration
@ConditionalOnProperty(name = "spring.cloud.gcp.pubsub.enabled", havingValue = "true", matchIfMissing = true)
public class KapetaGooglePubSubConfiguration {

    @Bean
    @ConditionalOnMissingBean(PubSubMessageConverter.class)
    public PubSubMessageConverter pubSubMessageConverter(ObjectMapper objectMapper) {
        return new JacksonPubSubMessageConverter(objectMapper);
    }

    @Bean
    public Init init(PubSubAdmin pubSubAdmin, PubSubTemplate pubSubTemplate, ConfigurableListableBeanFactory configurableListableBeanFactory) {
        return new Init(pubSubAdmin, pubSubTemplate, configurableListableBeanFactory);
    }

    public static class Init implements ApplicationListener<ContextRefreshedEvent> {

        private final PubSubAdmin pubSubAdmin;
        private final PubSubTemplate pubSubTemplate;
        private final ConfigurableListableBeanFactory configurableListableBeanFactory;

        public Init(PubSubAdmin pubSubAdmin, PubSubTemplate pubSubTemplate, ConfigurableListableBeanFactory configurableListableBeanFactory) {
            this.pubSubAdmin = pubSubAdmin;
            this.pubSubTemplate = pubSubTemplate;
            this.configurableListableBeanFactory = configurableListableBeanFactory;
        }

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            //noinspection rawtypes
            Collection<KapetaPubSubPublisherTemplate> publishers = event.getApplicationContext().getBeansOfType(KapetaPubSubPublisherTemplate.class).values();
            publishers.forEach(kapetaPubSubPublisherTemplate ->
                    ensureTopic(pubSubAdmin, kapetaPubSubPublisherTemplate.getTopic()));

            //noinspection rawtypes
            Collection<KapetaPubSubSubscriptionManager> subscriptions = event.getApplicationContext().getBeansOfType(KapetaPubSubSubscriptionManager.class).values();
            subscriptions.forEach(kapetaPubSubSubscriptionManager -> {
                ensureTopicAndSubscription(pubSubAdmin, kapetaPubSubSubscriptionManager.getTopic(), kapetaPubSubSubscriptionManager.getSubscription());
                kapetaPubSubSubscriptionManager.initialize(pubSubTemplate, configurableListableBeanFactory);
            });
        }

        private static void ensureTopic(PubSubAdmin pubSubAdmin, String topicName) {
            var topic = pubSubAdmin.getTopic(topicName);
            if (topic == null) {
                pubSubAdmin.createTopic(topicName);
            }
        }

        private static void ensureTopicAndSubscription(PubSubAdmin pubSubAdmin, String topicName, String subscriptionName) {
            ensureTopic(pubSubAdmin, topicName);
            var subscription = pubSubAdmin.getSubscription(subscriptionName);
            if (subscription == null) {
                pubSubAdmin.createSubscription(subscriptionName, topicName);
            }
        }
    }
}
