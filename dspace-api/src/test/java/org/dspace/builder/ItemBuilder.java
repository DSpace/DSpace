/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import static org.dspace.content.MetadataSchemaEnum.CRIS;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Builder to construct Item objects
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class ItemBuilder extends AbstractDSpaceObjectBuilder<Item> {

    private boolean withdrawn = false;
    private String handle = null;
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

    public ItemBuilder withTitleForLanguage(final String title, final String language) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "title", null, language, title);
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

    public ItemBuilder withProvenanceData(final String provenanceData) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "description", "provenance", provenanceData);
    }

    public ItemBuilder withCrisOwner(String value, String authority) {
        return addMetadataValue(item, CRIS.getName(), "owner", null, null, value, authority, CF_ACCEPTED);
    }

    public ItemBuilder withDoiIdentifier(String doi) {
        return addMetadataValue(item, "dc", "identifier", "doi", doi);
    }

    public ItemBuilder withOrcidIdentifier(String orcid) {
        return addMetadataValue(item, "person", "identifier", "orcid", orcid);
    }

    public ItemBuilder withIsniIdentifier(String isni) {
        return addMetadataValue(item, "person", "identifier", "isni", isni);
    }

    public ItemBuilder withResearcherIdentifier(String rid) {
        return addMetadataValue(item, "person", "identifier", "rid", rid);
    }

    public ItemBuilder withScopusAuthorIdentifier(String id) {
        return addMetadataValue(item, "person", "identifier", "scopus-author-id", id);
    }

    public ItemBuilder withPatentNo(String patentNo) {
        return addMetadataValue(item, "dc", "identifier", "patentno", patentNo);
    }

    public ItemBuilder withFullName(String fullname) {
        return setMetadataSingleValue(item, "crisrp", "name", null, fullname);
    }

    public ItemBuilder withVernacularName(String vernacularName) {
        return setMetadataSingleValue(item, "crisrp", "name", "translated", vernacularName);
    }

    public ItemBuilder withVariantName(String variant) {
        return addMetadataValue(item, "crisrp", "name", "variant", variant);
    }

    public ItemBuilder withGivenName(String givenName) {
        return setMetadataSingleValue(item, "person", "givenName", null, givenName);
    }

    public ItemBuilder withFamilyName(String familyName) {
        return setMetadataSingleValue(item, "person", "familyName", null, familyName);
    }

    public ItemBuilder withBirthDate(String birthDate) {
        return setMetadataSingleValue(item, "person", "birthDate", null, birthDate);
    }

    public ItemBuilder withGender(String gender) {
        return setMetadataSingleValue(item, "oairecerif", "person", "gender", gender);
    }

    public ItemBuilder withJobTitle(String jobTitle) {
        return setMetadataSingleValue(item, "person", "jobTitle", null, jobTitle);
    }

    public ItemBuilder withPersonMainAffiliation(String affiliation) {
        return setMetadataSingleValue(item, "person", "affiliation", "name", affiliation);
    }

    public ItemBuilder withWorkingGroup(String workingGroup) {
        return addMetadataValue(item, "crisrp", "workgroup", null, workingGroup);
    }

    public ItemBuilder withPersonalSiteUrl(String url) {
        return addMetadataValue(item, "oairecerif", "identifier", "url", url);
    }

    public ItemBuilder withPersonalSiteTitle(String title) {
        return addMetadataValue(item, "crisrp", "site", "title", title);
    }

    public ItemBuilder withPersonEmail(String email) {
        return addMetadataValue(item, "person", "email", null, email);
    }

    public ItemBuilder withPersonAffiliation(String affiliation) {
        return addMetadataValue(item, "oairecerif", "person", "affiliation", affiliation);
    }

    public ItemBuilder withPersonAffiliationStartDate(String startDate) {
        return addMetadataValue(item, "oairecerif", "affiliation", "startDate", startDate);
    }

    public ItemBuilder withPersonAffiliationEndDate(String endDate) {
        return addMetadataValue(item, "oairecerif", "affiliation", "endDate", endDate);
    }

    public ItemBuilder withPersonAffiliationRole(String role) {
        return addMetadataValue(item, "oairecerif", "affiliation", "role", role);
    }

    public ItemBuilder withDescriptionAbstract(String description) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "description", "abstract", description);
    }

    public ItemBuilder withPersonEducation(String education) {
        return addMetadataValue(item, "crisrp", "education", null, education);
    }

    public ItemBuilder withPersonEducationStartDate(String startDate) {
        return addMetadataValue(item, "crisrp", "education", "start", startDate);
    }

    public ItemBuilder withPersonEducationEndDate(String endDate) {
        return addMetadataValue(item, "crisrp", "education", "end", endDate);
    }

    public ItemBuilder withPersonEducationRole(String role) {
        return addMetadataValue(item, "crisrp", "education", "role", role);
    }

    public ItemBuilder withPersonCountry(String country) {
        return addMetadataValue(item, "crisrp", "country", null, country);
    }

    public ItemBuilder withPersonQualification(String qualification) {
        return addMetadataValue(item, "crisrp", "qualification", null, qualification);
    }

    public ItemBuilder withPersonQualificationStartDate(String startDate) {
        return addMetadataValue(item, "crisrp", "qualification", "start", startDate);
    }

    public ItemBuilder withPersonQualificationEndDate(String endDate) {
        return addMetadataValue(item, "crisrp", "qualification", "end", endDate);
    }

    public ItemBuilder withPersonKnowsLanguages(String lanugage) {
        return addMetadataValue(item, "person", "knowsLanguage", null, lanugage);
    }

    public ItemBuilder makeUnDiscoverable() {
        item.setDiscoverable(false);
        return this;
    }

    public ItemBuilder withHandle(String handle) {
        this.handle = handle;
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

    /**
     * Create an admin group for the collection with the specified members
     *
     * @param members epersons to add to the admin group
     * @return this builder
     * @throws SQLException
     * @throws AuthorizeException
     */
    public ItemBuilder withAdminUser(EPerson ePerson) throws SQLException, AuthorizeException {
        return setAdminPermission(item, ePerson, null);
    }


    @Override
    public Item build() {
        try {
            installItemService.installItem(context, workspaceItem, this.handle);
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
       try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            item = c.reloadEntity(item);
            if (item != null) {
                 delete(c, item);
                 c.complete();
            }
       }
    }

    @Override
    protected DSpaceObjectService<Item> getService() {
        return itemService;
    }

    /**
     * Delete the Test Item referred to by the given UUID
     * @param uuid UUID of Test Item to delete
     * @throws SQLException
     * @throws IOException
     */
    public static void deleteItem(UUID uuid) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Item item = itemService.find(c, uuid);
            if (item != null) {
                try {
                    itemService.delete(c, item);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e);
                }
            }
            c.complete();
        }
    }
}
