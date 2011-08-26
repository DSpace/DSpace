/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite.jena;

import org.dspace.foresite.Agent;
import org.dspace.foresite.OREException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;

import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;

/**
 * @Author Richard Jones
 */
public class AgentJena extends OREResourceJena implements Agent
{
    public AgentJena()
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
    // Methods from Agent
    ///////////////////////////////////////////////////////////////////

    public void initialise()
    {
        res = model.createResource(); // blank node
		res.addProperty(RDF.type, model.createResource("http://purl.org/dc/terms/Agent"));
	}

	public void initialise(URI uri)
	{
		res = model.createResource(uri.toString());
		res.addProperty(RDF.type, model.createResource("http://purl.org/dc/terms/Agent"));
	}

	/* Refactored out for 0.9
	public List<URI> getSeeAlso()
            throws OREException
    {
        try
        {
            List<URI> ret = new ArrayList<URI>();
            StmtIterator itr = res.listProperties(RDFS.seeAlso);

            while(itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                URI uri = new URI(((Literal) statement.getObject()).getLexicalForm());
                ret.add(uri);
            }
            return ret;
        }
        catch (URISyntaxException e)
        {
            throw new OREException("Object of rdfs:seeAlso is not a valid URI", e);
        }
    }

    public void addSeeAlso(URI uri)
    {
        res.addProperty(RDFS.seeAlso, model.createTypedLiteral(uri));
    }

    public void setSeeAlso(List<URI> uris)
    {
        this.clearSeeAlso();

        for (URI uri : uris)
        {
            this.addSeeAlso(uri);
        }
    }

    public void clearSeeAlso()
    {
        NodeIterator itr = model.listObjectsOfProperty(res, RDFS.seeAlso);
        while (itr.hasNext())
        {
            RDFNode node = itr.nextNode();
            model.removeAll(res, RDFS.seeAlso, node);
        }
    }*/

    public List<String> getNames()
    {
        List<String> names = new ArrayList<String>();
        StmtIterator itr = res.listProperties(FOAF.name);
        while (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            names.add(statement.getString());
        }
        return names;
    }

    public void setNames(List<String> names)
    {
        for (String name : names)
        {
            this.addName(name);
        }
    }

    public void addName(String name)
    {
        res.addProperty(FOAF.name, model.createTypedLiteral(name));
    }

    public List<URI> getMboxes()
			throws OREException
	{
		try
		{
			List<URI> mboxes = new ArrayList<URI>();
			StmtIterator itr = res.listProperties(FOAF.mbox);
			while (itr.hasNext())
			{
				Statement statement = itr.nextStatement();
				mboxes.add(new URI(((Resource) statement.getObject()).getURI()));
			}
			return mboxes;
		}
		catch (URISyntaxException e)
		{
			throw new OREException(e);
		}
	}

    public void setMboxes(List<URI> mboxes)
    {
        for (URI mbox : mboxes)
        {
            this.addMbox(mbox);
        }
    }

    public void addMbox(URI mbox)
    {
		// ensure the mbox value is an email URI
		/*
		if (!mbox.startsWith("mailto:"))
		{
			mbox = "mailto:" + mbox;
		}*/
		res.addProperty(FOAF.mbox, model.createResource(mbox.toString()));
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

	///////////////////////////////////////////////////////////////////
    // override methods from GraphResource
    ///////////////////////////////////////////////////////////////////

	/* moved up to OREResourceJena
	public void setResource(Resource resource)
    {
        StmtIterator itr = resource.listProperties();
        model.removeAll();
        model.add(itr);

        res = (Resource) resource.inModel(model);
    }*/

	// our version of setModel doesn't need to validate the URI
	public void setModel(Model model, URI resourceURI)
			throws OREException
	{
		this.model = model;
        this.res = model.createResource(resourceURI.toString());
    }
}
