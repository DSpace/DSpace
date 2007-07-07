/*
 * DIMDisseminationCrosswalk.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/04/25 19:44:35 $
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

package org.dspace.app.xmlui.crosswalk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import java.sql.SQLException;

import org.dspace.core.Constants;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.authorize.AuthorizeException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

/**
 * This is a simple dissemination crosswalk that generates an item, community,
 * or collection's metadata in the DSpace Intermediate Metadata (DIM) format.
 * 
 * @author Scott Phillips
 */

public class DIMDisseminationCrosswalk extends SelfNamedPlugin implements
        DisseminationCrosswalk
{

    /**
     * DSpace XML Namespace in JDOM form.
     */
    public static final Namespace DIM_NS =
        Namespace.getNamespace("dim","http://www.dspace.org/xmlns/dspace/dim");
    
    
	
	
    /** Our namespace, DIM */
    //private static final Namespace namespaces[] = { XSLTCrosswalk.DIM_NS };

    
    /** 
     * When used in a self named context, use the following names 
     */
    public static String[] getPluginNames()
    {
        String[] names = {"DIM","dim"};
        return names;
    }
    
    /**
     * Disseminate the DSpace object as a single element.
     */
    public Element disseminateElement(DSpaceObject dso)
    	throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        if (!canDisseminate(dso))
            throw new CrosswalkObjectNotSupported("Unable to dissimenate DSpaceObject of unknown type: " + dso.getClass().getName());

        Element dim = internalDisseminate(dso);
        return dim;
    }

    /** 
     * Disseminate the DSpace object as a list of individual fields.
     */
    public List disseminateList(DSpaceObject dso) 
    	throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        Element dim = this.disseminateElement(dso);

        @SuppressWarnings("unchecked") // This cast is correct
        List<Element> fields = dim.getChildren();

        // Detach all the children from the dim element.
        for (Element field : fields)
            field.detach();

        return fields;
    }

    /** 
     * Return the namespaces used by this crosswalk.
     */
    public Namespace[] getNamespaces()
    {
    	Namespace[] namespaces = {DIM_NS};
        return namespaces;
    }

    /**
     * The schema location for this crosswalk.
     */
    public String getSchemaLocation()
    {
        return DIM_NS.getURI();
    }

    /**
     * Determine if the given DSO can be handled by this crosswalk.
     */
    public boolean canDisseminate(DSpaceObject dso)
    {
        if (dso.getType() == Constants.ITEM || 
        	dso.getType() == Constants.COLLECTION || 
        	dso.getType() == Constants.COMMUNITY)
            return true;
        else
            return false;
    }

    /**
     * This crosswalk prefers single element form.
     */
    public boolean preferList()
    {
        return false;
    }
    
    
    
    
    
    
    /**
     * Generate an intermediate representation of a DSpace object.
     * 
     * @param dso The dspace object to build a representation of.
     */
    private Element internalDisseminate(DSpaceObject dso)
    {
        Element dim = new Element("dim", DIM_NS);
        String type = Constants.typeText[dso.getType()];
        dim.setAttribute("dspaceType",type);
        
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item) dso;
            
            DCValue[] dcvs = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            for (int i = 0; i < dcvs.length; i++)
            {
                DCValue dcv = dcvs[i];
                Element field = 
                createField(dcv.schema, dcv.element, dcv.qualifier, 
                            dcv.language, dcv.value);
                
                dim.addContent(field);
            }
        } 
        else if (dso.getType() == Constants.COLLECTION) 
        {
            Collection collection = (Collection) dso;
            
            String description = collection.getMetadata("introductory_text");
            String description_abstract = collection.getMetadata("short_description");
            String description_table = collection.getMetadata("side_bar_text");
            String identifier_uri = "http://hdl.handle.net/" + collection.getHandle();
            String provenance = collection.getMetadata("provenance_description");
            String rights = collection.getMetadata("copyright_text");
            String rights_license = collection.getMetadata("license");
            String title = collection.getMetadata("name");
            
            dim.addContent(createField("dc","description",null,null,description));
            dim.addContent(createField("dc","description","abstract",null,description_abstract));
            dim.addContent(createField("dc","description","tableofcontents",null,description_table));
            dim.addContent(createField("dc","identifier","uri",null,identifier_uri));
            dim.addContent(createField("dc","provenance",null,null,provenance));
            dim.addContent(createField("dc","rights",null,null,rights));
            dim.addContent(createField("dc","rights","license",null,rights_license));
            dim.addContent(createField("dc","title",null,null,title));
        } 
        else if (dso.getType() == Constants.COMMUNITY) 
        {
            Community community = (Community) dso;
            
            String description = community.getMetadata("introductory_text");
            String description_abstract = community.getMetadata("short_description");
            String description_table = community.getMetadata("side_bar_text");
            String identifier_uri = "http://hdl.handle.net/" + community.getHandle();
            String rights = community.getMetadata("copyright_text");
            String title = community.getMetadata("name");
            
            dim.addContent(createField("dc","description",null,null,description));
            dim.addContent(createField("dc","description","abstract",null,description_abstract));
            dim.addContent(createField("dc","description","tableofcontents",null,description_table));
            dim.addContent(createField("dc","identifier","uri",null,identifier_uri));
            dim.addContent(createField("dc","rights",null,null,rights));
            dim.addContent(createField("dc","title",null,null,title));
        }
        
        return dim;
    }

    /**
     * Create a new DIM field element with the given attributes.
     * 
     * @param schema The schema the DIM field belongs too.
     * @param element The element the DIM field belongs too.
     * @param qualifier The qualifier the DIM field belongs too.
     * @param language The language the DIM field belongs too.
     * @param value The value of the DIM field.
     * @return A new DIM field element
     */
    private Element createField(String schema, String element, String qualifier, String language, String value)
    {
        Element field = new Element("field",DIM_NS);
        field.setAttribute("mdschema",schema);
        field.setAttribute("element",element);
        if (qualifier != null)
            field.setAttribute("qualifier",qualifier);
        if (language != null)
            field.setAttribute("language",language);
        
        // Check if the field's value contains XML. If it does then we want to parse
        // it into the dom, otherwise just escape it as a text string. First we will
        // check to see if the there are an equal number of < and > brackets. If so then
        // we will attempt to parse the output.
        

        int countOpen = countOccurances(value,'<');
        int countClose = countOccurances(value, '>');
        
        boolean success = false;
        if (countOpen == countClose)
        {
        	try {
	        	String xml = "<fragment>"+value+"</fragment>";
	     	   
	     	   	ByteArrayInputStream inputStream = new ByteArrayInputStream(xml.getBytes());
	     	   
		 	    SAXBuilder builder = new SAXBuilder();
				Document document = builder.build(inputStream);
				
				Element root = document.getRootElement();
				field.addContent(root.removeContent());
				success = true;
			} 
        	catch (Exception e) 
			{
        		// ignore any errors we get, and just add the string literaly.
			}
	 	       
        }
        
        if (success ==  false)
        {
        	// There is an unequal number of brackets, thus it's not 
        	// xml. Just pass it as a regular string.
        	field.setText(value);
        }
        
        return field;
    }  
    
    
    
    
    
    private int countOccurances(String string, char character)
    {
    	if (string == null || string.length() == 0)
    		return 0;
    	
    	int fromIndex = -1;
        int count = 0;
        
        while (true)
        {
        	fromIndex = string.indexOf('>', fromIndex+1);
        	
        	if (fromIndex == -1)
        		break;
        	
        	count++;
        }
        
        return count;
    }
    
    
    
    
}
