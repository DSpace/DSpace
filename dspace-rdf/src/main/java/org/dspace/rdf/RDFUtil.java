/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf;

import com.hp.hpl.jena.rdf.model.Model;
import java.sql.SQLException;
import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class RDFUtil {
    private static final Logger log = Logger.getLogger(RDFUtil.class);
        
    /**
     * Loads converted data of a DSpaceObject identified by the URI provided
     * as {@code identifier}. This method uses the RDFStorage configurated in
     * the DSpace configuration.  Close the model
     * ({@link com.hp.hpl.jena.rdf.model.Model#close() Model.close()}) as soon 
     * as possible to free system resources.
     * @param identifier A URI representing the object you want to load data about.
     * @return A model containing the RDF data to the specified identifier or 
     *         null if no data could be found.
     */
    public static Model loadModel(String identifier)
    {
        return RDFConfiguration.getRDFStorage().load(identifier);
    }
    
    /**
     * Generates a URI identifying the provided DSpaceObject. This method
     * automatically loads and instantiates the URIGenerator configured in
     * DSpace configuration.
     * Please note that URIs can be generated for DSpaceObjects of the 
     * type SITE, COMMUNITY, COLLECTION or ITEM only. Currently dspace-rdf 
     * doesn't support Bundles or Bitstreams as independent entity.
     * @param context DSpace Context.
     * @param dso DSpace Object you want to get an identifier for.
     * @return URI to identify the DSO or null if no URI could be generated.
     *         This can happen f.e. if you use a URIGenerator that uses
     *         persistent identifier like DOIs or Handles but there is no such
     *         identifier assigned to the provided DSO.
     */
    public static String generateIdentifier(Context context, DSpaceObject dso)
            throws SQLException
    {
        return RDFConfiguration.getURIGenerator().generateIdentifier(context, dso);
    }
    
    /**
     * Generates a URI identifying the provided DSpaceObject. This method
     * automatically loads and instantiates the URIGenerator configured in
     * DSpace configuration.
     * Please note that URIs can be generated for DSpaceObjects of the 
     * type SITE, COMMUNITY, COLLECTION or ITEM only. Currently dspace-rdf 
     * doesn't support Bundles or Bitstreams as independent entity.
     * @param context DSpace Context.
     * @param type Type of the DSpaceObject you want to generate a URI for (g.e. 
     *             {@link org.dspace.core.Constants#ITEM Constants.ITEM}.
     * @param id ID of the DSpaceObject you want to generate a URI for.
     * @param handle Handle of the DSpaceObject you want to generate a URI for.
     * @return URI to identify the DSO or null if no URI could be generated.
     *         This can happen f.e. if you use a URIGenerator that uses
     *         persistent identifier like DOIs or Handles but there is no such
     *         identifier assigned to the provided DSO.
     */
    public static String generateIdentifier(Context context, int type, int id,
            String handle) throws SQLException
    {
        return RDFConfiguration.getURIGenerator().generateIdentifier(context, 
                type, id, handle);
    }
    /**
     * Converts the the provided DSpaceObject into RDF and returns the model.
     * Please note that dspace-rdf doesn't support Bundles or Bitstreams as 
     * independent entity. You can convert DSpaceObjects of type SITE,
     * COMMUNITY, COLLECTION or ITEM.
     * @param context Consider that the converted data will be stored in a
     *                triple store, that is outside the range of the DSpace
     *                authorization mechanism. Unless you are really sure what 
     *                you are doing, you should provide the context of an 
     *                anonymous user here, as the triple store probably provides 
     *                a public SPARQL endpoint.
     * @param dso DSpaceObject to convert.
     * @return The converted data or null if the conversion result is empty.
     *         Remember to close the model as soon as you don't need it anymore.
     * @throws RDFMissingIdentifierException If no identifier could be generated.
     * @throws java.sql.SQLException
     * @throws ItemNotArchivedException If you want to convert an Item that is
     *                                  not archived.
     * @throws ItemWithdrawnException If you want to convert an Item that is 
     *                                withdrawn.
     * @throws ItemNotDiscoverableException If you want to convert an Item that 
     *                                      is not discoverable.
     * @throws AuthorizeException If the DSpaceObject does not have READ
     *                            permissions with the provided context.
     * @throws IllegalArgumentException If the DSpaceObject is not of type SITE,
     *                                  COMMUNITY, COLLECTION or ITEM.
     */
    public static Model convert(Context context, DSpaceObject dso)
            throws RDFMissingIdentifierException, SQLException, ItemNotArchivedException,
            ItemWithdrawnException, ItemNotDiscoverableException, 
            AuthorizeException, IllegalArgumentException
    {
        if (dso.getType() != Constants.SITE
                && dso.getType() != Constants.COMMUNITY
                && dso.getType() != Constants.COLLECTION
                && dso.getType() != Constants.ITEM)
        {
            throw new IllegalArgumentException(dso.getTypeText()
                    + " is currently not supported as independent entity.");
        }
        
        if (!RDFConfiguration.isConvertType(dso.getTypeText()))
        {
            return null;
        }
        
        isPublic(context, dso);
        return RDFConfiguration.getRDFConverter().convert(context, dso);
    }
    
    /**
     * Converts a DSpaceObject into RDF data and stores them using the configured
     * {@link org.dspace.rdf.storage.RDFStorage RDFStorage}. 
     * Please note that dspace-rdf doesn't support Bundles or Bitstreams as 
     * independent entity. You can convert DSpaceObjects of type SITE,
     * COMMUNITY, COLLECTION or ITEM.
     * @param context Consider that the converted data will be stored in a
     *                triple store, that is outside the range of the DSpace
     *                authorization mechanism. Unless you are really sure what 
     *                you are doing, you should provide the context of an 
     *                anonymous user here, as the triple store probably provides 
     *                a public SPARQL endpoint.
     * @param dso DSpaceObject to convert.
     * @return The converted data or null if the conversion result is empty.
     *         Remember to close the model as soon as you don't need it anymore.
     * @throws RDFMissingIdentifierException If no identifier could be generated.
     * @throws java.sql.SQLException
     * @throws ItemNotArchivedException If you want to convert an Item that is
     *                                  not archived.
     * @throws ItemWithdrawnException If you want to convert an Item that is 
     *                                withdrawn.
     * @throws ItemNotDiscoverableException If you want to convert an Item that 
     *                                      is not discoverable.
     * @throws AuthorizeException If the DSpaceObject does not have READ
     *                            permissions with the provided context.
     * @throws IllegalArgumentException If the DSpaceObject is not of type SITE,
     *                                  COMMUNITY, COLLECTION or ITEM.
     */
    public static Model convertAndStore(Context context, DSpaceObject dso)
            throws RDFMissingIdentifierException, SQLException, ItemNotArchivedException,
            ItemWithdrawnException, ItemNotDiscoverableException,
            AuthorizeException, IllegalArgumentException
    {
        Model convertedData = convert(context, dso);
        
        String identifier = generateIdentifier(context, dso);
        if (StringUtils.isEmpty(identifier))
        {
            log.error("Cannot generate identifier for dso from type " 
                    + dso.getTypeText() + " (id: " + dso.getID() + ").");
            if (convertedData != null) convertedData.close();
            throw new RDFMissingIdentifierException(dso.getType(), dso.getID());
        }

        if (convertedData == null)
        {
            // if data about this dso is stored in the triplestore already, we 
            // should remove it as a conversion currently result in no data
            RDFConfiguration.getRDFStorage().delete(identifier);
            return null;
        }
        
        RDFConfiguration.getRDFStorage().store(identifier, convertedData);
        return convertedData;
    }
    
    /**
     * Checks whether the provided DSpaceObject is readable within the provided
     * context and if the DSO is an Item whether it is archived, discoverable
     * and not withdrawn.
     * 
     * @param context Consider that the converted data will be stored in a
     *                triple store, that is outside the range of the DSpace
     *                authorization mechanism. Unless you are really sure what 
     *                you are doing, you should provide the context of an 
     *                anonymous user here, as the triple store probably provides 
     *                a public SPARQL endpoint.
     * @param dso The DSpaceObjet to check.
     * @throws SQLException
     * @throws ItemNotArchivedException If {@code dso} is an Item and is not
     *                                  archived.
     * @throws ItemWithdrawnException If {@code dso} is an Item and is withdrawn.
     * @throws ItemNotDiscoverableException If {@code dso} is an Item and is not
     *                                      discoverable.
     * @throws AuthorizeException If {@code context} does not grant {@code READ} 
     *                            permissions for {@code dso}.
     */
    public static void isPublic(Context context, DSpaceObject dso)
            throws SQLException, ItemNotArchivedException, ItemWithdrawnException,
            ItemNotDiscoverableException, AuthorizeException
    {
        // as there is no way to set site permissions in XMLUI or JSPUI, we
        // ignore the permissions of the repository root (DSpaceObject of type
        // Site).
        if (dso instanceof Site)
        {
            return;
        }
        AuthorizeManager.authorizeAction(context, dso, Constants.READ);
        if (dso instanceof Item)
        {
            Item item = (Item) dso;
            if (!item.isArchived()) throw new ItemNotArchivedException();
            if (!item.isDiscoverable()) throw new ItemNotDiscoverableException();
            if (item.isWithdrawn()) throw new ItemWithdrawnException();
        }
    }
    
    /**
     * Does the same as {@link #isPublic(Context, DSpaceObject) 
     * isPublic(Context, DSpaceObject)} but returns a boolean instead of throwing
     * exceptions. For those who don't want to deal with catching exceptions.
     * @param context Consider that the converted data will be stored in a
     *                triple store, that is outside the range of the DSpace
     *                authorization mechanism. Unless you are really sure what 
     *                you are doing, you should provide the context of an 
     *                anonymous user here, as the triple store probably provides 
     *                a public SPARQL endpoint.
     * @param dso The DSpaceObjet to check.
     * @return true if {@link #isPublic(Context, DSpaceObject) 
     * isPublic(Context, DSpaceObject)} doesn't throw an exception, false if it 
     * did.
     * @throws SQLException 
     */
    public static boolean isPublicBoolean(Context context, DSpaceObject dso) 
            throws SQLException
    {
        try {
            RDFUtil.isPublic(context, dso);
        } catch (ItemNotArchivedException | ItemWithdrawnException 
                | ItemNotDiscoverableException | AuthorizeException ex) {
            return false;
        }
        return true;
    }
    
    /**
     * Deletes the data identified by the URI from the triple store.
     * @param uri URI to identify the named graph to delete.
     */
    public static void delete(String uri)
    {
        RDFConfiguration.getRDFStorage().delete(uri);
    }
    
    /**
     * This is a shortcut to generate an RDF identifier for a DSpaceObject and
     * to delete the identified data from the named graph.
     * @param ctx
     * @param type DSpaceObject type (g.e. {@link Constants#ITEM Constants.ITEM}).
     * @param id Id of the DspaceObject.
     * @param handle Handle of the DSpaceObject.
     * @throws SQLException
     * @throws RDFMissingIdentifierException In case that no Identifier could be generated.
     */
    public static void delete(Context ctx, int type, int id, String handle)
            throws SQLException, RDFMissingIdentifierException
    {
        String uri = RDFConfiguration.getURIGenerator().generateIdentifier(ctx,
                        type, id, handle);
        if (uri != null)
        {
            RDFConfiguration.getRDFStorage().delete(uri);
        } else {
            throw new RDFMissingIdentifierException(type, id);
        }
    }

}
