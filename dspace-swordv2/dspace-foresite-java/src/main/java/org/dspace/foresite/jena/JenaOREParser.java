/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite.jena;

import org.dspace.foresite.OREParser;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.OREParserException;
import org.dspace.foresite.OREException;

import java.io.InputStream;
import java.util.Properties;
import java.net.URI;
import java.net.URISyntaxException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @Author Richard Jones
 */
public class JenaOREParser implements OREParser
{
    private String type = "RDF/XML";

    public ResourceMap parse(InputStream is)
			throws OREParserException
    {
        try
        {
            Model model = this.parseToModel(is);
            Selector selector = new SimpleSelector(null, ORE.describes, (RDFNode) null);
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
            throw new OREParserException(e);
        }
		catch (OREException e)
		{
			throw new OREParserException(e);
		}
	}

	public ResourceMap parse(InputStream is, URI uri)
			throws OREParserException
	{
		try
		{
			Model model = this.parseToModel(is);
			ResourceMap rem = JenaOREFactory.createResourceMap(model, uri);
			return rem;
		}
		catch (OREException e)
		{
			throw new OREParserException(e);
		}
	}

	public void configure(Properties properties)
    {
        this.type = properties.getProperty("type");
    }

	///////////////////////////////////////////////////////////////////
	// Private methods
	///////////////////////////////////////////////////////////////////

	private Model parseToModel(InputStream is)
	{
		Model model = ModelFactory.createDefaultModel();
        model = model.read(is, null, type);
		return model;
	}
}
