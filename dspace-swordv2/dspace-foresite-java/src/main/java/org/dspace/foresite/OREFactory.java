/*
 * OREFactory.java
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
package org.dspace.foresite;

import org.dspace.foresite.jena.ResourceMapJena;
import org.dspace.foresite.jena.AgentJena;
import org.dspace.foresite.jena.AggregationJena;
import org.dspace.foresite.jena.AggregatedResourceJena;
import org.dspace.foresite.jena.ProxyJena;
import org.dspace.foresite.jena.TripleJena;

import java.net.URI;

/**
 * @Author Richard Jones
 */
public class OREFactory
{
    public static ResourceMap createResourceMap(URI uri)
			throws OREException
	{
        ResourceMap rem = new ResourceMapJena();
        rem.initialise(uri);
        return rem;
    }

    public static Agent createAgent()
    {
        Agent agent = new AgentJena();
        agent.initialise();
        return agent;
    }

	public static Agent createAgent(URI uri)
	{
		Agent agent = new AgentJena();
		agent.initialise(uri);
		return agent;
	}

	public static Aggregation createAggregation(URI uri)
			throws OREException
	{
        Aggregation aggregation = new AggregationJena();
        aggregation.initialise(uri);
        return aggregation;
    }

    public static AggregatedResource createAggregatedResource(URI uri)
			throws OREException
	{
        AggregatedResource ar = new AggregatedResourceJena();
        ar.initialise(uri);
        return ar;
    }

    public static Proxy createProxy(URI uri)
    {
        Proxy proxy = new ProxyJena();
        proxy.initialise(uri);
        return proxy;
    }

    public static Triple createTriple(URI subject, Predicate pred, URI object)
            throws OREException
    {
        Triple triple = new TripleJena();
        triple.initialise(subject);
        triple.relate(pred, object);
        return triple;
    }

    public static Triple createTriple(URI subject, Predicate pred, Object object)
            throws OREException
    {
        Triple triple = new TripleJena();
        triple.initialise(subject);
        triple.relate(pred, object);
        return triple;
    }

    public static Triple createTriple(OREResource subject, Predicate pred, OREResource object)
            throws OREException
    {
        Triple triple = new TripleJena();
        triple.initialise(subject);
        triple.relate(pred, object);
        return triple;
    }

    public static Triple createTriple(OREResource subject, Predicate pred, URI object)
            throws OREException
    {
        Triple triple = new TripleJena();
        triple.initialise(subject);
        triple.relate(pred, object);
        return triple;
    }

    public static Triple createTriple(OREResource subject, Predicate pred, Object object)
            throws OREException
    {
        Triple triple = new TripleJena();
        triple.initialise(subject);
        triple.relate(pred, object);
        return triple;
    }
}
