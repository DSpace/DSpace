/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.ReloadableEntityObjectRepository;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility class to manipulate the AuthorizationRest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class AuthorizationRestUtil {

    @Autowired
    private Utils utils;

    /**
     * Extract the feature name from the Authorization business ID. See {@link Authorization#getID()}
     * 
     * @param id
     *            the Authorization business ID
     * @return the feature name
     */
    public String getFeatureName(String id) {
        return splitIdParts(id)[1];
    }

    /**
     * Get the object addressed in the authorization extracting its type and primary key from the authorization business
     * ID ({@link Authorization#getID()}) and using the appropriate service
     * 
     * @param context
     *            the DSpace context
     * @param id
     *            the Authorization business ID. See {@link Authorization#getID()}
     * @return the object addressed in the authorization
     * @throws SQLException
     *             if an error occur retrieving the data from the database
     * @throws IllegalArgumentException
     *             if the specified id doesn't contain syntactically valid object information
     */
    public BaseObjectRest getObject(Context context, String id) throws SQLException {
        String[] parts = splitIdParts(id);
        String objIdStr = parts[3];
        String[] objType;
        try {
            objType = parts[2].split("\\.");
            DSpaceRestRepository repository = utils
                .getResourceRepositoryByCategoryAndModel(objType[0], English.plural(objType[1]));
            Serializable pk = utils.castToPKClass((ReloadableEntityObjectRepository) repository, objIdStr);
            try {
                // disable the security as we only need to retrieve the object to further process the authorization
                context.turnOffAuthorisationSystem();
                return (BaseObjectRest) repository.findOne(context, pk);
            } finally {
                context.restoreAuthSystemState();
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "The id " + id + " not resolve to a valid object", e);
        }
    }

    /**
     * Get the eperson in the authorization extracting its uuid from the authorization business ID
     * ({@link Authorization#getID()}) and retrieving the corresponding eperson object with the {@link EPersonService}.
     * Please note that reference to deleted eperson will result in an IllegalArgumentException
     * 
     * @param context
     *            the DSpace context
     * @param id
     *            the Authorization business ID. See {@link Authorization#getID()}
     * @return the eperson addressed in the authorization or null if not specified.
     * @throws SQLException
     *             if an error occur retrieving the data from the database
     * @throws IllegalArgumentException
     *             if the specified id doesn't contain syntactically valid object information
     */
    public EPerson getEperson(Context context, String id) throws SQLException {
        String epersonIdStr = splitIdParts(id)[0];
        if (StringUtils.isBlank(epersonIdStr)) {
            return null;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(epersonIdStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("The authorization id " + id +
                    " contains a reference to an invalid eperson uuid " + epersonIdStr);
        }
        EPersonService service = EPersonServiceFactory.getInstance().getEPersonService();
        EPerson ep = service.find(context, uuid);
        if (ep == null) {
            throw new IllegalArgumentException("No eperson found with the uuid " + epersonIdStr);
        }
        return ep;
    }

    /**
     * Split the business ID in an array with a fixed length (4) as follow eperson uuid, feature name, object type id,
     * object id
     * 
     * @param id
     *            the Authorization business ID. See {@link Authorization#getID()}
     * @return an array with a fixed length (4) as follow eperson uuid, feature name, object type id, object id
     */
    private String[] splitIdParts(String id) {
        String[] idParts = id.split("_");
        String eperson = null;
        String feature = null;
        String objType = null;
        String objId = null;
        if (idParts.length == 4) {
            eperson = idParts[0];
            feature = idParts[1];
            objType = idParts[2];
            objId = idParts[3];
        } else if (idParts.length == 3) {
            feature = idParts[0];
            objType = idParts[1];
            objId = idParts[2];
        } else {
            throw new IllegalArgumentException(
                    "the authoization id is invalid, it must have the form " +
                    "[eperson-uuid_]feature-id_object-type_object-id");
        }
        return new String[] { eperson, feature, objType, objId };
    }
}
