/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Builder to construct Collection objects
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class CollectionBuilder extends AbstractDSpaceObjectBuilder<Collection> {

    private Collection collection;

    protected CollectionBuilder(Context context) {
        super(context);

    }

    public static CollectionBuilder createCollection(final Context context, final Community parent) {
        CollectionBuilder builder = new CollectionBuilder(context);
        return builder.create(parent);
    }

    private CollectionBuilder create(final Community parent) {
        try {
            this.collection = collectionService.create(context, parent);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    public CollectionBuilder withName(final String name) {
        return setMetadataSingleValue(collection, MetadataSchema.DC_SCHEMA, "title", null, name);
    }

    public CollectionBuilder withLogo(final String content) throws AuthorizeException, IOException, SQLException {

        InputStream is = IOUtils.toInputStream(content, CharEncoding.UTF_8);
        try {
            collectionService.setLogo(context, collection, is);
            return this;

        } finally {
            is.close();
        }
    }

    public CollectionBuilder withTemplateItem() throws SQLException, AuthorizeException {
        collectionService.createTemplateItem(context, collection);
        return this;
    }

    public CollectionBuilder withWorkflowGroup(int step, EPerson... members) throws SQLException, AuthorizeException {
        Group g = collectionService.createWorkflowGroup(context, collection, step);
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

    protected void cleanup() throws Exception {
        deleteWorkflowGroups(collection);
        delete(collection);
    }

    public void deleteWorkflowGroups(Collection collection) throws Exception {

        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            for (int i = 1; i <= 3; i++) {
                Group g = collectionService.getWorkflowGroup(c, collection, i);
                if (g != null) {
                    Group attachedDso = c.reloadEntity(g);
                    if (attachedDso != null) {
                        collectionService.setWorkflowGroup(c, collection, i, null);
                        groupService.delete(c, attachedDso);
                    }
                }
            }
            c.complete();
        }

        indexingService.commit();
    }

    @Override
    protected DSpaceObjectService<Collection> getService() {
        return collectionService;
    }
}
