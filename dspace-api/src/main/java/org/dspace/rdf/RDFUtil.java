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
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.rdf.factory.RDFFactory;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class RDFUtil {
    private static final Logger log = Logger.getLogger(RDFUtil.class);
    private static final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    
    public static final String CONTENT_NEGOTIATION_KEY = "rdf.contentNegotiation.enable";
    /**
     * Key of the Property to load the types of DSpaceObjects that should get
     * converted.
     */
    public static final String CONVERTER_DSOTYPES_KEY = "rdf.converter.DSOtypes";
    /**
     * Property key to load the password if authentication for the graph store
     * endpoint is required.
     */
    public static final String STORAGE_GRAPHSTORE_PASSWORD_KEY = "rdf.storage.graphstore.password";
    /**
     * Property key to load the URL of the dspace-rdf module. This is necessary
     * to create links from the jspui or xmlui to RDF representation of
     * DSpaceObjects.
     */
    public static final String CONTEXT_PATH_KEY = "rdf.contextPath";
    /**
     * Property key to load the public address of the SPARQL endpoint.
     */
    public static final String SPARQL_ENDPOINT_KEY = "rdf.public.sparql.endpoint";
    /**
     * Property key to load the username if authentication for the internal
     * SPARQL endpoint is required.
     */
    public static final String STORAGE_SPARQL_LOGIN_KEY = "rdf.storage.sparql.login";
    /**
     * Property key to load the password if authentication for the internal
     * SPARQL endpoint is required.
     */
    public static final String STORAGE_SPARQL_PASSWORD_KEY = "rdf.storage.sparql.password";
    /**
     * Property key to load the address of the SPARQL 1.1 GRAPH STORE HTTP
     * PROTOCOL endpoint.
     */
    public static final String STORAGE_GRAPHSTORE_ENDPOINT_KEY = "rdf.storage.graphstore.endpoint";
    /**
     * Property key to load the address of the SPARQL endpoint to use within
     * DSpace. If the property is empty or does not exist, the public SPARQL
     * endpoint will be used.
     */
    public static final String STORAGE_SPARQL_ENDPOINT_KEY = "rdf.storage.sparql.endpoint";
    /**
     * Property key to load the username if authentication for the graph store
     * endpoint is required.
     */
    public static final String STORAGE_GRAPHSTORE_LOGIN_KEY = "rdf.storage.graphstore.login";

    /**
     * Loads converted data of a DSpaceObject identified by the URI provided
     * as {@code identifier}. This method uses the RDFStorage configurated in
     * the DSpace configuration.  Close the model
     * ({@link com.hp.hpl.jena.rdf.model.Model#close() Model.close()}) as soon 
     * as possible to free system resources.
     *
     * @param identifier A URI representing the object you want to load data about.
     * @return A model containing the RDF data to the specified identifier or 
     *         null if no data could be found.
     */
    public static Model loadModel(String identifier)
    {
        return RDFFactory.getInstance().getRDFStorage().load(identifier);
    }
    
    /**
     * Generates a URI identifying the provided DSpaceObject. This method
     * automatically loads and instantiates the URIGenerator configured in
     * DSpace configuration.
     * Please note that URIs can be generated for DSpaceObjects of the 
     * type SITE, COMMUNITY, COLLECTION or ITEM only. Currently dspace-rdf 
     * doesn't support Bundles or Bitstreams as independent entity.
     *
     * @param context DSpace Context.
     * @param dso DSpace Object you want to get an identifier for.
     * @return URI to identify the DSO or null if no URI could be generated.
     *         This can happen f.e. if you use a URIGenerator that uses
     *         persistent identifier like DOIs or Handles but there is no such
     *         identifier assigned to the provided DSO.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public static String generateIdentifier(Context context, DSpaceObject dso)
            throws SQLException
    {
        return RDFFactory.getInstance().getURIGenerator().generateIdentifier(context, dso);
    }
    
    /**
     * Generates a URI identifying the provided DSpaceObject. This method
     * automatically loads and instantiates the URIGenerator configured in
     * DSpace configuration.
     * Please note that URIs can be generated for DSpaceObjects of the 
     * type SITE, COMMUNITY, COLLECTION or ITEM only. Currently dspace-rdf 
     * doesn't support Bundles or Bitstreams as independent entity.
     *
     * @param context DSpace Context.
     * @param type Type of the DSpaceObject you want to generate a URI for (e.g. 
     *             {@link org.dspace.core.Constants#ITEM Constants.ITEM}.
     * @param id UUID of the DSpaceObject you want to generate a URI for.
     * @param handle Handle of the DSpaceObject you want to generate a URI for.
     * @param identifier identifiers of the object.
     *     
     * @return URI to identify the DSO or null if no URI could be generated.
     *         This can happen f.e. if you use a URIGenerator that uses
     *         persistent identifier like DOIs or Handles but there is no such
     *         identifier assigned to the provided DSO.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public static String generateIdentifier(Context context, int type, UUID id,
            String handle, List<String> identifier)
            throws SQLException
    {
        return RDFFactory.getInstance().getURIGenerator().generateIdentifier(context, 
                type, id, handle, identifier);
    }
    /**
     * Converts the the provided DSpaceObject into RDF and returns the model.
     * Please note that dspace-rdf doesn't support Bundles or Bitstreams as 
     * independent entity. You can convert DSpaceObjects of type SITE,
     * COMMUNITY, COLLECTION or ITEM.
     *
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
     * @throws SQLException if database error
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
        String[] dsoTypes = DSpaceServicesFactory.getInstance()
                .getConfigurationService()
                .getArrayProperty(CONVERTER_DSOTYPES_KEY);
        if (dsoTypes == null || dsoTypes.length == 0)
        {
            log.warn("Property rdf." + CONVERTER_DSOTYPES_KEY + " was not found "
                    + "or is empty. Will convert all type of DSpace Objects.");
        } else {
            boolean found = false;
            for (String type : dsoTypes)
            {
                if (StringUtils.equalsIgnoreCase(Constants.typeText[dso.getType()], type.trim()))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                log.warn("Configuration of DSpaceObjects of type " 
                        + Constants.typeText[dso.getType()] 
                        + " prohibitted by configuration.");
                return null;
            }
        }
        isPublic(context, dso);
        return RDFFactory.getInstance().getRDFConverter().convert(context, dso);
    }
    
    /**
     * Converts a DSpaceObject into RDF data and stores them using the configured
     * {@link org.dspace.rdf.storage.RDFStorage RDFStorage}. 
     * Please note that dspace-rdf doesn't support Bundles or Bitstreams as 
     * independent entity. You can convert DSpaceObjects of type SITE,
     * COMMUNITY, COLLECTION or ITEM.
     *
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
     * @throws SQLException if database error
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
                    + ContentServiceFactory.getInstance().getDSpaceObjectService(dso).getTypeText(dso) + " (id: " + dso.getID() + ").");
            if (convertedData != null) convertedData.close();
            throw new RDFMissingIdentifierException(dso.getType(), dso.getID());
        }

        if (convertedData == null)
        {
            // if data about this dso is stored in the triplestore already, we 
            // should remove it as a conversion currently result in no data
            RDFFactory.getInstance().getRDFStorage().delete(identifier);
            return null;
        }
        
        RDFFactory.getInstance().getRDFStorage().store(identifier, convertedData);
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
     * @throws SQLException if database error
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
        authorizeService.authorizeAction(context, dso, Constants.READ);
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
     *
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
     * @throws SQLException if database error
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
     *
     * @param uri URI to identify the named graph to delete.
     */
    public static void delete(String uri)
    {
        RDFFactory.getInstance().getRDFStorage().delete(uri);
    }
    
    /**
     * This is a shortcut to generate an RDF identifier for a DSpaceObject and
     * to delete the identified data from the named graph.
     *
     * @param ctx
     *     The relevant DSpace Context.
     * @param type DSpaceObject type (e.g. {@link Constants#ITEM Constants.ITEM}).
     * @param id Id of the DspaceObject.
     * @param handle Handle of the DSpaceObject.
     * @param identifiers list of identifiers
     * @throws SQLException if database error
     * @throws RDFMissingIdentifierException In case that no Identifier could be generated.
     */
    public static void delete(Context ctx, int type, UUID id, String handle, List<String> identifiers)
            throws SQLException, RDFMissingIdentifierException
    {
        String uri = RDFFactory.getInstance().getURIGenerator()
                .generateIdentifier(ctx, type, id, handle, identifiers);
        if (uri != null)
        {
            RDFFactory.getInstance().getRDFStorage().delete(uri);
        } else {
            throw new RDFMissingIdentifierException(type, id);
        }
    }

}
