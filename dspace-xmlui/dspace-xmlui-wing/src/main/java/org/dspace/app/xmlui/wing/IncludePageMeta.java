/*
 * IncludePageMeta.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/04/25 15:29:51 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.xmlui.wing;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.xml.sax.SAXException;

/**
 * Include metadata in the resulting DRI document as derived from the sitemap
 * parameters.
 * 
 * Parameters should consist of a dublin core name and value. The format for 
 * a parameter name must follow the form: "<element>.<qualifier>.<language>#order"
 * The qualifier, language, and order are all optional components. The order
 * component is an integer and is needed to insure that parameter names are 
 * unique. Since Cocoon's parameters are Hashes duplicate names are not allowed
 * the order syntax allows the sitemap programer to specify an order in which 
 * these metadata values should be placed inside the document.
 * 
 * The following are a valid examples:
 * 
 * <map:parameter name="theme.name.en" value="My Theme"/>
 * 
 * <map:parameter name="theme.path" value="/MyTheme/"/>
 * 
 * <map:parameter name="theme.css#1" value="style.css"/>
 * 
 * <map:parameter name="theme.css#2" value="style.css-ie"/>
 * 
 * <map:parameter name="theme.css#2" value="style.css-ff"/>
 * 
 * @author Scott Phillips
 */
public class IncludePageMeta extends AbstractWingTransformer implements CacheableProcessingComponent
{

    /** The metadata loaded from the sitemap parameters. */
    private List<Metadata> metadataList;

    /**
     * Generate the unique key.
     * This key must be unique inside the space of this component.
     *
     * @return The generated key hashes the src
     */
    public Serializable getKey()
    {
        String key = "";
     
        for (Metadata metadata : metadataList)
        {
            key = "-" + metadata.getName() + "=" + metadata.getValue();
        }
        return HashUtil.hash(key);
    }

    /**
     * Generate the validity object.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    public SourceValidity getValidity()
    {
        return NOPValidity.SHARED_INSTANCE;
    }
    
    
    
    /**
     * Extract the metadata name value pairs from the sitemap parameters.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        try
        {
            String[] names = parameters.getNames();
            metadataList = new ArrayList<Metadata>();
            for (String name : names)
            {
            	String[] nameParts = name.split("#");
            	
            	String dcName = null;
            	int order = -1;
            	if (nameParts.length == 1)
            	{
            		dcName = nameParts[0];
            		order = 1;
            	}
            	else if (nameParts.length == 2)
            	{
            		dcName = nameParts[0];
            		order = Integer.valueOf(nameParts[1]);
            	}
            	else
            	{
            		throw new ProcessingException("Unable to parse page metadata name, '" + name + "', into parts.");
            	}
            	
                String[] dcParts = dcName.split("\\.");
                String element = null;
                String qualifier = null;
                String language = null;
                if (dcParts.length == 1)
                {
                    element = dcParts[0];
                }
                else if (dcParts.length == 2)
                {
                    element = dcParts[0];
                    qualifier = dcParts[1];
                }
                else if (dcParts.length == 3)
                {
                    element = dcParts[0];
                    qualifier = dcParts[1];
                    language = dcParts[2];
                }
                else
                {
                    throw new ProcessingException("Unable to parse page metadata name, '" + name + "', into parts.");
                }
            	
                String value = parameters.getParameter(name);
                
                Metadata metadata = new Metadata(element,qualifier,language,order,value);
                metadataList.add(metadata);
            }
            
            Collections.sort(metadataList);
        }
        catch (ParameterException pe)
        {
            throw new ProcessingException(pe);
        }
        
        // Initialize the Wing framework.
        try
        {
            this.setupWing();
        }
        catch (WingException we)
        {
            throw new ProcessingException(we);
        }
    }

    /**
     * Include the metadata in the page metadata.
     */
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {

        for (Metadata metadata : metadataList)
        {
        	String element = metadata.getElement();
        	String qualifier = metadata.getQualifier();
        	String language = metadata.getLanguage();
            String value = metadata.getValue();

            // Add our new metadata.
            pageMeta.addMetadata(element, qualifier, language)
                    .addContent(value);
        }
    }
    
    
    /**
     * Private class to keep track of metadata name/value pairs.
     */
    class Metadata implements Comparable<Metadata> {
    	
    	private String element;
    	private String qualifier;
    	private String language;
    	private int order;
    	private String value;
    	
    	public Metadata(String element,String qualifier, String language, int order, String value)
    	{
    		this.element = element;
    		this.qualifier = qualifier;
    		this.language = language;
    		this.order = order;
    		this.value = value;
    	}
    	
    	public String getElement()
    	{
    		return this.element;
    	}
    	
    	public String getQualifier()
    	{
    		return this.qualifier;
    	}
    	
    	public String getLanguage()
    	{
    		return this.language;
    	}
    	
    	public int getOrder()
    	{
    		return this.order;
    	}
    	
    	public String getName()
    	{
    		String name = this.element;
    		if (this.qualifier != null)
    		{
    			name += "." + this.qualifier;
    			if (this.language != null)
    			{
    				name += "." + this.language;
    			}
    		}
    		
    		name += "#" + order;
    		return name;
    	}
    	
    	public String getValue()
    	{
    		return this.value;
    	}
    	
    	
    	public int compareTo(Metadata other) 
    	{
    		String myName = this.element     + "." +this.qualifier   + "." + this.language;
    		String otherName = other.element + "." + other.qualifier + "." + other.language;
    		
    		int result = myName.compareTo(otherName);
    		if (result == 0)
    		{
    			if (this.order == other.order )
    			{
    				result = 0; // These two metadata element's names are completely identical.
    			}
    			else if (this.order > other.order)
    			{
    				result = 1; // The other metadata element belongs AFTER this element.
    			}
    			else 
    			{
    				result = -1; // The other metadata element belongs BEFORE this element.
    			}
    		}
    		
    		return result;
    	}
    }
  
}
