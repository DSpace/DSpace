/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Factory class to generate DAOs based on the configuration
 *
 * @author bollini
 */
public class AuthorityDAOFactory {
    
    /**
     * Get an instance of the relevant Read Only DAO class, which will
     * conform to the BrowseDAO interface
     *
     * @param context	the DSpace context
     * @return			the relevant DAO
     * @throws IllegalStateException
     */
    public static AuthorityDAO getInstance(Context context)
            throws IllegalStateException
    {
            String db = ConfigurationManager.getProperty("db.name");
            if ("postgres".equals(db))
            {
                    return new AuthorityDAOPostgres(context);
            }
            else if ("oracle".equals(db))
            {
                    return new AuthorityDAOOracle(context);
            }
            else
            {
                    throw new IllegalStateException("The configuration for db.name is either invalid, or contains an unrecognised database");
            }
    }
}
