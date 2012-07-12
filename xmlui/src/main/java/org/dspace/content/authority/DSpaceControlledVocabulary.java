/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.SelfNamedPlugin;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ar.edu.unlp.sedici.dspace.utils.MailReporter;

/**
 * ChoiceAuthority source that reads the JSPUI-style hierarchical vocabularies
 * from ${dspace.dir}/config/controlled-vocabularies/*.xml and turns them into
 * autocompleting authorities.
 *
 * Configuration:
 *   This MUST be configured as a self-named plugin, e.g.:
 *     plugin.selfnamed.org.dspace.content.authority.ChoiceAuthority = \
 *        org.dspace.content.authority.DSpaceControlledVocabulary
 *
 * It AUTOMATICALLY configures a plugin instance for each XML file in the
 * controlled vocabularies directory. The name of the plugin is the basename
 * of the file; e.g., "${dspace.dir}/config/controlled-vocabularies/nsi.xml"
 * would generate a plugin called "nsi".
 *
 * Each configured plugin comes with three configuration options:
 *   vocabulary.plugin._plugin_.hierarchy.store = <true|false>    # Store entire hierarchy along with selected value. Default: TRUE
 *   vocabulary.plugin._plugin_.hierarchy.suggest = <true|false>  # Display entire hierarchy in the suggestion list.  Default: TRUE
 *   vocabulary.plugin._plugin_.delimiter = "<string>"              # Delimiter to use when building hierarchy strings. Default: "::"
 *
 *
 * @author Michael B. Klein
 *
 */

public class DSpaceControlledVocabulary extends SelfNamedPlugin implements ChoiceAuthority
{

	private static Logger log = Logger.getLogger(DSpaceControlledVocabulary.class);
    private static String xpathTemplate = "//isComposedBy/node[contains(translate(@label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'%s')]";
    private static String idTemplate = "//isComposedBy/node[@id = '%s']";
    private static String pluginNames[] = null;

    private String vocabularyName = null;
    private InputSource vocabulary = null;
    private Boolean suggestHierarchy = true;
    private Boolean storeHierarchy = true;
    private String hierarchyDelimiter = "::";

    protected static final int MAX_PAGE_SIZE=40;
    protected static final int DEFAULT_PAGE_SIZE=20;

    public DSpaceControlledVocabulary()
    {
    	super();
    }

    public static String[] getPluginNames()
    {
        if (pluginNames == null)
        {
            initPluginNames();
        }
        
        return (String[]) ArrayUtils.clone(pluginNames);
    }

    private static synchronized void initPluginNames()
    {
        if (pluginNames == null)
        {
        	class xmlFilter implements java.io.FilenameFilter
            {
        		public boolean accept(File dir, String name)
                {
        			return name.endsWith(".xml");
        		}
        	}
            String vocabulariesPath = ConfigurationManager.getProperty("dspace.dir") + "/config/controlled-vocabularies/";
        	String[] xmlFiles = (new File(vocabulariesPath)).list(new xmlFilter());
        	List<String> names = new ArrayList<String>();
        	for (String filename : xmlFiles)
            {
        		names.add((new File(filename)).getName().replace(".xml",""));
        	}
        	pluginNames = names.toArray(new String[names.size()]);
            log.info("Got plugin names = "+Arrays.deepToString(pluginNames));
        }
    }

    private void init()
    {
    	if (vocabulary == null)
        {
        	log.info("Initializing " + this.getClass().getName());
        	vocabularyName = this.getPluginInstanceName();
            String vocabulariesPath = ConfigurationManager.getProperty("dspace.dir") + "/config/controlled-vocabularies/";
            String configurationPrefix = "vocabulary.plugin." + vocabularyName;
            storeHierarchy = ConfigurationManager.getBooleanProperty(configurationPrefix + ".hierarchy.store", storeHierarchy);
            suggestHierarchy = ConfigurationManager.getBooleanProperty(configurationPrefix + ".hierarchy.suggest", suggestHierarchy);
            String configuredDelimiter = ConfigurationManager.getProperty(configurationPrefix + ".delimiter");
            if (configuredDelimiter != null)
            {
            	hierarchyDelimiter = configuredDelimiter.replaceAll("(^\"|\"$)","");
            }
        	String filename = vocabulariesPath + vocabularyName + ".xml";
        	log.info("Loading " + filename);
            vocabulary = new InputSource(filename);
    	}
    }

    private String buildString(Node node)
    {
    	if (node.getNodeType() == Node.DOCUMENT_NODE)
        {
    		return "";
    	}
        else
        {
    		String parentValue = buildString(node.getParentNode());
    		String currentValue;
    		if (node.getAttributes().getNamedItem("label") == null)
            	currentValue = "(missing label)";
            else 
    			currentValue = node.getAttributes().getNamedItem("label").getNodeValue();
    		
    		if ("".equals(parentValue))
            	return currentValue;
    		else
            	return parentValue + this.hierarchyDelimiter + currentValue ;
    	}
    }

    
    public Choices getMatches(String field, String text, int collection, int start, int limit, String locale)
    {
    	init();
    	
    	if (start <=0)
    		start=0;
    	
    	if (limit <=0 || limit > MAX_PAGE_SIZE)
    		limit=DEFAULT_PAGE_SIZE;
    	
    	if (log.isDebugEnabled())	
    		log.debug(limit+" matches requested for field '" + field + "' with value '"+ text+"' starting from "+start);
    	
    	XPath xpath = XPathFactory.newInstance().newXPath();
    	String xpathExpression = String.format(xpathTemplate, text.replaceAll("'", "&apos;").toLowerCase());
    	NodeList results;
    	
    	try {
    		results = (NodeList)xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODESET);
    	} catch(XPathExpressionException e) {
    		String url = "url://get-authority-url/?field="+field+"&text="+text+"&start="+start+"&limit="+limit;
    		String message = "XPathExpressionException on expression "+xpathExpression;
    		log.warn(message,e);
    		
    		MailReporter.reportUnknownException(message, e, url);
    		
    		return new Choices(true);
    	}
    	
    	
    	int totalResults = results.getLength();
    	int end = Math.min(start + limit, totalResults);
    	Choice[] choices = new Choice[end-start];
    	String label, value, authority, hierarchyLabel;
    	
    	for (int i=0; i<choices.length; i++)
        {
    		Node node = results.item(i);
        	hierarchyLabel = this.buildString(node);
        	if (node.getAttributes().getNamedItem("label") == null)
        	{
        		log.warn("Missing label for authority on field "+field + ". Check ControlledVocabulary "+this.vocabularyName + " and look for text '"+text);
        		label = hierarchyLabel;
        		value = hierarchyLabel;
        	}
        	else 
        	{
        		if (this.suggestHierarchy)
	        		label = hierarchyLabel;
	        	else
	            	label = node.getAttributes().getNamedItem("label").getNodeValue();
	        	
	        	if (this.storeHierarchy)
	            	value = hierarchyLabel;
	        	else
	        		value = node.getAttributes().getNamedItem("label").getNodeValue();
        	}
        	
        	if (node.getAttributes().getNamedItem("id") == null){
        		log.warn("Missing id for authority on field "+field + ". Check ControlledVocabulary "+this.vocabularyName + " and look for label '"+label);
        		authority = "";
        	}else{
        		authority = node.getAttributes().getNamedItem("id").getNodeValue();
        	}
        	
        	choices[i] = new Choice(authority,value,label);
        }
        
    	return new Choices(choices, 0, choices.length, Choices.CF_AMBIGUOUS, (end < totalResults));
    }

	
    public Choices getBestMatch(String field, String text, int collection, String locale)
    {
    	init();
    	log.debug("Getting best match for '" + text + "'");
        return getMatches(field, text, collection, 0, 2, locale);
    }

    public String getLabel(String field, String key, String locale)
    {
    	init();
    	String xpathExpression = String.format(idTemplate, key);
    	XPath xpath = XPathFactory.newInstance().newXPath();
    	try {
    		Node node = (Node)xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODE);
    		return node.getAttributes().getNamedItem("label").getNodeValue();
    	} catch(Exception e) {
    		String url = "url://get-label-url/?field="+field+"&key="+key+"&locale="+locale;
    		String message = "Exception on expression "+xpathExpression;
    		log.warn(message,e);
    		
    		MailReporter.reportUnknownException(message, e, url);
    		//continue
    	}
    	return key;
    	
    }
}
