/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
    private static final Logger log = Logger.getLogger(XMLUIConfiguration.class);
    
    /** The configured Aspects */
    private static final List<Aspect> aspects = new ArrayList<>();

    /** The configured Theme rules */
    private static final List<Theme> themes = new ArrayList<>();

    /**
     * Initialize the XMLUI Configuration.
	 *
	 * Load and parse the xmlui.xconf configuration file for a list of 
     * installed aspects and themes. Multiple configuration paths may be 
     * supplied but only the first valid file (exists and readable) will
     * be used.
     * 
     * @param configPaths Multiple configuration paths may be specified.
     * @throws java.io.IOException if file access fails.
     * @throws org.jdom.JDOMException if file cannot be parsed.
     */
    public static void loadConfig(String ... configPaths)
            throws IOException, JDOMException
    {
        if (configPaths == null || configPaths.length == 0)
        {
            throw new IllegalStateException("The xmlui configuration path must be defined.");
        }

        File configFile = null;
        
        for (String configPath : configPaths )
        {
        	if (configPath != null)
            {
                configFile = new File(configPath);
            }
        	
        	if (configFile != null && configFile.exists() && configFile.canRead())
        	{
        		log.info("Loading XMLUI configuration from: "+configPath);
        		break;
        	}
        	else
        	{
        		log.debug("Failed to load XMLUI configuration from: "+configPath);
        	}
        }
        
        if (configFile == null)
        {
        	StringBuilder allPaths = new StringBuilder();
        	for (String configPath : configPaths)
        	{
        		if (allPaths.length() > 0)
                {
        			allPaths.append(", ");
                }
                
        		allPaths.append(configPath);
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
            {
                throw new IllegalStateException("All aspects muth define a path");
            }
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
            {
                throw new IllegalStateException("All themes muth define a path");
            }
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
