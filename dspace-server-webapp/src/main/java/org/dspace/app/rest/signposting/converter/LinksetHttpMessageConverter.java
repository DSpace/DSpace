/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.converter;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.lang.reflect.Type;

import org.apache.commons.lang.NotImplementedException;
import org.dspace.app.rest.signposting.model.LinksetRest;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Converter for converting LinksetRest message into application/linkset format.
 */
public class LinksetHttpMessageConverter extends AbstractGenericHttpMessageConverter<LinksetRest> {

    public LinksetHttpMessageConverter() {
        super(MediaType.valueOf("application/linkset"));
    }

    @Override
    protected void writeInternal(LinksetRest linksetRest, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        StringBuilder responseBody = new StringBuilder();
        linksetRest.getLinksetNodes().forEach(linksetNodes -> {
            if (isNotBlank(linksetNodes.getLink())) {
                responseBody.append(format("<%s> ", linksetNodes.getLink()));
            }
            if (nonNull(linksetNodes.getRelation())) {
                responseBody.append(format("; rel=\"%s\" ", linksetNodes.getRelation().getName()));
            }
            if (isNotBlank(linksetNodes.getType())) {
                responseBody.append(format("; type=\"%s\" ", linksetNodes.getType()));
            }
            if (isNotBlank(linksetNodes.getAnchor())) {
                responseBody.append(format("; anchor=\"%s\" ", linksetNodes.getAnchor()));
            }
            responseBody.append(", ");
        });
        outputMessage.getBody().write(responseBody.toString().trim().getBytes());
        outputMessage.getBody().flush();
    }

    @Override
    protected LinksetRest readInternal(Class<? extends LinksetRest> clazz, HttpInputMessage inputMessage)
            throws HttpMessageNotReadableException {
        throw new NotImplementedException();
    }

    @Override
    protected boolean canRead(MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        boolean isAppropriateClass = LinksetRest.class.isAssignableFrom(clazz);
        boolean isAppropriateMediaType = getSupportedMediaTypes().stream()
                .anyMatch(supportedType -> supportedType.isCompatibleWith(mediaType));
        return isAppropriateClass && isAppropriateMediaType;
    }

    @Override
    public LinksetRest read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new NotImplementedException();
    }
}
