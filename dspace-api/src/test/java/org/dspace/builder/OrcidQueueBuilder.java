/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.orcid.OrcidOperation;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.service.OrcidQueueService;

/**
 * Builder to construct OrcidQueue objects
 *
 * @author Mykhaylo Boychuk (4science)
 */
public class OrcidQueueBuilder extends  AbstractBuilder<OrcidQueue, OrcidQueueService> {

    private OrcidQueue orcidQueue;

    protected OrcidQueueBuilder(Context context) {
        super(context);
    }

    @Override
    protected OrcidQueueService getService() {
        return orcidQueueService;
    }

    @Override
    public void cleanup() throws Exception {
        delete(orcidQueue);
    }

    public static OrcidQueueBuilder createOrcidQueue(Context context, Item profileItem, Item entity) {
        OrcidQueueBuilder builder = new OrcidQueueBuilder(context);
        return builder.createEntityInsertionRecord(context, profileItem, entity);
    }

    public static OrcidQueueBuilder createOrcidQueue(Context context, Item profileItem, Item entity, String putCode) {
        OrcidQueueBuilder builder = new OrcidQueueBuilder(context);
        return builder.createEntityUpdateRecord(context, profileItem, entity, putCode);
    }

    public static OrcidQueueBuilder createOrcidQueue(Context context, Item profileItem, String description,
        String type, String putCode) {
        OrcidQueueBuilder builder = new OrcidQueueBuilder(context);
        return builder.createEntityDeletionRecord(context, profileItem, description, type, putCode);
    }

    private OrcidQueueBuilder createEntityDeletionRecord(Context context, Item profileItem,
        String description, String type, String putCode) {
        try {
            this.context = context;
            this.orcidQueue = getService().createEntityDeletionRecord(context, profileItem, description, type, putCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    private OrcidQueueBuilder createEntityUpdateRecord(Context context, Item profileItem, Item entity, String putCode) {
        try {
            this.context = context;
            this.orcidQueue = getService().createEntityUpdateRecord(context, profileItem, entity, putCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    private OrcidQueueBuilder createEntityInsertionRecord(Context context, Item profileItem, Item entity) {
        try {
            this.context = context;
            this.orcidQueue = getService().createEntityInsertionRecord(context, profileItem, entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public OrcidQueue build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, orcidQueue);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return orcidQueue;
    }

    public OrcidQueueBuilder withPutCode(String putCode) {
        orcidQueue.setPutCode(putCode);
        return this;
    }

    public OrcidQueueBuilder withMetadata(String metadata) throws SQLException {
        orcidQueue.setMetadata(metadata);
        return this;
    }

    public OrcidQueueBuilder withRecordType(String recordType) throws SQLException {
        orcidQueue.setRecordType(recordType);
        return this;
    }

    public OrcidQueueBuilder withOperation(OrcidOperation operation) throws SQLException {
        orcidQueue.setOperation(operation);
        return this;
    }

    public OrcidQueueBuilder withDescription(String description) throws SQLException {
        orcidQueue.setDescription(description);
        return this;
    }

    @Override
    public void delete(Context c, OrcidQueue orcidQueue) throws Exception {
        if (orcidQueue != null) {
            getService().delete(c, orcidQueue);
        }
    }

    public void delete(OrcidQueue orcidQueue) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            OrcidQueue attachedTab = c.reloadEntity(orcidQueue);
            if (attachedTab != null) {
                getService().delete(c, attachedTab);
            }
            c.complete();
        }
        indexingService.commit();
    }

}
