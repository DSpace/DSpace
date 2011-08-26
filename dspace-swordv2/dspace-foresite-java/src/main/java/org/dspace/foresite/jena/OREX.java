/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite.jena;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * @Author Richard Jones
 */
public class OREX
{
	// set up the model
    private static Model model = ModelFactory.createDefaultModel();

    // set up the namespace basics
    public static final String NS = "http://foresite.cheshire3.org/orex/terms/";

    public static String getURI() { return NS; }

    public static final Resource NAMESPACE = model.createResource(NS);

    // set up the Properties that OREX can assert

    public static final Property isAuthoritativeFor = model.createProperty(NS + "isAuthoritativeFor");
}
