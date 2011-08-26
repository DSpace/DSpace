/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
