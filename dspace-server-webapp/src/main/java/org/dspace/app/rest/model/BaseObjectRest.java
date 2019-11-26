/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.hateoas.Identifiable;

/**
 * Base class for any REST resource that need to be addressable
 *
 * @param <T> the class of the resource identifier
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public abstract class BaseObjectRest<T extends Serializable> extends RestAddressableModel implements Identifiable<T> {

    protected T id;

    @JsonInclude(Include.NON_EMPTY)
    private List<ErrorRest> errors;

    @Override
    public T getId() {
        return id;
    }

    public void setId(T id) {
        this.id = id;
    }

    public List<ErrorRest> getErrors() {
        if (this.errors == null) {
            this.errors = new ArrayList<ErrorRest>();
        }
        return errors;
    }

    public void setErrors(List<ErrorRest> errors) {
        this.errors = errors;
    }

}
