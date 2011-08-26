/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import java.net.URI;

/**
 * @Author Richard Jones
 */
public interface Triple
{
    void initialise(URI uri) throws OREException;

    void initialise(OREResource subject) throws OREException;

    OREResource getSubject() throws OREException;

    URI getSubjectURI() throws OREException;

    Predicate getPredicate() throws OREException;

    OREResource getObject() throws OREException;

    URI getObjectURI() throws OREException;

    String getObjectLiteral() throws OREException;

    void relate(Predicate pred, URI uri) throws OREException;

    void relate(Predicate pred, OREResource resource) throws OREException;

    void relate(Predicate pred, Object literal) throws OREException;

    String getLiteralType() throws OREException;

    boolean isLiteral() throws OREException;

    // public abstract void setLiteralType(String literalType);
}
