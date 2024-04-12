/*
 * Copyright 2024 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.kapeta.spring.config.BeanHelper;
import com.kapeta.spring.config.providers.KapetaConfigurationProvider;
import com.kapeta.spring.pubsub.types.PubSubBlockDefinition;
import com.kapeta.spring.pubsub.types.PubSubProviderConsumer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.io.IOException;
import java.util.Optional;

public class KapetaPubSubSubscriptionManager<T> {
    private final KapetaConfigurationProvider kapetaConfigurationProvider;
    private final String resourceName;
    private final KapetaMessageConsumer<T> messageConsumer;
    private final Class<T> dtoClass;

    public KapetaPubSubSubscriptionManager(KapetaConfigurationProvider kapetaConfigurationProvider, String resourceName,
                                           Class<T> dtoClass, KapetaMessageConsumer<T> messageConsumer) {
        this.kapetaConfigurationProvider = kapetaConfigurationProvider;
        this.resourceName = resourceName;
        this.messageConsumer = messageConsumer;
        this.dtoClass = dtoClass;
    }

    public String getTopic() {
        return getProvider().map(p -> p.getSpec().getTopic()).orElseThrow();
    }

    public String getSubscription() {
        return getProvider().map(p -> p.getSpec().getSubscription()).orElseThrow();
    }

    private Optional<PubSubProviderConsumer> getProvider() {
        try {
            return kapetaConfigurationProvider.getInstanceForConsumer(resourceName, PubSubBlockDefinition.class).getBlock().getSpec().getProviders().stream()
                    .filter(p -> p.getMetadata().getName().equals(resourceName))
                    .findAny();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initialize(PubSubTemplate pubSubTemplate, ConfigurableListableBeanFactory configurableListableBeanFactory) {
        var beanHelper = new BeanHelper(configurableListableBeanFactory);
        beanHelper.registerBean(camelCaseName() + "InputChannel", PublishSubscribeChannel.class, new PublishSubscribeChannel());
        MessageChannel messageChannel = configurableListableBeanFactory.getBean(camelCaseName() + "InputChannel", MessageChannel.class);

        beanHelper.registerBean(camelCaseName() + "InboundChannelAdapter", PubSubInboundChannelAdapter.class,
                createPubSubInboundChannelAdapter(messageChannel, pubSubTemplate));
        PubSubInboundChannelAdapter deployCommandInboundChannelAdapter = configurableListableBeanFactory.getBean(camelCaseName() + "InboundChannelAdapter", PubSubInboundChannelAdapter.class);
        deployCommandInboundChannelAdapter.start();

        IntegrationFlowContext integrationFlowContext = configurableListableBeanFactory.getBean(IntegrationFlowContext.class);
        integrationFlowContext.registration(createIntegrationFlow())
                .id(camelCaseName() + "ServiceActivator")
                .autoStartup(true)
                .register();
    }

    private IntegrationFlow createIntegrationFlow() {
        return f -> f.channel(camelCaseName() + "InputChannel").handle(this::handleMessage);
    }

    private void handleMessage(Message<?> message) {
        BasicAcknowledgeablePubsubMessage originalMessage =
                message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);

        if (originalMessage == null) {
            throw new NullPointerException("Original message is null");
        }

        try {
            //noinspection unchecked
            messageConsumer.acceptMessage((T) message.getPayload());
            originalMessage.ack();
        } catch (Exception e) {
            originalMessage.nack();
        }
    }

    private PubSubInboundChannelAdapter createPubSubInboundChannelAdapter(MessageChannel messageChannel,
                                                                          PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, getSubscription());
        adapter.setOutputChannel(messageChannel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(this.dtoClass);
        return adapter;
    }

    private String camelCaseName() {
        return this.dtoClass.getSimpleName().substring(0, 1).toLowerCase() + this.dtoClass.getSimpleName().substring(1);
    }
}
