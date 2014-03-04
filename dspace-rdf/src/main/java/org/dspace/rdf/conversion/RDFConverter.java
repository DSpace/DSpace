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

public interface RDFConverter {

    public Model convert(Context context, DSpaceObject dso)
            throws SQLException, AuthorizeException;
}
