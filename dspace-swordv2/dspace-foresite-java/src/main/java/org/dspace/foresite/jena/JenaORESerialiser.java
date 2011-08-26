/*
 * JenaORESerialiser.java
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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.dspace.foresite.ORESerialiser;
import org.dspace.foresite.ResourceMapDocument;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.ORESerialiserException;
import org.dspace.foresite.OREException;
import org.dspace.foresite.REMValidator;

import java.util.Properties;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

/**
 * @Author Richard Jones
 */
public class JenaORESerialiser implements ORESerialiser
{
    private String type = "RDF/XML";

	public ResourceMapDocument serialise(ResourceMap rem)
            throws ORESerialiserException
    {
        try
        {
			// make a clone so we can modify the resource map
			ResourceMap resMap = this.cloneResourceMap(rem);

			// prepare the map for serialisation, which will include setting required fields
			this.prepForSerialisation(resMap);

			// now we can serialise raw the valid resource map
			return this.serialiseRaw(resMap);
        }
        catch (OREException e)
        {
            throw new ORESerialiserException(e);
        }
    }

	public ResourceMapDocument serialiseRaw(ResourceMap resMap)
			throws ORESerialiserException
	{
		try
        {
			Model model = ((ResourceMapJena) resMap).getModel();
            OutputStream out = new ByteArrayOutputStream();
            model.write(out, this.type);

			// build the serialiation document
			ResourceMapDocument rmd = new ResourceMapDocument();
            rmd.setSerialisation(out.toString());
            rmd.setMimeType(this.getMimeType());
			rmd.setUri(resMap.getURI());

			return rmd;
        }
        catch (OREException e)
        {
            throw new ORESerialiserException(e);
        }
	}

	public void configure(Properties properties)
    {
        this.type = properties.getProperty("type");
	}

	///////////////////////////////////////////////////////////////////
	// private methods
	///////////////////////////////////////////////////////////////////


	// FIXME: some of this clearly belongs in some sort of Validator
	private void prepForSerialisation(ResourceMap rem)
			throws OREException
	{
		REMValidator validator = new REMValidatorJena();
		validator.prepForSerialisation(rem);
	}

	private String getMimeType()
    {
        if ("RDF/XML".equals(type))
        {
            // return "application/rdf+xml";
			return "text/xml";
		}
        // return "application/octet-stream";
		return "text/plain";
	}

	private ResourceMap cloneResourceMap(ResourceMap rem)
			throws OREException
	{
		Model model = ((ResourceMapJena) rem).getModel();
		StmtIterator itr = model.listStatements();
		Model nModel = ModelFactory.createDefaultModel();
		nModel.add(itr);
		ResourceMap nrem = JenaOREFactory.createResourceMap(nModel, rem.getURI());
		return nrem;
	}
}
