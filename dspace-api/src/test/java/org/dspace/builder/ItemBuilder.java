/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import static org.dspace.content.LicenseUtils.getLicenseText;
import static org.dspace.content.MetadataSchemaEnum.DC;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.profile.OrcidEntitySyncPreference;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.app.profile.OrcidSynchronizationMode;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
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
    private WorkspaceItem workspaceItem;
    private Item item;
    private Group readerGroup = null;
    private String handle = null;

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

    public ItemBuilder withAuthor(final String authorName, final String authority) {
        return addMetadataValue(item, DC.getName(), "contributor", "author", null, authorName, authority, 600);
    }

    public ItemBuilder withAuthor(final String authorName, final String authority, final int confidence) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "contributor", "author",
                                null, authorName, authority, confidence);
    }

    public ItemBuilder withEditor(final String editorName) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "contributor", "editor", editorName);
    }

    public ItemBuilder withDescriptionAbstract(String description) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "description", "abstract", description);
    }

    public ItemBuilder withLanguage(String language) {
        return addMetadataValue(item, "dc", "language", "iso", language);
    }

    public ItemBuilder withIsPartOf(String isPartOf) {
        return addMetadataValue(item, "dc", "relation", "ispartof", isPartOf);
    }

    public ItemBuilder withDoiIdentifier(String doi) {
        return addMetadataValue(item, "dc", "identifier", "doi", doi);
    }

    public ItemBuilder withScopusIdentifier(String scopus) {
        return addMetadataValue(item, "dc", "identifier", "scopus", scopus);
    }

    public ItemBuilder withRelationFunding(String funding) {
        return addMetadataValue(item, "dc", "relation", "funding", funding);
    }

    public ItemBuilder withRelationFunding(String funding, String authority) {
        return addMetadataValue(item, DC.getName(), "relation", "funding", null, funding, authority, 600);
    }

    public ItemBuilder withRelationGrantno(String grantno) {
        return addMetadataValue(item, "dc", "relation", "grantno", grantno);
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

    public ItemBuilder withSubject(final String subject, final String authority, final int confidence) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "subject", null, null,
                                subject, authority, confidence);
    }

    public ItemBuilder withType(final String type) {
        return addMetadataValue(item, "dc", "type", null, type);
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

    public ItemBuilder enableIIIF() {
        return addMetadataValue(item, "dspace", "iiif", "enabled", "true");
    }

    public ItemBuilder disableIIIF() {
        return addMetadataValue(item, "dspace", "iiif", "enabled", "false");
    }

    public ItemBuilder enableIIIFSearch() {
        return addMetadataValue(item, "iiif", "search", "enabled", "true");
    }

    public ItemBuilder withIIIFViewingHint(String hint) {
        return addMetadataValue(item, "iiif", "viewing", "hint", hint);
    }

    public ItemBuilder withIIIFCanvasNaming(String naming) {
        return addMetadataValue(item, "iiif", "canvas", "naming", naming);
    }

    public ItemBuilder withIIIFCanvasWidth(int i) {
        return addMetadataValue(item, "iiif", "image", "width", String.valueOf(i));
    }

    public ItemBuilder withIIIFCanvasHeight(int i) {
        return addMetadataValue(item, "iiif", "image", "height", String.valueOf(i));
    }

    public ItemBuilder withMetadata(final String schema, final String element, final String qualifier,
        final String value) {
        return addMetadataValue(item, schema, element, qualifier, value);
    }

    public ItemBuilder withDspaceObjectOwner(String value, String authority) {
        return addMetadataValue(item, "dspace", "object", "owner", null, value, authority, CF_ACCEPTED);
    }

    public ItemBuilder withOrcidIdentifier(String orcid) {
        return addMetadataValue(item, "person", "identifier", "orcid", orcid);
    }

    public ItemBuilder withOrcidAccessToken(String accessToken) {
        return addMetadataValue(item, "dspace", "orcid", "access-token", accessToken);
    }

    public ItemBuilder withOrcidAuthenticated(String authenticated) {
        return addMetadataValue(item, "dspace", "orcid", "authenticated", authenticated);
    }

    public ItemBuilder withOrcidSynchronizationPublicationsPreference(OrcidEntitySyncPreference value) {
        return withOrcidSynchronizationPublicationsPreference(value.name());
    }

    public ItemBuilder withOrcidSynchronizationPublicationsPreference(String value) {
        return setMetadataSingleValue(item, "dspace", "orcid", "sync-publications", value);
    }

    public ItemBuilder withOrcidSynchronizationFundingsPreference(OrcidEntitySyncPreference value) {
        return withOrcidSynchronizationFundingsPreference(value.name());
    }

    public ItemBuilder withOrcidSynchronizationFundingsPreference(String value) {
        return setMetadataSingleValue(item, "dspace", "orcid", "sync-fundings", value);
    }

    public ItemBuilder withOrcidSynchronizationProfilePreference(OrcidProfileSyncPreference value) {
        return withOrcidSynchronizationProfilePreference(value.name());
    }

    public ItemBuilder withOrcidSynchronizationProfilePreference(String value) {
        return addMetadataValue(item, "dspace", "orcid", "sync-profile", value);
    }

    public ItemBuilder withOrcidSynchronizationMode(OrcidSynchronizationMode mode) {
        return withOrcidSynchronizationMode(mode.name());
    }

    private ItemBuilder withOrcidSynchronizationMode(String mode) {
        return setMetadataSingleValue(item, "dspace", "orcid", "sync-mode", mode);
    }

    public ItemBuilder withPersonCountry(String country) {
        return addMetadataValue(item, "person", "country", null, country);
    }

    public ItemBuilder withScopusAuthorIdentifier(String id) {
        return addMetadataValue(item, "person", "identifier", "scopus-author-id", id);
    }

    public ItemBuilder withResearcherIdentifier(String rid) {
        return addMetadataValue(item, "person", "identifier", "rid", rid);
    }

    public ItemBuilder withUrlIdentifier(String urlIdentifier) {
        return addMetadataValue(item, "oairecerif", "identifier", "url", urlIdentifier);
    }

    public ItemBuilder withVernacularName(String vernacularName) {
        return setMetadataSingleValue(item, "person", "name", "translated", vernacularName);
    }

    public ItemBuilder withVariantName(String variant) {
        return addMetadataValue(item, "person", "name", "variant", variant);
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

    public ItemBuilder withFundingIdentifier(String identifier) {
        return addMetadataValue(item, "oairecerif", "funding", "identifier", identifier);
    }

    public ItemBuilder withFundingAwardUrl(String url) {
        return addMetadataValue(item, "crisfund", "award", "url", url);
    }

    public ItemBuilder withOrgUnitCountry(String addressCountry) {
        return addMetadataValue(item, "organization", "address", "addressCountry", addressCountry);
    }

    public ItemBuilder withOrgUnitLocality(String addressLocality) {
        return addMetadataValue(item, "organization", "address", "addressLocality", addressLocality);
    }

    public ItemBuilder withOrgUnitCrossrefIdentifier(String crossrefid) {
        return addMetadataValue(item, "organization", "identifier", "crossrefid", crossrefid);
    }

    public ItemBuilder withFundingStartDate(String startDate) {
        return addMetadataValue(item, "oairecerif", "funding", "startDate", startDate);
    }

    public ItemBuilder withFundingEndDate(String endDate) {
        return addMetadataValue(item, "oairecerif", "funding", "endDate", endDate);
    }

    public ItemBuilder withFunder(String funder) {
        return addMetadataValue(item, "oairecerif", "funder", null, funder);
    }

    public ItemBuilder withFunder(String funder, String authority) {
        return addMetadataValue(item, "oairecerif", "funder", null, null, funder, authority, 600);
    }

    public ItemBuilder withFundingInvestigator(String investigator) {
        return addMetadataValue(item, "crisfund", "investigators", null, investigator);
    }

    public ItemBuilder withFundingInvestigator(String investigator, String authority) {
        return addMetadataValue(item, "crisfund", "investigators", null, null, investigator, authority, 600);
    }

    public ItemBuilder withFundingCoInvestigator(String investigator) {
        return addMetadataValue(item, "crisfund", "coinvestigators", null, investigator);
    }

    public ItemBuilder withFundingCoInvestigator(String investigator, String authority) {
        return addMetadataValue(item, "crisfund", "coinvestigators", null, null, investigator, authority, 600);
    }

    public ItemBuilder withInternalId(String internalId) {
        return addMetadataValue(item, "oairecerif", "internalid", null, internalId);
    }

    public ItemBuilder withDescription(String description) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "description", null, description);
    }

    public ItemBuilder withAmount(String amount) {
        return addMetadataValue(item, "oairecerif", "amount", null, amount);
    }

    public ItemBuilder withAmountCurrency(String currency) {
        return addMetadataValue(item, "oairecerif", "amount", "currency", currency);
    }

    /**
     * Create an admin group for the collection with the specified members
     *
     * @param ePerson epersons to add to the admin group
     * @return this builder
     * @throws SQLException
     * @throws AuthorizeException
     */
    public ItemBuilder withAdminUser(EPerson ePerson) throws SQLException, AuthorizeException {
        return setAdminPermission(item, ePerson, null);
    }

    public ItemBuilder withPersonEmail(String email) {
        return addMetadataValue(item, "person", "email", null, email);
    }

    @Override
    public Item build() {
        try {
            installItemService.installItem(context, workspaceItem, handle);
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
            c.setDispatcher("noindex");
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

    public ItemBuilder grantLicense() {
        String license;
        try {
            EPerson submitter = workspaceItem.getSubmitter();
            submitter = context.reloadEntity(submitter);
            license = getLicenseText(context.getCurrentLocale(), workspaceItem.getCollection(), item, submitter);
            LicenseUtils.grantLicense(context, item, license, null);
        } catch (Exception e) {
            handleException(e);
        }
        return this;
    }

    public ItemBuilder withAuthorAffiliation(String affiliation) {
        return addMetadataValue(item, "oairecerif", "author", "affiliation", affiliation);
    }
}
