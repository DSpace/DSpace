/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.config;

public class XOAIManagerResolverException extends Exception {
    public XOAIManagerResolverException() {}

    public XOAIManagerResolverException(String message) {
        super(message);
    }

    public XOAIManagerResolverException(String message, Throwable cause) {
        super(message, cause);
    }

    public XOAIManagerResolverException(Throwable cause) {
        super(cause);
    }
}
