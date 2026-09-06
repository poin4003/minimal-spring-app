package com.app.features.post.catalogpost.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.features.post.catalogpost.enums.CatalogAttributeValueType;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@MappedSuperclass
@Data
public abstract class AbstractCatalogAttributeValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CatalogAttributeEntity attribute;

    @Size(max = 100)
    @Column(name = "custom_key", length = 100)
    private String customKey;

    @Size(max = 150)
    @Column(name = "custom_name", length = 150)
    private String customName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "value_type", nullable = false)
    private CatalogAttributeValueType valueType;

    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    @Column(name = "number_value", precision = 24, scale = 6)
    private BigDecimal numberValue;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "date_value")
    private LocalDate dateValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CatalogAttributeOptionEntity option;

    @Size(max = 32)
    @Column(name = "unit", length = 32)
    private String unit;

    @PositiveOrZero
    @Column(name = "position", nullable = false)
    private int position;

    @AssertTrue(message = "Catalog attribute reference is invalid")
    @Transient
    public boolean isAttributeReferenceValid() {
        boolean canonical = attribute != null;
        boolean custom = hasText(customKey) && hasText(customName);
        return canonical != custom;
    }

    @AssertTrue(message = "Catalog attribute value does not match its type")
    @Transient
    public boolean isTypedValueValid() {
        if (valueType == null) {
            return false;
        }

        return switch (valueType) {
            case TEXT, URL, PHONE -> hasText(textValue)
                    && numberValue == null
                    && booleanValue == null
                    && dateValue == null
                    && option == null;
            case NUMBER, MONEY -> numberValue != null
                    && !hasText(textValue)
                    && booleanValue == null
                    && dateValue == null
                    && option == null;
            case BOOLEAN -> booleanValue != null
                    && !hasText(textValue)
                    && numberValue == null
                    && dateValue == null
                    && option == null;
            case DATE -> dateValue != null
                    && !hasText(textValue)
                    && numberValue == null
                    && booleanValue == null
                    && option == null;
            case OPTION -> (option != null) != hasText(textValue)
                    && numberValue == null
                    && booleanValue == null
                    && dateValue == null;
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
