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
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Builder to construct WorkspaceItem objects
 *
 **/
public class WorkspaceItemBuilder extends AbstractBuilder<WorkspaceItem, WorkspaceItemService> {

    /** Keep a reference to the underlying Item for cleanup **/
    private Item item;

    private WorkspaceItem workspaceItem;

    protected WorkspaceItemBuilder(Context context) {
        super(context);
    }

    public static WorkspaceItemBuilder createWorkspaceItem(final Context context, final Collection col) {
        WorkspaceItemBuilder builder = new WorkspaceItemBuilder(context);
        return builder.create(context, col);
    }

    private WorkspaceItemBuilder create(final Context context, final Collection col) {
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
    public WorkspaceItem build() {
        try {
            context.dispatchEvents();
            indexingService.commit();
            return workspaceItem;
        } catch (Exception e) {
            return handleException(e);
        }

    }

    private void deleteItem(Item dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Item attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                itemService.delete(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }

    @Override
    public void delete(WorkspaceItem dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            WorkspaceItem attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                getService().deleteAll(c, attachedDso);
                item = null;
            }
            c.complete();
        }

        indexingService.commit();
    }

    @Override
    public void cleanup() throws Exception {
        delete(workspaceItem);
        if (item != null) {
            deleteItem(item);
        }
    }

    @Override
    protected WorkspaceItemService getService() {
        return workspaceItemService;
    }

    protected WorkspaceItemBuilder addMetadataValue(final String schema,
            final String element, final String qualifier, final String value) {
        try {
            itemService.addMetadata(context, workspaceItem.getItem(), schema, element, qualifier, Item.ANY, value);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    protected WorkspaceItemBuilder setMetadataSingleValue(final String schema,
            final String element, final String qualifier, final String value) {
        try {
            itemService.setMetadataSingleValue(context, workspaceItem.getItem(), schema, element, qualifier, Item.ANY,
                    value);
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    public WorkspaceItemBuilder withTitle(final String title) {
        return setMetadataSingleValue(MetadataSchemaEnum.DC.getName(), "title", null, title);
    }

    public WorkspaceItemBuilder withIssueDate(final String issueDate) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "date", "issued", new DCDate(issueDate).toString());
    }

    public WorkspaceItemBuilder withAuthor(final String authorName) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "contributor", "author", authorName);
    }

    public WorkspaceItemBuilder withSubject(final String subject) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "subject", null, subject);
    }

    public WorkspaceItemBuilder withAbstract(final String descriptionAbstract) {
        return addMetadataValue(MetadataSchemaEnum.DC.getName(), "description", "abstract", descriptionAbstract);
    }

    public WorkspaceItemBuilder grantLicense() {
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

    public WorkspaceItemBuilder withFulltext(String name, String source, InputStream is) {
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
