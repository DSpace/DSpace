/*
 * TripleJena.java
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
