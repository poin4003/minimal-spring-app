package com.app.config.pipeline;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import an.awesome.pipelinr.Command;
import an.awesome.pipelinr.Notification;
import an.awesome.pipelinr.Pipeline;
import an.awesome.pipelinr.Pipelinr;

@Configuration
public class PipelinrConfig {

    @Bean
    @SuppressWarnings("rawtypes")
    public Pipeline pipeline(
            ObjectProvider<Command.Handler> commandHandlers,
            ObjectProvider<Notification.Handler> notificationHandlers,
            ObjectProvider<Command.Middleware> middlewares) {

        return new Pipelinr()
                .with(() -> commandHandlers.orderedStream())
                .with(() -> notificationHandlers.orderedStream())
                .with(() -> middlewares.orderedStream());
    }
}
