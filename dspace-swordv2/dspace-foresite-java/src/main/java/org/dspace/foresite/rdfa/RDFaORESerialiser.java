/*
 * RDFaORESerialiser.java
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
package org.dspace.foresite.rdfa;

import org.dspace.foresite.ORESerialiser;
import org.dspace.foresite.ResourceMapDocument;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.ORESerialiserException;
import org.dspace.foresite.ORESerialiserFactory;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.ByteArrayInputStream;

/**
 * @Author Richard Jones
 */
public class RDFaORESerialiser implements ORESerialiser
{
    public ResourceMapDocument serialise(ResourceMap rem)
            throws ORESerialiserException
    {
        try
        {
            // first get hold of the resource map in RDF/XML
            ORESerialiser rdfxmlSerialiser = ORESerialiserFactory.getInstance("RDF/XML");
            ResourceMapDocument rmd = rdfxmlSerialiser.serialise(rem);

            // now get an XSLT transform on the go to convert to base RDFa
			TransformerFactory tFactory = TransformerFactory.newInstance();

			// FIXME: this doesn't seem all that reliable
			InputStream is = this.getClass().getResourceAsStream("rdfxml2rdfa.xsl");
			StreamSource xslt = new StreamSource(is);

			//Reader reader = new FileReader("/home/richard/workspace/dspace-trunk/ore4j/src/main/resources/rdfxml2rdfa.xsl");
            // StreamSource xslt = new StreamSource(reader);

			Transformer transformer = tFactory.newTransformer(xslt);

            String serialisation = rmd.getSerialisation();
            if (!serialisation.startsWith("<?xml"))
            {
                serialisation = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + serialisation;
            }
            StreamSource rdfxml = new StreamSource(new ByteArrayInputStream(serialisation.getBytes()));
            // StreamSource rdfxml = new StreamSource(serialisation);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            StreamResult result = new StreamResult(baos);
            transformer.transform(rdfxml, result);
            String baseRDFa = baos.toString();

            // now do some essential find and replace to fix the RDFa for page embedding
            String rx = ">[^<]+</a>";
            Pattern pattern = Pattern.compile(rx);
            Matcher matcher = pattern.matcher(baseRDFa);
            String preppedRDFa = matcher.replaceAll("></a>");

            // strip the xml document declaration from the front of the string
            int xmlHead = preppedRDFa.indexOf("?>");
            preppedRDFa = preppedRDFa.substring(xmlHead + 2);

            rmd.setSerialisation(preppedRDFa);

            return rmd;
        }
        catch (TransformerConfigurationException e)
        {
            throw new ORESerialiserException(e);
        }
        catch(TransformerException e)
        {
            throw new ORESerialiserException(e);
        }
        //catch (FileNotFoundException e)
        //{
        //    throw new ORESerialiserException(e);
        //}
    }

	public ResourceMapDocument serialiseRaw(ResourceMap rem) throws ORESerialiserException
	{
		return null;
	}

	public void configure(Properties properties)
    {

    }
}
