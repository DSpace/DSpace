/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
