/*
 * ProxyJena.java
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

import org.dspace.foresite.Proxy;
import org.dspace.foresite.AggregatedResource;
import org.dspace.foresite.Aggregation;
import org.dspace.foresite.OREException;
import org.dspace.foresite.Agent;

import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @Author Richard Jones
 */
public class ProxyJena extends OREResourceJena implements Proxy
{
    public ProxyJena()
    {
        super();
    }

    ///////////////////////////////////////////////////////////////////
    // Methods from OREResourceJena
    ///////////////////////////////////////////////////////////////////

    public void empty()
    {

    }

    ///////////////////////////////////////////////////////////////////
    // Methods from Proxy
    ///////////////////////////////////////////////////////////////////

    public void initialise(URI uri)
    {
        res = model.createResource(uri.toString());
        res.addProperty(RDF.type, ORE.Proxy);
    }

    public AggregatedResource getProxyFor()
            throws OREException
    {
        try
        {
            StmtIterator itr = res.listProperties(ORE.proxyFor);
            if (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                Resource resource = ((Resource) statement.getObject());
                AggregatedResource ar = JenaOREFactory.createAggregatedResource(model, new URI(resource.getURI()));
                return ar;
            }
            return null;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public void setProxyFor(AggregatedResource proxyFor)
    {
        // FIXME: we may want to just leave these out!
    }

    public void setProxyForURI(URI uri)
    {
        res.addProperty(ORE.proxyFor, model.createResource(uri.toString()));
    }

    public Aggregation getProxyIn()
            throws OREException
    {
        try
        {
            StmtIterator itr = res.listProperties(ORE.proxyIn);
            if (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                Resource resource = ((Resource) statement.getObject());
                Aggregation agg = JenaOREFactory.createAggregation(model, new URI(resource.getURI()));
                return agg;
            }
            return null;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public void setProxyIn(Aggregation proxyIn)
    {
        // FIXME: we may want to just leave these out!
    }

    public void setProxyInURI(URI uri)
    {
        res.addProperty(ORE.proxyIn, model.createResource(uri.toString()));
    }

	public void assertRelation(URI uri, Proxy proxy)
			throws OREException
	{
		res.addProperty(model.createProperty(uri.toString()), ((ProxyJena) proxy).getResource());
	}

	public List<Proxy> getRelated(URI uri)
			throws OREException
	{
		try
		{
			List<Proxy> related = new ArrayList<Proxy>();
			Selector selector = new SimpleSelector(res, model.createProperty(uri.toString()), (RDFNode) null);
			StmtIterator itr = model.listStatements(selector);
			while (itr.hasNext())
			{
				Statement statement = itr.nextStatement();
				if (statement.getObject() instanceof Resource)
				{
					Proxy proxy = JenaOREFactory.createProxy(model, new URI(((Resource) statement.getObject()).getURI()));
					related.add(proxy);
				}
			}
			return related;
		}
		catch (URISyntaxException e)
		{
			throw new OREException(e);
		}
	}

	public void setLineage(URI externalProxy) throws OREException
	{
		res.addProperty(ORE.lineage, model.createResource(externalProxy.toString()));
	}

	public URI getLineage() throws OREException
	{
		StmtIterator itr = res.listProperties(ORE.lineage);
		try
		{
			if (itr.hasNext())
			{
				Statement statement = itr.nextStatement();
				if (statement.getObject() instanceof Resource)
				{
					return new URI(((Resource) statement.getObject()).getURI());
				}
			}
			return null;
		}
		catch (URISyntaxException e)
		{
			throw new OREException(e);
		}
	}

	public List<Agent> getCreators()
	{
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void setCreators(List<Agent> creators)
	{
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void addCreator(Agent creator)
	{
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void clearCreators()
	{
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
