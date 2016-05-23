/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.conversion;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.sql.SQLException;
import java.util.List;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class RDFConverterImpl implements RDFConverter
{
    private static final Logger log = Logger.getLogger(RDFConverterImpl.class);
    
    protected ConfigurationService configurationService;
    protected List<ConverterPlugin> plugins;
    
    @Autowired(required=true)
    public void setConfigurationService(ConfigurationService configurationService)
    {
        this.configurationService = configurationService;
    }
    
    @Autowired(required=true)
    public void setPlugins(List<ConverterPlugin> plugins)
    {
        this.plugins = plugins;
        if (log.isDebugEnabled())
        {
            StringBuilder pluginNames = new StringBuilder();
            for (ConverterPlugin plugin : plugins)
            {
                if (pluginNames.length() > 0)
                {
                    pluginNames.append(", ");
                }
                pluginNames.append(plugin.getClass().getCanonicalName());
            }
            log.debug("Loaded the following plugins: " + pluginNames.toString());
        }
    }
    
    public List<ConverterPlugin> getConverterPlugins()
    {
        return this.plugins;
    }
    
    @Override
    public Model convert(Context context, DSpaceObject dso)
            throws SQLException, AuthorizeException
    {
        if (this.plugins.isEmpty())
        {
            log.warn("No RDFConverterPlugins were loaded, cannot convert any data!");
            return null;
        }
        Model model = ModelFactory.createDefaultModel();
        
        for (ConverterPlugin plugin : this.plugins)
        {
            if (plugin.supports(dso.getType()))
            {
                Model convertedData = plugin.convert(context, dso);
                if (convertedData != null)
                {
                    model.setNsPrefixes(convertedData);
                    model.add(convertedData);
                    convertedData.close();
                }
            }
        }
        
        if (model.isEmpty())
        {
            model.close();
            return null;
        } else {
            return model;
        }
    }
}
