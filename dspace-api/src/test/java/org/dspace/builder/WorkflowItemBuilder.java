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
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;

/**
 * Builder to construct WorkflowItem objects
 *
 **/
public class WorkflowItemBuilder extends AbstractBuilder<XmlWorkflowItem, XmlWorkflowItemService> {

    /** Keep a reference to the underlying Item for cleanup **/
    private Item item;

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
            item = workspaceItem.getItem();
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
            context.dispatchEvents();
            indexingService.commit();
            return workflowItem;
        } catch (Exception e) {
            return handleException(e);
        }

    }

    @Override
    public void delete(Context c, XmlWorkflowItem dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
            item = null;
        }
    }

    private void deleteWsi(Context c, WorkspaceItem dso) throws Exception {
        if (dso != null) {
            workspaceItemService.deleteAll(c, dso);
            item = null;
        }
    }

    private void deleteItem(Context c, Item dso) throws Exception {
        if (dso != null) {
            // if we still have a reference to an item it could be an approved workflow or a rejected one. In the
            // last case we need to remove the "new" workspaceitem
            WorkspaceItem wi = workspaceItemService.findByItem(c, item);
            if (wi != null) {
                workspaceItemService.deleteAll(c, wi);
            } else {
                itemService.delete(c, dso);
            }
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
            item = c.reloadEntity(item);
            if (workspaceItem != null) {
                deleteWsi(c, workspaceItem);
            }
            if (workflowItem != null) {
                delete(c, workflowItem);
            }
            if (item != null) {
                deleteItem(c, item);
            }
            c.complete();
            indexingService.commit();
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

    /**
     * Set the person who submitted the Item.
     *
     * @param ePerson the submitter.
     * @return this builder.
     */
    public WorkflowItemBuilder withSubmitter(EPerson ePerson) {
        workspaceItem.getItem().setSubmitter(ePerson);
        return this;
    }

    /**
     * Set the dc.title field.
     *
     * @param title The Item's title.
     * @return this builder.
     */
    public WorkflowItemBuilder withTitle(final String title) {
        return setMetadataSingleValue(MetadataSchemaEnum.DC.getName(), "title", null, title);
    }

    /**
     * Set the dc.date.issued field.
     *
     * @param issueDate ISO-8601 date without timezone.
     * @return this builder.
     */
    public WorkflowItemBuilder withIssueDate(final String issueDate) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "date", "issued", new DCDate(issueDate).toString());
    }

    /**
     * Set the dc.contributor.author field.
     *
     * @param authorName Author's full name.
     * @return this builder.
     */
    public WorkflowItemBuilder withAuthor(final String authorName) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "contributor", "author", authorName);
    }

    /**
     * Set the dc.subject field.
     *
     * @param subject the subject of the Item.
     * @return this builder.
     */
    public WorkflowItemBuilder withSubject(final String subject) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "subject", null, subject);
    }

    /**
     * Grant the owning Collection's license to the Item.
     *
     * @return this builder.
     */
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

    /**
     * Add to the Item a Bitstream in the ORIGINAL Bundle.
     *
     * @param name name of the Bitstream.
     * @param source source of the content.
     * @param is the content of the Bitstream.
     * @return this builder.
     */
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

    /**
     * Delete a workflow item and all the underlying resources committing the changes to SOLR as well
     * 
     * @param id
     *            the id of the workflowitem to delete
     * @throws SQLException
     * @throws IOException
     * @throws SearchServiceException
     */
    public static void deleteWorkflowItem(Integer id)
            throws SQLException, IOException, SearchServiceException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            XmlWorkflowItem wi = workflowItemService.find(c, id);
            if (wi != null) {
                try {
                    workflowItemService.delete(c, wi);
                } catch (AuthorizeException e) {
                    // cannot occur, just wrap it to make the compiler happy
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            c.complete();
        }
        indexingService.commit();
    }

}
