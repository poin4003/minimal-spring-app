package com.app.config.processor;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import com.app.core.annotation.RegisterMqType;
import com.app.core.constant.AppConstants;
import com.app.core.mq.type.PayloadTypeRegistry;

@Component
public class MqTypeRegistrar implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(
        @NonNull ConfigurableListableBeanFactory beanFactory
    ) {

        PayloadTypeRegistry registry =
            beanFactory.getBean(PayloadTypeRegistry.class);

        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(
            new AnnotationTypeFilter(RegisterMqType.class)
        );

        for (BeanDefinition bd : scanner.findCandidateComponents(AppConstants.BASE_PACKAGE)) {

            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());

                RegisterMqType ann =
                    clazz.getAnnotation(RegisterMqType.class);

                registry.register(ann.value(), clazz);

            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                    "Failed to load MQ type class", e
                );
            }
        }
    }
}
