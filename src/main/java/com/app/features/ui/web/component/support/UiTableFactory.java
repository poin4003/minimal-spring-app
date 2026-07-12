package com.app.features.ui.web.component.support;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.app.features.ui.web.annotation.UiColumn;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.component.view.UiTableActionView;
import com.app.features.ui.web.component.view.UiTableCellView;
import com.app.features.ui.web.component.view.UiTableColumnView;
import com.app.features.ui.web.component.view.UiTableDefinition;
import com.app.features.ui.web.component.view.UiTableRowView;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.enums.UiCellType;

@Component
public class UiTableFactory {

    public <T> UiTableView build(
            UiTableDefinition definition,
            List<T> rows,
            Class<T> rowType) {
        return build(definition, rows, rowType, item -> List.of());
    }

    public <T> UiTableView build(
            UiTableDefinition definition,
            List<T> rows,
            Class<T> rowType,
            Function<T, List<UiTableActionView>> actionBuilder) {
        List<Field> fields = UiComponentFieldSupport.findAnnotatedFields(
                rowType,
                UiColumn.class,
                uiColumn -> uiColumn.order());

        List<UiTableColumnView> columns = fields.stream()
                .map(field -> toColumn(field, field.getAnnotation(UiColumn.class)))
                .toList();

        List<UiTableRowView> tableRows = rows.stream()
                .map(row -> toRow(row, fields, actionBuilder))
                .toList();

        boolean showActions = tableRows.stream()
                .map(uiTableRowView -> uiTableRowView.getActions())
                .filter(Objects::nonNull)
                .anyMatch(actions -> !actions.isEmpty());

        UiPaginationView pagination = definition.getPagination();

        return UiTableView.builder()
                .title(definition.getTitle())
                .description(definition.getDescription())
                .emptyMessage(definition.getEmptyMessage())
                .showActions(showActions)
                .columns(columns)
                .rows(tableRows)
                .pagination(pagination)
                .build();
    }

    private UiTableColumnView toColumn(Field field, UiColumn annotation) {
        return UiTableColumnView.builder()
                .key(resolveKey(annotation.key(), field.getName()))
                .label(annotation.label())
                .type(annotation.type())
                .build();
    }

    private <T> UiTableRowView toRow(
            T row,
            List<Field> fields,
            Function<T, List<UiTableActionView>> actionBuilder) {
        List<UiTableCellView> cells = fields.stream()
                .map(field -> toCell(field, row, field.getAnnotation(UiColumn.class)))
                .toList();

        List<UiTableActionView> actions = actionBuilder.apply(row);
        if (actions == null) {
            actions = List.of();
        }

        return UiTableRowView.builder()
                .cells(cells)
                .actions(actions)
                .build();
    }

    private UiTableCellView toCell(Field field, Object row, UiColumn annotation) {
        Object rawValue = UiComponentFieldSupport.readValue(field, row);

        return UiTableCellView.builder()
                .key(resolveKey(annotation.key(), field.getName()))
                .type(annotation.type())
                .text(formatValue(rawValue, annotation.emptyValue()))
                .badgeClass(resolveBadgeClass(annotation, rawValue))
                .build();
    }

    private String resolveBadgeClass(UiColumn annotation, Object rawValue) {
        if (annotation.type() != UiCellType.BADGE) {
            return null;
        }

        if (StringUtils.hasText(annotation.badgeClass())) {
            return annotation.badgeClass();
        }

        if (rawValue instanceof Enum<?> enumValue) {
            return switch (enumValue.name()) {
                case "ACTIVE" -> "text-bg-success";
                case "INACTIVE" -> "text-bg-secondary";
                case "LOCKED" -> "text-bg-danger";
                default -> "text-bg-primary";
            };
        }

        return "text-bg-primary";
    }

    private String formatValue(Object rawValue, String emptyValue) {
        if (rawValue == null) {
            return emptyValue;
        }

        if (rawValue instanceof Collection<?> values) {
            if (values.isEmpty()) {
                return emptyValue;
            }

            return values.stream()
                    .map(this::formatSingleValue)
                    .collect(Collectors.joining(", "));
        }

        return formatSingleValue(rawValue);
    }

    private String formatSingleValue(Object rawValue) {
        if (rawValue instanceof Enum<?> enumValue) {
            return enumValue.name();
        }

        return String.valueOf(rawValue);
    }

    private String resolveKey(String annotatedKey, String fallbackKey) {
        return StringUtils.hasText(annotatedKey)
                ? annotatedKey
                : fallbackKey;
    }
}
