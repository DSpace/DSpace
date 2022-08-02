/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.validator;

import static org.dspace.orcid.model.validator.OrcidValidationError.AMOUNT_CURRENCY_REQUIRED;
import static org.dspace.orcid.model.validator.OrcidValidationError.DISAMBIGUATED_ORGANIZATION_REQUIRED;
import static org.dspace.orcid.model.validator.OrcidValidationError.DISAMBIGUATED_ORGANIZATION_VALUE_REQUIRED;
import static org.dspace.orcid.model.validator.OrcidValidationError.DISAMBIGUATION_SOURCE_INVALID;
import static org.dspace.orcid.model.validator.OrcidValidationError.DISAMBIGUATION_SOURCE_REQUIRED;
import static org.dspace.orcid.model.validator.OrcidValidationError.EXTERNAL_ID_REQUIRED;
import static org.dspace.orcid.model.validator.OrcidValidationError.FUNDER_REQUIRED;
import static org.dspace.orcid.model.validator.OrcidValidationError.ORGANIZATION_ADDRESS_REQUIRED;
import static org.dspace.orcid.model.validator.OrcidValidationError.ORGANIZATION_CITY_REQUIRED;
import static org.dspace.orcid.model.validator.OrcidValidationError.ORGANIZATION_COUNTRY_REQUIRED;
import static org.dspace.orcid.model.validator.OrcidValidationError.ORGANIZATION_NAME_REQUIRED;
import static org.dspace.orcid.model.validator.OrcidValidationError.PUBLICATION_DATE_INVALID;
import static org.dspace.orcid.model.validator.OrcidValidationError.TITLE_REQUIRED;
import static org.dspace.orcid.model.validator.OrcidValidationError.TYPE_REQUIRED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dspace.orcid.model.validator.impl.OrcidValidatorImpl;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.common.WorkType;
import org.orcid.jaxb.model.v3.release.common.Amount;
import org.orcid.jaxb.model.v3.release.common.DisambiguatedOrganization;
import org.orcid.jaxb.model.v3.release.common.Organization;
import org.orcid.jaxb.model.v3.release.common.OrganizationAddress;
import org.orcid.jaxb.model.v3.release.common.PublicationDate;
import org.orcid.jaxb.model.v3.release.common.Title;
import org.orcid.jaxb.model.v3.release.common.Year;
import org.orcid.jaxb.model.v3.release.record.ExternalID;
import org.orcid.jaxb.model.v3.release.record.ExternalIDs;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.FundingTitle;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkTitle;

/**
 * Unit tests for {@link OrcidValidatorImpl}
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class OrcidValidatorTest {

    @Mock(lenient = true)
    private ConfigurationService configurationService;

    @InjectMocks
    private OrcidValidatorImpl validator;

    @Before
    public void before() {
        when(configurationService.getBooleanProperty("orcid.validation.work.enabled", true)).thenReturn(true);
        when(configurationService.getBooleanProperty("orcid.validation.funding.enabled", true)).thenReturn(true);
        when(configurationService.getArrayProperty("orcid.validation.organization.identifier-sources"))
            .thenReturn(new String[] { "RINGGOLD", "GRID", "FUNDREF", "LEI" });
    }

    @Test
    public void testWorkWithoutTitleAndTypeAndExternalIds() {

        List<OrcidValidationError> errors = validator.validateWork(new Work());
        assertThat(errors, hasSize(3));
        assertThat(errors, containsInAnyOrder(TITLE_REQUIRED, TYPE_REQUIRED, EXTERNAL_ID_REQUIRED));
    }

    @Test
    public void testWorkWithoutWorkTitle() {

        Work work = new Work();
        work.setWorkType(WorkType.DATA_SET);
        work.setWorkExternalIdentifiers(new ExternalIDs());
        work.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        List<OrcidValidationError> errors = validator.validateWork(work);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(TITLE_REQUIRED));
    }

    @Test
    public void testWorkWithoutTitle() {

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.setWorkType(WorkType.DATA_SET);
        work.setWorkExternalIdentifiers(new ExternalIDs());
        work.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        List<OrcidValidationError> errors = validator.validateWork(work);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(TITLE_REQUIRED));
    }

    @Test
    public void testWorkWithNullTitle() {

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.getWorkTitle().setTitle(new Title(null));
        work.setWorkType(WorkType.DATA_SET);
        work.setWorkExternalIdentifiers(new ExternalIDs());
        work.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        List<OrcidValidationError> errors = validator.validateWork(work);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(TITLE_REQUIRED));
    }

    @Test
    public void testWorkWithEmptyTitle() {

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.getWorkTitle().setTitle(new Title(""));
        work.setWorkType(WorkType.DATA_SET);
        work.setWorkExternalIdentifiers(new ExternalIDs());
        work.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        List<OrcidValidationError> errors = validator.validateWork(work);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(TITLE_REQUIRED));
    }

    @Test
    public void testWorkWithoutType() {

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.getWorkTitle().setTitle(new Title("Work title"));
        work.setWorkExternalIdentifiers(new ExternalIDs());
        work.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        List<OrcidValidationError> errors = validator.validateWork(work);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(TYPE_REQUIRED));
    }

    @Test
    public void testWorkWithoutExternalIds() {

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.getWorkTitle().setTitle(new Title("Work title"));
        work.setWorkType(WorkType.DATA_SET);

        List<OrcidValidationError> errors = validator.validateWork(work);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(EXTERNAL_ID_REQUIRED));
    }

    @Test
    public void testWorkWithEmptyExternalIds() {

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.getWorkTitle().setTitle(new Title("Work title"));
        work.setWorkType(WorkType.DATA_SET);
        work.setWorkExternalIdentifiers(new ExternalIDs());

        List<OrcidValidationError> errors = validator.validateWork(work);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(EXTERNAL_ID_REQUIRED));
    }

    @Test
    public void testdWorkWithPublicationDateWithoutYear() {

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.setWorkType(WorkType.DATA_SET);
        work.getWorkTitle().setTitle(new Title("Work title"));
        work.setWorkExternalIdentifiers(new ExternalIDs());
        work.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        PublicationDate publicationDate = new PublicationDate();
        work.setPublicationDate(publicationDate);

        List<OrcidValidationError> errors = validator.validateWork(work);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(PUBLICATION_DATE_INVALID));
    }

    @Test
    public void testdWorkWithPublicationDateWithInvalidYear() {

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.setWorkType(WorkType.DATA_SET);
        work.getWorkTitle().setTitle(new Title("Work title"));
        work.setWorkExternalIdentifiers(new ExternalIDs());
        work.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        PublicationDate publicationDate = new PublicationDate();
        Year year = new Year();
        year.setValue("INVALID");
        publicationDate.setYear(year);
        work.setPublicationDate(publicationDate);

        List<OrcidValidationError> errors = validator.validateWork(work);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(PUBLICATION_DATE_INVALID));
    }

    @Test
    public void testdWorkWithPublicationDateWithYearPriorTo1900() {

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.setWorkType(WorkType.DATA_SET);
        work.getWorkTitle().setTitle(new Title("Work title"));
        work.setWorkExternalIdentifiers(new ExternalIDs());
        work.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        PublicationDate publicationDate = new PublicationDate();
        publicationDate.setYear(new Year(1850));
        work.setPublicationDate(publicationDate);

        List<OrcidValidationError> errors = validator.validateWork(work);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(PUBLICATION_DATE_INVALID));
    }

    @Test
    public void testValidWork() {

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.setWorkType(WorkType.DATA_SET);
        work.getWorkTitle().setTitle(new Title("Work title"));
        work.setWorkExternalIdentifiers(new ExternalIDs());
        work.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        PublicationDate publicationDate = new PublicationDate();
        publicationDate.setYear(new Year(1956));
        work.setPublicationDate(publicationDate);

        List<OrcidValidationError> errors = validator.validateWork(work);
        assertThat(errors, empty());
    }

    @Test
    public void testFundingWithoutTitleAndExternalIdsAndOrganization() {

        List<OrcidValidationError> errors = validator.validateFunding(new Funding());
        assertThat(errors, hasSize(3));
        assertThat(errors, containsInAnyOrder(EXTERNAL_ID_REQUIRED, FUNDER_REQUIRED, TITLE_REQUIRED));
    }

    @Test
    public void testFundingWithoutExternalIdsAndOrganization() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Funding title"));

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(2));
        assertThat(errors, containsInAnyOrder(EXTERNAL_ID_REQUIRED, FUNDER_REQUIRED));
    }

    @Test
    public void testFundingWithoutTitleAndOrganization() {

        Funding funding = new Funding();
        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(2));
        assertThat(errors, containsInAnyOrder(TITLE_REQUIRED, FUNDER_REQUIRED));
    }

    @Test
    public void testFundingWithoutTitleAndExternalIds() {

        Funding funding = new Funding();
        funding.setOrganization(buildValidOrganization());

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(2));
        assertThat(errors, containsInAnyOrder(TITLE_REQUIRED, EXTERNAL_ID_REQUIRED));
    }

    @Test
    public void testFundingWithoutTitle() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        funding.setOrganization(buildValidOrganization());

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(TITLE_REQUIRED));
    }

    @Test
    public void testFundingWithNullTitle() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title(null));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        funding.setOrganization(buildValidOrganization());

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(TITLE_REQUIRED));
    }

    @Test
    public void testFundingWithEmptyTitle() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title(""));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        funding.setOrganization(buildValidOrganization());

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(TITLE_REQUIRED));
    }

    @Test
    public void testFundingWithEmptyExternalIds() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());

        funding.setOrganization(buildValidOrganization());

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(EXTERNAL_ID_REQUIRED));
    }

    @Test
    public void testFundingWithOrganizationWithoutName() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        Organization organization = buildValidOrganization();
        organization.setName(null);
        funding.setOrganization(organization);

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(ORGANIZATION_NAME_REQUIRED));
    }

    @Test
    public void testFundingWithOrganizationWithEmptyName() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        Organization organization = buildValidOrganization();
        organization.setName("");
        funding.setOrganization(organization);

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(ORGANIZATION_NAME_REQUIRED));
    }

    @Test
    public void testFundingWithOrganizationWithoutAddress() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        Organization organization = buildValidOrganization();
        organization.setAddress(null);
        funding.setOrganization(organization);

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(ORGANIZATION_ADDRESS_REQUIRED));
    }

    @Test
    public void testFundingWithOrganizationWithoutCity() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        Organization organization = buildValidOrganization();
        organization.getAddress().setCity(null);
        funding.setOrganization(organization);

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(ORGANIZATION_CITY_REQUIRED));
    }

    @Test
    public void testFundingWithOrganizationWithoutCountry() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        Organization organization = buildValidOrganization();
        organization.getAddress().setCountry(null);
        funding.setOrganization(organization);

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(ORGANIZATION_COUNTRY_REQUIRED));
    }

    @Test
    public void testFundingWithOrganizationWithoutDisambiguatedOrganization() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        Organization organization = buildValidOrganization();
        organization.setDisambiguatedOrganization(null);
        funding.setOrganization(organization);

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(DISAMBIGUATED_ORGANIZATION_REQUIRED));
    }

    @Test
    public void testFundingWithOrganizationWithoutDisambiguatedOrganizationId() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        Organization organization = buildValidOrganization();
        organization.getDisambiguatedOrganization().setDisambiguatedOrganizationIdentifier(null);
        funding.setOrganization(organization);

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(DISAMBIGUATED_ORGANIZATION_VALUE_REQUIRED));
    }

    @Test
    public void testFundingWithOrganizationWithoutDisambiguatedOrganizationSource() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        Organization organization = buildValidOrganization();
        organization.getDisambiguatedOrganization().setDisambiguationSource(null);
        funding.setOrganization(organization);

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(DISAMBIGUATION_SOURCE_REQUIRED));
    }

    @Test
    public void testFundingWithOrganizationWithInvalidDisambiguationSource() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        Organization organization = buildValidOrganization();
        organization.getDisambiguatedOrganization().setDisambiguationSource("INVALID");
        funding.setOrganization(organization);

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(DISAMBIGUATION_SOURCE_INVALID));
    }

    @Test
    public void testFundingWithoutAmountCurrency() {
        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        funding.setOrganization(buildValidOrganization());

        funding.setAmount(new Amount());
        funding.getAmount().setContent("20000");

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(AMOUNT_CURRENCY_REQUIRED));
    }

    @Test
    public void testValidFunding() {
        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title("Title"));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        funding.setOrganization(buildValidOrganization());

        List<OrcidValidationError> errors = validator.validateFunding(funding);
        assertThat(errors, empty());
    }

    @Test
    public void testWithWorkValidationEnabled() {

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.getWorkTitle().setTitle(new Title("Work title"));
        work.setWorkExternalIdentifiers(new ExternalIDs());
        work.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        List<OrcidValidationError> errors = validator.validate(work);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(TYPE_REQUIRED));
    }

    @Test
    public void testWithWorkValidationDisabled() {

        when(configurationService.getBooleanProperty("orcid.validation.work.enabled", true)).thenReturn(false);

        Work work = new Work();
        work.setWorkTitle(new WorkTitle());
        work.getWorkTitle().setTitle(new Title("Work title"));

        List<OrcidValidationError> errors = validator.validate(work);
        assertThat(errors, empty());
    }

    @Test
    public void testWithFundingValidationEnabled() {

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title(""));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        funding.setOrganization(buildValidOrganization());

        List<OrcidValidationError> errors = validator.validate(funding);
        assertThat(errors, hasSize(1));
        assertThat(errors, containsInAnyOrder(TITLE_REQUIRED));
    }

    @Test
    public void testWithFundingValidationDisabled() {

        when(configurationService.getBooleanProperty("orcid.validation.funding.enabled", true)).thenReturn(false);

        Funding funding = new Funding();
        funding.setTitle(new FundingTitle());
        funding.getTitle().setTitle(new Title(""));

        funding.setExternalIdentifiers(new ExternalIDs());
        funding.getExternalIdentifiers().getExternalIdentifier().add(buildValidExternalID());

        funding.setOrganization(buildValidOrganization());

        List<OrcidValidationError> errors = validator.validate(funding);
        assertThat(errors, empty());
    }

    private ExternalID buildValidExternalID() {
        ExternalID externalID = new ExternalID();
        externalID.setRelationship(Relationship.SELF);
        externalID.setType("TYPE");
        externalID.setValue("VALUE");
        return externalID;
    }

    private Organization buildValidOrganization() {
        Organization organization = new Organization();
        organization.setName("Organization");

        OrganizationAddress address = new OrganizationAddress();
        address.setCity("City");
        address.setCountry(Iso3166Country.BA);
        organization.setAddress(address);

        DisambiguatedOrganization disambiguatedOrganization = new DisambiguatedOrganization();
        disambiguatedOrganization.setDisambiguatedOrganizationIdentifier("ID");
        disambiguatedOrganization.setDisambiguationSource("LEI");
        organization.setDisambiguatedOrganization(disambiguatedOrganization);

        return organization;
    }

}
