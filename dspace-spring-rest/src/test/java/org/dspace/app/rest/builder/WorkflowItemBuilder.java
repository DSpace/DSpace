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
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;

/**
 * Builder to construct WorkflowItem objects
 *
 **/
public class WorkflowItemBuilder extends AbstractBuilder<XmlWorkflowItem, XmlWorkflowItemService> {

    private WorkspaceItem workspaceItem;

    private XmlWorkflowItem workflowItem;

    protected WorkflowItemBuilder(Context context) {
        super(context);
    }

    /**
     * Create a WorkflowItemBuilder. Until the build is finalized the builder works on a workspaceitem to add metadata,
     * files, grant license, etc. The builder could result in a null workflowItem if the selected collection doesn't
     * have a workflow enabled
     * 
     * @param context
     *            the dspace context
     * @param col
     *            the collection where the submission will occur
     * @return a WorkflowItemBuilder
     */
    public static WorkflowItemBuilder createWorkflowItem(final Context context, final Collection col) {
        WorkflowItemBuilder builder = new WorkflowItemBuilder(context);
        return builder.create(context, col);
    }

    private WorkflowItemBuilder create(final Context context, final Collection col) {
        this.context = context;

        try {
            workspaceItem = workspaceItemService.create(context, col, false);
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    @Override
    public XmlWorkflowItem build() {
        try {
            workflowItem = workflowService.start(context, workspaceItem);
            workspaceItem = null;
            return workflowItem;
        } catch (Exception e) {
            return handleException(e);
        }

    }

    @Override
    public void delete(XmlWorkflowItem dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            XmlWorkflowItem attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                getService().delete(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
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
    protected void cleanup() throws Exception {
        if (workspaceItem != null) {
            deleteWsi(workspaceItem);
        }
        if (workflowItem != null) {
            delete(workflowItem);
        }
    }

    @Override
    protected XmlWorkflowItemService getService() {
        return workflowItemService;
    }

    protected WorkflowItemBuilder addMetadataValue(final String schema,
            final String element, final String qualifier, final String value) {
        try {
            itemService.addMetadata(context, workspaceItem.getItem(), schema, element, qualifier, Item.ANY, value);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    protected WorkflowItemBuilder setMetadataSingleValue(final String schema,
            final String element, final String qualifier, final String value) {
        try {
            itemService.setMetadataSingleValue(context, workspaceItem.getItem(), schema, element, qualifier, Item.ANY,
                    value);
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    public WorkflowItemBuilder withTitle(final String title) {
        return setMetadataSingleValue(MetadataSchema.DC_SCHEMA, "title", null, title);
    }

    public WorkflowItemBuilder withIssueDate(final String issueDate) {
        return addMetadataValue(MetadataSchema.DC_SCHEMA, "date", "issued", new DCDate(issueDate).toString());
    }

    public WorkflowItemBuilder withAuthor(final String authorName) {
        return addMetadataValue(MetadataSchema.DC_SCHEMA, "contributor", "author", authorName);
    }

    public WorkflowItemBuilder withSubject(final String subject) {
        return addMetadataValue(MetadataSchema.DC_SCHEMA, "subject", null, subject);
    }

    public WorkflowItemBuilder grantLicense() {
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

    public WorkflowItemBuilder withFulltext(String name, String source, InputStream is) {
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
