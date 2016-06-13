/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.Context;

/**
 * Define interface to manage list of DSpaceObject. 
 * 
 * @author pascarelli
 *
 */
public interface StreamGenericDisseminationCrosswalk extends StreamDisseminationCrosswalk
{

    public void disseminate(Context context, List<DSpaceObject> dso, OutputStream out)
    throws CrosswalkException, IOException, SQLException, AuthorizeException;
    
}
