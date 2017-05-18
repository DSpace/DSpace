/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.DSpaceObjectRest;
import org.springframework.core.convert.converter.Converter;

public abstract class DSpaceConverter<M, R> implements Converter<M, R> {
	@Override
	public R convert(M source) {
		return fromModel(source);
	}

	public R fromModel(M obj) {
		return fromModel(obj, DSpaceObjectRest.PRJ_DEFAULT);
	}
	
	public abstract R fromModel(M obj, String projection);

	public abstract M toModel(R obj);
}
