/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.springframework.core.convert.converter.Converter;

public abstract class DSpaceConverter<M, R> implements Converter<M, R> {
	@Override
	public R convert(M source) {
		return fromModel(source);
	}

	public abstract R fromModel(M obj);

	public abstract M toModel(R obj);
}
