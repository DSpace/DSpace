/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rdf.storage;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * Please use 
 * {@link org.dspace.rdf.RDFUtil#generateIdentifier(Context, DSpaceObject)} to 
 * get URIs for RDF data.
 * Please note that URIs can be generated for DSpaceObjects of the 
 * type SITE, COMMUNITY, COLLECTION or ITEM only. Currently dspace-rdf 
 * doesn't support Bundles or Bitstreams as independent entity.
 * 
 * {@link org.dspace.rdf.RDFizer#RDFizer} uses a URIGenerator to generate URIs to
 * Identify DSpaceObjects in RDF. You can configure which URIGenerator should be
 * used. See DSpace documentation on how to configure RDFizer.
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 * @see org.dspace.rdf.RDFizer#RDFizer
 * @see org.dspace.rdf.RDFUtil#RDFUtil
 */
public interface URIGenerator {
    
    /**
     * Generate a URI that can be used to identify the specified DSpaceObject in
     * RDF data. Please note that URIs can be generated for DSpaceObjects of the 
     * type SITE, COMMUNITY, COLLECTION or ITEM only. Currently dspace-rdf 
     * doesn't support Bundles or Bitstreams as independent entity. This method
     * should work even if the DSpaceObject does not exist anymore.
     * @param context DSpace Context.
     * @param type Type of the DSpaceObject you want to generate a URI for (e.g. 
     *             {@link org.dspace.core.Constants#ITEM Constants.ITEM}.
     * @param id UUID of the DSpaceObject you want to generate a URI for.
     * @param handle Handle of the DSpaceObject you want to generate a URI for.
     * @param identifiers
     *     list of identifiers
     * @return May return null, if no URI could be generated.
     * @see org.dspace.rdf.RDFUtil#generateIdentifier(Context, DSpaceObject)
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public String generateIdentifier(Context context, int type, UUID id, String handle, List<String> identifiers)
        throws SQLException;
    
    /**
     * Shortcut for {@code generateIdentifier(context, dso.getType(), dso.getID(), dso.getHandle())}.
     * 
     * @param context
     *     The relevant DSpace Context.
     * @param dso
     *     DSpace object to generate identifier for
     * @return May return null, if no URI could be generated.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public String generateIdentifier(Context context, DSpaceObject dso)
        throws SQLException;
}
