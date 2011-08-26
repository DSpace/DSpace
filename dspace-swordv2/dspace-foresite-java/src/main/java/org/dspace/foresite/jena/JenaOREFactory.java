/*
 * JenaOREFactory.java
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

import org.dspace.foresite.Aggregation;
import org.dspace.foresite.Agent;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.AggregatedResource;
import org.dspace.foresite.Proxy;
import org.dspace.foresite.Triple;
import org.dspace.foresite.OREException;
import org.dspace.foresite.Predicate;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.AnonId;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @Author Richard Jones
 */
public class JenaOREFactory
{
    public static Aggregation createAggregation(Resource resource)
    {
        AggregationJena aggregation = new AggregationJena();
        aggregation.setResource(resource);
        return aggregation;
    }

    public static Aggregation createAggregation(Model model, URI resourceURI)
			throws OREException
	{
        AggregationJena aggregation = new AggregationJena();
        aggregation.setModel(model, resourceURI);
        return aggregation;
    }

    public static Agent createAgent(Resource resource)
    {
        AgentJena agent = new AgentJena();
        agent.setResource(resource);
        return agent;
    }

	public static Agent createAgent(Model model, AnonId resourceID)
			throws OREException
	{
		AgentJena agent = new AgentJena();
        agent.setModel(model, resourceID);
        return agent;
	}

	public static Agent createAgent(Model model, URI resourceURI)
			throws OREException
	{
		AgentJena agent = new AgentJena();
        agent.setModel(model, resourceURI);
        return agent;
	}

	public static ResourceMap createResourceMap(Model model, URI resourceURI)
			throws OREException
	{
        ResourceMapJena rem = new ResourceMapJena();
        rem.setModel(model, resourceURI);
        return rem;
    }

    public static AggregatedResource createAggregatedResource(Model model, URI resourceURI)
			throws OREException
	{
        AggregatedResourceJena ar = new AggregatedResourceJena();
        ar.setModel(model, resourceURI);
        return ar;
    }

    public static Proxy createProxy(Model model, URI resourceURI)
			throws OREException
	{
        ProxyJena proxy = new ProxyJena();
        proxy.setModel(model, resourceURI);
        return proxy;
    }

    public static Triple createTriple(Statement statement)
            throws OREException
    {
        try
        {
            URI subject = new URI(statement.getSubject().getURI());
            Property property = statement.getPredicate();

            Predicate pred = new Predicate();
            pred.setURI(new URI(property.getURI()));

            TripleJena triple = new TripleJena();
            triple.initialise(new URI(statement.getSubject().getURI()));

            RDFNode node = statement.getObject();
            if (node instanceof Resource)
            {
				String uris = ((Resource) node).getURI();

				// if the object is not a blank node, we include it
				if (uris != null)
				{
					URI object = new URI(uris);
                	triple.relate(pred, object);
				}
            }
            else
            {
                String object = ((Literal) statement.getObject()).getLexicalForm();
                triple.relate(pred, object);
            }
            
            return triple;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public static Statement createStatement(Triple triple)
            throws OREException
    {
        Model model = ModelFactory.createDefaultModel();

        Resource resource = model.createResource(triple.getSubjectURI().toString());
        Property property = model.createProperty(triple.getPredicate().getURI().toString());

        RDFNode node;
        if (triple.isLiteral())
        {
            node = model.createTypedLiteral(triple.getObjectLiteral());
        }
        else
        {
            node = model.createResource(triple.getObjectURI().toString());
        }

        Statement statement = model.createStatement(resource, property, node);

        return statement;
    }
}
