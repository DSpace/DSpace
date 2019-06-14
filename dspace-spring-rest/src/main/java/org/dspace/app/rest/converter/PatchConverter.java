/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import com.fasterxml.jackson.databind.JsonNode;
import org.dspace.app.rest.model.patch.Patch;

/**
 * <p>
 * A strategy interface for producing {@link Patch} instances from a patch document representation (such as JSON Patch)
 * and rendering a Patch to a patch document representation. This decouples the {@link Patch} class from any specific
 * patch format or library that holds the representation.
 * </p>
 * <p>
 * For example, if the {@link Patch} is to be represented as JSON Patch, the representation type could be
 * {@link JsonNode} or some other JSON library's type that holds a JSON document.
 * </p>
 *
 * Based on {@link org.springframework.data.rest.webmvc.json.patch.PatchConverter}
 *
 * @param <T>
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public interface PatchConverter<T> {

    /**
     * Convert a patch document representation to a {@link Patch}.
     *
     * @param patchRepresentation the representation of a patch.
     * @return the {@link Patch} object that the document represents.
     */
    Patch convert(T patchRepresentation);

    /**
     * Convert a {@link Patch} to a representation object.
     *
     * @param patch the {@link Patch} to convert.
     * @return the patch representation object.
     */
    T convert(Patch patch);
}