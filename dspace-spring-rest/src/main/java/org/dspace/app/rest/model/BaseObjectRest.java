/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;

import org.springframework.hateoas.Identifiable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base class for any REST resource that need to be addressable
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 * @param <T>
 *            the class of the resource identifier
 */
public abstract class BaseObjectRest<T extends Serializable> implements Identifiable<T>, RestModel {
	@JsonIgnore
	protected T id;

	@Override
	public T getId() {
		return id;
	}

	public void setId(T id) {
		this.id = id;
	}
}
