/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
