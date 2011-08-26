/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite.jena;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.AnonId;

import java.net.URI;

import org.dspace.foresite.OREException;

/**
 * @Author Richard Jones
 */
public interface GraphResource
{
    Resource getResource();

    void setResource(Resource resource);

    Model getModel();

    void setModel(Model model, URI resourceURI) throws OREException;

	void setModel(Model model, AnonId blankID) throws OREException;
}
