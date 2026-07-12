package com.app.features.ui.web.component.support;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.app.features.ui.web.annotation.UiField;
import com.app.features.ui.web.component.view.UiModalDefinition;
import com.app.features.ui.web.component.view.UiModalFieldOptionView;
import com.app.features.ui.web.component.view.UiModalFieldView;
import com.app.features.ui.web.component.view.UiModalView;
import com.app.features.ui.web.enums.UiInputType;

@Component
public class UiModalFactory {

    public <T> UiModalView build(
            UiModalDefinition definition,
            Class<T> formType,
            T formData) {
        return build(definition, formType, formData, Map.of(), Map.of());
    }

    public <T> UiModalView build(
            UiModalDefinition definition,
            Class<T> formType,
            T formData,
            Map<String, List<UiModalFieldOptionView>> optionsByField) {
        return build(definition, formType, formData, optionsByField, Map.of());
    }

    public <T> UiModalView build(
            UiModalDefinition definition,
            Class<T> formType,
            T formData,
            Map<String, List<UiModalFieldOptionView>> optionsByField,
            Map<String, String> errorsByField) {
        List<Field> fields = UiComponentFieldSupport.findAnnotatedFields(
                formType,
                UiField.class,
                uiField -> uiField.order());

        List<UiModalFieldView> modalFields = fields.stream()
                .map(field -> toField(field, formData, optionsByField, errorsByField))
                .toList();

        return UiModalView.builder()
                .id(definition.getId())
                .title(definition.getTitle())
                .description(definition.getDescription())
                .triggerLabel(definition.getTriggerLabel())
                .triggerButtonClass(definition.getTriggerButtonClass())
                .dialogClass(definition.getDialogClass())
                .actionPath(definition.getActionPath())
                .method(normalizeMethod(definition.getMethod()))
                .submitLabel(definition.getSubmitLabel())
                .fields(modalFields)
                .build();
    }

    private UiModalFieldView toField(
            Field field,
            Object formData,
            Map<String, List<UiModalFieldOptionView>> optionsByField,
            Map<String, String> errorsByField) {
        UiField annotation = field.getAnnotation(UiField.class);
        Object rawValue = UiComponentFieldSupport.readValue(field, formData);
        String fieldName = resolveKey(annotation.key(), field.getName());
        String value = rawValue == null ? null : String.valueOf(rawValue);
        List<UiModalFieldOptionView> options = optionsByField.getOrDefault(fieldName, List.of()).stream()
                .map(option -> option.toBuilder()
                        .selected(option.isSelected() || option.getValue().equals(value))
                        .build())
                .toList();

        return UiModalFieldView.builder()
                .name(fieldName)
                .label(annotation.label())
                .type(annotation.type())
                .value(value)
                .placeholder(emptyToNull(annotation.placeholder()))
                .helpText(emptyToNull(annotation.helpText()))
                .required(annotation.required())
                .readOnly(annotation.readOnly())
                .checked(resolveChecked(annotation.type(), rawValue))
                .rows(annotation.rows())
                .options(options)
                .errorMessage(errorsByField.get(fieldName))
                .build();
    }

    private boolean resolveChecked(UiInputType type, Object rawValue) {
        if (type != UiInputType.CHECKBOX) {
            return false;
        }

        if (rawValue instanceof Boolean checked) {
            return checked;
        }

        return false;
    }

    private String normalizeMethod(String method) {
        if (!StringUtils.hasText(method)) {
            return "post";
        }

        return method.toLowerCase();
    }

    private String resolveKey(String annotatedKey, String fallbackKey) {
        return StringUtils.hasText(annotatedKey)
                ? annotatedKey
                : fallbackKey;
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value)
                ? value
                : null;
    }
}
