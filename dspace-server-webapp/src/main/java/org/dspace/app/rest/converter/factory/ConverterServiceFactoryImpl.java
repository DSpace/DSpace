/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter.factory;

import org.dspace.app.rest.converter.ConverterService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("converterServiceFactory")
public class ConverterServiceFactoryImpl implements ConverterServiceFactory, InitializingBean {
    @Autowired(required = true)
    private ConverterService converter;

    private static ConverterServiceFactory instance;

    @Override
    public ConverterService getConverterService() {
        return converter;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        instance = this;
    }

    public static ConverterServiceFactory getInstance() {
        return instance;
    }
}
