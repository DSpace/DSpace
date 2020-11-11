/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import static org.dspace.content.MetadataSchemaEnum.CRIS;
import static org.dspace.content.MetadataSchemaEnum.DC;
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

    public ItemBuilder withAlternativeTitle(final String title) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "title", "alternative", title);
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

    public ItemBuilder withAuthor(final String authorName, final String authority) {
        return addMetadataValue(item, DC.getName(), "contributor", "author", null, authorName, authority, 600);
    }

    public ItemBuilder withAuthorAffiliation(String affiliation) {
        return addMetadataValue(item, "oairecerif", "author", "affiliation", affiliation);
    }

    public ItemBuilder withEditor(final String editorName) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "contributor", "editor", editorName);
    }

    public ItemBuilder withEditorAffiliation(String affiliation) {
        return addMetadataValue(item, "oairecerif", "editor", "affiliation", affiliation);
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

    public ItemBuilder withIsbnIdentifier(String isbn) {
        return addMetadataValue(item, "dc", "identifier", "isbn", isbn);
    }

    public ItemBuilder withIssnIdentifier(String issn) {
        return addMetadataValue(item, "dc", "identifier", "issn", issn);
    }

    public ItemBuilder withIsiIdentifier(String issn) {
        return addMetadataValue(item, "dc", "identifier", "isi", issn);
    }

    public ItemBuilder withScopusIdentifier(String issn) {
        return addMetadataValue(item, "dc", "identifier", "scopus", issn);
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

    public ItemBuilder withPersonMainAffiliation(final String affiliation, final String authority) {
        return addMetadataValue(item, "person", "affiliation", "name", null, affiliation, authority, 600);
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

    public ItemBuilder withPersonAffiliationName(String name, String authority) {
        return addMetadataValue(item, "person", "affiliation", "name", null, name, authority, 600);
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

    public ItemBuilder withDescription(String description) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "description", null, description);
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

    public ItemBuilder withPersonKnowsLanguages(String languages) {
        return addMetadataValue(item, "person", "knowsLanguage", null, languages);
    }

    public ItemBuilder withRelationProject(String project, String authority) {
        return addMetadataValue(item, DC.getName(), "relation", "project", null, project, authority, 600);
    }

    public ItemBuilder withRelationFunding(String funding, String authority) {
        return addMetadataValue(item, DC.getName(), "relation", "funding", null, funding, authority, 600);
    }

    public ItemBuilder withInternalId(String internalId) {
        return addMetadataValue(item, "oairecerif", "internalid", null, internalId);
    }

    public ItemBuilder withAcronym(String acronym) {
        return addMetadataValue(item, "oairecerif", "acronym", null, acronym);
    }

    public ItemBuilder withProjectStartDate(String startDate) {
        return addMetadataValue(item, "oairecerif", "project", "startDate", startDate);
    }

    public ItemBuilder withProjectEndDate(String endDate) {
        return addMetadataValue(item, "oairecerif", "project", "endDate", endDate);
    }

    public ItemBuilder withProjectStatus(String status) {
        return addMetadataValue(item, "oairecerif", "project", "status", status);
    }

    public ItemBuilder withProjectPartner(String partner) {
        return addMetadataValue(item, "crispj", "partnerou", null, partner);
    }

    public ItemBuilder withProjectOrganization(String organization) {
        return addMetadataValue(item, "crispj", "organization", null, organization);
    }

    public ItemBuilder withProjectInvestigator(String investigator) {
        return addMetadataValue(item, "crispj", "investigator", null, investigator);
    }

    public ItemBuilder withProjectCoinvestigators(String coinvestigators) {
        return addMetadataValue(item, "crispj", "coinvestigators", null, coinvestigators);
    }

    public ItemBuilder withProjectCoordinator(String coordinator) {
        return addMetadataValue(item, "crispj", "coordinator", null, coordinator);
    }

    public ItemBuilder withProjectCoordinator(String coordinator, String authority) {
        return addMetadataValue(item, "crispj", "coordinator", null, null, coordinator, authority, 600);
    }

    public ItemBuilder withType(String type) {
        return addMetadataValue(item, "dc", "type", null, type);
    }

    public ItemBuilder withLanguage(String language) {
        return addMetadataValue(item, "dc", "language", "iso", language);
    }

    public ItemBuilder withFunder(String funder) {
        return addMetadataValue(item, "oairecerif", "funder", null, funder);
    }

    public ItemBuilder withFunder(String funder, String authority) {
        return addMetadataValue(item, "oairecerif", "funder", null, null, funder, authority, 600);
    }

    public ItemBuilder withPublisher(String publisher) {
        return addMetadataValue(item, "dc", "publisher", null, publisher);
    }

    public ItemBuilder withRelationPublication(String publication) {
        return addMetadataValue(item, "dc", "relation", "publication", publication);
    }

    public ItemBuilder withRelationDoi(String doi) {
        return addMetadataValue(item, "dc", "relation", "doi", doi);
    }

    public ItemBuilder withRelationConference(String conference) {
        return addMetadataValue(item, "dc", "relation", "conference", conference);
    }

    public ItemBuilder withRelationDataset(String dataset) {
        return addMetadataValue(item, "dc", "relation", "dataset", dataset);
    }

    public ItemBuilder withRelationEquipment(String equipment) {
        return addMetadataValue(item, "dc", "relation", "equipment", equipment);
    }

    public ItemBuilder withVolume(String volume) {
        return addMetadataValue(item, "oaire", "citation", "volume", volume);
    }

    public ItemBuilder withIssue(String issue) {
        return addMetadataValue(item, "oaire", "citation", "issue", issue);
    }

    public ItemBuilder withIsPartOf(String isPartOf) {
        return addMetadataValue(item, "dc", "relation", "ispartof", isPartOf);
    }

    public ItemBuilder withCitationStartPage(String startPage) {
        return addMetadataValue(item, "oaire", "citation", "startPage", startPage);
    }

    public ItemBuilder withCitationEndPage(String endPage) {
        return addMetadataValue(item, "oaire", "citation", "endPage", endPage);
    }

    public ItemBuilder withOpenaireId(String openaireid) {
        return addMetadataValue(item, "crispj", "openaireid", null, openaireid);
    }

    public ItemBuilder makeUnDiscoverable() {
        item.setDiscoverable(false);
        return this;
    }

    public ItemBuilder withUrlIdentifier(String urlIdentifier) {
        return addMetadataValue(item, "oairecerif", "identifier", "url", urlIdentifier);
    }

    public ItemBuilder withOAMandate(String oamandate) {
        return addMetadataValue(item, "oairecerif", "oamandate", null, oamandate);
    }

    public ItemBuilder withOAMandateURL(String oamandateUrl) {
        return addMetadataValue(item, "oairecerif", "oamandate", "url", oamandateUrl);
    }

    public ItemBuilder withEquipmentOwnerOrgUnit(String ownerOrgUnit) {
        return addMetadataValue(item, "crisequipment", "ownerou", null, ownerOrgUnit);
    }

    public ItemBuilder withEquipmentOwnerPerson(String ownerPerson) {
        return addMetadataValue(item, "crisequipment", "ownerrp", null, ownerPerson);
    }

    public ItemBuilder withOrgUnitLegalName(String legalName) {
        return addMetadataValue(item, "organization", "legalName", null, legalName);
    }

    public ItemBuilder withParentOrganization(String parent) {
        return addMetadataValue(item, "organization", "parentOrganization", null, parent);
    }

    public ItemBuilder withParentOrganization(String parent, String authority) {
        return addMetadataValue(item, "organization", "parentOrganization", null, null, parent, authority, 600);
    }

    public ItemBuilder withOrgUnitIdentifier(String identifier) {
        return addMetadataValue(item, "organization", "identifier", null, identifier);
    }

    public ItemBuilder withFundingIdentifier(String identifier) {
        return addMetadataValue(item, "oairecerif", "funding", "identifier", identifier);
    }

    public ItemBuilder withAmount(String amount) {
        return addMetadataValue(item, "oairecerif", "amount", null, amount);
    }

    public ItemBuilder withAmountCurrency(String currency) {
        return addMetadataValue(item, "oairecerif", "amount", "currency", currency);
    }

    public ItemBuilder withFundingStartDate(String startDate) {
        return addMetadataValue(item, "oairecerif", "funding", "startDate", startDate);
    }

    public ItemBuilder withFundingEndDate(String endDate) {
        return addMetadataValue(item, "oairecerif", "funding", "endDate", endDate);
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
