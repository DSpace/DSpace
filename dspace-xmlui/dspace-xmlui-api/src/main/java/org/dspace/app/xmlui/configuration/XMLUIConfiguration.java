/*
 * XMLUIConfiguration.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/01/11 03:06:43 $
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

package org.dspace.app.xmlui.configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * This class reads the XMLUI configuration file.
 * 
 * @author Scott Phillips
 */

public class XMLUIConfiguration
{

    /** log4j category */
    private static Logger log = Logger.getLogger(XMLUIConfiguration.class);
    
    /** The configured Aspects */
    private static List<Aspect> aspects = new ArrayList<Aspect>();

    /** The configured Theme rules */
    private static List<Theme> themes = new ArrayList<Theme>(); 

    /**
     * Initialize the XMLUI Configuration.
	 *
	 * Load and parse the xmlui.xconf configuration file for a list of 
     * installed aspects and themes. Multiple configuration paths may be 
     * supplied but only the first valid file (exists and readable) will
     * be used.
     * 
     * @param configPath Multiple paths configuration paths may be specified
     */
    public static void loadConfig(String ... configPaths) throws IOException,
            JDOMException
    {
        if (configPaths == null || configPaths.length == 0)
            throw new IllegalStateException(
                    "The xmlui configuration path must be defined.");

        File configFile = null;
        
        for (String configPath : configPaths )
        {
        	if (configPath != null)
        		configFile = new File(configPath);
        	
        	if (configFile != null && configFile.exists() && configFile.canRead())
        	{
        		log.info("Loading XMLUI configuration from: "+configPath);
        		break;
        	}
        	else
        	{
        		log.debug("Faild to load XMLUI configuration from: "+configPath);
        	}
        }
        
        if (configFile == null)
        {
        	String allPaths = "";
        	boolean first = true;
        	for (String configPath : configPaths)
        	{
        		if (first)
        			first = false;
        		else
        			allPaths += ", ";
        		allPaths += configPath;
        	}
        	throw new IllegalStateException("None of the xmlui configuration paths were valid: "+ allPaths);
        }
        
        // FIXME: Sometime in the future require that the xmlui.xconf be valid. 
        // SAXBuilder builder = new SAXBuilder(true);
        SAXBuilder builder = new SAXBuilder();
        Document config = builder.build(configFile);

        @SuppressWarnings("unchecked") // This cast is correct
        List<Element> aspectElements = XPath.selectNodes(config,
                "//xmlui/aspects/aspect");

        @SuppressWarnings("unchecked") // This cast is correct
        List<Element> themeElements = XPath.selectNodes(config,
                "//xmlui/themes/theme");

        for (Element aspectElement : aspectElements)
        {
            String path = aspectElement.getAttributeValue("path");
            String name = aspectElement.getAttributeValue("name");

            if (path == null || path.length() == 0)
                throw new IllegalStateException(
                        "All aspects muth define a path");
            aspects.add(new Aspect(name, path));
            log.info("Aspect Installed: name='"+name+"', path='"+path+"'.");
        }
        // Put them in the order that people expect.
        Collections.reverse(aspects);
        

        for (Element themeElement : themeElements)
        {
            String name = themeElement.getAttributeValue("name");
            String path = themeElement.getAttributeValue("path");
            String id = themeElement.getAttributeValue("id");
            String regex = themeElement.getAttributeValue("regex");
            String handle = themeElement.getAttributeValue("handle");

            if (path == null || path.length() == 0)
                throw new IllegalStateException("All themes muth define a path");
            themes.add(new Theme(name, path, id, regex, handle));
            log.info("Theme Installed: name='"+name+"', path='"+path+"', id='"+id+"', regex='"+regex+"', handle='"+handle+"'.");
        }
    }

    /**
     * 
     * @return The configured Aspect chain.
     */
    public static List<Aspect> getAspectChain() {
        return aspects;
    }
    
    /**
     * 
     * @return The configured Theme rules.
     */
    public static List<Theme> getThemeRules() {
        return themes;
    }
    
    
}
