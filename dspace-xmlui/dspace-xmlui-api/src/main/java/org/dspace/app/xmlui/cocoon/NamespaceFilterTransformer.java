/*
 * NamespaceFilterTransformer.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2005/11/01 21:10:38 $
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

package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.transformation.AbstractTransformer;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Attributes2Impl;

/**
 * Filter out an unwanted namespace from the pipeline. Any elements or attributes
 * which use this namespaces will be removed from pipeline.
 * 
 * <map:transformer type="NamespaceFilterTransformer" src="http://apache.org/cocoon/i18n/2.1"/>
 * 
 * @author Scott Phillips
 */

public class NamespaceFilterTransformer extends AbstractTransformer implements CacheableProcessingComponent
{

	/** The namespace to be filtered out of the document */
	private String filterNamespace;
	
	/** The prefixes used to identified the namespace */
	private Stack<String> filterPrefixes = new Stack<String>();
	
	
	/**
	 * Return the cache key
	 */
	public Serializable getKey() {
		if (filterNamespace != null)
			return filterNamespace;
		else 
			return "1";
	}


	/**
	 * This cache never invalidates, always return a validating cache.
	 */
	public SourceValidity getValidity() {
		// Always returned cached;
		return NOPValidity.SHARED_INSTANCE;
	}
	
	
	/**
	   * Setup the processing instruction transformer. The only parameter that
	   * matters in the src parameter which should be the path to an XSL
	   * stylesheet to be applied by the clients browser.
	   * 
	   * Setup the namespace filter by getting a list of namespaces to be filtered
	   * from the pipeline.
	   */
	  public void setup(SourceResolver resolver, Map objectModel, String src,
	          Parameters parameters) throws ProcessingException, SAXException,
	          IOException
	  {
	  		filterNamespace = src;
	  		filterPrefixes.clear();
	  }
	
	
	/** Should this namespace be filtered out of the document? */
	private boolean filter(String test)
	{
		return (filterNamespace != null && filterNamespace.equals(test));
	} 
	
	/** Should this prefix be filtered out of the document? */
	private boolean filterPrefix(String test)
	{
		if (filterPrefixes.isEmpty())
			return false;
		String peek = filterPrefixes.peek();
		return (peek != null && peek.equals(test));
	}
	  
	  /**
     * Begin the scope of a prefix-URI Namespace mapping.
     *
     * @param prefix The Namespace prefix being declared.
     * @param uri The Namespace URI the prefix is mapped to.
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException 
    {
    	if (filter(uri))
    	{
    		filterPrefixes.push(prefix);
    	}
    	else
    	{
    		contentHandler.startPrefixMapping(prefix, uri);
    	}
    }

    /**
     * End the scope of a prefix-URI mapping.
     *
     * @param prefix The prefix that was being mapping.
     */
    public void endPrefixMapping(String prefix) throws SAXException 
    {
    	if (filterPrefix(prefix))
    	{
    		filterPrefixes.pop();
    	}
    	else
    	{
    		contentHandler.endPrefixMapping(prefix);
    	}
    }
    
    
    /**
     * Receive notification of the beginning of an element.
     */
    public void startElement(String uri, String loc, String raw, Attributes a) throws SAXException 
    {
	    // Reset the namespace context flag.
    	if (!filter(uri))
    	{
    		List<Integer> filterAttributeIndexs = new ArrayList<Integer>();
    		
    		for (int i = 0; i < a.getLength(); i++)
    		{
    			if (filter(a.getURI(i)))
    				// Add it to the list of filtered indexes.
    				filterAttributeIndexs.add(i);
    		}
    		
    		// Check if any of the attributes are to be filtered if so, filter them out.
    		if (!filterAttributeIndexs.isEmpty())
    		{
    			// New set of attributes.
    			Attributes2Impl a2 = new Attributes2Impl();
    			
    			for (int i = 0; i < a.getLength(); i++)
    			{
    				if (filterAttributeIndexs.contains(i))
    					// This index is to be filtered.
    					continue;
    				
	    			String a_uri = a.getURI(i);
	    			String a_localName = a.getLocalName(i);
	    			String a_qName = a.getQName(i);
	    			String a_type = a.getType(i);
	    			String a_value = a.getValue(i);
	    			
	    			// Add the new attribute
	    			a2.addAttribute(a_uri, a_localName, a_qName, a_type, a_value);
    			}
    			
    			// Use our new filtered attributes.
    			a = a2;
    		}
    		
    		contentHandler.startElement(uri, loc, raw, a);
    	}
    }


    /**
     * Receive notification of the end of an element.
     */
    public void endElement(String uri, String loc, String raw) throws SAXException 
    {
    	if (!filter(uri))
    	{
    		contentHandler.endElement(uri, loc, raw);
    	}
    }
    
}
