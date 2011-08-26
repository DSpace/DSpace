/*
 * ResourceMapJena.java
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

import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.Agent;
import org.dspace.foresite.Aggregation;
import org.dspace.foresite.Triple;
import org.dspace.foresite.OREException;
import org.dspace.foresite.OREFactory;
import org.dspace.foresite.ReMSerialisation;
import org.dspace.foresite.AggregatedResource;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @Author Richard Jones
 */
public class ResourceMapJena extends OREResourceJena implements ResourceMap
{
	public ResourceMapJena()
    {
        // allow OREResourceJena to construct the model for us
        super();
    }

    ///////////////////////////////////////////////////////////////////
    // Methods from OREResourceJena
    ///////////////////////////////////////////////////////////////////

    public void empty()
    {

    }

    ///////////////////////////////////////////////////////////////////
    // Methods from ResourceMap
    ///////////////////////////////////////////////////////////////////
    
    public void initialise(URI uri)
			throws OREException
	{
		// FIXME: are we 100% sure that this is a valid way of determining
		// protocol-based-ness.  See RFC3986 for reference.
		//
		// we need to ensure that the URI is protocol based
		String ident = uri.toString();
		String rx = ".+://.+";
		Pattern p = Pattern.compile(rx);
		Matcher m = p.matcher(ident);
		if (!m.matches())
		{
			throw new OREException("Illegal URI: " + uri.toString() + "; ResourceMap requires a protocol-based URI");
		}

		res = model.createResource(uri.toString());
        res.addProperty(RDF.type, ORE.ResourceMap);
    }

	public boolean isAuthoritative() throws OREException
	{
		StmtIterator itr = res.listProperties(OREX.isAuthoritativeFor);
		if (itr.hasNext())
		{
			return true;
		}
		return false;
	}

	public void setAuthoritative(boolean authoritative) throws OREException
	{
		if (authoritative)
		{
			Selector selector = new SimpleSelector(null, ORE.isDescribedBy, res);
			StmtIterator itr = model.listStatements(selector);
			if (itr.hasNext())
			{
				Statement statement = itr.nextStatement();
				res.addProperty(OREX.isAuthoritativeFor, statement.getSubject());
			}
		}
		else
		{
			res.removeAll(OREX.isAuthoritativeFor);
		}
	}

	public List<Agent> getCreators()
    {
        List<Agent> agents = new ArrayList<Agent>();
        StmtIterator itr = res.listProperties(DC.creator);
        while (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            Agent agent = JenaOREFactory.createAgent((Resource) statement.getObject());
            agents.add(agent);
        }

        return agents;
    }

    public void setCreators(List<Agent> creators)
    {
        this.clearCreators();
        for (Agent creator : creators)
        {
            this.addCreator(creator);
        }
    }

    public void addCreator(Agent creator)
    {
        Resource resource = ((AgentJena) creator).getResource();
        res.addProperty(DC.creator, resource);
        this.addResourceToModel(resource);
    }

    // FIXME: if creators are shared, then there will be problems with the
    // resulting REM.  Therefore need to check for shared creators
    public void clearCreators()
    {
        List<Agent> creators = this.getCreators();
        for (Agent creator : creators)
        {
            Model cModel = ((AgentJena) creator).getModel();
            StmtIterator itr = cModel.listStatements();
            model.remove(itr);
        }
    }

    public Date getCreated()
            throws OREException
    {
        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat(JenaOREConstants.dateFormat);
            StmtIterator itr = res.listProperties(DCTerms.created);
            if (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                String value = ((Literal) statement.getObject()).getLexicalForm();
                Date created = sdf.parse(value);
                return created;
            }
            return null;
        }
        catch (ParseException e)
        {
            throw new OREException(e);
        }
    }

    public void setCreated(Date created)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(JenaOREConstants.dateFormat);
        String date = sdf.format(created);
        res.addProperty(DCTerms.created, model.createTypedLiteral(date, JenaOREConstants.dateTypedLiteral));
    }

    public Date getModified()
            throws OREException
    {
        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat(JenaOREConstants.dateFormat);
            StmtIterator itr = res.listProperties(DCTerms.modified);
            if (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                String value = ((Literal) statement.getObject()).getLexicalForm();
                Date modified = sdf.parse(value);
                return modified;
            }
            return null;
        }
        catch (ParseException e)
        {
            throw new OREException(e);
        }
    }

    public void setModified(Date modified)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(JenaOREConstants.dateFormat);
        String date = sdf.format(modified);
        res.addProperty(DCTerms.modified, model.createTypedLiteral(date, JenaOREConstants.dateTypedLiteral));
    }

    public String getRights()
    {
        Statement statement = res.getProperty(DC.rights);
        return statement.getString();
    }

    public void setRights(String rights)
    {
        res.addProperty(DC.rights, model.createTypedLiteral(rights));
    }

    public void removeRights()
    {
        res.removeAll(DC.rights);
    }

	// FIXME: this must only create one aggregation ever, or throw and error!
	public Aggregation createAggregation(URI uri)
            throws OREException
    {
        Aggregation agg = OREFactory.createAggregation(uri);
        this.setAggregation(agg);
        ((AggregationJena) agg).setModel(model, uri);
        return agg;
    }

    public Aggregation getAggregation()
            throws OREException
    {
        try
        {
            StmtIterator itr = res.listProperties(ORE.describes);
            if (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                String resURI = ((Resource) statement.getObject()).getURI();
                Aggregation aggregation = JenaOREFactory.createAggregation(model, new URI(resURI));
                return aggregation;
            }
            return null;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public void setAggregation(Aggregation aggregation)
            throws OREException
    {
        try
        {
            ReMSerialisation serialisation = new ReMSerialisation();
            serialisation.setURI(new URI(res.getURI()));
			StmtIterator itr = res.listProperties(DC.format);
			String mime = "application/octet-stream";
			if (itr.hasNext())
			{
				Statement statement = itr.nextStatement();
				mime = ((Literal) statement.getObject()).getLexicalForm();
			}
			serialisation.setMimeType(mime);
            aggregation.addReMSerialisation(serialisation);

			// FIXME: I don't think we need all of this code, hence the comments
			// Resource resource = ((AggregationJena) aggregation).getResource();
            Model aModel = ((AggregationJena) aggregation).getModel();
            // res.addProperty(ORE.describes, resource);
            this.addModelToModel(aModel);
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public void removeAggregation()
            throws OREException
    {
        AggregationJena agg = (AggregationJena) this.getAggregation();
        // FIXME: how do we ensure that we successfully recursively remove the aggregation?
        // can we do it with Jena, or do we need to utilise our own API.  I think the
        // latter!

        // agg.empty();
    }

	public List<AggregatedResource> getAggregatedResources() throws OREException
	{
		Aggregation agg = this.getAggregation();
		return agg.getAggregatedResources();
	}

	// FIXME: how do we actually do this?
    public List<Triple> doSparql(String sparql)
    {
        return null;
    }

	///////////////////////////////////////////////////////////////////
	// methods from OREResourceJena
	///////////////////////////////////////////////////////////////////

	public void setModel(Model model, URI resourceURI)
			throws OREException
	{
		// verify that the URI for the resource map is not already in use for another
		// part of the object
		Resource subject = model.createResource(resourceURI.toString());
		Selector aggSelector = new SimpleSelector(subject, ORE.aggregates, (RDFNode) null);
		Selector proxySelector = new SimpleSelector(subject, ORE.proxyFor, (RDFNode) null);
		StmtIterator aggItr = model.listStatements(aggSelector);
		StmtIterator proxyItr = model.listStatements(proxySelector);
		if (aggItr.hasNext() || proxyItr.hasNext())
		{
			throw new OREException("ResourceMap may not have the same URI as an Aggregation or Proxy");
		}

		// now go back and build the resource map in the super class
		super.setModel(model, resourceURI);
	}

	///////////////////////////////////////////////////////////////////
    // Private Methods
    ///////////////////////////////////////////////////////////////////

    private void addResourceToModel(Resource resource)
    {
        StmtIterator itr = resource.listProperties();
        model.add(itr);
    }

    private void addModelToModel(Model externalModel)
    {
        StmtIterator itr = externalModel.listStatements();
        model.add(itr);
    }
}
