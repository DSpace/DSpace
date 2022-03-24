/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

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
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;

/**
 * Builder to construct PoolTask objects
 *
 **/
public class ClaimedTaskBuilder extends AbstractBuilder<ClaimedTask, ClaimedTaskService> {

    private WorkspaceItem workspaceItem;

    private XmlWorkflowItem workflowItem;

    private ClaimedTask claimedTask;

    private EPerson user;

    protected ClaimedTaskBuilder(Context context) {
        super(context);
    }

    /**
     * Create a ClaimedTaskBuilder. Until the build is finalized the builder works on a workspaceitem to add metadata,
     * files, grant license, etc. The builder could result in a null claimedtask if the selected collection doesn't
     * have a workflow enabled
     * 
     * @param context
     *            the dspace context
     * @param col
     *            the collection where the submission will occur
     * @param user
     *            the user that will own the claimed task
     * @return a ClaimedTaskBuilder
     */
    public static ClaimedTaskBuilder createClaimedTask(final Context context, final Collection col,
            final EPerson user) {
        ClaimedTaskBuilder builder = new ClaimedTaskBuilder(context);
        return builder.create(context, col, user);
    }

    private ClaimedTaskBuilder create(final Context context, final Collection col, final EPerson user) {
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
    public ClaimedTask build() {
        try {
            workflowItem = workflowService.start(context, workspaceItem);
            workspaceItem = null;
            PoolTask task = poolTaskService.findByWorkflowIdAndEPerson(context, workflowItem, user);

            // temporary switch to the wf user
            EPerson submitter = context.getCurrentUser();
            context.setCurrentUser(user);
            XmlWorkflowServiceFactory factory = XmlWorkflowServiceFactory.getInstance();
            Workflow workflow = factory.getWorkflowFactory().getWorkflow(task.getWorkflowItem().getCollection());
            Step step = workflow.getStep(task.getStepID());
            WorkflowActionConfig currentActionConfig = step.getActionConfig(task.getActionID());
            workflowService
                .doState(context, user, null, task.getWorkflowItem().getID(), workflow,
                    currentActionConfig);
            claimedTask = getService().findByWorkflowIdAndEPerson(context, workflowItem, user);
            // restore the submitter as current user
            context.setCurrentUser(submitter);
            context.dispatchEvents();
            indexingService.commit();
            return claimedTask;
        } catch (Exception e) {
            return handleException(e);
        }

    }

    @Override
    public void delete(Context c, ClaimedTask claimedTask) throws Exception {
        if (claimedTask != null) {
            // to delete a pooltask keeping the system in a consistent state you need to delete the underline
            // workflowitem
            WorkflowItemBuilder.deleteWorkflowItem(claimedTask.getWorkflowItem().getID());
        }
    }

    private void deleteWsi(Context c, WorkspaceItem dso) throws Exception {
        if (dso != null) {
            workspaceItemService.deleteAll(c, dso);
        }
    }


    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            workspaceItem = c.reloadEntity(workspaceItem);
            workflowItem = c.reloadEntity(workflowItem);
            if (workspaceItem != null) {
                deleteWsi(c, workspaceItem);
            }
            if (workflowItem != null) {
            // to delete the claimedtask keeping the system in a consistent state you need to delete the underline
            // workflowitem
            WorkflowItemBuilder.deleteWorkflowItem(workflowItem.getID());
            }
        }
    }

    @Override
    protected ClaimedTaskService getService() {
        return claimedTaskService;
    }

    protected ClaimedTaskBuilder addMetadataValue(final String schema,
            final String element, final String qualifier, final String value) {
        try {
            itemService.addMetadata(context, workspaceItem.getItem(), schema, element, qualifier, Item.ANY, value);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    protected ClaimedTaskBuilder setMetadataSingleValue(final String schema,
            final String element, final String qualifier, final String value) {
        try {
            itemService.setMetadataSingleValue(context, workspaceItem.getItem(), schema, element, qualifier, Item.ANY,
                    value);
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    public ClaimedTaskBuilder withTitle(final String title) {
        return setMetadataSingleValue(MetadataSchemaEnum.DC.getName(), "title", null, title);
    }

    public ClaimedTaskBuilder withIssueDate(final String issueDate) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "date", "issued", new DCDate(issueDate).toString());
    }

    public ClaimedTaskBuilder withAuthor(final String authorName) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "contributor", "author", authorName);
    }

    public ClaimedTaskBuilder withSubject(final String subject) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "subject", null, subject);
    }

    public ClaimedTaskBuilder grantLicense() {
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

    public ClaimedTaskBuilder withFulltext(String name, String source, InputStream is) {
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
