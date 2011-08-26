/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite.jena;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @Author Richard Jones
 */
public class ORE
{
    // set up the model
    private static Model model = ModelFactory.createDefaultModel();

    // set up the namespace basics
    public static final String NS = "http://www.openarchives.org/ore/terms/";

    public static String getURI() { return NS; }

    public static final Resource NAMESPACE = model.createResource(NS);

	// set up the Properties that ORE can assert

    public static final Property aggregates = model.createProperty(NS + "aggregates");

    public static final Property isAggregatedBy = model.createProperty(NS + "isAggregatedBy");

    public static final Property describes = model.createProperty(NS + "describes");

    public static final Property isDescribedBy = model.createProperty(NS + "isDescribedBy");

    public static final Property similarTo = model.createProperty(NS + "similarTo");

    public static final Property proxyFor = model.createProperty(NS + "proxyFor");

    public static final Property proxyIn = model.createProperty(NS + "proxyIn");

    public static final Property lineage = model.createProperty(NS + "lineage");

    // set up the Resource types that ORE can assert

    public static final Resource ResourceMap = model.createResource(NS + "ResourceMap");

    public static final Resource Aggregation = model.createResource(NS + "Aggregation");

    public static final Resource AggregatedResource = model.createResource(NS + "AggregatedResource");

    public static final Resource Proxy = model.createResource(NS + "Proxy");
}
