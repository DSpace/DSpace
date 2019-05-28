/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.io.InputStream;

import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;

/**
 * Builder to construct PoolTask objects
 *
 **/
public class PoolTaskBuilder extends AbstractBuilder<PoolTask, PoolTaskService> {

    private WorkspaceItem workspaceItem;

    private XmlWorkflowItem workflowItem;

    private PoolTask poolTask;

    private EPerson user;

    protected PoolTaskBuilder(Context context) {
        super(context);
    }

    /**
     * Create a PoolTaskBuilder. Until the build is finalized the builder works on a workspaceitem to add metadata,
     * files, grant license, etc. The builder could result in a null pooltask if the selected collection doesn't
     * have a workflow enabled
     * 
     * @param context
     *            the dspace context
     * @param col
     *            the collection where the submission will occur
     * @param user
     *            the user that will own the pool task
     * @return a PoolTaskBuilder
     */
    public static PoolTaskBuilder createPoolTask(final Context context, final Collection col, final EPerson user) {
        PoolTaskBuilder builder = new PoolTaskBuilder(context);
        return builder.create(context, col, user);
    }

    private PoolTaskBuilder create(final Context context, final Collection col, final EPerson user) {
        this.context = context;
        this.user = user;
        try {
            workspaceItem = workspaceItemService.create(context, col, false);
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    @Override
    public PoolTask build() {
        try {
            workflowItem = workflowService.start(context, workspaceItem);
            workspaceItem = null;
            poolTask = getService().findByWorkflowIdAndEPerson(context, workflowItem, user);
            context.dispatchEvents();
            indexingService.commit();
            return poolTask;
        } catch (Exception e) {
            return handleException(e);
        }

    }

    @Override
    public void delete(PoolTask poolTask) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            PoolTask attachedPoolTask = c.reloadEntity(poolTask);
            if (attachedPoolTask != null) {
                // to delete a pooltask keeping the system in a consistent state you need to delete the underline
                // workflowitem
                WorkflowItemBuilder.deleteWorkflowItem(attachedPoolTask.getWorkflowItem().getID());
            }
            c.complete();
        }
    }

    private void deleteWsi(WorkspaceItem dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            WorkspaceItem attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                workspaceItemService.deleteAll(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }


    @Override
    public void cleanup() throws Exception {
        if (workspaceItem != null) {
            deleteWsi(workspaceItem);
        }
        if (workflowItem != null) {
            // to delete the pooltask keeping the system in a consistent state you need to delete the underline
            // workflowitem
            WorkflowItemBuilder.deleteWorkflowItem(workflowItem.getID());
        }
    }

    @Override
    protected PoolTaskService getService() {
        return poolTaskService;
    }

    protected PoolTaskBuilder addMetadataValue(final String schema,
            final String element, final String qualifier, final String value) {
        try {
            itemService.addMetadata(context, workspaceItem.getItem(), schema, element, qualifier, Item.ANY, value);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    protected PoolTaskBuilder setMetadataSingleValue(final String schema,
            final String element, final String qualifier, final String value) {
        try {
            itemService.setMetadataSingleValue(context, workspaceItem.getItem(), schema, element, qualifier, Item.ANY,
                    value);
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    public PoolTaskBuilder withTitle(final String title) {
        return setMetadataSingleValue(MetadataSchemaEnum.DC.getName(), "title", null, title);
    }

    public PoolTaskBuilder withIssueDate(final String issueDate) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "date", "issued", new DCDate(issueDate).toString());
    }

    public PoolTaskBuilder withAuthor(final String authorName) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "contributor", "author", authorName);
    }

    public PoolTaskBuilder withSubject(final String subject) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "subject", null, subject);
    }

    public PoolTaskBuilder grantLicense() {
        Item item = workspaceItem.getItem();
        String license;
        try {
            EPerson submitter = workspaceItem.getSubmitter();
            submitter = context.reloadEntity(submitter);
            license = LicenseUtils.getLicenseText(context.getCurrentLocale(), workspaceItem.getCollection(), item,
                    submitter);
            LicenseUtils.grantLicense(context, item, license, null);
        } catch (Exception e) {
            handleException(e);
        }
        return this;
    }

    public PoolTaskBuilder withFulltext(String name, String source, InputStream is) {
        try {
            Item item = workspaceItem.getItem();
            Bitstream b = itemService.createSingleBitstream(context, is, item);
            b.setName(context, name);
            b.setSource(context, source);
        } catch (Exception e) {
            handleException(e);
        }
        return this;
    }
}
