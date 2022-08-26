/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.lang.reflect.InvocationTargetException;

import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Factory class to generate DAOs based on the configuration
 *
 * @author Richard Jones
 */
public class BrowseDAOFactory {

    /**
     * Default constructor
     */
    private BrowseDAOFactory() { }


    /**
     * Get an instance of the relevant Read Only DAO class, which will
     * conform to the BrowseDAO interface
     *
     * @param context the DSpace context
     * @return the relevant DAO
     * @throws BrowseException if browse error
     */
    public static BrowseDAO getInstance(Context context)
        throws BrowseException {
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        String className = configurationService.getProperty("browseDAO.class");
        if (className == null) {
            // SOLR implementation is the default since DSpace 4.0
            return new SolrBrowseDAO(context);
        }
        try {
            return (BrowseDAO) Class
                .forName(configurationService.getProperty("browseDAO.class"))
                .getConstructor(Context.class).newInstance(context);
        } catch (ClassNotFoundException | IllegalAccessException
                | IllegalArgumentException | InstantiationException
                | NoSuchMethodException | SecurityException |
                InvocationTargetException e) {
            throw new BrowseException("The configuration for browseDAO is invalid: " + className, e);
        }
    }
}
