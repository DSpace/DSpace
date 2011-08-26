/*
 * AggregationJena.java
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
import org.dspace.foresite.AggregatedResource;
import org.dspace.foresite.ReMSerialisation;
import org.dspace.foresite.OREException;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.OREFactory;
import org.dspace.foresite.Proxy;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.net.URI;
import java.net.URISyntaxException;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @Author Richard Jones
 */
public class AggregationJena extends OREResourceJena implements Aggregation
{
    public AggregationJena()
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
    // Methods from Aggregation
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
			throw new OREException("Illegal URI: " + uri.toString() + "; Aggregation requires a protocol-based URI");
		}

		res = model.createResource(uri.toString());
        res.addProperty(RDF.type, ORE.Aggregation);
    }

    public List<Agent> getCreators()
    {
        List<Agent> creators = new ArrayList<Agent>();
        StmtIterator itr = res.listProperties(DC.creator);
        while (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            Resource resource = ((Resource) statement.getObject());
			Agent creator = JenaOREFactory.createAgent(resource);
            creators.add(creator);
        }
        return creators;
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
        // creator is only a flat set of triples, so can be added this way
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
            Statement statement = res.getProperty(DCTerms.created);
            if (statement == null)
            {
                return null;
            }
            String date = ((Literal) statement.getObject()).getLexicalForm();
            Date created = sdf.parse(date);
            return created;
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
            Statement statement = res.getProperty(DCTerms.modified);
            if (statement == null)
            {
                return null;
            }
            String date = ((Literal) statement.getObject()).getLexicalForm();
            Date created = sdf.parse(date);
            return created;
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

    public List<String> getRights()
    {
        List<String> rights = new ArrayList<String>();
        StmtIterator itr = res.listProperties(DC.rights);
        while (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            String right = ((Literal) statement.getObject()).getLexicalForm();
            rights.add(right);
        }
        return rights;
    }

    public void setRights(List<String> rights)
    {
        this.clearRights();
        for (String right : rights)
        {
            this.addRights(right);
        }
    }

    public void addRights(String rights)
    {
        res.addProperty(DC.rights, model.createTypedLiteral(rights));
    }

    public void clearRights()
    {
        res.removeAll(DC.rights);
    }

    public List<String> getTitles()
    {
        List<String> titles = new ArrayList<String>();
        StmtIterator itr = res.listProperties(DC.title);
        while (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            String title = ((Literal) statement.getObject()).getLexicalForm();
            titles.add(title);
        }
        return titles;
    }

    public void setTitles(List<String> titles)
    {
        this.clearTitles();
        for (String title : titles)
        {
            this.addTitle(title);
        }
    }

    public void addTitle(String title)
    {
        res.addProperty(DC.title, title);
    }

    public void clearTitles()
    {
        StmtIterator itr = res.listProperties(DC.title);
        model.remove(itr);
    }

    public List<URI> getTypes()
			throws OREException
	{
		try
		{
			List<URI> types = new ArrayList<URI>();
			StmtIterator itr = res.listProperties(RDF.type);
			while (itr.hasNext())
			{
				Statement statement = itr.nextStatement();
				RDFNode node = statement.getObject();
				if (node instanceof Resource)
				{
					types.add(new URI(((Resource) node).getURI()));
				}
				else if (node instanceof Literal)
				{
					throw new OREException("Type MAY NOT be Literal; error in graph");
				}
			}
			return types;
		}
		catch (URISyntaxException e)
		{
			throw new OREException(e);
		}
	}

    public void setTypes(List<URI> types)
    {
        this.clearTypes();
        for (URI type : types)
        {
            this.addType(type);
        }

		// ensure that the required type is still set
		Selector selector = new SimpleSelector(res, RDF.type, ORE.Aggregation);
		StmtIterator itr = model.listStatements(selector);
		if (!itr.hasNext())
		{
			res.addProperty(RDF.type, ORE.Aggregation);
		}
	}

    public void addType(URI type)
    {
        res.addProperty(RDF.type, model.createResource(type.toString()));
    }

    public void clearTypes()
    {
        StmtIterator itr = res.listProperties(RDF.type);
        model.remove(itr);

		// ensure that the required type is still set
		res.addProperty(RDF.type, ORE.Aggregation);
	}

    public List<URI> getSimilarTo()
            throws OREException
    {
        try
        {
            List<URI> similars = new ArrayList<URI>();
            StmtIterator itr = res.listProperties(ORE.similarTo);
            while (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                RDFNode node = statement.getObject();
                similars.add(new URI(((Literal) node).getLexicalForm()));
            }
            return similars;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public void setSimilarTo(List<URI> similarTo)
    {
        this.clearSimilarTo();
        for (URI similar : similarTo)
        {
            this.addSimilarTo(similar);
        }
    }

    public void addSimilarTo(URI similarTo)
    {
        res.addProperty(ORE.similarTo, model.createTypedLiteral(similarTo));
    }

    public void clearSimilarTo()
    {
        StmtIterator itr = res.listProperties(ORE.similarTo);
        model.remove(itr);
    }

	public List<URI> getSeeAlso()
			throws OREException
	{
		try
        {
            List<URI> similars = new ArrayList<URI>();
            StmtIterator itr = res.listProperties(RDFS.seeAlso);
            while (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                RDFNode node = statement.getObject();
                similars.add(new URI(((Literal) node).getLexicalForm()));
            }
            return similars;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
	}

	public void setSeeAlso(List<URI> seeAlso)
			throws OREException
	{
		this.clearSeeAlso();
        for (URI similar : seeAlso)
        {
            this.addSeeAlso(similar);
        }
	}

	public void addSeeAlso(URI seeAlso)
			throws OREException
	{
		res.addProperty(RDFS.seeAlso, model.createTypedLiteral(seeAlso));
	}

	public void clearSeeAlso()
			throws OREException
	{
		StmtIterator itr = res.listProperties(RDFS.seeAlso);
        model.remove(itr);
	}

	public AggregatedResource createAggregatedResource(Aggregation aggregation)
			throws OREException
	{
		AggregatedResource ar = JenaOREFactory.createAggregatedResource(model, aggregation.getURI());

		// transfer information from the aggregation to the aggregated resource
		// where appropriate
		//
		// FIXME: what list of things should be transferrd over:
		//
		// List<URI> types = aggregation.getTypes();
		// List<Triple> triples = aggregation.listTriples();
		// etc.

		// now set the appropriate type information
		Resource resource = ((AggregatedResourceJena) ar).getResource();
		resource.addProperty(RDF.type, ORE.Aggregation);

		// FIXME: what happens to this type information under clearTypes()?

		// now add the resource
		this.addAggregatedResource(ar);
		return ar;
	}

	public AggregatedResource createAggregatedResource(URI uri)
            throws OREException
    {
		// have to validate to ensure that uri is not the same as the URI of
		// this object
		if (uri.equals(res.getURI()))
		{
			throw new OREException("Cannot create an AggregatedResource with the same URI as its Aggregation");
		}

		// now go ahead and create the resource
		AggregatedResource ar = JenaOREFactory.createAggregatedResource(model, uri);
        this.addAggregatedResource(ar);
        return ar;
    }

    public List<AggregatedResource> getAggregatedResources()
            throws OREException
    {
        try
        {
            List<AggregatedResource> ars = new ArrayList<AggregatedResource>();
            StmtIterator itr = res.listProperties(ORE.aggregates);
            while (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                String resURI = ((Resource) statement.getObject()).getURI();
                AggregatedResource ar = JenaOREFactory.createAggregatedResource(model, new URI(resURI));
                ars.add(ar);
            }
            return ars;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public void setAggregatedResources(List<AggregatedResource> resources)
            throws OREException
    {
        this.clearAggregatedResources();
        for (AggregatedResource ar : resources)
        {
            this.addAggregatedResource(ar);
        }
    }

    public void addAggregatedResource(AggregatedResource resource)
            throws OREException
    {
        try
        {
			// have to validate to ensure that uri is not the same as the URI of
			// this object
			if (resource.getURI().equals(res.getURI()))
			{
				throw new OREException("Cannot add an AggregatedResource with the same URI as its Aggregation");
			}

			// tell the aggregated resource who it is aggregated by
			resource.addAggregation(new URI(res.getURI()));

			// add the AggregatedResource Model to our internal model
			Resource ar = ((AggregatedResourceJena) resource).getResource();
            Model arModel = ((AggregatedResourceJena) resource).getModel();
            this.addModelToModel(arModel);

			// relate the aggregation to the aggregated resource
			res.addProperty(ORE.aggregates, ar);
		}
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public void clearAggregatedResources()
            throws OREException
    {
		// first, burn all the aggregated resources
		List<AggregatedResource> ars = this.getAggregatedResources();
		for (AggregatedResource ar : ars)
		{
			ar.empty();
		}

		// now remove references to them from the Aggregation
		StmtIterator itr = res.listProperties(ORE.aggregates);
		model.remove(itr);
	}

	/* Refactoring ...
	public ReMSerialisation getPrimarySerialisation()
            throws OREException
    {
        ResourceMap rem = this.getResourceMap();
        Resource resource = ((ResourceMapJena) rem).getResource();
        StmtIterator itr = resource.listProperties(DC.format);
        String mime = "application/octet-stream";
        if (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            if (statement.getObject() instanceof Literal)
            {
                mime = ((Literal) statement.getObject()).getLexicalForm();
            }
        }
        ReMSerialisation rems = new ReMSerialisation(mime, rem.getURI());
        return rems;
    }*/

    public List<ReMSerialisation> getReMSerialisations()
            throws OREException
    {
        try
        {
            List<ReMSerialisation> serialisations = new ArrayList<ReMSerialisation>();
            StmtIterator itr = res.listProperties(ORE.isDescribedBy);
            while (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                Resource resource = (Resource) statement.getObject();
                StmtIterator itr2 = resource.listProperties(DC.format);
				StmtIterator itr3 = resource.listProperties(OREX.isAuthoritativeFor);
				String mime = "application/octet-stream";
                if (itr2.hasNext())
                {
                    Statement stmt = itr2.nextStatement();
                    mime = ((Literal) stmt.getObject()).getLexicalForm();
                }
				boolean authoritative = false;
				if (itr3.hasNext())
				{
					authoritative = true;
				}
				ReMSerialisation serialisation = new ReMSerialisation(mime, new URI(resource.getURI()));
				serialisation.setAuthoritative(authoritative);

				serialisations.add(serialisation);
            }
            return serialisations;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public void setReMSerialisations(List<ReMSerialisation> serialisations)
            throws OREException
    {
        for (ReMSerialisation serial : serialisations)
		{
			this.addReMSerialisation(serial);
		}
    }

	/* Refactoring ...
	public void setPrimarySerialisation(URI uri)
            throws OREException
    {
        // see if there's a primary serialisation already
        ResourceMap map = this.getResourceMap();

        // if there is no resource map we can't set a serialisation
        if (map != null)
        {
            // don't do anything if this is nothing original
            if (map.getURI().equals(uri))
            {
                return;
            }
        }

        // remove the current link to the serialisation
        Selector selector = new SimpleSelector(null, ORE.describes, (RDFNode) null);
        StmtIterator itr = model.listStatements(selector);
        model.remove(itr);

        // add the new link to the serialisation
        Resource resource = model.getResource(uri.toString());
        resource.addProperty(ORE.describes, res);
    }*/

	// FIXME: I'm not 100% sure that this method is going to work as desired
	public void clearReMSerialisations()
    {
        StmtIterator itr = res.listProperties(ORE.isDescribedBy);
        while (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            Resource resource = (Resource) statement.getObject();
            StmtIterator itr2 = resource.listProperties();
            model.remove(itr2);
        }
        model.remove(itr);
    }

    public void addReMSerialisation(ReMSerialisation serialisation)
			throws OREException
	{
		URI uri = serialisation.getURI();
		ResourceMap rem = this.getResourceMap(uri);
		if (rem != null)
		{
			return;
		}
		this.addResourceMapURI(uri);

		String mime = serialisation.getMimeType();
		if (mime == null)
		{
			mime = "application/octet-stream";
		}
		Literal literal = model.createTypedLiteral(mime);
		Statement statement = model.createStatement(model.getResource(uri.toString()), DC.format, literal);
		model.add(statement);
    }

	/* Refactoring ...
	public ResourceMap getResourceMap()
            throws OREException
    {
        try
        {
            Selector selector = new SimpleSelector(null, ORE.describes, res);
            StmtIterator itr = model.listStatements(selector);
            if (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                Resource resource = (Resource) statement.getSubject();
                ResourceMap rem = JenaOREFactory.createResourceMap(model, new URI(resource.getURI()));
                return rem;
            }
            return null;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }*/

	public ResourceMap createResourceMap(URI uri)
			throws OREException
	{
		// FIXME: ok, so we're creating a resource map out of this model, which
		// means that the REM already contains this Aggregation!  Is this really working?
		ResourceMap rem = JenaOREFactory.createResourceMap(model, uri);

		// find out if this Aggregation already has an authoritative resource map
		List<ResourceMap> arems = this.getAuthoritative();
		if (arems.size() == 0)
		{
			// if there are no authority resource maps, make this newly created one one
			rem.setAuthoritative(true);
		}

		// now ensure that isDescribedBy is set
		model.createStatement(res, ORE.isDescribedBy, ((ResourceMapJena) rem).getResource());

		return rem;
	}

	public List<ResourceMap> getAuthoritative()
			throws OREException
	{
		try
		{
			List<ResourceMap> rems = new ArrayList<ResourceMap>();
			Selector selector = new SimpleSelector(null, OREX.isAuthoritativeFor, res);
			StmtIterator itr = model.listStatements(selector);
			while (itr.hasNext())
			{
				Statement statement = itr.nextStatement();
				ResourceMap rem = JenaOREFactory.createResourceMap(model, new URI(statement.getSubject().getURI()));
				rems.add(rem);
			}
			return rems;
		}
		catch (URISyntaxException e)
		{
			throw new OREException(e);
		}
	}

	public List<ResourceMap> getResourceMaps() throws OREException
	{
		try
		{
			List<ResourceMap> rems = new ArrayList<ResourceMap>();
			Selector selector = new SimpleSelector(res, ORE.isDescribedBy, (RDFNode) null);
			StmtIterator itr = model.listStatements(selector);
			while (itr.hasNext())
			{
				Statement statement = itr.nextStatement();
				ResourceMap rem = JenaOREFactory.createResourceMap(model, new URI(statement.getSubject().getURI()));
				rems.add(rem);
			}
			return rems;
		}
		catch (URISyntaxException e)
		{
			throw new OREException(e);
		}
	}

	public void addResourceMapURI(URI uri) throws OREException
	{
		Resource resource = model.createResource(uri.toString());
		model.add(model.createStatement(resource, ORE.describes, res));
		model.add(model.createStatement(res, ORE.isDescribedBy, resource));
		model.add(model.createStatement(resource, RDF.type, ORE.ResourceMap));
	}

	public ResourceMap getResourceMap(URI uri) throws OREException
	{
		Selector selector = new SimpleSelector(null, ORE.isAggregatedBy, res);
		StmtIterator itr = model.listStatements(selector);
		if (itr.hasNext())
		{
			ResourceMap rem = JenaOREFactory.createResourceMap(model, uri);
			return rem;
		}
		return null;
	}

	public Proxy createProxy(URI proxyURI, URI arURI)
            throws OREException
    {
		// first validate whether this is allowed:
		// - proxy must be unique in Aggregation
		// - proxy must be unique to AggregatedResource

		Selector selector = new SimpleSelector(model.createResource(proxyURI.toString()), null, (RDFNode) null);
		StmtIterator itr = model.listStatements(selector);
		if (itr.hasNext())
		{
			throw new OREException("URI: " + proxyURI.toString() + " is already in use by this Aggregation");
		}

		// go ahead and create the proxy
		Proxy proxy = OREFactory.createProxy(proxyURI);

        // if the URI doesn't exist, it will be created
        proxy.setProxyForURI(arURI);

        this.addProxy(proxy);

        ((ProxyJena) proxy).setModel(model, proxyURI);
        return proxy;
    }

    public void addProxy(Proxy proxy)
            throws OREException
    {
        proxy.setProxyInURI(this.getURI());
        Model pModel = ((ProxyJena) proxy).getModel();
        this.addModelToModel(pModel);
    }

    public List<Proxy> getProxies()
            throws OREException
    {
        try
        {
            Selector selector = new SimpleSelector(null, ORE.proxyIn, res);
            List<Proxy> proxies = new ArrayList<Proxy>();
            StmtIterator itr = model.listStatements(selector);
            while (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                URI resURI = new URI(statement.getSubject().getURI());
                Proxy proxy = JenaOREFactory.createProxy(model, resURI);
                proxies.add(proxy);
            }
            return proxies;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public void clearProxies()
            throws OREException
    {
        List<Proxy> proxies = this.getProxies();

        // proxy.empty();
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
