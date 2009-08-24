/*
 * OREDisseminationCrosswalk.java
 *
 * Version: $Revision: 2108 $
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
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
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
//import org.dspace.core.Utils;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * ORE dissemination crosswalk
 * <p>
 * Produces an Atom-encoded ORE aggregation of a DSpace item.
 *
 * @author Alexey Maslov
 * @version $Revision: 1 $
 */
public class OREDisseminationCrosswalk
    implements DisseminationCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(OREDisseminationCrosswalk.class);

    /* Schema for Atom only available in Relax NG format */
    public static final String ATOM_RNG = "http://tweety.lanl.gov/public/schemas/2008-06/atom-tron.sch";
    
    /* Namespaces */
    public static final Namespace ATOM_NS =
        Namespace.getNamespace("atom", "http://www.w3.org/2005/Atom");
    private static final Namespace ORE_NS =
        Namespace.getNamespace("ore", "http://www.openarchives.org/ore/terms/");
    private static final Namespace ORE_ATOM =
        Namespace.getNamespace("oreatom", "http://www.openarchives.org/ore/atom/");
    private static final Namespace RDF_NS =
        Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    private static final Namespace DCTERMS_NS =
        Namespace.getNamespace("dcterms", "http://purl.org/dc/terms/");
    private static final Namespace DS_NS =
    	Namespace.getNamespace("ds","http://www.dspace.org/objectModel/");

    private static final Namespace namespaces[] = { ATOM_NS, ORE_NS, ORE_ATOM, RDF_NS, DCTERMS_NS, DS_NS };

    
    public Namespace[] getNamespaces()
    {
        return namespaces;
    }

    /* There is (and currently can be) no XSD schema that validates Atom feeds, only RNG */ 
    public String getSchemaLocation()
    {
        return ATOM_NS.getURI() + " " + ATOM_RNG;
    }
    
    /**
     * Disseminate an Atom-encoded ORE ReM mapped from a DSpace Item
     * @param item 
     * @return
     * @throws CrosswalkException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    private Element disseminateItem(Item item) throws CrosswalkException, IOException, SQLException, AuthorizeException 
    {
    	String oaiUrl = null;
        String dsUrl = ConfigurationManager.getProperty("dspace.url");
        
        String remSource = ConfigurationManager.getProperty("ore.authoritative.source");
    	if (remSource == null || remSource.equalsIgnoreCase("oai")) 
    		oaiUrl = ConfigurationManager.getProperty("dspace.oai.url");
    	else if (remSource.equalsIgnoreCase("xmlui") || remSource.equalsIgnoreCase("manakin"))
    		oaiUrl = dsUrl;
    	
    	if (oaiUrl == null)
    		throw new CrosswalkInternalException("Base uri for the ore generator has not been set. Check the ore.authoritative.source setting.");
        
    	String uriA = oaiUrl + "/metadata/handle/" + item.getHandle() + "/ore.xml";
    	
        // Top level atom feed element 
        Element aggregation = new Element("entry",ATOM_NS);
        aggregation.addNamespaceDeclaration(ATOM_NS);
        aggregation.addNamespaceDeclaration(ORE_NS);
        aggregation.addNamespaceDeclaration(ORE_ATOM);
        aggregation.addNamespaceDeclaration(DCTERMS_NS);

        // Atom-entry specific info
        Element atomId = new Element("id",ATOM_NS);
        atomId.addContent(uriA);
        aggregation.addContent(atomId);
        
        Element aggLink;
        DCValue[] uris = item.getMetadata(MetadataSchema.DC_SCHEMA,"identifier","uri",Item.ANY);
        for (DCValue uri : uris) {
        	aggLink = new Element("link",ATOM_NS);
        	aggLink.setAttribute("rel", "alternate");
        	aggLink.setAttribute("href", uri.value);
            aggregation.addContent(aggLink);
        }
        
        // Information about the resource map, as separate entity from the aggregation it describes
        Element uriALink = new Element("link",ATOM_NS);
        uriALink.setAttribute("rel", "http://www.openarchives.org/ore/terms/describes");
        uriALink.setAttribute("href", uriA);
        
        Element uriRLink = new Element("link",ATOM_NS);
        uriRLink.setAttribute("rel","self");
        uriRLink.setAttribute("href", uriA + "#atom");
        uriRLink.setAttribute("type","application/atom+xml");
        
        Element remPublished = new Element("published",ATOM_NS);
        remPublished.addContent(Utils.formatISO8601Date(new Date()));
        Element remUpdated = new Element("updated",ATOM_NS);
        remUpdated.addContent(Utils.formatISO8601Date(new Date()));
        
        Element remCreator = new Element("source",ATOM_NS);
        Element remGenerator = new Element("generator",ATOM_NS);
        remGenerator.addContent(ConfigurationManager.getProperty("dspace.name"));
        remGenerator.setAttribute("uri", oaiUrl);
        remCreator.addContent(remGenerator);
        
        aggregation.addContent(uriALink);
        aggregation.addContent(uriRLink);
        aggregation.addContent(remPublished);
        aggregation.addContent(remUpdated);
        aggregation.addContent(remCreator);
        
        // Information about the aggregation (item) itself 
        Element aggTitle = new Element("title",ATOM_NS);
        DCValue[] titles = item.getMetadata(MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
        if (titles != null && titles.length>0)
        	aggTitle.addContent(titles[0].value);
        else
        	aggTitle.addContent("");
        aggregation.addContent(aggTitle);
        
        Element aggAuthor;
        Element aggAuthorName;
        DCValue[] authors = item.getMetadata(MetadataSchema.DC_SCHEMA,"contributor","author",Item.ANY);
        for (DCValue author : authors) {
        	aggAuthor = new Element("author",ATOM_NS);
        	aggAuthorName = new Element("name",ATOM_NS);
        	aggAuthorName.addContent(author.value);
        	aggAuthor.addContent(aggAuthorName);
        	aggregation.addContent(aggAuthor);
        }
        
        Element oreCategory = new Element("category",ATOM_NS);
        oreCategory.setAttribute("scheme", ORE_NS.getURI());
        oreCategory.setAttribute("term", ORE_NS.getURI()+"Aggregation");
        oreCategory.setAttribute("label","Aggregation");
                
        Element updateCategory = new Element("category",ATOM_NS);
        updateCategory.setAttribute("scheme", ORE_ATOM.getURI()+"modified");
        updateCategory.setAttribute("term", Utils.formatISO8601Date(item.getLastModified()));
        
        Element dsCategory = new Element("category",ATOM_NS);
        dsCategory.setAttribute("scheme", DS_NS.getURI());
        dsCategory.setAttribute("term", "DSpaceItem");
        dsCategory.setAttribute("label", "DSpace Item");
        
        aggregation.addContent(oreCategory);
        aggregation.addContent(updateCategory);
        aggregation.addContent(dsCategory);
        
        
        // metadata section
        Element arLink;
        Element rdfDescription, rdfType, dcModified, dcDesc;
        Element triples = new Element("triples", ORE_ATOM);
        
        // metadata about the item
        rdfDescription = new Element("Description", RDF_NS);
        rdfDescription.setAttribute("about", uriA, RDF_NS);
        
        rdfType = new Element("type", RDF_NS);
        rdfType.setAttribute("resource", DS_NS.getURI()+"DSpaceItem", RDF_NS);
        dcModified = new Element("modified", DCTERMS_NS);
        dcModified.addContent(Utils.formatISO8601Date(item.getLastModified()));
        
        rdfDescription.addContent(rdfType);
        rdfDescription.addContent(dcModified);
        triples.addContent(rdfDescription);
        
        // Add a link and an oreatom metadata entry for each bitstream in the item
        Bundle[] bundles = item.getBundles();
        Bitstream[] bitstreams;
        for (Bundle bundle : bundles) 
        {
        	// Omit the special "ORE" bitstream
        	if (bundle.getName().equals("ORE"))
        		continue;
        	
        	bitstreams = bundle.getBitstreams();
        	for (Bitstream bs : bitstreams) 
        	{
        		arLink = new Element("link",ATOM_NS);
        		arLink.setAttribute("rel", ORE_NS.getURI()+"aggregates");
        		arLink.setAttribute("href",dsUrl + "/bitstream/handle/" + item.getHandle() + "/" + URLencode(bs.getName()) + "?sequence=" + bs.getSequenceID());
        		arLink.setAttribute("title",bs.getName());
        		arLink.setAttribute("type",bs.getFormat().getMIMEType());
        		arLink.setAttribute("length",Long.toString(bs.getSize()));
        		
        		aggregation.addContent(arLink);
        		
        		// metadata about the bitstream
                rdfDescription = new Element("Description", RDF_NS);
                rdfDescription.setAttribute("about", dsUrl + "/bitstream/handle/" + item.getHandle() + "/" + URLencode(bs.getName()) + "?sequence=" + bs.getSequenceID(), RDF_NS);
                
                rdfType = new Element("type", RDF_NS);
                rdfType.setAttribute("resource", DS_NS.getURI()+"DSpaceBitstream", RDF_NS);
                dcDesc = new Element("description", DCTERMS_NS);
                dcDesc.addContent(bundle.getName());
                
                rdfDescription.addContent(rdfType);
                rdfDescription.addContent(dcDesc);
                triples.addContent(rdfDescription);
        	}
        }
        
        aggregation.addContent(triples);
        
        // Add a link to the OAI-PMH served metadata (oai_dc is always on) 
        /*
        Element pmhMeta = new Element("entry",ATOM_NS);

        pUri = new Element("id",ATOM_NS);
        String oaiId = new String("oai:" + ConfigurationManager.getProperty("dspace.hostname") + ":" + item.getHandle());
		pUri.addContent(oaiId + "#oai_dc");
		pmhMeta.addContent(pUri);
		
		Element pmhAuthor = new Element("author",ATOM_NS);
		Element pmhAuthorName = new Element("name",ATOM_NS);
		Element pmhAuthorUri = new Element("uri",ATOM_NS);
		pmhAuthorName.addContent(ConfigurationManager.getProperty("dspace.name"));
		pmhAuthorUri.addContent(oaiUrl);
		pmhAuthor.addContent(pmhAuthorName);
		pmhAuthor.addContent(pmhAuthorUri);
		pmhMeta.addContent(pmhAuthor);
		
		arUri = new Element("link",ATOM_NS);
		arUri.setAttribute("rel","alternate");
		arUri.setAttribute("href",oaiUrl + "/request?verb=GetRecord&amp;identifier=" + oaiId + "&amp;metadataprefix=oai_dc");
		pmhMeta.addContent(arUri);
		
		Element rdfDesc = new Element("Description",RDF_NS);
		rdfDesc.setAttribute("about",oaiUrl + "/request?verb=GetRecord&amp;identifier=" + oaiId + "&amp;metadataprefix=oai_dc",RDF_NS);
		Element dcTerms = new Element("dcterms",DCTERMS_NS);
		dcTerms.setAttribute("resource","http://www.openarchives.org/OAI/2.0/oai_dc/",RDF_NS);
		rdfDesc.addContent(dcTerms);
		pmhMeta.addContent(rdfDesc);
		
		arUpdated = new Element("updated",ATOM_NS);
		arUpdated.addContent(Utils.formatISO8601Date(item.getLastModified()));
		pmhMeta.addContent(arUpdated);
		
		arTitle = new Element("title",ATOM_NS);
		arTitle.addContent("");
		pmhMeta.addContent(arTitle);
		
		aggregation.addContent(pmhMeta);*/
        
        return aggregation;
    }
    
    public Element disseminateElement(DSpaceObject dso)	throws CrosswalkException, IOException, SQLException, AuthorizeException 
	{
    	switch(dso.getType()) {
	    	case Constants.ITEM: return disseminateItem((Item)dso);
	    	case Constants.COLLECTION: break;
	    	case Constants.COMMUNITY: break;
	    	default: throw new CrosswalkObjectNotSupported("ORE implementation unable to disseminate unknown DSpace object.");
    	}
    	
		return null;
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
		//Character reserved[] = {';' , '/' , '?' , ':' , '@' , '&' , '=' , '+' , '$' , ',' ,'%', '#'};
		
		Set<Character> URLcharsSet = new HashSet<Character>();
		URLcharsSet.addAll(Arrays.asList(lowalpha));
		URLcharsSet.addAll(Arrays.asList(upalpha));
		URLcharsSet.addAll(Arrays.asList(digit));
		URLcharsSet.addAll(Arrays.asList(mark));
		//URLcharsSet.addAll(Arrays.asList(reserved));
		
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
    
   
    public List disseminateList(DSpaceObject dso) throws CrosswalkException, IOException, SQLException, AuthorizeException
	{
	    List result = new ArrayList(1);
	    result.add(disseminateElement(dso));
	    return result;
	}

    /* Only interested in disseminating items at this time */
    public boolean canDisseminate(DSpaceObject dso)
    {
    	if (dso.getType() == Constants.ITEM || dso.getType() == Constants.COLLECTION || dso.getType() == Constants.COMMUNITY)
    		return true;
    	else
    		return false;
    }

    public boolean preferList()
    {
        return false;
    }
	
}
