/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.factory;

import org.dspace.rdf.conversion.RDFConverter;
import org.dspace.rdf.storage.RDFStorage;
import org.dspace.rdf.storage.URIGenerator;
import org.dspace.utils.DSpace;

/**
 * Abstract factory to get services for the rdf package, use RDFFactory.getInstance() to retrieve an implementation.
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public abstract class RDFFactory
{
    public abstract RDFStorage getRDFStorage();

    public abstract URIGenerator getURIGenerator();

    public abstract RDFConverter getRDFConverter();

    public static RDFFactory getInstance()
    {
        return new DSpace().getServiceManager().getServiceByName("rdfFactory", RDFFactory.class);
    }
}