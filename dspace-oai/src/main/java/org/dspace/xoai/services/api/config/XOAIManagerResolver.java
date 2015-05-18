/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.config;

import com.lyncode.xoai.dataprovider.core.XOAIManager;

public interface XOAIManagerResolver {
    XOAIManager getManager () throws XOAIManagerResolverException;
}
