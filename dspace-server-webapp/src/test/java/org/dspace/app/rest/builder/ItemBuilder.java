/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * Builder to construct Item objects
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class ItemBuilder extends AbstractDSpaceObjectBuilder<Item> {

    private boolean withdrawn = false;
    private WorkspaceItem workspaceItem;
    private Item item;
    private Group readerGroup = null;

    protected ItemBuilder(Context context) {
        super(context);
    }

    public static ItemBuilder createItem(final Context context, final Collection col) {
        ItemBuilder builder = new ItemBuilder(context);
        return builder.create(context, col);
    }

    private ItemBuilder create(final Context context, final Collection col) {
        this.context = context;

        try {
            workspaceItem = workspaceItemService.create(context, col, false);
            item = workspaceItem.getItem();
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    public ItemBuilder withTitle(final String title) {
        return setMetadataSingleValue(item, MetadataSchemaEnum.DC.getName(), "title", null, title);
    }

    public ItemBuilder withIssueDate(final String issueDate) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(),
                                "date", "issued", new DCDate(issueDate).toString());
    }

    public ItemBuilder withIdentifierOther(final String identifierOther) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "identifier", "other", identifierOther);
    }

    public ItemBuilder withAuthor(final String authorName) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "contributor", "author", authorName);
    }

    public ItemBuilder withPersonIdentifierFirstName(final String personIdentifierFirstName) {
        return addMetadataValue(item, "person", "givenName", null, personIdentifierFirstName);
    }

    public ItemBuilder withPersonIdentifierLastName(final String personIdentifierLastName) {
        return addMetadataValue(item, "person", "familyName", null, personIdentifierLastName);
    }

    public ItemBuilder withSubject(final String subject) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "subject", null, subject);
    }

    public ItemBuilder withRelationshipType(final String relationshipType) {
        return addMetadataValue(item, "relationship", "type", null, relationshipType);
    }

    public ItemBuilder withPublicationIssueNumber(final String issueNumber) {
        return addMetadataValue(item, "publicationissue", "issueNumber", null, issueNumber);
    }

    public ItemBuilder withPublicationVolumeNumber(final String volumeNumber) {
        return addMetadataValue(item, "publicationvolume", "volumeNumber", null, volumeNumber);
    }

    public ItemBuilder makeUnDiscoverable() {
        item.setDiscoverable(false);
        return this;
    }

    /**
     * Withdrawn the item under build. Please note that an user need to be loggedin the context to avoid NPE during the
     * creation of the provenance metadata
     *
     * @return the ItemBuilder
     */
    public ItemBuilder withdrawn() {
        withdrawn = true;
        return this;
    }

    public ItemBuilder withEmbargoPeriod(String embargoPeriod) {
        return setEmbargo(embargoPeriod, item);
    }

    public ItemBuilder withReaderGroup(Group group) {
        readerGroup = group;
        return this;
    }

    @Override
    public Item build() {
        try {
            installItemService.installItem(context, workspaceItem);
            itemService.update(context, item);

            //Check if we need to make this item private. This has to be done after item install.
            if (readerGroup != null) {
                setOnlyReadPermission(workspaceItem.getItem(), readerGroup, null);
            }

            if (withdrawn) {
                itemService.withdraw(context, item);
            }

            context.dispatchEvents();

            indexingService.commit();
            return item;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public void cleanup() throws Exception {
        delete(item);
    }

    @Override
    protected DSpaceObjectService<Item> getService() {
        return itemService;
    }
}
