/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.conversion;

import com.hp.hpl.jena.rdf.model.Model;
import java.sql.SQLException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public interface ConverterPlugin {
    public void setConfigurationService(ConfigurationService configurationService);
    
    /**
     * Convert the specified DSpaceObject or a part of it into RDF.
     * @param context Please check the READ permission for the provided context
     *                before converting any data!
     * @param dso The DSpaceObject that should be converted.
     * @return A Jena Model containing the generated RDF.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public Model convert(Context context, DSpaceObject dso)
        throws SQLException, AuthorizeException;
    
    /**
     * Returns all type of DSpaceObjects that are supported by this plugin.
     * @param type Resource type as defined in org.dspace.core.Constants.
     * @return A boolean whether the requested type is supported by this plugin.
     * @see org.dspace.core.Constants
     */
    public boolean supports(int type);
}
