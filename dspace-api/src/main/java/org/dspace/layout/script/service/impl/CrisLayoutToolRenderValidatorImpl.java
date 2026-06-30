/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service.impl;

import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.dspace.layout.enumeration.RenderingSubTypeValidationRule.ALLOWED;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.dspace.layout.enumeration.RenderingSubTypeValidationRule;
import org.dspace.layout.script.service.CrisLayoutToolRenderValidator;

/**
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class CrisLayoutToolRenderValidatorImpl implements CrisLayoutToolRenderValidator {

    private String name;

    private List<String> fieldTypes;

    private RenderingSubTypeValidationRule subTypeValidationRule = ALLOWED;

    private List<String> subTypes = new ArrayList<>();

    @Override
    public Optional<String> validate(String renderType, String fieldType) {
        if (isFieldTypeNotSupported(fieldType)) {
            return Optional.of("Rendering named " + getName() + " is not supported by field type '" + fieldType + "'");
        }
        return validateRenderingSubType(renderType);
    }

    private boolean isFieldTypeNotSupported(String fieldType) {
        return !fieldTypes.contains(fieldType);
    }

    private Optional<String> validateRenderingSubType(String renderType) {

        if (isSubTypeNotAllowed() && hasSubType(renderType)) {
            return Optional.of("Rendering named " + getName() + " don't supports sub types");
        }

        if (isSubTypeMandatory() && !hasSubType(renderType)) {
            return Optional.of("Rendering named " + getName() + " requires a sub type");
        }

        if (hasSubType(renderType) && isSubTypeNotSupported(renderType)) {
            return Optional.of("Rendering named " + getName() + " don't supports the configured sub type");
        }

        return Optional.empty();

    }

    private boolean isSubTypeNotSupported(String renderType) {
        String subType = renderType.split("\\.")[1];
        return isNotEmpty(subTypes) && !subTypes.contains(subType);
    }

    private boolean isSubTypeNotAllowed() {
        return getSubTypeValidationRule().equals(RenderingSubTypeValidationRule.NOT_ALLOWED);
    }

    private boolean isSubTypeMandatory() {
        return getSubTypeValidationRule().equals(RenderingSubTypeValidationRule.MANDATORY);
    }

    private boolean hasSubType(String renderType) {
        return renderType.contains(".");
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFieldTypes() {
        return fieldTypes;
    }

    public void setFieldType(String fieldType) {
        this.fieldTypes = fieldType != null ? asList(fieldType.split(FIELD_TYPE_SEPARATOR)) : List.of();
    }

    public RenderingSubTypeValidationRule getSubTypeValidationRule() {
        return subTypeValidationRule;
    }

    public void setSubTypeValidationRule(RenderingSubTypeValidationRule subTypeValidationRule) {
        this.subTypeValidationRule = subTypeValidationRule;
    }

    public List<String> getSubTypes() {
        return subTypes;
    }

    public void setSubTypes(Supplier<List<String>> supplier) {
        if (isSubTypeNotAllowed()) {
            throw new IllegalStateException("Subtypes is not allowed for RENDERING " + getName());
        }
        this.subTypes = supplier.get();
    }
}
