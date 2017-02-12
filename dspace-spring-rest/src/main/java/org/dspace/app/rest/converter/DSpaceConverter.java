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
