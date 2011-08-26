/*
 * ORE.java
 *
 * Copyright (c) 2008, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
