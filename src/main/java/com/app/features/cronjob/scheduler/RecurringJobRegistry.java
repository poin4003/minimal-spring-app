package com.app.features.cronjob.scheduler;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.app.features.cronjob.annotation.AppRecurringJob;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecurringJobRegistry {

    private final ApplicationContext applicationContext;

    private Map<String, RecurringJobDefinition> definitions = Map.of();

    @PostConstruct
    void loadDefinitions() {
        Map<String, JobHandler> handlers = applicationContext.getBeansOfType(JobHandler.class);
        LinkedHashMap<String, RecurringJobDefinition> loaded = new LinkedHashMap<>();

        for (JobHandler handler : handlers.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(handler);
            AppRecurringJob annotation = AnnotationUtils.findAnnotation(targetClass, AppRecurringJob.class);
            if (annotation == null) {
                continue;
            }

            RecurringJobDefinition definition = RecurringJobDefinition.builder()
                    .id(annotation.id())
                    .name(annotation.name())
                    .defaultCron(annotation.defaultCron())
                    .zoneId(annotation.zoneId())
                    .handlerClass(targetClass.asSubclass(JobHandler.class))
                    .build();

            if (loaded.putIfAbsent(definition.getId(), definition) != null) {
                throw new IllegalStateException("Duplicate recurring job id: " + definition.getId());
            }
        }

        this.definitions = Map.copyOf(loaded);
    }

    public Collection<RecurringJobDefinition> getAll() {
        return definitions.values();
    }

    public RecurringJobDefinition getRequired(String id) {
        RecurringJobDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Recurring job definition not found: " + id);
        }
        return definition;
    }
}
