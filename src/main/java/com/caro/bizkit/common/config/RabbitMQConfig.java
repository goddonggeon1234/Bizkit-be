package com.caro.bizkit.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitMQConfig {

    // ── Exchange ──────────────────────────────────────────────────────────────

    @Bean
    DirectExchange aiExchange() {
        return new DirectExchange("ai.exchange");
    }

    @Bean
    DirectExchange aiResult() {
        return new DirectExchange("ai.result");
    }

    @Bean
    DirectExchange aiDlx() {
        return new DirectExchange("ai.dlx");
    }

    // ── Jobs 큐 (TTL 200s + DLX) ──────────────────────────────────────────────

    @Bean
    Queue cardJobsQueue() {
        return QueueBuilder.durable("ai.card.jobs")
                .deadLetterExchange("ai.dlx")
                .deadLetterRoutingKey("card.jobs")
                .withArgument("x-message-ttl", 200_000L)
                .build();
    }

    @Bean
    Queue jobJobsQueue() {
        return QueueBuilder.durable("ai.job.jobs")
                .deadLetterExchange("ai.dlx")
                .deadLetterRoutingKey("job.jobs")
                .withArgument("x-message-ttl", 200_000L)
                .build();
    }

    @Bean
    Queue hexJobsQueue() {
        return QueueBuilder.durable("ai.hex.jobs")
                .deadLetterExchange("ai.dlx")
                .deadLetterRoutingKey("hex.jobs")
                .withArgument("x-message-ttl", 200_000L)
                .build();
    }

    // ── Result 큐 (DLX + delivery limit) ─────────────────────────────────────

    @Bean
    Queue cardResultQueue() {
        return QueueBuilder.durable("ai.card.result")
                .deadLetterExchange("ai.dlx")
                .deadLetterRoutingKey("card.result")
                .build();
    }

    @Bean
    Queue jobResultQueue() {
        return QueueBuilder.durable("ai.job.result")
                .deadLetterExchange("ai.dlx")
                .deadLetterRoutingKey("job.result")
                .build();
    }

    @Bean
    Queue hexResultQueue() {
        return QueueBuilder.durable("ai.hex.result")
                .deadLetterExchange("ai.dlx")
                .deadLetterRoutingKey("hex.result")
                .build();
    }

    // ── DLQ ───────────────────────────────────────────────────────────────────

    @Bean
    Queue jobsDlq() {
        return QueueBuilder.durable("ai.jobs.dlq")
                .withArgument("x-message-ttl", 604_800_000L) // 7일
                .build();
    }

    @Bean
    Queue resultDlq() {
        return QueueBuilder.durable("ai.result.dlq")
                .withArgument("x-message-ttl", 604_800_000L) // 7일
                .build();
    }

    // ── Jobs Binding ──────────────────────────────────────────────────────────

    @Bean
    Binding cardJobsBinding(Queue cardJobsQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(cardJobsQueue).to(aiExchange).with("card");
    }

    @Bean
    Binding jobJobsBinding(Queue jobJobsQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(jobJobsQueue).to(aiExchange).with("job");
    }

    @Bean
    Binding hexJobsBinding(Queue hexJobsQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(hexJobsQueue).to(aiExchange).with("hex");
    }

    // ── Result Binding ────────────────────────────────────────────────────────

    @Bean
    Binding cardResultBinding(Queue cardResultQueue, DirectExchange aiResult) {
        return BindingBuilder.bind(cardResultQueue).to(aiResult).with("card");
    }

    @Bean
    Binding jobResultBinding(Queue jobResultQueue, DirectExchange aiResult) {
        return BindingBuilder.bind(jobResultQueue).to(aiResult).with("job");
    }

    @Bean
    Binding hexResultBinding(Queue hexResultQueue, DirectExchange aiResult) {
        return BindingBuilder.bind(hexResultQueue).to(aiResult).with("hex");
    }

    // ── DLQ Binding ───────────────────────────────────────────────────────────

    @Bean
    Binding cardJobsDlqBinding(Queue jobsDlq, DirectExchange aiDlx) {
        return BindingBuilder.bind(jobsDlq).to(aiDlx).with("card.jobs");
    }

    @Bean
    Binding jobJobsDlqBinding(Queue jobsDlq, DirectExchange aiDlx) {
        return BindingBuilder.bind(jobsDlq).to(aiDlx).with("job.jobs");
    }

    @Bean
    Binding hexJobsDlqBinding(Queue jobsDlq, DirectExchange aiDlx) {
        return BindingBuilder.bind(jobsDlq).to(aiDlx).with("hex.jobs");
    }

    @Bean
    Binding cardResultDlqBinding(Queue resultDlq, DirectExchange aiDlx) {
        return BindingBuilder.bind(resultDlq).to(aiDlx).with("card.result");
    }

    @Bean
    Binding jobResultDlqBinding(Queue resultDlq, DirectExchange aiDlx) {
        return BindingBuilder.bind(resultDlq).to(aiDlx).with("job.result");
    }

    @Bean
    Binding hexResultDlqBinding(Queue resultDlq, DirectExchange aiDlx) {
        return BindingBuilder.bind(resultDlq).to(aiDlx).with("hex.result");
    }

    // ── 메시지 컨버터 / 재시도 ────────────────────────────────────────────────

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    RetryOperationsInterceptor retryInterceptor() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(new org.springframework.retry.backoff.ExponentialRandomBackOffPolicy() {{
            setInitialInterval(4_000L);
            setMultiplier(2.0);
            setMaxInterval(8_000L);
        }});
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));

        return RetryInterceptorBuilder.stateless()
                .retryOperations(retryTemplate)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            RetryOperationsInterceptor retryInterceptor
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setAdviceChain(retryInterceptor);
        return factory;
    }
}
