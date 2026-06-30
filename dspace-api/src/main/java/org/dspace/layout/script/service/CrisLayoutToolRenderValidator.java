/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service;

import java.util.Optional;

/**
 * Validate the layout configuration rendering before upload to avoid unexpected
 * behavior when item details pages are loaded.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public interface CrisLayoutToolRenderValidator {

    public static final String FIELD_TYPE_SEPARATOR = "\\|\\|";

    /**
     * @param  renderType value of rendering type
     * @param  fieldType  value of field type
     * @return            the validation error message, if any
     */
    public Optional<String> validate(String renderType, String fieldType);

    /**
     * @return rendering type value
     */
    public String getName();

}
