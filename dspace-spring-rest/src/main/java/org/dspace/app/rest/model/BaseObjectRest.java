package org.dspace.app.rest.model;

import java.io.Serializable;

import org.springframework.hateoas.Identifiable;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
