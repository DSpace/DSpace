/*
 * DCInputAuthority.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2009/07/23 05:07:01 $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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
/* Modified for LINDAT/CLARIN */

package org.dspace.content.authority;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * ChoiceAuthority source that reads the same input-forms which drive
 * configurable submission.
 *
 * Configuration:
 *   This MUST be configured aas a self-named plugin, e.g.:
 *     plugin.selfnamed.org.dspace.content.authority.ChoiceAuthority = \
 *        org.dspace.content.authority.DCInputAuthority
 *
 * It AUTOMATICALLY configures a plugin instance for each <value-pairs>
 * element (within <form-value-pairs>) of the input-forms.xml.  The name
 * of the instance is the "value-pairs-name" attribute, e.g.
 * the element: <value-pairs value-pairs-name="common_types" dc-term="type">
 * defines a plugin instance "common_types".
 *
 * IMPORTANT NOTE: Since these value-pairs do NOT include authority keys,
 * the choice lists derived from them do not include authority values.
 * So you should not use them as the choice source for authority-controlled
 * fields.
 */
public class OpenAIREAuthority implements ChoiceAuthority
{
    private static Logger log = Logger.getLogger(OpenAIREAuthority.class);
    private static final String Module = "openaire";
    private static final String FileProperty = "projects.filename";
    private static final String MinimumChars = "projects.suggest.minchar";
    private static final String MaxSuggests = "projects.suggest.max";
    private static final int MaxSuggestionLength = 66;

	static {
		try {
			load();
		} catch (Exception ex) {
			OpenAIREAuthority.log.error("Error",ex);
			OpenAIREAuthority.pairs = new ArrayList<Pair<String,String>>();
		}
	}
	
    private static List<Pair<String, String>> pairs = null;
    
    /**
     * Loads the data, parsing the file defined at the 'openaire.projects.filename' dspace
     * property.
     */
    @SuppressWarnings("unused")
	private static void load () {
    	if (OpenAIREAuthority.pairs == null) {
    		OpenAIREAuthority.pairs = new ArrayList<Pair<String, String>>();
    		String defsFile = ConfigurationManager.getProperty(OpenAIREAuthority.Module, OpenAIREAuthority.FileProperty);
    		if (defsFile == null) OpenAIREAuthority.log.error("Configuration "+OpenAIREAuthority.FileProperty+" not found in dspace.cfg file.");
    		else {
    			try {
        			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document doc = builder.parse(new File(defsFile));
					doc.getDocumentElement().normalize();
					
					NodeList nodes = doc.getElementsByTagName("pair");
					for (int i = 0; i < nodes.getLength(); i++) {
						Node node = nodes.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							// OK!
							Element element = (Element) node;
							Pair<String, String> pair = new Pair<String, String>();
							pair.fst = element.getElementsByTagName("displayed-value").item(0).getFirstChild().getNodeValue();
							pair.snd = element.getElementsByTagName("stored-value").item(0).getFirstChild().getNodeValue();
							OpenAIREAuthority.pairs.add(pair);
						} else 
							throw new Exception("Invalid Node readed");
					}
				} catch (Exception e) {
					OpenAIREAuthority.log.error("Unable to parse file "+defsFile, e);
				}
    		}
    	}
    }
    
    private static void reload(){
    	OpenAIREAuthority.pairs = null;
    	load();
    }

    private String getPrettyChoiceText (String text) {
    	if (text.length() > OpenAIREAuthority.MaxSuggestionLength) {
    		return text.substring(0, OpenAIREAuthority.MaxSuggestionLength - 3) + "...";
    	} else return text;
    }
    
	public Choices getMatches(String field, String text, int collection, int start,
			int limit, String locale) {
		OpenAIREAuthority.load();
		int minChars = ConfigurationManager.getIntProperty(OpenAIREAuthority.Module, OpenAIREAuthority.MinimumChars, 4);
		int maxSuggests = ConfigurationManager.getIntProperty(OpenAIREAuthority.Module, OpenAIREAuthority.MaxSuggests, 10);
		
        List<Choice> result = new ArrayList<Choice>();
        int dflt = -1;
        int type = Choices.CF_NOTFOUND;
        int added = 0;
        if (text != null && text.length() >= minChars) {
	        for (int i = 0; i < OpenAIREAuthority.pairs.size() && added < maxSuggests; ++i)
	        {
	        	if (OpenAIREAuthority.pairs.get(i).fst.toLowerCase().contains(text.toLowerCase()))
	        	{
	        		Choice cs = new Choice(String.valueOf(i), OpenAIREAuthority.pairs.get(i).snd, this.getPrettyChoiceText(OpenAIREAuthority.pairs.get(i).fst));
	        		result.add(cs);
	        		added++;
		            if (OpenAIREAuthority.pairs.get(i).fst.equalsIgnoreCase(text))
		                dflt = i;
	        	}
	        }
	        if (added > 0) type = Choices.CF_AMBIGUOUS;
	        else type = Choices.CF_NOTFOUND;
        }
        return new Choices(result.toArray(new Choice[] {}), 0, result.size(), type, false, dflt);
	}

	public Choices getBestMatch(String field, String text, int collection, String locale) {
		OpenAIREAuthority.load();
        for (int i = 0; i < OpenAIREAuthority.pairs.size(); ++i)
        {
            if (OpenAIREAuthority.pairs.get(i).fst.toLowerCase().equals(text.toLowerCase()))
            {
                Choice v[] = new Choice[1];
                v[0] = new Choice(String.valueOf(i), OpenAIREAuthority.pairs.get(i).snd, this.getPrettyChoiceText(OpenAIREAuthority.pairs.get(i).fst));
                return new Choices(v, 0, v.length, Choices.CF_UNCERTAIN, false, 0);
            }
        }
        return new Choices(Choices.CF_NOTFOUND);
	}

	public String getLabel(String field, String key, String locale) {
		OpenAIREAuthority.load();
        return OpenAIREAuthority.pairs.get(Integer.parseInt(key)).fst;
	}
	
	public static void main(String[] args){
		OpenAIREAuthority.reload();
	}
    
}

class Pair<T, V> {
	public T fst;
	public V snd;
}
