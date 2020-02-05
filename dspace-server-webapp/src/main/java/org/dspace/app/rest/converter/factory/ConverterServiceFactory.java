/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter.factory;

import org.dspace.app.rest.converter.ConverterService;

/**
 * Interface of ConverterServiceFactory.
 */
public interface ConverterServiceFactory {

    public ConverterService getConverterService();
}
