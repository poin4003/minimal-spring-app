package com.app.core.utils;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JsonNodeConverter {

    private final ObjectMapper objectMapper;

    public <T> T fromNode(JsonNode node, Class<T> clazz) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.convertValue(node, clazz);
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert JsonNode to {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    public JsonNode toNode(Object object) {
        if (object == null) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.valueToTree(object);
    }
}
