/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.discovery.indexobject.IndexableMetadataField;
import org.dspace.event.Event;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the MetadataField object.
 * This class is responsible for all business logic calls for the MetadataField object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataFieldServiceImpl implements MetadataFieldService {
    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataFieldServiceImpl.class);

    @Autowired(required = true)
    protected MetadataFieldDAO metadataFieldDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected MetadataValueService metadataValueService;
    @Autowired(required = true)
    protected MetadataSchemaService metadataSchemaService;
    @Autowired
    protected SiteService siteService;

    protected MetadataFieldServiceImpl() {

    }

    @Override
    public MetadataField create(Context context, MetadataSchema metadataSchema, String element, String qualifier,
                                String scopeNote) throws AuthorizeException, SQLException, NonUniqueMetadataException {
        // Check authorisation: Only admins may create DC types
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators may modify the metadata registry");
        }

        // Ensure the element and qualifier are unique within a given schema.
        if (hasElement(context, -1, metadataSchema, element, qualifier)) {
            throw new NonUniqueMetadataException("Please make " + element + "."
                                                     + qualifier + " unique within schema #" + metadataSchema.getID());
        }

        // Create a table row and update it with the values
        MetadataField metadataField = new MetadataField();
        metadataField.setElement(element);
        metadataField.setQualifier(qualifier);
        metadataField.setScopeNote(scopeNote);
        metadataField.setMetadataSchema(metadataSchema);
        metadataField = metadataFieldDAO.create(context, metadataField);
        metadataFieldDAO.save(context, metadataField);

        log.info(LogHelper.getHeader(context, "create_metadata_field",
                                      "metadata_field_id=" + metadataField.getID()));
        // Update the index of type metadatafield
        this.triggerEventToUpdateIndex(context, metadataField.getID());
        return metadataField;
    }

    @Override
    public MetadataField find(Context context, int id) throws SQLException {
        return metadataFieldDAO.findByID(context, MetadataField.class, id);
    }

    @Override
    public MetadataField findByElement(Context context, MetadataSchema metadataSchema, String element, String qualifier)
        throws SQLException {
        return metadataFieldDAO.findByElement(context, metadataSchema, element, qualifier);
    }

    @Override
    public MetadataField findByElement(Context context, String metadataSchemaName, String element, String qualifier)
        throws SQLException {
        return metadataFieldDAO.findByElement(context, metadataSchemaName, element, qualifier);
    }

    @Override
    public MetadataField findByString(Context context, String mdString, char separator) throws SQLException {
        String[] seq = StringUtils.split(mdString, separator);
        String schema = seq.length > 1 ? seq[0] : null;
        String element = seq.length > 1 ? seq[1] : null;
        String qualifier = seq.length == 3 ? seq[2] : null;
        if (schema == null || element == null) {
            return null;
        } else {
            return this.findByElement(context, schema, element, qualifier);
        }
    }

    @Override
    public List<MetadataField> findFieldsByElementNameUnqualified(Context context, String metadataSchemaName,
                                                                  String element) throws SQLException {
        return metadataFieldDAO.findFieldsByElementNameUnqualified(context, metadataSchemaName, element);
    }

    @Override
    public List<MetadataField> findAll(Context context) throws SQLException {
        return metadataFieldDAO.findAll(context, MetadataField.class);
    }

    @Override
    public List<MetadataField> findAllInSchema(Context context, MetadataSchema metadataSchema) throws SQLException {
        return metadataFieldDAO.findAllInSchema(context, metadataSchema);
    }

    @Override
    public void update(Context context, MetadataField metadataField)
        throws SQLException, AuthorizeException, NonUniqueMetadataException, IOException {
        // Check authorisation: Only admins may update the metadata registry
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators may modiffy the Dublin Core registry");
        }

        // Ensure the element and qualifier are unique within a given schema.
        if (hasElement(context, metadataField.getID(), metadataField.getMetadataSchema(), metadataField.getElement(),
                       metadataField.getQualifier())) {
            throw new NonUniqueMetadataException(
                "Please make " + metadataField.getMetadataSchema().getName() + "." + metadataField.getElement() + "."
                    + metadataField.getQualifier());
        }

        metadataFieldDAO.save(context, metadataField);

        log.info(LogHelper.getHeader(context, "update_metadatafieldregistry",
                                      "metadata_field_id=" + metadataField.getID() + "element=" + metadataField
                                          .getElement()
                                          + "qualifier=" + metadataField.getQualifier()));
        // Update the index of type metadatafield
        this.triggerEventToUpdateIndex(context, metadataField.getID());
    }

    @Override
    public void delete(Context context, MetadataField metadataField) throws SQLException, AuthorizeException {
        // Check authorisation: Only admins may create DC types
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators may modify the metadata registry");
        }

        // Check for existing usages of this field
        List<MetadataValue> values = null;
        try {
            values = metadataValueService.findByField(context, metadataField);
        } catch (IOException io) {
            // ignore
        }

        // Only remove this field if it is NOT in use (as we don't want to bulk delete metadata values)
        if (CollectionUtils.isEmpty(values)) {
            metadataFieldDAO.delete(context, metadataField);
        } else {
            throw new IllegalStateException("Metadata field " + metadataField
                .toString() + " cannot be deleted as it is currently used by one or more objects.");
        }

        log.info(LogHelper.getHeader(context, "delete_metadata_field",
                                      "metadata_field_id=" + metadataField.getID()));
        // Update the index of type metadatafield
        this.triggerEventToUpdateIndex(context, metadataField.getID());
    }

    /**
     * Calls a MODIFY SITE event with the identifier of the changed mdField, so it can be indexed in
     * {@link org.dspace.discovery.IndexEventConsumer}, with type of {@link org.dspace.discovery.IndexableObject} in
     * {@link Event}.detail and the identifiers of the changed mdFields in {@link Event}.identifiers
     *
     * @param context   DSpace context
     * @param mdFieldId ID of the metadata field that needs to be (re)indexed
     */
    private void triggerEventToUpdateIndex(Context context, int mdFieldId) {
        context.addEvent(new Event(Event.MODIFY, Constants.SITE, null, IndexableMetadataField.TYPE, new ArrayList<>(
            Arrays.asList(Integer.toString(mdFieldId)))));
    }

    /**
     * A sanity check that ensures a given element and qualifier are unique
     * within a given schema. The check happens in code as we cannot use a
     * database constraint.
     *
     * @param context        dspace context
     * @param fieldId        field id
     * @param metadataSchema metadataSchema
     * @param element        element
     * @param qualifier      qualifier
     * @return true if unique
     * @throws SQLException if database error
     */
    protected boolean hasElement(Context context, int fieldId, MetadataSchema metadataSchema, String element,
                                 String qualifier) throws SQLException {
        return metadataFieldDAO.find(context, fieldId, metadataSchema, element, qualifier) != null;
    }

}
