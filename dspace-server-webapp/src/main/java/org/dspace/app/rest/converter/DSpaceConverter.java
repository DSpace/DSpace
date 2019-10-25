/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.function.Function;

import org.springframework.core.convert.converter.Converter;

public interface DSpaceConverter<M, R> extends Converter<M, R>, Function<M, R> {

    @Override
    default R apply(M source) {
        return convert(source);
    }

    @Override
    public default R convert(M source) {
        return fromModel(source);
    }

    public abstract R fromModel(M obj);

    public abstract M toModel(R obj);
}
