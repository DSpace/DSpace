/*
 * OREResourceJena.java
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

import org.dspace.foresite.OREResource;
import org.dspace.foresite.OREException;
import org.dspace.foresite.Triple;
import org.dspace.foresite.TripleSelector;
import org.dspace.foresite.Predicate;
import org.dspace.foresite.OREFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.AnonId;

/**
 * @Author Richard Jones
 */
public abstract class OREResourceJena implements OREResource, GraphResource
{
    protected Model model;

    protected Resource res;

    protected OREResourceJena()
    {
        model = ModelFactory.createDefaultModel();
    }

    ///////////////////////////////////////////////////////////////////
    // Methods from OREResource
    ///////////////////////////////////////////////////////////////////

    public URI getURI()
            throws OREException
    {
        try
        {
            return new URI(res.getURI());
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public List<Triple> listTriples()
            throws OREException
    {
        StmtIterator itr = res.listProperties();
		List<Triple> triples = new ArrayList<Triple>();
        while (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            Triple triple = JenaOREFactory.createTriple(statement);
			triples.add(triple);
		}
        return triples;
    }

	public List<Triple> listAllTriples()
			throws OREException
	{
		StmtIterator itr = model.listStatements();
		List<Triple> triples = new ArrayList<Triple>();
        while (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            Triple triple = JenaOREFactory.createTriple(statement);
			triples.add(triple);
		}
        return triples;
	}

	public List<Triple> listTriples(TripleSelector selector)
            throws OREException
    {
		// force the source to be the current resource
		selector.setSubjectURI(this.getURI());
		return this.listAllTriples(selector);
    }

	public List<Triple> listAllTriples(TripleSelector selector)
			throws OREException
	{
		// get the possible selection values
		URI subjectURI = selector.getSubjectURI();
		Predicate predInit = selector.getPredicate();
		URI objectURI = selector.getObjectURI();
		Object objectLiteral = selector.getLiteral();

		// delegate to a general handler
		return listTriples(subjectURI, predInit, objectURI, objectLiteral);
	}

	public void addTriples(List<Triple> triples)
            throws OREException
    {
        for (Triple triple : triples)
        {
            this.addTriple(triple);
        }
    }

    public void addTriple(Triple triple)
            throws OREException
    {
		// for the graph to be connected, we need to ensure that any
		// given triple refers to an existing part of the model

		Statement statement = JenaOREFactory.createStatement(triple);

		boolean connected = false;

		Resource subject = statement.getSubject();
		Selector selector1 = new SimpleSelector(subject, null, (RDFNode) null);
		Selector selector2 = new SimpleSelector(null, null, subject);
		StmtIterator itr1 = model.listStatements(selector1);
		StmtIterator itr2 = model.listStatements(selector2);
		if (itr1.hasNext() || itr2.hasNext())
		{
			connected = true;
		}

		RDFNode object = statement.getObject();
		Resource oResource = null;
		if (object instanceof Resource)
		{
			oResource = (Resource) object;
		}
		if (oResource != null && !connected)
		{
			Selector selector3 = new SimpleSelector(oResource, null, (RDFNode) null);
			Selector selector4 = new SimpleSelector(null, null, oResource);
			StmtIterator itr3 = model.listStatements(selector3);
			StmtIterator itr4 = model.listStatements(selector4);
			if (itr3.hasNext() || itr4.hasNext())
			{
				connected = true;
			}
		}

		if (!connected)
		{
			throw new OREException("Illegal Triple; graph must be connected");
		}

		// FIXME: consider rejecting any statements which have ORE semantics in them
		// and throw an error telling the developer to use the damn API, that's what
		// it's there for!

		// if we get this far, then it's fine to add the statement
		model.add(statement);
    }

    public void removeTriple(Triple triple)
            throws OREException
    {
        Statement statement = JenaOREFactory.createStatement(triple);
        model.remove(statement);
    }

    public Triple createTriple(Predicate pred, OREResource resource) throws OREException
    {
        Triple triple = OREFactory.createTriple(this, pred, resource);
        this.addTriple(triple);
        return triple;
    }

    public Triple createTriple(Predicate pred, URI uri) throws OREException
    {
        Triple triple = OREFactory.createTriple(this, pred, uri);
        this.addTriple(triple);
        return triple;
    }

    public Triple createTriple(Predicate pred, Object literal) throws OREException
    {
        Triple triple = OREFactory.createTriple(this, pred, literal);
        this.addTriple(triple);
        return triple;
    }

	///////////////////////////////////////////////////////////////////
	// methods from OREResource which remain Abstract
	///////////////////////////////////////////////////////////////////

	public abstract void empty();

	///////////////////////////////////////////////////////////////////
    // Methods from GraphResource
    ///////////////////////////////////////////////////////////////////

    public Resource getResource()
    {
        return res;
    }

	/* old version
	public void setResource(Resource resource)
    {
        StmtIterator itr = resource.listProperties();
        model.removeAll();
        model.add(itr);

        res = model.getResource(resource.getURI());
    }*/

	public void setResource(Resource resource)
    {
        StmtIterator itr = resource.listProperties();
        model.removeAll();
        model.add(itr);

        res = (Resource) resource.inModel(model);
    }

	public Model getModel()
    {
        return model;
    }

    public void setModel(Model model, URI resourceURI)
			throws OREException
	{
		// FIXME: are we 100% sure that this is a valid way of determining
		// protocol-based-ness.  See RFC3986 for reference.
		//
		// we need to ensure that the URI is protocol based
		String ident = resourceURI.toString();
		String rx = ".+://.+";
		Pattern p = Pattern.compile(rx);
		Matcher m = p.matcher(ident);
		if (!m.matches())
		{
			throw new OREException("Illegal URI: " + resourceURI.toString() + "; GraphResource implementer requires a protocol-based URI");
		}

		this.model = model;
        this.res = model.createResource(resourceURI.toString());
    }

	public void setModel(Model model, AnonId blankID)
			throws OREException
	{
		this.model = model;
        this.res = model.createResource(blankID);
    }

	///////////////////////////////////////////////////////////////////
	// Protected utility methods
	///////////////////////////////////////////////////////////////////


	protected List<Triple> listTriples(URI subjectURI, Predicate predInit, URI objectURI, Object objectLiteral)
			throws OREException
	{
		try
		{
			// prepare null or content for the Jena selector
			Resource subject = null;
			if (subjectURI != null)
			{
				subject = model.createResource(subjectURI.toString());
			}

			Property predicate = null;
			if (predInit != null)
			{
				predicate = model.createProperty(predInit.getURI().toString());
			}

			RDFNode object = null;
			if (objectLiteral != null)
			{
				object = model.createTypedLiteral(objectLiteral);
			}
			else if (objectURI != null)
			{
				object = model.createResource(objectURI.toString());
			}

			// construct the selector
			Selector sel = new SimpleSelector(subject, predicate, object);

			// pull the statements out and translate into Triples
			List<Triple> triples = new ArrayList<Triple>();
			StmtIterator itr = model.listStatements(sel);
			while (itr.hasNext())
			{
				Statement statement = itr.nextStatement();
				Resource resource = statement.getSubject();
				Property property = statement.getPredicate();
				Predicate pred = new Predicate();
				pred.setNamespace(property.getNameSpace());
				pred.setName(property.getLocalName());
				pred.setURI(new URI(property.getURI()));

				Triple triple = new TripleJena();
				triple.initialise(new URI(resource.getURI()));

				RDFNode node = statement.getObject();
				if (node instanceof Literal)
				{
					String literal = ((Literal) node).getLexicalForm();
					triple.relate(pred, literal);
				}
				else
				{
					URI obj = new URI(((Resource) node).getURI());
					triple.relate(pred, obj);
				}

				triples.add(triple);
			}

			return triples;
		}
		catch (URISyntaxException e)
		{
			throw new OREException(e);
		}
	}
}
