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
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.rdf.RDFConfiguration;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class RDFConverterImpl implements RDFConverter
{
    protected ConfigurationService configurationService;
    protected List<ConverterPlugin> plugins;
    private static final Logger log = Logger.getLogger(RDFConverterImpl.class);
    
    public RDFConverterImpl()
    {
        this.configurationService = new DSpace().getConfigurationService();
        this.plugins = new ArrayList<ConverterPlugin>();
        
        String pluginNames[] = RDFConfiguration.getConverterPlugins();
        
        if (pluginNames == null || pluginNames.length == 0)
        {
            log.error("Cannot load RDF converter plugins!");
            throw new RuntimeException("Cannot load rdf converter plugins!");
        }
        
        for (String plugin : pluginNames)
        {
            try
            {
                Class pluginClass = Class.forName(plugin);
                ConverterPlugin pluginInstance =
                        (ConverterPlugin) pluginClass.newInstance();
                pluginInstance.setConfigurationService(this.configurationService);
                this.plugins.add(pluginInstance);
            }
            catch (ClassNotFoundException ex)
            {
                log.warn("Cannot load plugin '" + plugin 
                        + "': class not found!", ex);
                // if we would ignore a plugin, we would generate incomplete RDF data.
                throw new RuntimeException(ex.getMessage(), ex);
            }
            catch (IllegalAccessException ex)
            {
                log.warn("Cannot load plugin '" + plugin 
                        + "': illegal access!", ex);
                // if we would ignore a plugin, we would generate incomplete RDF data.
                throw new RuntimeException(ex.getMessage(), ex);
            }
            catch (InstantiationException ex)
            {
                log.warn("Cannot load plugin '" + plugin 
                        + "': cannot instantiate the module!", ex);
                // if we would ignore a plugin, we would generate incomplete RDF data.
                throw new RuntimeException(ex.getMessage(), ex);
            }
            log.debug("Successfully loaded RDFConverterPlugin " 
                    + plugin + ".");
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
