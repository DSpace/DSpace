/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.cocoon;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.configuration.MutableConfiguration;
import org.apache.cocoon.transformation.I18nTransformer;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.configuration.Aspect;
import org.dspace.app.xmlui.configuration.XMLUIConfiguration;

/**
 * This is a simple extension to the standard Cocoon I18N transformer
 * that specializes the configuration based upon currently installed
 * aspects.
 *
 * <p>
 * This transformer modified the base configuration by adding two 
 * {@code <location/>} parameters for each aspect into the default catalogue.
 * The first location parameter is contained within the catalogue's
 * base location + the aspect path. The second location parameter is
 * located inside the aspect's path + "/i18n/"
 *
 * <p>
 * This allows aspect developers to place their default messages files
 * inside the aspect, and place translations into various languages inside 
 * the base i18n/ directory.
 *
 * <p>
 * EXAMPLE:
 *
 * <p>
 * For instance, let's say that the i18n transformer's configuration
 * were as follows:
 * <pre>{@code
 * <catalogues default="default">
 *   <catalogue id="default" name="messages" aspects="true">
 *     <location>context://i18n</location>
 *   </catalogue>
 * </catalogues>
 * }</pre>
 * 
 * And there were two aspects installed:
 * <br>{@code <aspect name="Browse Artifacts" path="resource://aspects/BrowseArtifacts/" />}
 * <br>{@code <aspect name="Administration" path="resource://aspects/Administrative/" />}
 *
 * <p>
 * The effective configuration would be:
 * <pre>{@code
 * <catalogues default="default">
 *   <catalogue id="default" name="messages" aspects="true">
 *     <location>context://i18n/</location>
 *     <location>context://i18n/aspects/BrowseArtifacts</location>
 *     <location>resource://aspects/BrowseArtifacts/i18n/</location>
 *     <location>context://i18n/aspects/Administrative</location>
 *     <location>resource://aspects/Administrative/i18n/</location>
 *   </catalogue>
 * </catalogues>
 * }</pre>
 *
 * @author Scott Phillips
 */

public class DSpaceI18NTransformer extends I18nTransformer {
	
    /** log4j category */
    private static Logger log = Logger.getLogger(DSpaceI18NTransformer.class);
	
	// If we can't find a base location, use this one by default.
	public static final String DEFAULT_BASE_LOCATION ="context://i18n/";
	

    /**
     * Intercept the configuration parameters coming from the Cocoon sitemap before
     * they are read by the cocoon i18n transformer.  We want to add in
     * {@code <location>} parameters for each.
     *
     * @param originalConf the intercepted configuration.
     * @throws org.apache.avalon.framework.configuration.ConfigurationException
     *      passed through.
     */
    @Override
    public void configure(Configuration originalConf) throws ConfigurationException 
    {
    	MutableConfiguration modifiedConf = new DefaultConfiguration(originalConf,true);
        MutableConfiguration cataloguesConf = modifiedConf.getMutableChild("catalogues", true);
        
        // Find the default catalogue and add our new locations to it.
        for (MutableConfiguration catalogueConf : cataloguesConf.getMutableChildren())
        {
        	if (!"false".equals(catalogueConf.getAttribute("aspects","false")))
			{
        		// Get the first location element to determine what the base path is.
        		String baseCatalogueLocationPath = DEFAULT_BASE_LOCATION;
        		Configuration baseCatalogueLocationConf = catalogueConf.getChild("location");
        		if (baseCatalogueLocationConf != null)
                {
                    baseCatalogueLocationPath = baseCatalogueLocationConf.getValue();
                }
        		if (!baseCatalogueLocationPath.endsWith("/"))
                {
                    baseCatalogueLocationPath += "/";
                } // Add a trailing slash if one doesn't exist
        			
        		String catalogueId = catalogueConf.getAttribute("id","unknown");
        		
        		// For each aspect add two new locations one inside the aspect's directory
        		// and another inside the base location for i18n files.
            	for (Aspect aspect : XMLUIConfiguration.getAspectChain())
            	{
            		// Add a catalogue location inside the default i18n directory in the webapp
            		// this will be of the form: "context://i18n/<aspectpath>/" thus for the artifact
            		// browser aspect it will be "context://i18n/aspects/BrowseArtifacts/"
            		String baseLocationPath = aspect.getPath();
            		int idx = baseLocationPath.indexOf("://");
            		if (idx > 0)
                    {
                        // remove the module directive from the aspect's path so it's just a normal path
                        baseLocationPath = baseLocationPath.substring(idx + 3, baseLocationPath.length());
                    }
            		
            		// Now that the module directive has been removed from the path, add in the base i18npath
            		baseLocationPath = baseCatalogueLocationPath + baseLocationPath;
            		
            		MutableConfiguration baseLocation = new DefaultConfiguration("location");
            		baseLocation.setValue(baseLocationPath);
            		catalogueConf.addChild(baseLocation);
            		
            		
            		// Add a catalogue location inside the aspect's directory 
            		// (most likely in the jar's resources but if it's not that's okay)
            		// For the artifact browser this would be: 
            		// "resource://aspects/BrowseArtifacts/i18n/"
            		String aspectLocationPath = aspect.getPath();
            		if (!aspectLocationPath.endsWith("/"))
                    {
                        aspectLocationPath += "/"; // Add a trailing slash if one doesn't exist
                    }
            		aspectLocationPath += "i18n/";
            		MutableConfiguration aspectLocation = new DefaultConfiguration("location");
            		aspectLocation.setValue(aspectLocationPath);
            		catalogueConf.addChild(aspectLocation);
            		
            		log.debug("Adding i18n location path for '"+catalogueId+"' catalogue: "+baseLocationPath);
            		log.debug("Adding i18n location path for '"+catalogueId+"' catalogue: "+aspectLocationPath);
            	} // for each aspect
			} // if catalogue has the aspect parameter
        } // for each catalogue
        
        // Pass off to cocoon's i18n transformer our modified configuration with new aspect locations.
        super.configure(modifiedConf);
    }

}
