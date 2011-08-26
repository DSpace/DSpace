/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite.jena;

import org.dspace.foresite.Triple;
import org.dspace.foresite.OREResource;
import org.dspace.foresite.Predicate;
import org.dspace.foresite.OREException;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Literal;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @Author Richard Jones
 */
public class TripleJena implements Triple
{
    Model model;

    Resource triple;

    public TripleJena()
    {
        model = ModelFactory.createDefaultModel();
    }

    ///////////////////////////////////////////////////////////////////
    // Methods from Triple
    ///////////////////////////////////////////////////////////////////

    public void initialise(URI uri)
    {
        triple = model.createResource(uri.toString());
    }

    public void initialise(OREResource resource)
            throws OREException
    {
        triple = model.createResource(resource.getURI().toString());
    }

    public void relate(Predicate pred, URI uri) throws OREException
    {
        Property property = model.createProperty(pred.getURI().toString());
        triple.addProperty(property, model.createResource(uri.toString()));
    }

    public void relate(Predicate pred, OREResource resource) throws OREException
    {
        Property property = model.createProperty(pred.getURI().toString());
        triple.addProperty(property, model.createResource(resource.getURI().toString()));
    }

    public void relate(Predicate pred, Object literal) throws OREException
    {
        Property property = model.createProperty(pred.getURI().toString());
        triple.addProperty(property, model.createTypedLiteral(literal));
    }

    public OREResource getSubject()
    {
        return null;
    }

    public URI getSubjectURI()
            throws OREException
    {
        try
        {
            return new URI(triple.getURI());
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public Predicate getPredicate()
            throws OREException
    {
        try
        {
            StmtIterator itr = triple.listProperties();
            if (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                Property property = statement.getPredicate();
                Predicate pred = new Predicate();
                pred.setURI(new URI(property.getURI()));
                return pred;
            }
            return null;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public OREResource getObject()
    {
        return null;
    }

    public URI getObjectURI()
            throws OREException
    {
        try
        {
            StmtIterator itr = triple.listProperties();
            if (itr.hasNext())
            {
                Statement statement = itr.nextStatement();
                RDFNode node = statement.getObject();
                if (node instanceof Literal)
                {
                    throw new OREException("Cannot get URI; object is Literal");
                }
                return new URI(((Resource) node).getURI());
            }
            return null;
        }
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public String getObjectLiteral()
            throws OREException
    {
        StmtIterator itr = triple.listProperties();
        if (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            RDFNode node = statement.getObject();
            if (node instanceof Resource)
            {
                throw new OREException("Cannot get Literal; object is a Resource");
            }
            return ((Literal) node).getLexicalForm();
        }
        return null;
    }

    public String getLiteralType()
            throws OREException
    {
        StmtIterator itr = triple.listProperties();
        if (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            RDFNode node = statement.getObject();
            if (node instanceof Resource)
            {
                throw new OREException("Cannot get Literal type; object is a Resource");
            }
            return ((Literal) node).getDatatypeURI();
        }
        return null;
    }

    public boolean isLiteral()
    {
        StmtIterator itr = triple.listProperties();
        if (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            RDFNode node = statement.getObject();
            if (node instanceof Resource)
            {
                return false;
            }
            return true;
        }
        return false;
    }
}
