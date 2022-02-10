/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.WorkflowConfigurationException;

/**
 * Builder to construct Collection objects.
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class CollectionBuilder extends AbstractDSpaceObjectBuilder<Collection> {

    private Collection collection;

    protected CollectionBuilder(Context context) {
        super(context);

    }

    /**
     * Begin building a Collection.
     *
     * @param context current DSpace session.
     * @param parent place the Collection in this Community.
     * @return this.
     */
    public static CollectionBuilder createCollection(final Context context, final Community parent) {
        CollectionBuilder builder = new CollectionBuilder(context);
        return builder.create(parent);
    }

    /**
     * Begin building a Collection with a given Handle.
     *
     * @param context current DSpace session.
     * @param parent place the Collection in this Community.
     * @param handle give the Collection this Handle.
     * @return this.
     */
    public static CollectionBuilder createCollection(final Context context,
                                                     final Community parent,
                                                     final String handle) {
        CollectionBuilder builder = new CollectionBuilder(context);
        return builder.create(parent, handle);
    }

    private CollectionBuilder create(final Community parent) {
        try {
            this.collection = collectionService.create(context, parent);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    private CollectionBuilder create(final Community parent, final String handle) {
        try {
            for (Collection collection : this.collectionService.findAll(context)) {
                if (collection.getHandle().equalsIgnoreCase(handle)) {
                    this.collection = collection;
                }
            }
            this.collection = this.collectionService.create(context, parent, handle);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    /**
     * Set the name of the Collection, with language not specified.
     *
     * @param name give the Collection this name (title).
     * @return this.
     */
    public CollectionBuilder withName(final String name) {
        return setMetadataSingleValue(collection, MetadataSchemaEnum.DC.getName(), "title", null, name);
    }

    public CollectionBuilder withEntityType(final String entityType) {
        return setMetadataSingleValue(collection, "dspace", "entity", "type", entityType);
    }

    /**
     * Set the name of the Collection in the given language.
     *
     * @param name give the Collection this name (title).
     * @param language ISO 639 language code and optional variant (en, en_us).
     * @return this.
     */
    public CollectionBuilder withNameForLanguage(final String name, final String language) {
        return addMetadataValue(collection, MetadataSchemaEnum.DC.getName(), "title", null, language, name);
    }

    /**
     * Set the Collection's logo.
     * <em>To a String.  Should this not be the bytes of an image of some sort?</em>
     *
     * @param content these characters will be converted to bytes, stored as
     *                  a Bitstream, and the Bitstream set as the Collection's logo.
     * @return this.
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     * @throws SQLException passed through.
     */
    public CollectionBuilder withLogo(final String content) throws AuthorizeException, IOException, SQLException {

        InputStream is = IOUtils.toInputStream(content, StandardCharsets.UTF_8);
        try {
            collectionService.setLogo(context, collection, is);
            return this;

        } finally {
            is.close();
        }
    }

    /**
     * Add a provenance record to the Collection.
     * @param provenance the record.
     * @return this.
     */
    public CollectionBuilder withProvenance(final String provenance) {
        return addMetadataValue(collection,
                                MetadataSchemaEnum.DC.getName(),
                                "description",
                                "provenance",
                                provenance);
    }

    /**
     * Generate a template Item for the Collection.  Consult the built Collection
     * to reference the Item.
     * @return this.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    public CollectionBuilder withTemplateItem() throws SQLException, AuthorizeException {
        collectionService.createTemplateItem(context, collection);
        return this;
    }

    /**
     * Create a submitter group for the collection with the specified members
     *
     * @param members epersons to add to the submitter group
     * @return this builder
     * @throws SQLException
     * @throws AuthorizeException
     */
    public CollectionBuilder withSubmitterGroup(EPerson... members) throws SQLException, AuthorizeException {
        Group g = collectionService.createSubmitters(context, collection);
        for (EPerson e : members) {
            groupService.addMember(context, g, e);
        }
        groupService.update(context, g);
        return this;
    }

    /**
     * Generate and populate a workflow group for the Collection.  Obsolete:
     * the 3-step workflow model has been removed. Use other withWorkflowGroup() method instead
     *
     * @param step number of the workflow step.
     * @param members make these users members of the group.
     * @return this
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     * @deprecated 7.0
     */
    @Deprecated
    public CollectionBuilder withWorkflowGroup(int step, EPerson... members) throws SQLException, AuthorizeException {
        Group g = collectionService.createWorkflowGroup(context, collection, step);
        for (EPerson e : members) {
            groupService.addMember(context, g, e);
        }
        groupService.update(context, g);
        return this;
    }

    /**
     * Generate and populate a role-based workflow group for the Collection.
     *
     * @param roleName the rolename for the group
     * @param members make these users members of the group.
     * @return this
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    public CollectionBuilder withWorkflowGroup(String roleName, EPerson... members)
            throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
        Group g = workflowService.createWorkflowRoleGroup(context, collection, roleName);
        for (EPerson e : members) {
            groupService.addMember(context, g, e);
        }
        groupService.update(context, g);
        return this;
    }

    /**
     * Create an admin group for the collection with the specified members
     *
     * @param members epersons to add to the admin group
     * @return this builder
     * @throws SQLException
     * @throws AuthorizeException
     */
    public CollectionBuilder withAdminGroup(EPerson... members) throws SQLException, AuthorizeException {
        Group g = collectionService.createAdministrators(context, collection);
        for (EPerson e : members) {
            groupService.addMember(context, g, e);
        }
        groupService.update(context, g);
        return this;
    }

    @Override
    public Collection build() {
        try {
            collectionService.update(context, collection);
            context.dispatchEvents();
            indexingService.commit();

        } catch (Exception e) {
            return handleException(e);
        }
        return collection;
    }

    @Override
    public void cleanup() throws Exception {
       try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            collection = c.reloadEntity(collection);
            if (collection != null) {
                deleteAdminGroup(c);
                deleteItemTemplate(c);
                deleteDefaultReadGroups(c, collection);
                deleteWorkflowGroups(c, collection);
                delete(c ,collection);
                c.complete();
            }
       }
    }

    private void deleteAdminGroup(Context c) throws SQLException, AuthorizeException, IOException {
        Group group = collection.getAdministrators();
        if (group != null) {
            collectionService.removeAdministrators(c, collection);
            groupService.delete(c, group);
        }
    }

    private void deleteItemTemplate(Context c) throws SQLException, AuthorizeException, IOException {
        if (collection.getTemplateItem() != null) {
                 collectionService.removeTemplateItem(c, collection);
        }
    }

    /**
     * Delete all workflow groups from a given Collection.  Obsolete:  depends
     * on the removed 3-step workflow.
     *
     * @param c current DSpace session.
     * @param collection the Collection to be depleted.
     * @throws Exception passed through.
     * @deprecated 7.0
     */
    @Deprecated
    public void deleteWorkflowGroups(Context c, Collection collection) throws Exception {
       for (int i = 1; i <= 3; i++) {
            Group group = collectionService.getWorkflowGroup(c, collection, i);
            if (group != null) {
                collectionService.setWorkflowGroup(c, collection, i, null);
                groupService.delete(c, group);
            }
       }
    }

    /**
     * Delete the default groups that grant READ permission to Items
     * and Bitstreams in a given Collection.
     *
     * @param c current DSpace session.
     * @param collection the Collection to be depleted.
     * @throws Exception passed through.
     */
    public void deleteDefaultReadGroups(Context c, Collection collection) throws Exception {
        Group defaultItemReadGroup = groupService.findByName(c, "COLLECTION_" +
              collection.getID().toString() + "_ITEM_DEFAULT_READ");
        Group defaultBitstreamReadGroup = groupService.findByName(c, "COLLECTION_" +
              collection.getID().toString() + "_BITSTREAM_DEFAULT_READ");
        if (defaultItemReadGroup != null) {
            groupService.delete(c, defaultItemReadGroup);
        }
        if (defaultBitstreamReadGroup != null) {
            groupService.delete(c, defaultBitstreamReadGroup);
        }
    }

    /**
     * Delete the Test Collection referred to by the given UUID
     *
     * @param uuid UUID of Test Collection to delete
     * @throws SQLException
     * @throws IOException
     * @throws SearchServiceException
     */
    public static void deleteCollection(UUID uuid) throws SQLException, IOException, SearchServiceException {
       try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Collection collection = collectionService.find(c, uuid);
            if (collection != null) {
                try {
                    collectionService.delete(c, collection);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            c.complete();
            indexingService.commit();
       }
    }

    @Override
    protected DSpaceObjectService<Collection> getService() {
        return collectionService;
    }
}
