/*
 * OREIngestionCrosswalk.java
 *
 * Version: $Revision: 1 $
 *
 * Date: $Date: 2007-07-30 12:26:50 -0500 (Mon, 30 Jul 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

package org.dspace.content.crosswalk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * ORE ingestion crosswalk
 * <p>
 * Processes an Atom-encoded ORE resource map and attemps to interpret it as a DSpace item
 *
 * @author Alexey Maslov
 * @version $Revision: 1 $
 */
public class OREIngestionCrosswalk
    implements IngestionCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(OREDisseminationCrosswalk.class);

    /* Namespaces */
    public static final Namespace ATOM_NS =
        Namespace.getNamespace("atom", "http://www.w3.org/2005/Atom");
    private static final Namespace ORE_ATOM =
        Namespace.getNamespace("oreatom", "http://www.openarchives.org/ore/atom/");
    private static final Namespace ORE_NS =
        Namespace.getNamespace("ore", "http://www.openarchives.org/ore/terms/");
    private static final Namespace RDF_NS =
        Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    private static final Namespace DCTERMS_NS =
        Namespace.getNamespace("dcterms", "http://purl.org/dc/terms/");
    private static final Namespace DS_NS =
    	Namespace.getNamespace("ds","http://www.dspace.org/objectModel/");

    

	public void ingest(Context context, DSpaceObject dso, List metadata) throws CrosswalkException, IOException, SQLException, AuthorizeException {

		// If this list contains only the root already, just pass it on
		List<Element> elements = metadata;
		if (elements.size() == 1) {
			ingest(context, dso, elements.get(0));
		}
		// Otherwise, wrap them up 
		else {
			Element wrapper = new Element("wrap",elements.get(0).getNamespace());
			wrapper.addContent(elements);

			ingest(context,dso,wrapper);
		}
	}

	
	
	public void ingest(Context context, DSpaceObject dso, Element root) throws CrosswalkException, IOException, SQLException, AuthorizeException {
		
		Date timeStart = new Date();
		
		if (dso.getType() != Constants.ITEM)
            throw new CrosswalkObjectNotSupported("OREIngestionCrosswalk can only crosswalk an Item.");
        Item item = (Item)dso;
        
        if (root == null) {
        	System.err.println("The element received by ingest was null");
        	return;
        }
                
        Document doc = new Document();
        doc.addContent(root.detach());
        
        XPath xpathLinks;
        List<Element> aggregatedResources;
        String entryId;
		try {
			xpathLinks = XPath.newInstance("/atom:entry/atom:link[@rel=\"" + ORE_NS.getURI()+"aggregates" + "\"]");
			xpathLinks.addNamespace(ATOM_NS);
	        aggregatedResources = xpathLinks.selectNodes(doc);
	        
	        xpathLinks = XPath.newInstance("/atom:entry/atom:link[@rel='alternate']/@href");
	        xpathLinks.addNamespace(ATOM_NS);
	        entryId = ((Attribute)xpathLinks.selectSingleNode(doc)).getValue();
		} catch (JDOMException e) {
			throw new CrosswalkException("JDOM exception occured while ingesting the ORE");
		}

		// Next for each resource, create a bitstream
    	XPath xpathDesc;
    	NumberFormat nf=NumberFormat.getInstance(); 
		nf.setGroupingUsed(false);
		nf.setMinimumIntegerDigits(4);  
		
    	int countInt=0;
    	String count;
        for (Element resource : aggregatedResources) 
        {
        	countInt++;
        	count = nf.format((long)countInt);
        	String href = resource.getAttributeValue("href"); 
        	log.debug("ORE processing: " + href);
        	
        	String bundleName;
        	Element desc = null;
        	try {
        		xpathDesc = XPath.newInstance("/atom:entry/oreatom:triples/rdf:Description[@rdf:about=\"" + this.URLencode(href) + "\"][1]");
        		xpathDesc.addNamespace(ATOM_NS);
        		xpathDesc.addNamespace(ORE_ATOM);
        		xpathDesc.addNamespace(RDF_NS);
        		desc = (Element)xpathDesc.selectSingleNode(doc);
        	} catch (JDOMException e) {
        		e.printStackTrace();
        	}
        	
        	if (desc != null && desc.getChild("type", RDF_NS).getAttributeValue("resource", RDF_NS).equals(DS_NS.getURI() + "DSpaceBitstream"))
        	{
        		bundleName = desc.getChildText("description", DCTERMS_NS);
        		log.debug("Setting bundle name to: " + bundleName);
        	}
        	else {
        		log.info("Could not obtain bundle name; using 'ORIGINAL'");
        		bundleName = "ORIGINAL";
        	}
        	
        	// Bundle names are not unique, so we just pick the first one if there's more than one. 
        	Bundle[] targetBundles = item.getBundles(bundleName);
        	Bundle targetBundle;
        	
        	// if null, create the new bundle and add it in
        	if (targetBundles.length == 0) {
        		targetBundle = item.createBundle(bundleName);
        		item.addBundle(targetBundle);
        	}
        	else {
        		targetBundle = targetBundles[0];
        	}
        	
        	URL ARurl = null;
        	InputStream in = null;
        	if (href != null) {
        		try {
		        	// Make sure the url string escapes all the oddball characters
        			String processedURL = URLencode(href);
        			// Generate a requeset for the aggregated resource
        			ARurl = new URL(processedURL);
		        	in = ARurl.openStream();
        		}
        		catch(FileNotFoundException fe) {
            		log.error("The provided URI failed to return a resource: " + href);
            	}
        		catch(ConnectException fe) {
            		log.error("The provided URI was invalid: " + href);
            	}
        	}
        	else {
        		throw new CrosswalkException("Entry did not contain link to resource: " + entryId);
        	}
        	
        	// ingest and update
        	if (in != null) {
	        	Bitstream newBitstream = targetBundle.createBitstream(in);
	        	
	        	String bsName = resource.getAttributeValue("title");
	        	newBitstream.setName(bsName);
	        	
	            // Identify the format
	        	String mimeString = resource.getAttributeValue("type");
	        	BitstreamFormat bsFormat = BitstreamFormat.findByMIMEType(context, mimeString);
	        	if (bsFormat == null) {
	        		bsFormat = FormatIdentifier.guessFormat(context, newBitstream);
	        	}
	        	newBitstream.setFormat(bsFormat);
	            newBitstream.update();
	            
	            targetBundle.addBitstream(newBitstream);
	        	targetBundle.update();
        	}
        	else {
        		throw new CrosswalkException("Could not retrieve bitstream: " + entryId);
        	}
        	
        }
        log.info("OREIngest for Item "+ item.getID() + " took: " + (new Date().getTime() - timeStart.getTime()) + "ms."); 
	}
	
	
	/**
     * Helper method to escape all chaacters that are not part of the canon set 
     * @param sourceString source unescaped string
     */
    private String URLencode(String sourceString) {
    	Character lowalpha[] = {'a' , 'b' , 'c' , 'd' , 'e' , 'f' , 'g' , 'h' , 'i' ,
				'j' , 'k' , 'l' , 'm' , 'n' , 'o' , 'p' , 'q' , 'r' ,
				's' , 't' , 'u' , 'v' , 'w' , 'x' , 'y' , 'z'};
		Character upalpha[] = {'A' , 'B' , 'C' , 'D' , 'E' , 'F' , 'G' , 'H' , 'I' ,
                'J' , 'K' , 'L' , 'M' , 'N' , 'O' , 'P' , 'Q' , 'R' ,
                'S' , 'T' , 'U' , 'V' , 'W' , 'X' , 'Y' , 'Z'};
		Character digit[] = {'0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' , '8' , '9'};
		Character mark[] = {'-' , '_' , '.' , '!' , '~' , '*' , '\'' , '(' , ')'};
		
		// reserved
		Character reserved[] = {';' , '/' , '?' , ':' , '@' , '&' , '=' , '+' , '$' , ',' ,'%', '#'};
		
		Set<Character> URLcharsSet = new HashSet<Character>();
		URLcharsSet.addAll(Arrays.asList(lowalpha));
		URLcharsSet.addAll(Arrays.asList(upalpha));
		URLcharsSet.addAll(Arrays.asList(digit));
		URLcharsSet.addAll(Arrays.asList(mark));
		URLcharsSet.addAll(Arrays.asList(reserved));
		
		String processedString = new String();
		for (int i=0; i<sourceString.length(); i++) {
			char ch = sourceString.charAt(i);
			if (URLcharsSet.contains(ch)) {
				processedString += ch;
			}
			else {
				processedString += "%" + Integer.toHexString((int)ch);
			}
		}
		
		return processedString;
    }
	
}
