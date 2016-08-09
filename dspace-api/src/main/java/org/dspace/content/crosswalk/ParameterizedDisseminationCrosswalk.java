/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.jdom.Element;

/**
 * Translate DSpace native metadata into an external XML format, with parameters.
 * This extends {@link DisseminationCrosswalk} by accepting a table of XSL-T global
 * parameter names and values, which will be passed to the selected transform.
 *
 * @author mhwood
 */
public interface ParameterizedDisseminationCrosswalk
        extends DisseminationCrosswalk
{
    /**
     * Execute crosswalk, returning one XML root element as
     * a JDOM <code>Element</code> object.
     * This is typically the root element of a document.
     * <p>
     *
     * @param context
     * @param dso the DSpace Object whose metadata to export.
     * @param parameters
     *  names and values of parameters to be passed into the transform.
     * @return root Element of the target metadata, never <code>null</code>.
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace object.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    public Element disseminateElement(Context context, DSpaceObject dso,
            Map<String, String> parameters)
        throws CrosswalkException, IOException, SQLException, AuthorizeException;
}
