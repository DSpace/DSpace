/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.configuration.RelationPreferenceConfiguration;
import org.dspace.app.cris.integration.orcid.WrapperEducation;
import org.dspace.app.cris.integration.orcid.WrapperEmployment;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.OUProperty;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedProperty;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.value.OUPointer;
import org.dspace.app.cris.model.orcid.OrcidHistory;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;
import org.dspace.app.cris.model.orcid.OrcidQueue;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.orcid.OrcidExternalIdentifierType;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.authority.orcid.jaxb.activities.Educations;
import org.dspace.authority.orcid.jaxb.activities.Employments;
import org.dspace.authority.orcid.jaxb.activities.FundingGroup;
import org.dspace.authority.orcid.jaxb.activities.Fundings;
import org.dspace.authority.orcid.jaxb.activities.WorkGroup;
import org.dspace.authority.orcid.jaxb.activities.Works;
import org.dspace.authority.orcid.jaxb.address.Address;
import org.dspace.authority.orcid.jaxb.address.AddressCtype;
import org.dspace.authority.orcid.jaxb.address.Addresses;
import org.dspace.authority.orcid.jaxb.bulk.Bulk;
import org.dspace.authority.orcid.jaxb.common.Amount;
import org.dspace.authority.orcid.jaxb.common.CurrencyCode;
import org.dspace.authority.orcid.jaxb.common.ElementSummary;
import org.dspace.authority.orcid.jaxb.common.ExternalId;
import org.dspace.authority.orcid.jaxb.common.ExternalIds;
import org.dspace.authority.orcid.jaxb.common.FuzzyDate;
import org.dspace.authority.orcid.jaxb.common.FuzzyDate.Day;
import org.dspace.authority.orcid.jaxb.common.FuzzyDate.Month;
import org.dspace.authority.orcid.jaxb.common.FuzzyDate.Year;
import org.dspace.authority.orcid.jaxb.common.Iso3166Country;
import org.dspace.authority.orcid.jaxb.common.LanguageCode;
import org.dspace.authority.orcid.jaxb.common.OrcidId;
import org.dspace.authority.orcid.jaxb.common.Organization;
import org.dspace.authority.orcid.jaxb.common.OrganizationAddress;
import org.dspace.authority.orcid.jaxb.common.RelationshipType;
import org.dspace.authority.orcid.jaxb.common.TranslatedTitle;
import org.dspace.authority.orcid.jaxb.common.Url;
import org.dspace.authority.orcid.jaxb.education.Education;
import org.dspace.authority.orcid.jaxb.education.EducationSummary;
import org.dspace.authority.orcid.jaxb.employment.Employment;
import org.dspace.authority.orcid.jaxb.employment.EmploymentSummary;
import org.dspace.authority.orcid.jaxb.funding.Contributors;
import org.dspace.authority.orcid.jaxb.funding.Funding;
import org.dspace.authority.orcid.jaxb.funding.FundingSummary;
import org.dspace.authority.orcid.jaxb.funding.FundingTitle;
import org.dspace.authority.orcid.jaxb.funding.FundingType;
import org.dspace.authority.orcid.jaxb.keyword.Keyword;
import org.dspace.authority.orcid.jaxb.keyword.KeywordCtype;
import org.dspace.authority.orcid.jaxb.keyword.Keywords;
import org.dspace.authority.orcid.jaxb.othername.OtherName;
import org.dspace.authority.orcid.jaxb.othername.OtherNameCtype;
import org.dspace.authority.orcid.jaxb.othername.OtherNames;
import org.dspace.authority.orcid.jaxb.person.externalidentifier.ExternalIdentifier;
import org.dspace.authority.orcid.jaxb.person.externalidentifier.ExternalIdentifiers;
import org.dspace.authority.orcid.jaxb.personaldetails.CreditName;
import org.dspace.authority.orcid.jaxb.researcherurl.ResearcherUrl;
import org.dspace.authority.orcid.jaxb.researcherurl.ResearcherUrlCtype;
import org.dspace.authority.orcid.jaxb.researcherurl.ResearcherUrls;
import org.dspace.authority.orcid.jaxb.work.Citation;
import org.dspace.authority.orcid.jaxb.work.CitationType;
import org.dspace.authority.orcid.jaxb.work.Contributor;
import org.dspace.authority.orcid.jaxb.work.ContributorAttributes;
import org.dspace.authority.orcid.jaxb.work.ContributorEmail;
import org.dspace.authority.orcid.jaxb.work.ContributorRole;
import org.dspace.authority.orcid.jaxb.work.ContributorSequence;
import org.dspace.authority.orcid.jaxb.work.Work;
import org.dspace.authority.orcid.jaxb.work.WorkContributors;
import org.dspace.authority.orcid.jaxb.work.WorkSummary;
import org.dspace.authority.orcid.jaxb.work.WorkTitle;
import org.dspace.authority.orcid.jaxb.work.WorkType;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.handle.HandleManager;
import org.dspace.util.SimpleMapConverter;
import org.dspace.utils.DSpace;

import it.cilea.osd.common.core.SingleTimeStampInfo;
import it.cilea.osd.jdyna.value.BooleanValue;
import it.cilea.osd.jdyna.value.DateValue;
import it.cilea.osd.jdyna.value.EmbeddedLinkValue;
import it.cilea.osd.jdyna.value.TextValue;
import it.cilea.osd.jdyna.widget.WidgetLink;
import it.cilea.osd.jdyna.widget.WidgetPointer;

/**
 * @author Pascarelli
 *
 */
public class PushToORCID
{
    public static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static final String ORCID_PUSH_MANUAL = "orcid-push-manual";

    public static final String EMAIL_TEMPLATE_NAME = "orcid-alerts";

    private static final int ORCID_PUBLICATION_PREFS_DISABLED = 0;

    private static final int ORCID_PUBLICATION_PREFS_ALL = 1;

    private static final int ORCID_PUBLICATION_PREFS_SELECTED = 2;

    private static final int ORCID_PROJECTS_PREFS_DISABLED = 0;

    private static final int ORCID_PROJECTS_PREFS_ALL = 1;

    private static final int ORCID_PROJECTS_PREFS_SELECTED = 2;

    private static DateFormat df = new SimpleDateFormat("dd-MM-yyyy");

    /** the logger */
    private static Logger log = Logger.getLogger(PushToORCID.class);

    //SENDER METHODS
    public static boolean sendOrcidQueue(ApplicationService applicationService,
            OrcidQueue orcidQueue)
    {
        boolean result = false;
        if (orcidQueue != null)
        {
            String owner = orcidQueue.getOwner();
            String uuid = orcidQueue.getFastlookupUuid();
            switch (orcidQueue.getTypeId())
            {
            case CrisConstants.RP_TYPE_ID:
                result = sendOrcidProfile(applicationService, owner);
                break;
            case CrisConstants.PROJECT_TYPE_ID:
                result = sendOrcidFunding(applicationService, owner, uuid);
                break;
            default:
                result = sendOrcidWork(applicationService, owner, uuid);
                break;
            }
        }
        return result;
    }

    public static boolean sendOrcidWork(ApplicationService applicationService,
            String crisId, String uuid)
    {
        boolean result = false;
        log.debug("Create DSpace context");
        Context context = null;
        try
        {
            ResearcherPage researcher = (ResearcherPage) applicationService
                    .getEntityByCrisId(crisId, ResearcherPage.class);

            context = new Context();
            context.turnOffAuthorisationSystem();

            OrcidService orcidService = OrcidService.getOrcid();

            log.info("Starts update ORCID Profile for:" + crisId);

            String orcid = null;
            for (RPProperty propOrcid : researcher.getAnagrafica4view()
                    .get("orcid"))
            {
                orcid = propOrcid.toString();
                break;
            }

            if (StringUtils.isNotBlank(orcid))
            {
                log.info("Prepare push for Work:" + uuid
                        + " for ResearcherPage crisID:" + crisId
                        + " AND orcid iD:" + orcid);
                try
                {
                    String tokenCreateWork = OrcidPreferencesUtils.getTokenReleasedForSync(researcher,
                                    OrcidService.SYSTEM_ORCID_TOKEN_ACTIVITIES_CREATE_SCOPE);
                    DSpaceObject dso = HandleManager.resolveToObject(context,
                            uuid);
                    if (tokenCreateWork != null)
                    {

                        log.info("(Q1)Prepare for Work:" + uuid
                                + " for ResearcherPage crisID:" + crisId);
                        result = buildOrcidWork(context, orcidService, applicationService,
                                crisId, orcid, false, tokenCreateWork, dso.getID());
                    }

                }
                catch (Exception ex)
                {
                    log.info("ERROR!!! (E1) ERROR for Work:" + uuid
                            + " for ResearcherPage crisID:" + crisId);
                    log.error(ex.getMessage());
                }
            }
            else
            {
                log.warn("WARNING!!! (W0) Orcid not found for ResearcherPage:"
                        + crisId);
            }
            log.info("Ends append ORCID  Work:" + uuid
                    + " for ResearcherPage crisID:" + crisId);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

        if (result)
        {
            applicationService.deleteOrcidQueueByOwnerAndUuid(crisId, uuid);
        }
        return result;
    }

    public static boolean sendOrcidProfile(
            ApplicationService applicationService, String crisId)
    {
        boolean result = false;
        try
        {
            Map<String, Map<String, List<String>>> mapResearcherMetadataToSend = new HashMap<String, Map<String, List<String>>>();
            Map<String, Map<String, List<Map<String, List<String>>>>> mapResearcherMetadataNestedToSend = new HashMap<String, Map<String, List<Map<String, List<String>>>>>();
            Map<String, String> orcidConfigurationMapping = PushToORCID
                    .prepareConfigurationMappingForProfile(applicationService);
            ResearcherPage researcher = (ResearcherPage) applicationService
                    .getEntityByCrisId(crisId, ResearcherPage.class);
            prepareMetadataForProfile(applicationService,
                    mapResearcherMetadataToSend,
                    mapResearcherMetadataNestedToSend, researcher, true);

            OrcidService orcidService = OrcidService.getOrcid();

            log.info("Starts update ORCID Profile/Affiliations for:" + crisId);

            String orcid = null;
            for (RPProperty propOrcid : researcher.getAnagrafica4view()
                    .get("orcid"))
            {
                orcid = propOrcid.toString();
                break;
            }

            if (StringUtils.isNotBlank(orcid))
            {
                log.info("Prepare push for ResearcherPage crisID:" + crisId
                        + " AND orcid iD:" + orcid);
                try
                {
                    String tokenUpdateBio = OrcidPreferencesUtils.getTokenReleasedForSync(researcher,
                            OrcidService.SYSTEM_ORCID_TOKEN_PROFILE_CREATE_SCOPE);
                    String tokenActivities = OrcidPreferencesUtils.getTokenReleasedForSync(researcher,
                            OrcidService.SYSTEM_ORCID_TOKEN_ACTIVITIES_CREATE_SCOPE);
                    if (tokenUpdateBio != null)
                    {

                        log.info(
                                "(Q1)Prepare OrcidProfile for ResearcherPage crisID:"
                                        + crisId);
                        result = buildOrcidProfile(orcidService,
                                applicationService, crisId,
                                mapResearcherMetadataToSend,
                                mapResearcherMetadataNestedToSend,
                                orcidConfigurationMapping, false,
                                tokenUpdateBio, tokenActivities, orcid);

                    }
                    else
                    {
                        log.warn("WARNING!!! (W1) Token Update Profile not released for:"
                                + crisId);
                    }

                }
                catch (Exception ex)
                {
                    log.error(
                            "ERROR!!! (E2) ERROR OrcidProfile/Affiliations ResearcherPage crisID:"
                                    + crisId,
                            ex);
                    result = false;
                }
            }
            else
            {
                log.warn("WARNING!!! (W0) Orcid not found for:" + crisId);
            }
            log.info("Ends update ORCID Profile/Affiliations for:" + crisId);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            result = false;
        }

        if (result)
        {
            applicationService.deleteOrcidQueueByOwnerAndTypeId(crisId, OrcidService.CONSTANT_PART_OF_RESEARCHER_TYPE);
        }
        return result;
    }

    public static boolean sendOrcidFunding(
            ApplicationService applicationService, String crisId, String uuid)
    {
        boolean result = false;
        Context context = null;
        try
        {
            ResearcherPage researcher = (ResearcherPage) applicationService
                    .getEntityByCrisId(crisId, ResearcherPage.class);

            context = new Context();
            context.turnOffAuthorisationSystem();

            OrcidService orcidService = OrcidService.getOrcid();

            log.info("Starts update ORCID Profile for:" + crisId);

            String orcid = null;
            for (RPProperty propOrcid : researcher.getAnagrafica4view()
                    .get("orcid"))
            {
                orcid = propOrcid.toString();
                break;
            }

            if (StringUtils.isNotBlank(orcid))
            {
                log.info("Prepare push for Funding:" + uuid
                        + " for ResearcherPage crisID:" + crisId
                        + " AND orcid iD:" + orcid);
                try
                {
                    String tokenCreateFunding = OrcidPreferencesUtils.getTokenReleasedForSync(
                            researcher, OrcidService.SYSTEM_ORCID_TOKEN_ACTIVITIES_CREATE_SCOPE);

                    Project project = (Project) applicationService
                            .getEntityByUUID(uuid);
                    if (tokenCreateFunding != null)
                    {

                        log.info("(Q1)Prepare Funding:" + uuid
                                + " for ResearcherPage crisID:" + crisId);
                        result = buildOrcidFunding(context,
                                orcidService, applicationService,
                                crisId, orcid, false, tokenCreateFunding, project);
                    }
                    else
                    {
                        log.warn("WARNING!!! (W1) Token not released for:"
                                + crisId);
                    }

                }
                catch (Exception ex)
                {
                    log.error(
                            "ERROR!!! (E2) ERROR for Funding:" + uuid
                                    + " for ResearcherPage crisID:" + crisId,
                            ex);
                }
            }
            else
            {
                log.warn("WARNING!!! (W0) Orcid not found for:" + crisId);
            }
            log.info("Ends update ORCID Funding:" + uuid
                    + " for ResearcherPage crisID:" + crisId);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

        if (result)
        {
            applicationService.deleteOrcidQueueByOwnerAndUuid(crisId, uuid);
        }
        return result;
    }

    private static void sendWrapperEducation(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, List<WrapperEducation> wrapperEducations,
            boolean delete, SingleTimeStampInfo timestampAttempt) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {

        if (wrapperEducations != null)
        {
            for (WrapperEducation wrapperEducation : wrapperEducations)
            {
                Education education = wrapperEducation.getEducation();
                String md5ContentPartOfResearcher = getMd5Hash(education.getOrganization().getName() + education.getStartDate().toString() + education.getEndDate().toString());
                sendPartOfResearcher(orcidService, applicationService, crisId, token, orcid, OrcidService.CONSTANT_EDUCATION_UUID, md5ContentPartOfResearcher, education, delete, timestampAttempt);
            }

        }
    }
    
    private static void sendWrapperEmployment(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, List<WrapperEmployment> wrapperEmployments,
            boolean delete, SingleTimeStampInfo timestampAttempt) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        
        if (wrapperEmployments != null)
        {
            for (WrapperEmployment wrapperEmployment : wrapperEmployments)
            {
                Employment employment = wrapperEmployment.getEmployment();
                String md5ContentPartOfResearcher = getMd5Hash(employment.getOrganization().getName() + employment.getStartDate().toString() + employment.getEndDate().toString());
                sendPartOfResearcher(orcidService, applicationService, crisId, token, orcid, OrcidService.CONSTANT_EMPLOYMENT_UUID, md5ContentPartOfResearcher, employment, delete, timestampAttempt);
            }

        }
    }

    private static void sendKeywords(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, List<Keyword> keywords, boolean deleteAndPost, SingleTimeStampInfo timestampAttempt) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        for (Keyword partOfResearcher : keywords)
        {
            String value = partOfResearcher.getContent();
            String md5ContentPartOfResearcher = getMd5Hash(value);
            sendPartOfResearcher(orcidService, applicationService, crisId,
                    token, orcid, OrcidService.CONSTANT_KEYWORD_UUID,
                    md5ContentPartOfResearcher, partOfResearcher,
                    deleteAndPost, timestampAttempt);
        }
    }

    private static void sendAddresses(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, List<Address> addresses, boolean deleteAndPost, SingleTimeStampInfo timestampAttempt) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        for (Address partOfResearcher : addresses)
        {
            String value = partOfResearcher.getCountry();
            String md5ContentPartOfResearcher = getMd5Hash(value);
            sendPartOfResearcher(orcidService, applicationService, crisId,
                    token, orcid, OrcidService.CONSTANT_ADDRESS_UUID,
                    md5ContentPartOfResearcher, partOfResearcher,
                    deleteAndPost, timestampAttempt);
        }
    }

    private static void sendResearcherUrls(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, List<ResearcherUrl> researcherUrl,
            boolean deleteAndPost, SingleTimeStampInfo timestampAttempt) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {

        for (ResearcherUrl partOfResearcher : researcherUrl)
        {
            String value = partOfResearcher.getUrl().getValue();
            String md5ContentPartOfResearcher = getMd5Hash(value);
            sendPartOfResearcher(orcidService, applicationService, crisId,
                    token, orcid, OrcidService.CONSTANT_RESEARCHERURL_UUID,
                    md5ContentPartOfResearcher, partOfResearcher,
                    deleteAndPost, timestampAttempt);
        }
    }

    private static void sendExternalIdentifiers(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, List<ExternalIdentifier> externalIdentifier,
            boolean deleteAndPost, SingleTimeStampInfo timestampAttempt) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        
        for (ExternalIdentifier partOfResearcher : externalIdentifier)
        {
            String value = partOfResearcher.getExternalIdUrl();
            String md5ContentPartOfResearcher = getMd5Hash(value);
            sendPartOfResearcher(orcidService, applicationService, crisId,
                    token, orcid, OrcidService.CONSTANT_EXTERNALIDENTIFIER_UUID,
                    md5ContentPartOfResearcher, partOfResearcher,
                    deleteAndPost, timestampAttempt);
        }
    }

    private static void sendOtherNames(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, List<OtherName> otherNames, boolean deleteAndPost, SingleTimeStampInfo timestampAttempt) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        for (OtherName partOfResearcher : otherNames)
        {
            String value = partOfResearcher.getContent();
            String md5ContentPartOfResearcher = getMd5Hash(value);
            sendPartOfResearcher(orcidService, applicationService, crisId,
                    token, orcid, OrcidService.CONSTANT_OTHERNAME_UUID,
                    md5ContentPartOfResearcher, partOfResearcher,
                    deleteAndPost, timestampAttempt);
        }
    }

    private static void sendPartOfResearcher(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, String constantUuidPrefix, String constantUuid, 
            Serializable partOfResearcher,
            boolean delete, SingleTimeStampInfo timestampAttempt)
    {
        if (partOfResearcher != null)
        {
            Integer constantType = OrcidService.CONSTANT_PART_OF_RESEARCHER_TYPE;
            String constantUuidPartOf = constantUuidPrefix +"::"+constantUuid;
            
            try
            {
                OrcidHistory orcidHistory = applicationService
                        .uniqueOrcidHistoryByOwnerAndEntityUUIDAndTypeId(crisId, constantUuidPartOf, constantType);
                if (delete)
                {
                    if (orcidHistory != null)
                    {
                        String putCode = orcidHistory.getPutCode();
                        switch (constantUuidPrefix)
                        {
                        case OrcidService.CONSTANT_OTHERNAME_UUID:
                            orcidService.deleteOtherName(orcid, token, putCode);
                            break;
                        case OrcidService.CONSTANT_RESEARCHERURL_UUID:
                            orcidService.deleteResearcherUrl(orcid, token,
                                    putCode);
                            break;
                        case OrcidService.CONSTANT_EXTERNALIDENTIFIER_UUID:
                            orcidService.deleteExternalIdentifier(orcid, token,
                                    putCode);
                            break;
                        case OrcidService.CONSTANT_ADDRESS_UUID:
                            orcidService.deleteAddress(orcid, token, putCode);
                            break;
                        case OrcidService.CONSTANT_KEYWORD_UUID:
                            orcidService.deleteKeyword(orcid, token, putCode);
                            break;
                        case OrcidService.CONSTANT_EMPLOYMENT_UUID:
                            orcidService.deleteEmployment(orcid, token, putCode);
                            break;
                        case OrcidService.CONSTANT_EDUCATION_UUID:
                            orcidService.deleteEducation(orcid, token, putCode);
                            break;                            
                        }
                        applicationService.delete(OrcidHistory.class,
                                orcidHistory.getId());
                    }
                }
                String putCode = null;
                String errorMessage = null;
                if (delete || orcidHistory == null || (orcidHistory != null && StringUtils.isBlank(orcidHistory.getPutCode())))
                {
                    orcidHistory = new OrcidHistory();
                    try
                    {
                        switch (constantUuidPrefix)
                        {
                        case OrcidService.CONSTANT_OTHERNAME_UUID:
                            putCode = orcidService.appendOtherName(orcid, token,
                                    (OtherName) partOfResearcher);
                            break;
                        case OrcidService.CONSTANT_RESEARCHERURL_UUID:
                            putCode = orcidService.appendResearcherUrl(orcid,
                                    token, (ResearcherUrl) partOfResearcher);
                            break;
                        case OrcidService.CONSTANT_EXTERNALIDENTIFIER_UUID:
                            putCode = orcidService.appendExternalIdentifier(orcid, token,
                                    (ExternalIdentifier) partOfResearcher);
                            break;
                        case OrcidService.CONSTANT_ADDRESS_UUID:
                            putCode = orcidService.appendAddress(orcid, token,
                                    (Address) partOfResearcher);
                            break;
                        case OrcidService.CONSTANT_KEYWORD_UUID:
                            putCode = orcidService.appendKeyword(orcid, token,
                                    (Keyword) partOfResearcher);
                            break;
                        case OrcidService.CONSTANT_EMPLOYMENT_UUID:
                            putCode = orcidService.appendEmployment(orcid, token,
                                    (Employment) partOfResearcher);
                            break;
                        case OrcidService.CONSTANT_EDUCATION_UUID:
                            putCode = orcidService.appendEducation(orcid, token,
                                    (Education) partOfResearcher);
                            break;                               
                        }
                        orcidHistory.setPutCode(putCode);
                    }
                    catch (Exception ex)
                    {
                        errorMessage = ex.getMessage();
                    }

                    orcidHistory.setEntityUuid(constantUuidPartOf);
                    orcidHistory.setTypeId(constantType);
                    orcidHistory.setOwner(crisId);
                }
                else
                {
                    putCode = orcidHistory.getPutCode();
                    StatusType status = null;
                    switch (constantUuidPrefix)
                    {
                    case OrcidService.CONSTANT_OTHERNAME_UUID:
                        OtherName otherName = (OtherName) partOfResearcher;
                        otherName.setPutCode(
                                new BigInteger(putCode));
                        status = orcidService.putOtherName(orcid, token,
                                putCode, otherName);
                        break;
                    case OrcidService.CONSTANT_RESEARCHERURL_UUID:
                        ResearcherUrl researcherUrl = (ResearcherUrl) partOfResearcher;
                        researcherUrl.setPutCode(
                                new BigInteger(putCode));
                        status = orcidService.putResearcherUrl(orcid, token,
                                putCode, researcherUrl);
                        break;
                    case OrcidService.CONSTANT_EXTERNALIDENTIFIER_UUID:
                        ExternalIdentifier externalIdentifier = (ExternalIdentifier) partOfResearcher;
                        externalIdentifier.setPutCode(
                                new BigInteger(putCode));
                        status = orcidService.putExternalIdentifier(orcid,
                                token, putCode,
                                externalIdentifier);
                        break;
                    case OrcidService.CONSTANT_ADDRESS_UUID:
                        Address address = (Address) partOfResearcher;
                        address.setPutCode(
                                new BigInteger(putCode));
                        status = orcidService.putAddress(orcid, token, putCode,
                                address);
                        break;
                    case OrcidService.CONSTANT_KEYWORD_UUID:
                        Keyword keyword = (Keyword) partOfResearcher;
                        keyword.setPutCode(
                                new BigInteger(putCode));
                        status = orcidService.putKeyword(orcid, token, putCode,
                                keyword);
                        break;
                    case OrcidService.CONSTANT_EMPLOYMENT_UUID:
                        Employment employment = (Employment) partOfResearcher;
                        employment.setPutCode(
                                new BigInteger(putCode));
                        status = orcidService.putEmployment(orcid, token, putCode,
                                employment);
                        break;
                    case OrcidService.CONSTANT_EDUCATION_UUID:
                        Education education = (Education) partOfResearcher;
                        education.setPutCode(
                                new BigInteger(putCode));
                        status = orcidService.putEducation(orcid, token, putCode,
                                education);
                        break;
                    }
                    if (!status.getFamily().equals(Family.SUCCESSFUL))
                    {
                        errorMessage = status.getStatusCode() + " REASON:"
                                + status.getReasonPhrase();
                    }
                }


                orcidHistory.setTimestampLastAttempt(timestampAttempt);
                orcidHistory.setOrcid(orcid);
                if (StringUtils.isNotBlank(errorMessage))
                {
                    // build message for orcid history
                    orcidHistory.setResponseMessage(errorMessage);
                    log.error("ERROR!!! (E1)::PUSHORCID::" + constantUuidPartOf
                            + " for ResearcherPage crisID:" + crisId);
                }
                else
                {
                    orcidHistory.setResponseMessage(null);
                    orcidHistory.setTimestampSuccessAttempt(timestampAttempt);
                    log.info("(A1)::PUSHORCID::" + constantUuidPartOf
                            + " for ResearcherPage crisID:" + crisId);
                }
                applicationService.saveOrUpdate(OrcidHistory.class,
                        orcidHistory);
            }
            catch (Exception e)
            {
                log.error("ERROR!!! (E0)::PUSHORCID::" + constantUuidPartOf
                        + " for ResearcherPage crisID:" + crisId, e);
            }
            finally
            {
                OrcidPreferencesUtils.printXML(partOfResearcher);
            }
        }

    }

    private static void sendWork(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, String constantUuidPrefix, String constantUuid, 
            Work work,
            boolean delete, SingleTimeStampInfo timestampAttempt)
    {
        if (work != null)
        {
            Integer constantType = Constants.ITEM;
            String constantUuidPartOf = constantUuid;
            
            try
            {
                OrcidHistory orcidHistory = applicationService
                        .uniqueOrcidHistoryByOwnerAndEntityUUIDAndTypeId(crisId, constantUuidPartOf, constantType);
                if (delete)
                {
                    if (orcidHistory != null)
                    {
                        String putCode = orcidHistory.getPutCode();
                        orcidService.deleteWork(orcid, token, putCode);
                        applicationService.delete(OrcidHistory.class,
                                orcidHistory.getId());
                    }
                }
                String putCode = null;
                String errorMessage = null;
                if (delete || orcidHistory == null || (orcidHistory != null && StringUtils.isBlank(orcidHistory.getPutCode())))
                {
                    orcidHistory = new OrcidHistory();
                    try
                    {
                        putCode = orcidService.appendWork(orcid, token,
                                work);
                        orcidHistory.setPutCode(putCode);
                    }
                    catch (Exception ex)
                    {
                        errorMessage = ex.getMessage();
                    }

                    orcidHistory.setEntityUuid(constantUuidPartOf);
                    orcidHistory.setTypeId(constantType);
                    orcidHistory.setOwner(crisId);
                }
                else
                {
                    StatusType status = null;
                    work.setPutCode(new BigInteger(orcidHistory.getPutCode()));
                    status = orcidService.putWork(orcid, token,
                            orcidHistory.getPutCode(), work);
                    if (!status.getFamily().equals(Family.SUCCESSFUL))
                    {
                        errorMessage = status.getStatusCode() + " REASON:"
                                + status.getReasonPhrase();
                    }
                }


                orcidHistory.setTimestampLastAttempt(timestampAttempt);
                orcidHistory.setOrcid(orcid);
                if (StringUtils.isNotBlank(errorMessage))
                {
                    // build message for orcid history
                    orcidHistory.setResponseMessage(errorMessage);
                    log.error("ERROR!!! (E1)::PUSHORCID::" + constantUuidPartOf
                            + " for ResearcherPage crisID:" + crisId);
                }
                else
                {
                    orcidHistory.setResponseMessage(null);
                    orcidHistory.setTimestampSuccessAttempt(timestampAttempt);
                    log.info("(A1)::PUSHORCID::" + constantUuidPartOf
                            + " for ResearcherPage crisID:" + crisId);
                }
                applicationService.saveOrUpdate(OrcidHistory.class,
                        orcidHistory);
            }
            catch (Exception e)
            {
                log.error("ERROR!!! (E0)::PUSHORCID::" + constantUuidPartOf
                        + " for ResearcherPage crisID:" + crisId, e);
            }
            finally
            {
                OrcidPreferencesUtils.printXML(work);
            }
        }

    }

    private static void sendFunding(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, String constantUuidPrefix, String constantUuid, 
            Funding funding,
            boolean delete, SingleTimeStampInfo timestampAttempt)
    {
        if (funding != null)
        {
            Integer constantType = CrisConstants.PROJECT_TYPE_ID;
            String constantUuidPartOf = constantUuid;
            
            try
            {
                OrcidHistory orcidHistory = applicationService
                        .uniqueOrcidHistoryByOwnerAndEntityUUIDAndTypeId(crisId, constantUuidPartOf, constantType);
                if (delete)
                {
                    if (orcidHistory != null)
                    {
                        String putCode = orcidHistory.getPutCode();
                        orcidService.deleteFunding(orcid, token, putCode);
                        applicationService.delete(OrcidHistory.class,
                                orcidHistory.getId());
                    }
                }
                String putCode = null;
                String errorMessage = null;
                if (delete || orcidHistory == null || (orcidHistory != null && StringUtils.isBlank(orcidHistory.getPutCode())))
                {
                    orcidHistory = new OrcidHistory();
                    try
                    {
                        putCode = orcidService.appendFunding(orcid, token,
                                funding);
                        orcidHistory.setPutCode(putCode);
                    }
                    catch (Exception ex)
                    {
                        errorMessage = ex.getMessage();
                    }

                    orcidHistory.setEntityUuid(constantUuidPartOf);
                    orcidHistory.setTypeId(constantType);
                    orcidHistory.setOwner(crisId);
                }
                else
                {
                    StatusType status = null;
                    funding.setPutCode(new BigInteger(orcidHistory.getPutCode()));
                    status = orcidService.putFunding(orcid, token,
                            orcidHistory.getPutCode(), funding);
                    if (!status.getFamily().equals(Family.SUCCESSFUL))
                    {
                        errorMessage = status.getStatusCode() + " REASON:"
                                + status.getReasonPhrase();
                    }
                }


                orcidHistory.setTimestampLastAttempt(timestampAttempt);
                orcidHistory.setOrcid(orcid);
                if (StringUtils.isNotBlank(errorMessage))
                {
                    // build message for orcid history
                    orcidHistory.setResponseMessage(errorMessage);
                    log.error("ERROR!!! (E1)::PUSHORCID::" + constantUuidPartOf
                            + " for ResearcherPage crisID:" + crisId);
                }
                else
                {
                    orcidHistory.setResponseMessage(null);
                    orcidHistory.setTimestampSuccessAttempt(timestampAttempt);
                    log.info("(A1)::PUSHORCID::" + constantUuidPartOf
                            + " for ResearcherPage crisID:" + crisId);
                }
                applicationService.saveOrUpdate(OrcidHistory.class,
                        orcidHistory);
            }
            catch (Exception e)
            {
                log.error("ERROR!!! (E0)::PUSHORCID::" + constantUuidPartOf
                        + " for ResearcherPage crisID:" + crisId, e);
            }
            finally
            {
                OrcidPreferencesUtils.printXML(funding);
            }
        }

    }
    
    public static void prepareAndSend(Context context, List<ResearcherPage> rps,
            RelationPreferenceService relationPreferenceService,
            SearchService searchService, ApplicationService applicationService,
            boolean delete) throws Exception
    {
        log.debug("Working... push to ORCID");

        Map<String, Map<String, List<String>>> mapResearcherMetadataToSend = new HashMap<String, Map<String, List<String>>>();
        Map<String, Map<String, List<Map<String, List<String>>>>> mapResearcherMetadataNestedToSend = new HashMap<String, Map<String, List<Map<String, List<String>>>>>();
        Map<String, List<Integer>> mapPublicationsToSend = new HashMap<String, List<Integer>>();
        Map<String, List<Integer>> mapProjectsToSend = new HashMap<String, List<Integer>>();

        boolean byPassManualMode = ConfigurationManager.getBooleanProperty(
                "cris", "system.script.pushtoorcid.force", false);

        boolean invitation = ConfigurationManager.getBooleanProperty("cris",
                "system.script.pushtoorcid.profile.invitation.email", false);

        boolean forceProfilePreferences = ConfigurationManager
                .getBooleanProperty("cris",
                        "system.script.pushtoorcid.profile.preference.force",
                        false);

        // if defined then the script works in this default mode
        String defaultPreference = ConfigurationManager.getProperty("cris",
                "system.script.pushtoorcid.default.preference");

        Map<String, String> mapResearcherOrcid = new HashMap<String, String>();
        Map<String, String> mapResearcherTokenProfileUpdate = new HashMap<String, String>();
        Map<String, String> mapResearcherTokenActivitiesUpdate = new HashMap<String, String>();
        List<ResearcherPage> listNewResearcherToPushOnOrcid = new ArrayList<ResearcherPage>();

        Map<String, String> orcidConfigurationMapping = prepareConfigurationMappingForProfile(
                applicationService);

        external: for (ResearcherPage researcher : rps)
        {

            String crisID = researcher.getCrisID();
            List<Integer> itemIDsToSend = new ArrayList<Integer>();
            List<Integer> projectsIDsToSend = new ArrayList<Integer>();

            boolean isManualMode = false;
            if (!byPassManualMode)
            {
                isManualMode = isManualModeEnable(researcher);
            }

            if (isManualMode)
            {
                sendEmail(context, researcher, crisID);
                continue external;
            }
            boolean prepareUpdateProfile = false;
            boolean prepareUpdate = false;

            boolean foundORCID = false;

            for (RPProperty orcid : researcher.getAnagrafica4view()
                    .get("orcid"))
            {
                mapResearcherOrcid.put(crisID, orcid.toString());
                foundORCID = true;
                break;
            }

            String publicationsPrefs = ResearcherPageUtils.getStringValue(
                    researcher, OrcidPreferencesUtils.ORCID_PUBLICATIONS_PREFS);
            String projectPrefs = ResearcherPageUtils.getStringValue(researcher,
                    OrcidPreferencesUtils.ORCID_PROJECTS_PREFS);

            if (!foundORCID)
            {
                listNewResearcherToPushOnOrcid.add(researcher);
            }
            else
            {
                for (RPProperty tokenRP : researcher.getAnagrafica4view().get(
                        OrcidService.SYSTEM_ORCID_TOKEN_PROFILE_CREATE_SCOPE))
                {
                    mapResearcherTokenProfileUpdate.put(crisID,
                            tokenRP.toString());
                    prepareUpdateProfile = true;
                    break;
                }
                for (RPProperty tokenRP : researcher.getAnagrafica4view().get(
                        OrcidService.SYSTEM_ORCID_TOKEN_ACTIVITIES_CREATE_SCOPE))
                {
                    mapResearcherTokenActivitiesUpdate.put(crisID,
                            tokenRP.toString());
                    prepareUpdate = true;
                    break;
                }

                if (prepareUpdateProfile)
                {
                    prepareMetadataForProfile(applicationService,
                            mapResearcherMetadataToSend,
                            mapResearcherMetadataNestedToSend, researcher,
                            forceProfilePreferences);
                }

                if (prepareUpdate)
                {
                    prepareMetadataForWorks(applicationService, relationPreferenceService,
                            searchService, mapPublicationsToSend, researcher,
                            crisID, itemIDsToSend, StringUtils.isNotBlank(publicationsPrefs)?publicationsPrefs:defaultPreference);
                    prepareMetadataForFundings(applicationService,
                            relationPreferenceService, searchService,
                            mapProjectsToSend, researcher, crisID,
                            projectsIDsToSend, StringUtils.isNotBlank(projectPrefs)?projectPrefs:defaultPreference);
                }
            }
        }

        log.debug("Create DSpace context and use browse indexing");

        OrcidService orcidService = OrcidService.getOrcid();
        if (invitation)
        {
            log.info("Starts invitation new ORCID Profile");
            for (ResearcherPage rp : listNewResearcherToPushOnOrcid)
            {
                log.info("Prepare email for ResearcherPage crisID:"
                        + rp.getCrisID());
                try
                {
                    // TODO v2.0 change the workflow to build profile from
                    // scratch
                    // https://members.orcid.org/api/integrate/create-records
                    log.warn("NOT YET IMPLEMENTED:"
                            + rp.getCrisID());                    
                }
                catch (Exception e)
                {
                    log.info(
                            "ERROR!!! ResearcherPage crisID:" + rp.getCrisID());
                    log.error(e.getMessage());
                }
            }
            log.info("Ends invitation new ORCID Profile");
        }

        log.info("Starts update ORCID Profile");
        for (String crisId : mapResearcherOrcid.keySet())
        {

            String orcid = mapResearcherOrcid.get(crisId);

            if (StringUtils.isNotBlank(orcid))
            {
                log.info("Prepare push for ResearcherPage crisID:" + crisId
                        + " AND orcid iD:" + orcid);
                try
                {
                    String tokenUpdateBio = mapResearcherTokenProfileUpdate
                            .get(crisId);
                    String tokenUpdateActivities = mapResearcherTokenActivitiesUpdate
                            .get(crisId);
                    if (StringUtils.isNotBlank(tokenUpdateBio))
                    {
                        log.info(
                                "(Q1)Prepare OrcidProfile for ResearcherPage crisID:"
                                        + crisId);
                        if (mapResearcherMetadataToSend.get(crisId).isEmpty())
                        {
                            log.warn(
                                    "(A1) OrcidProfile no Profile Preferences found for ResearcherPage crisID:"
                                            + crisId);
                        }
                        else
                        {
                            // v2.0 has simplified this process by implementing
                            // put codes. Individual items on the ORCID Registry
                            // now have their own put code, which can be used to
                            // read the full metadata for the item or to update
                            // or delete an item that your system has added to
                            // an ORCID record. Only one item can be edited or
                            // deleted at a time. Put codes are retroactive:
                            // items added in 1.2 and earlier have all been
                            // assigned put codes.

                            buildOrcidProfile(orcidService, applicationService,
                                    crisId, mapResearcherMetadataToSend,
                                    mapResearcherMetadataNestedToSend,
                                    orcidConfigurationMapping, delete, tokenUpdateBio, tokenUpdateActivities, orcid);

                        }
                    }

                    if (StringUtils.isNotBlank(tokenUpdateActivities))
                    {
                        log.info(
                                "(Q2)Prepare OrcidWorks for ResearcherPage crisID:"
                                        + crisId);
                        for(Integer item : mapPublicationsToSend.get(crisId)) {
                            buildOrcidWork(context, orcidService, applicationService, crisId, orcid, delete, tokenUpdateActivities, item);
                        }

                        log.info("(A2) OrcidWorks for ResearcherPage crisID:"
                                + crisId);

                        log.info(
                                "(Q3)Prepare FundingList for ResearcherPage crisID:"
                                        + crisId);


                        for(Integer ii : mapProjectsToSend.get(crisId)) {
                            Project project = applicationService.get(Project.class, ii);
                            buildOrcidFunding(context, orcidService, applicationService, crisId, orcid, delete, tokenUpdateActivities, project);
                        }
                    }
                }
                catch (Exception ex)
                {
                    log.info("ERROR!!! ResearcherPage crisID:" + crisId);
                    log.error(ex.getMessage());
                }
            }
        }
        log.info("Ends update ORCID Profile");

    }

    /**
     * Build personal information
     * 
     * @return
     * @throws UnsupportedEncodingException 
     * @throws NoSuchAlgorithmException 
     */
    public static boolean buildOrcidProfile(OrcidService orcidService,
            ApplicationService applicationService, String crisId,
            Map<String, Map<String, List<String>>> mapResearcherMetadataToSend,
            Map<String, Map<String, List<Map<String, List<String>>>>> mapResearcherMetadataNestedToSend,
            Map<String, String> orcidMetadataConfiguration,
            boolean deleteAndPost, String tokenProfile, String tokenActivities, String orcid)
            throws RuntimeException
    {

        boolean result = true;
        try
        {
            
            Map<String, List<String>> metadata = mapResearcherMetadataToSend
                    .get(crisId);

            // retrieve data from map
            
            SingleTimeStampInfo timestampAttemptToRetrieve = new SingleTimeStampInfo(
                    new Date());
            //retrieve from Orcid Registry checking the same source name client
            String sourceName = OrcidService.getSourceClientName();
            retrieveOtherNames(orcidService, applicationService, crisId, tokenProfile,
                    orcid, timestampAttemptToRetrieve, sourceName);
            retrieveResearcherUrls(orcidService, applicationService, crisId,
                    tokenProfile, orcid, timestampAttemptToRetrieve, sourceName);
            retrieveExternalIdentifiers(orcidService, applicationService,
                    crisId, tokenProfile, orcid, timestampAttemptToRetrieve, sourceName);
            retrieveAddresses(orcidService, applicationService, crisId, tokenProfile,
                    orcid, timestampAttemptToRetrieve, sourceName);
            retrieveKeywords(orcidService, applicationService, crisId, tokenProfile,
                    orcid, timestampAttemptToRetrieve, sourceName);
            if(StringUtils.isNotBlank(tokenActivities)) {
                retrieveEducations(orcidService, applicationService, crisId, tokenActivities,
                    orcid, timestampAttemptToRetrieve, sourceName);
                retrieveEmployments(orcidService, applicationService, crisId, tokenActivities,
                    orcid, timestampAttemptToRetrieve, sourceName);
            }
            
            //preparing the current data
            List<OtherName> otherNames = prepareOtherNames(metadata);
            List<ResearcherUrl> researcherUrls = prepareResearcherUrls(
                    applicationService, orcidMetadataConfiguration, metadata);
            List<ExternalIdentifier> externalIdentifiers = prepareExternalIdentifiers(
                    applicationService, orcidMetadataConfiguration, metadata);            
            List<Address> addresses = prepareAddress(metadata);
            List<Keyword> keywords = prepareKeywords(metadata);
            
            List<WrapperEducation> wrapperEducations = null;
            List<WrapperEmployment> wrapperEmployments = null;
            if (StringUtils.isNotBlank(tokenActivities))
            {
                wrapperEducations = buildEducations(
                        applicationService, crisId, mapResearcherMetadataToSend,
                        mapResearcherMetadataNestedToSend);
                wrapperEmployments = buildEmployments(
                        applicationService, crisId, mapResearcherMetadataToSend,
                        mapResearcherMetadataNestedToSend);
            }
            //send
            SingleTimeStampInfo timestampAttempt = new SingleTimeStampInfo(
                    new Date());
            
            log.info(
                    "(Q4)Send Personal Information for ResearcherPage crisID:"
                            + crisId);
            
            sendOtherNames(orcidService, applicationService, crisId, tokenProfile,
                    orcid, otherNames, deleteAndPost, timestampAttempt);
            sendResearcherUrls(orcidService, applicationService, crisId, tokenProfile,
                    orcid, researcherUrls, deleteAndPost, timestampAttempt);
            sendExternalIdentifiers(orcidService, applicationService, crisId,
                    tokenProfile, orcid, externalIdentifiers, deleteAndPost, timestampAttempt);
            sendAddresses(orcidService, applicationService, crisId, tokenProfile,
                    orcid, addresses, deleteAndPost, timestampAttempt);
            sendKeywords(orcidService, applicationService, crisId, tokenProfile, orcid,
                    keywords, deleteAndPost, timestampAttempt);
            
            if (StringUtils.isNotBlank(tokenActivities))
            {
                log.info(
                        "(Q4)Send Affiliations (Employments and Educations) for ResearcherPage crisID:"
                                + crisId);
                sendWrapperEducation(orcidService, applicationService, crisId,
                        tokenActivities, orcid, wrapperEducations,
                        deleteAndPost, timestampAttempt);

                sendWrapperEmployment(orcidService, applicationService, crisId,
                        tokenActivities, orcid, wrapperEmployments,
                        deleteAndPost, timestampAttempt);
            }
            
            
            //delete all values not in the current system both in the history then into Orcid Registry
            List<OrcidHistory> histories = applicationService.findOrcidHistoryByOrcidAndTypeId(orcid, OrcidService.CONSTANT_PART_OF_RESEARCHER_TYPE);
            for(OrcidHistory history : histories) {
                if (history.getTimestampLastAttempt() != null)
                {
                    if (history.getTimestampLastAttempt().getTimestamp()
                            .compareTo(timestampAttempt.getTimestamp()) != 0)
                    {
                        String putCode = history.getPutCode();
                        String constantUuidPrefix = history.getEntityUuid()
                                .substring(0,
                                        history.getEntityUuid().indexOf("::"));
                        switch (constantUuidPrefix)
                        {
                        case OrcidService.CONSTANT_OTHERNAME_UUID:
                            orcidService.deleteOtherName(orcid, tokenProfile, putCode);
                            break;
                        case OrcidService.CONSTANT_RESEARCHERURL_UUID:
                            orcidService.deleteResearcherUrl(orcid, tokenProfile,
                                    putCode);
                            break;
                        case OrcidService.CONSTANT_EXTERNALIDENTIFIER_UUID:
                            orcidService.deleteExternalIdentifier(orcid, tokenProfile,
                                    putCode);
                            break;
                        case OrcidService.CONSTANT_ADDRESS_UUID:
                            orcidService.deleteAddress(orcid, tokenProfile, putCode);
                            break;
                        case OrcidService.CONSTANT_KEYWORD_UUID:
                            orcidService.deleteKeyword(orcid, tokenProfile, putCode);
                            break;
                        case OrcidService.CONSTANT_EMPLOYMENT_UUID:
                            orcidService.deleteEmployment(orcid, tokenActivities,
                                    putCode);
                            break;
                        case OrcidService.CONSTANT_EDUCATION_UUID:
                            orcidService.deleteEducation(orcid, tokenActivities, putCode);
                            break;
                        }
                        applicationService.delete(OrcidHistory.class,
                                history.getId());
                    }
                }
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        return result;
    }
    
    public static boolean buildOrcidFunding(Context context,
            OrcidService orcidService, ApplicationService applicationService, String crisId, String orcid, boolean deleteAndPost, String token, Project project) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException
    {
        
        boolean result = true;
        try
        {
            SingleTimeStampInfo timestampAttemptToRetrieve = new SingleTimeStampInfo(
                    new Date());
            // retrieve from Orcid Registry checking the same source name
            // client
            String sourceName = OrcidService.getSourceClientName();

            retrieveFunding(orcidService, applicationService, crisId, token,
                    orcid, timestampAttemptToRetrieve, sourceName, project);

            // preparing the current data
            Funding funding = prepareFunding(context, applicationService, crisId, project);

            // send
            SingleTimeStampInfo timestampAttempt = new SingleTimeStampInfo(
                    new Date());

            log.info("(Q4)Send Work for ResearcherPage crisID:" + crisId);
            String value = project.getUuid();
            sendFunding(orcidService, applicationService, crisId, token, orcid,
                    getMd5Hash(value), value, funding, deleteAndPost,
                    timestampAttempt);

            // delete all values not in the current system both in the history
            // then into Orcid Registry
            List<OrcidHistory> histories = applicationService
                    .findOrcidHistoryByOrcidAndEntityUUIDAndTypeId(orcid, value, CrisConstants.PROJECT_TYPE_ID);
            for (OrcidHistory history : histories)
            {
                if (history.getTimestampLastAttempt() != null)
                {
                    if (history.getTimestampLastAttempt().getTimestamp()
                            .compareTo(timestampAttempt.getTimestamp()) != 0)
                    {
                        String putCode = history.getPutCode();
                        orcidService.deleteFunding(orcid, token, putCode);
                        applicationService.delete(OrcidHistory.class,
                                history.getId());
                    }
                }
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        return result;
        
    }

    private static Funding prepareFunding(Context context,
            ApplicationService applicationService, String crisId,
            Project project) throws SQLException
    {
        OrcidFundingMetadata itemMetadata = new OrcidFundingMetadata(context,
                project);

        Funding funding = new Funding();
        OrcidHistory orcidHistory = applicationService
                .uniqueOrcidHistoryByOwnerAndEntityUUIDAndTypeId(crisId,
                        project.getUuid(), CrisConstants.PROJECT_TYPE_ID);
        
        if (orcidHistory!=null)
        {
            String putCode = orcidHistory.getPutCode();
            if(StringUtils.isNotBlank(putCode)) {
                BigInteger bi = new BigInteger(putCode);
                funding.setPutCode(bi);
            }
        }

        if (StringUtils.isNotBlank(itemMetadata.getAmount()))
        {
            Amount amount = new Amount();
            CurrencyCode currencyCode = CurrencyCode
                    .fromValue(itemMetadata.getCurrencyCode());
            amount.setValue(itemMetadata.getAmount());
            amount.setCurrencyCode(currencyCode);
            funding.setAmount(amount);
        }

        DecimalFormat dateMonthAndDayFormat = new DecimalFormat("00");
        
        if (StringUtils.isNotBlank(itemMetadata.getStartYear())
                || StringUtils.isNotBlank(itemMetadata.getStartMonth())
                || StringUtils.isNotBlank(itemMetadata.getStartDay()))
        {
            FuzzyDate fuzzyDate = new FuzzyDate();
            if (StringUtils.isNotBlank(itemMetadata.getStartYear()))
            {
                Year year = new Year();
                year.setValue(itemMetadata.getStartYear());
                fuzzyDate.setYear(year);
            }
            if (StringUtils.isNotBlank(itemMetadata.getStartMonth()))
            {
                Month month = new Month();
                month.setValue(dateMonthAndDayFormat
                        .format(Long.parseLong(itemMetadata.getStartMonth())));
                fuzzyDate.setMonth(month);
            }
            if (StringUtils.isNotBlank(itemMetadata.getStartDay()))
            {
                Day day = new Day();
                day.setValue(dateMonthAndDayFormat
                        .format(Long.parseLong(itemMetadata.getStartDay())));
                fuzzyDate.setDay(day);
            }
            funding.setStartDate(fuzzyDate);
        }
        if (StringUtils.isNotBlank(itemMetadata.getEndYear())
                || StringUtils.isNotBlank(itemMetadata.getEndMonth())
                || StringUtils.isNotBlank(itemMetadata.getEndDay()))
        {
            FuzzyDate fuzzyDate = new FuzzyDate();
            if (StringUtils.isNotBlank(itemMetadata.getEndYear()))
            {
                Year year = new Year();
                year.setValue(itemMetadata.getEndYear());
                fuzzyDate.setYear(year);
            }
            if (StringUtils.isNotBlank(itemMetadata.getEndMonth()))
            {
                Month month = new Month();
                month.setValue(dateMonthAndDayFormat
                        .format(Long.parseLong(itemMetadata.getEndMonth())));
                fuzzyDate.setMonth(month);
            }
            if (StringUtils.isNotBlank(itemMetadata.getEndDay()))
            {
                Day day = new Day();
                day.setValue(dateMonthAndDayFormat
                        .format(Long.parseLong(itemMetadata.getEndDay())));
                fuzzyDate.setDay(day);
            }
            funding.setEndDate(fuzzyDate);
            return funding;
        }

        if (itemMetadata.getExternalIdentifier() != null)
        {
            ExternalIds fundingExternalIdentifiers = new ExternalIds();
            for (String valIdentifier : itemMetadata.getExternalIdentifier())
            {
                ExternalId fundingExternalIdentifier = new ExternalId();
                fundingExternalIdentifier.setExternalIdType(
                        itemMetadata.getExternalIdentifierType(valIdentifier));
                fundingExternalIdentifier.setExternalIdValue(valIdentifier);
                fundingExternalIdentifier.setExternalIdRelationship(RelationshipType.SELF);
                fundingExternalIdentifiers.getExternalId()
                        .add(fundingExternalIdentifier);
            }
            funding.setExternalIds(fundingExternalIdentifiers);
        }

        if (StringUtils.isNotBlank(itemMetadata.getTitle()))
        {
            FundingTitle fundingTitle = new FundingTitle();
            fundingTitle.setTitle(itemMetadata.getTitle());
            funding.setTitle(fundingTitle);
        }

        if (StringUtils.isNotBlank(itemMetadata.getType()))
        {
            funding.setType(FundingType.fromValue(itemMetadata.getType()));
        }

        if (StringUtils.isNotBlank(itemMetadata.getAbstract()))
        {
            funding.setShortDescription(itemMetadata.getAbstract());
        }
        if (StringUtils.isNotBlank(itemMetadata.getURL()))
        {
            Url url = new Url();
            url.setValue(itemMetadata.getURL());
            funding.setUrl(url);
        }
        
        boolean buildFundingContributors = false;
        Contributors fundingContributors = new Contributors();
        if (itemMetadata.getContributorsLead() != null)
        {
            for (String valContributor : itemMetadata.getContributorsLead())
            {
                addFundingContributor(fundingContributors, valContributor,
                        "lead");
                buildFundingContributors = true;
            }
        }

        if (itemMetadata.getContributorsCoLead() != null)
        {
            for (String valContributor : itemMetadata.getContributorsCoLead())
            {
                addFundingContributor(fundingContributors, valContributor,
                        "colead");
                buildFundingContributors = true;
            }
        }
        if (buildFundingContributors)
        {
            funding.setContributors(fundingContributors);
        }
        
        if (StringUtils.isNotBlank(itemMetadata.getOrganization()))
        {
            Organization organization = new Organization();
            organization.setName(itemMetadata.getOrganization());
            OrganizationAddress organizationAddress = new OrganizationAddress();
            organizationAddress.setCity(itemMetadata.getOrganizationCity());
            organizationAddress.setCountry(Iso3166Country
                    .fromValue(itemMetadata.getOrganizationCountry()));
            organization.setAddress(organizationAddress);
            funding.setOrganization(organization);
        }
        return funding;
    }

    private static void addFundingContributor(Contributors fundingContributors,
            String valContributor, String type)
    {
        org.dspace.authority.orcid.jaxb.funding.Contributor contributor = new org.dspace.authority.orcid.jaxb.funding.Contributor();

        Integer id = ResearcherPageUtils.getRealPersistentIdentifier(
                valContributor, ResearcherPage.class);
        String name = valContributor;
        String email = "";
        String orcid = "";
        if (null != id)
        {
            ResearcherPage ro = ResearcherPageUtils.getCrisObject(id,
                    ResearcherPage.class);
            name = ResearcherPageUtils.getStringValue(ro, "fullName");
            email = ResearcherPageUtils.getStringValue(ro, "email");
            orcid = ResearcherPageUtils.getStringValue(ro, "orcid");

            if (StringUtils.isNotBlank(email))
            {
                org.dspace.authority.orcid.jaxb.common.ContributorEmail contributorEmail = new org.dspace.authority.orcid.jaxb.common.ContributorEmail();
                contributorEmail.setValue(email);
                contributor.setContributorEmail(contributorEmail);
            }
            if (StringUtils.isNotBlank(orcid))
            {
                String domainOrcid = ConfigurationManager.getProperty("cris",
                        "external.domainname.authority.service.orcid");
                OrcidId orcidID = new OrcidId();
                orcidID.setHost(domainOrcid);
                orcidID.setPath(orcid);
                orcidID.setUri(domainOrcid + orcid);
                contributor.setContributorOrcid(orcidID);
            }
        }

        org.dspace.authority.orcid.jaxb.common.CreditName creditName = new org.dspace.authority.orcid.jaxb.common.CreditName();
        creditName.setValue(name);
        contributor.setCreditName(creditName);

        org.dspace.authority.orcid.jaxb.funding.ContributorAttributes attributes = new org.dspace.authority.orcid.jaxb.funding.ContributorAttributes();
        attributes.setContributorRole(
                org.dspace.authority.orcid.jaxb.funding.ContributorRole
                        .fromValue(type));
        fundingContributors.getContributor().add(contributor);
    }

    /**
     * At the minimum, the work must include the following elements: 1) Work
     * title 2) Work type 3) Unique work identifier—add as many of these as your
     * system is aware; it aids in grouping on ORCID records, so researchers
     * don’t have to group works manually (Work identifier type, Value of the
     * identifier, Identifier URL (optional), Relationship: self/part of This is
     * to indicate the relationship of the work to the identifier. For example,
     * if the work is a book chapter, and the identifier is the ISBN for the
     * book, then the relationship would be “part of”; if the identifier is the
     * DOI for the book chapter itself, then the relationship would be “self”)
     * 
     * We also suggest that you include all other known metadata available for a
     * work , in particular: 4) Publication date, which aids display sorting 4)
     * Journal title (where relevant) 5) Citation (we recommend BibTeX), which
     * can be used to record any additional metadata not captured by the schema,
     * e.g. co-authors, page numbers
     * 
     * @param context
     * @param ii
     * @param putCode
     * @return
     * @throws SQLException
     * @throws UnsupportedEncodingException 
     * @throws NoSuchAlgorithmException 
     */
    public static boolean buildOrcidWork(Context context,
            OrcidService orcidService, ApplicationService applicationService,
            String crisId, String orcid, boolean deleteAndPost, String token,
            Integer ii) throws SQLException, NoSuchAlgorithmException,
            UnsupportedEncodingException
    {
        boolean result = true;
        try
        {
            
            Item item = Item.find(context, ii);
            
            SingleTimeStampInfo timestampAttemptToRetrieve = new SingleTimeStampInfo(
                    new Date());
            // retrieve from Orcid Registry checking the same source name
            // client
            String sourceName = OrcidService.getSourceClientName();
            retrieveWork(orcidService, applicationService, crisId, token,
                    orcid, timestampAttemptToRetrieve, sourceName, item);

            // preparing the current data
            Work work = prepareWork(context, applicationService, crisId, item);

            // send
            SingleTimeStampInfo timestampAttempt = new SingleTimeStampInfo(
                    new Date());

            log.info("(Q4)Send Work for ResearcherPage crisID:" + crisId);
            String value = item.getHandle();
            sendWork(orcidService, applicationService, crisId, token, orcid,
                    getMd5Hash(value), value, work, deleteAndPost,
                    timestampAttempt);

            // delete all values not in the current system both in the history
            // then into Orcid Registry
            List<OrcidHistory> histories = applicationService
                    .findOrcidHistoryByOrcidAndEntityUUIDAndTypeId(orcid, value, Constants.ITEM);
            for (OrcidHistory history : histories)
            {
                if (history.getTimestampLastAttempt() != null)
                {
                    if (history.getTimestampLastAttempt().getTimestamp()
                            .compareTo(timestampAttempt.getTimestamp()) != 0)
                    {
                        String putCode = history.getPutCode();
                        orcidService.deleteWork(orcid, token, putCode);
                        applicationService.delete(OrcidHistory.class,
                                history.getId());
                    }
                }
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        return result;
    }

    private static Work prepareWork(Context context, ApplicationService applicationService, String crisId,
            Item item) throws SQLException
    {
        Work orcidWork = new Work();
        
        OrcidHistory orcidHistory = applicationService
                .uniqueOrcidHistoryByOwnerAndEntityUUIDAndTypeId(crisId,
                        item.getHandle(), Constants.ITEM);
        
        if (orcidHistory!=null)
        {
            String putCode = orcidHistory.getPutCode();
            if(StringUtils.isNotBlank(putCode)) {
                BigInteger bi = new BigInteger(putCode);
                orcidWork.setPutCode(bi);
            }
        }

        OrcidWorkMetadata itemMetadata = new OrcidWorkMetadata(context, item);

        WorkTitle worktitle = new WorkTitle();
        worktitle.setTitle(itemMetadata.getTitle());

        if (StringUtils.isNotBlank(itemMetadata.getTranslatedTitle()))
        {
            TranslatedTitle translatedTitle = new TranslatedTitle();
            translatedTitle.setValue(itemMetadata.getTranslatedTitle());
            String translatedLanguageCode = itemMetadata
                    .getTranslatedTitleLanguage();

            try
            {
                LanguageCode langCode = LanguageCode
                        .fromValue(translatedLanguageCode);
                translatedTitle.setLanguageCode(langCode);
            }
            catch (Exception ex)
            {
                translatedTitle.setLanguageCode(LanguageCode.EN);
            }
            worktitle.setTranslatedTitle(translatedTitle);
        }

        String citationVal = itemMetadata.getCitation();
        if (StringUtils.isNotBlank(citationVal))
        {
            Citation citation = new Citation();
            citation.setCitationType(
                    CitationType.fromValue(itemMetadata.getCitationType()));
            citation.setCitationValue(citationVal);
            orcidWork.setCitation(citation);
        }

        orcidWork.setTitle(worktitle);

        if (StringUtils.isNotBlank(itemMetadata.getJournalTitle()))
        {
            orcidWork.setJournalTitle(itemMetadata.getJournalTitle());
        }

        DecimalFormat dateMonthAndDayFormat = new DecimalFormat("00");

        if (StringUtils.isNotBlank(itemMetadata.getYear())
                || StringUtils.isNotBlank(itemMetadata.getMonth())
                || StringUtils.isNotBlank(itemMetadata.getDay()))
        {
            FuzzyDate publicationDate = new FuzzyDate();
            if (StringUtils.isNotBlank(itemMetadata.getYear()))
            {
                Year year = new Year();
                year.setValue(itemMetadata.getYear());
                publicationDate.setYear(year);
            }
            if (StringUtils.isNotBlank(itemMetadata.getMonth()))
            {
                Month month = new Month();
                month.setValue(dateMonthAndDayFormat
                        .format(Long.parseLong(itemMetadata.getMonth())));
                publicationDate.setMonth(month);
            }
            if (StringUtils.isNotBlank(itemMetadata.getDay()))
            {
                Day day = new Day();
                day.setValue(dateMonthAndDayFormat
                        .format(Long.parseLong(itemMetadata.getDay())));
                publicationDate.setDay(day);
            }
            orcidWork.setPublicationDate(publicationDate);
        }

        if (StringUtils.isNotBlank(itemMetadata.getURL()))
        {
            Url url = new Url();
            url.setValue(itemMetadata.getURL());
            orcidWork.setUrl(url);
        }

        // add source internal id
        ExternalIds workExternalIdentifiers = new ExternalIds();
        ExternalId workExternalIdentifierInternal = new ExternalId();

        // add other external id
        if (itemMetadata.getExternalIdentifier() != null
                && !itemMetadata.getExternalIdentifier().isEmpty())
        {
            for (String valIdentifier : itemMetadata.getExternalIdentifier())
            {
                ExternalId workExternalIdentifier = new ExternalId();
                workExternalIdentifier.setExternalIdValue(valIdentifier);
                workExternalIdentifier.setExternalIdUrl(valIdentifier);
                workExternalIdentifier.setExternalIdType(
                        itemMetadata.getExternalIdentifierType(valIdentifier));
                workExternalIdentifier.setExternalIdRelationship(RelationshipType.SELF);
                workExternalIdentifiers.getExternalId()
                        .add(workExternalIdentifier);
            }
        }

        boolean forceLocalId = ConfigurationManager.getBooleanProperty("cris",
                "system.script.pushtoorcid.works.local.identifier.force",
                false);
        if (itemMetadata.getExternalIdentifier() == null
                || itemMetadata.getExternalIdentifier().isEmpty()
                || forceLocalId)
        {
            workExternalIdentifierInternal
                    .setExternalIdValue("" + item.getID());
            workExternalIdentifierInternal.setExternalIdType(
                    OrcidExternalIdentifierType.SOURCE_ID.toString());
            workExternalIdentifierInternal.setExternalIdUrl("" + item.getID());
            workExternalIdentifierInternal
                    .setExternalIdRelationship(RelationshipType.SELF);
            workExternalIdentifiers.getExternalId()
                    .add(workExternalIdentifierInternal);
        }
        orcidWork.setExternalIds(workExternalIdentifiers);

        // export if have an authority value
        WorkContributors workContributors = new WorkContributors();
        boolean haveContributor = false;
        for (String valContributor : itemMetadata.getAuthors())
        {
            Contributor contributor = new Contributor();

            Integer id = ResearcherPageUtils.getRealPersistentIdentifier(
                    valContributor, ResearcherPage.class);
            String name = valContributor;
            String email = "";
            String orcid = "";
            if (null != id)
            {
                ResearcherPage ro = ResearcherPageUtils.getCrisObject(id,
                        ResearcherPage.class);
                name = ResearcherPageUtils.getStringValue(ro, "fullName");
                email = ResearcherPageUtils.getStringValue(ro, "email");
                orcid = ResearcherPageUtils.getStringValue(ro, "orcid");

                if (StringUtils.isNotBlank(email))
                {
                    ContributorEmail contributorEmail = new ContributorEmail();
                    contributorEmail.setValue(email);
                    contributor.setContributorEmail(contributorEmail);
                }
                if (StringUtils.isNotBlank(orcid))
                {
                    String domainOrcid = ConfigurationManager.getProperty(
                            "cris",
                            "external.domainname.authority.service.orcid");
                    OrcidId orcidID = new OrcidId();

                    orcidID.setHost(domainOrcid);
                    orcidID.setPath(orcid);
                    orcidID.setUri(domainOrcid + orcid);
                    contributor.setContributorOrcid(orcidID);
                }
            }

            org.dspace.authority.orcid.jaxb.common.CreditName creditName = new org.dspace.authority.orcid.jaxb.common.CreditName();
            creditName.setValue(name);
            contributor.setCreditName(creditName);

            ContributorAttributes attributes = new ContributorAttributes();
            // TODO now supported only author/additional
            attributes.setContributorRole(ContributorRole.AUTHOR);
            attributes.setContributorSequence(ContributorSequence.ADDITIONAL);
            contributor.setContributorAttributes(attributes);
            workContributors.getContributor().add(contributor);
            haveContributor = true;
        }
        if (haveContributor)
        {
            orcidWork.setContributors(workContributors);
        }

        if (StringUtils.isNotBlank(itemMetadata.getLanguage()))
        {
            LanguageCode language = LanguageCode
                    .fromValue(itemMetadata.getLanguage());
            orcidWork.setLanguageCode(language);
        }

        SimpleMapConverter mapConverterModifier = new DSpace()
                .getServiceManager().getServiceByName(
                        "mapConverterOrcidWorkType", SimpleMapConverter.class);
        if (mapConverterModifier == null)
        {
            orcidWork.setType(WorkType.valueOf(itemMetadata.getWorkType()));
        }
        else
        {
            orcidWork.setType(WorkType.fromValue(
                    mapConverterModifier.getValue(itemMetadata.getWorkType())));
        }

        return orcidWork;
    }

    private static List<WrapperEmployment> buildEmployments(
            ApplicationService applicationService, String crisId,
            Map<String, Map<String, List<String>>> mapResearcherMetadataToSend,
            Map<String, Map<String, List<Map<String, List<String>>>>> mapResearcherMetadataNestedToSend)
    {

        log.info(
                "Prepare Employments for ResearcherPage crisID:"
                        + crisId);
        
        Map<String, List<Map<String, List<String>>>> metadataNested = mapResearcherMetadataNestedToSend
                .get(crisId);

        List<Map<String, List<String>>> employments = metadataNested
                .get("affiliations-employment");

        List<WrapperEmployment> affiliations = new ArrayList<WrapperEmployment>();
        if (employments != null)
        {
            for (Map<String, List<String>> employment : employments)
            {
                WrapperEmployment wrapper = new WrapperEmployment();
                Employment affiliation = new Employment();

                wrapper.setEmployment(affiliation);
                wrapper.setId(Integer.parseInt(employment.get("id").get(0)));
                wrapper.setUuid(employment.get("uuid").get(0));
                wrapper.setType(
                        Integer.parseInt(employment.get("type").get(0)));
                List<String> listStartDate = employment
                        .get("affiliationstartdate");

                DecimalFormat dateMonthAndDayFormat = new DecimalFormat("00");
                if (listStartDate != null && !listStartDate.isEmpty())
                {
                    String stringStartDate = listStartDate.get(0);
                    Date startDate;
                    Calendar cal1 = Calendar.getInstance();
                    try
                    {
                        startDate = df.parse(stringStartDate);
                        cal1.setTime(startDate);
                    }
                    catch (ParseException e)
                    {
                        log.error(e.getMessage());
                    }

                    FuzzyDate fuzzyStartDate = new FuzzyDate();
                    try
                    {
                        int yearSD = cal1.get(Calendar.YEAR);
                        Year year = new Year();
                        year.setValue(String.valueOf(yearSD));
                        fuzzyStartDate.setYear(year);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }

                    try
                    {
                        int monthSD = cal1.get(Calendar.MONTH);
                        Month month = new Month();
                        month.setValue(dateMonthAndDayFormat
                                .format(monthSD));
                        fuzzyStartDate.setMonth(month);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }

                    try
                    {
                        int daySD = cal1.get(Calendar.DAY_OF_MONTH);
                        Day day = new Day();
                        day.setValue(dateMonthAndDayFormat
                                .format(daySD));
                        fuzzyStartDate.setDay(day);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }
                    affiliation.setStartDate(fuzzyStartDate);
                }

                List<String> listEndDate = employment.get("affiliationenddate");
                if (listEndDate != null && !listEndDate.isEmpty())
                {
                    String stringEndDate = listStartDate.get(0);
                    Date endDate;
                    Calendar cal2 = Calendar.getInstance();
                    try
                    {
                        endDate = df.parse(stringEndDate);
                        cal2.setTime(endDate);
                    }
                    catch (ParseException e)
                    {
                        log.error(e.getMessage());
                    }

                    FuzzyDate fuzzyEndDate = new FuzzyDate();
                    try
                    {
                        int yearED = cal2.get(Calendar.YEAR);
                        Year year = new Year();
                        year.setValue(String.valueOf(yearED));
                        fuzzyEndDate.setYear(year);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }

                    try
                    {
                        int monthED = cal2.get(Calendar.MONTH);
                        Month month = new Month();
                        month.setValue(dateMonthAndDayFormat
                                .format(monthED));
                        fuzzyEndDate.setMonth(month);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }

                    try
                    {
                        int dayED = cal2.get(Calendar.DAY_OF_MONTH);
                        Day day = new Day();
                        day.setValue(dateMonthAndDayFormat
                                .format(dayED));
                        fuzzyEndDate.setDay(day);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }

                    affiliation.setEndDate(fuzzyEndDate);
                }

                List<String> listRole = employment.get("affiliationrole");
                if (listRole != null && !listRole.isEmpty())
                {
                    String stringRole = listRole.get(0);
                    affiliation.setRoleTitle(stringRole);
                }

                List<String> listOrgUnitname = employment
                        .get("affiliationorgunit.parentorgunit");
                Organization organization = new Organization();
                String stringOUname = "";
                if (listOrgUnitname != null && !listOrgUnitname.isEmpty())
                {
                    stringOUname = listOrgUnitname.get(0);
                    List<String> listDeptname = employment
                            .get("affiliationorgunit");
                    if (listDeptname != null && !listDeptname.isEmpty())
                    {
                        String stringDeptname = listDeptname.get(0);
                        affiliation.setDepartmentName(stringDeptname);
                    }
                }
                else
                {
                    List<String> listDeptname = employment
                            .get("affiliationorgunit");
                    if (listDeptname != null && !listDeptname.isEmpty())
                    {
                        stringOUname = listDeptname.get(0);
                    }
                }

                OrganizationAddress organizationAddress = new OrganizationAddress();
                List<String> listOrgunitcity = employment
                        .get("affiliationorgunit.city");
                if (listOrgunitcity != null && !listOrgunitcity.isEmpty())
                {
                    organizationAddress.setCity(listOrgunitcity.get(0));
                }

                List<String> listOrgunitcountry = employment
                        .get("affiliationorgunit.iso-country");
                if (listOrgunitcountry != null && !listOrgunitcountry.isEmpty())
                {
                    Iso3166Country isoCountry = Iso3166Country
                            .fromValue(listOrgunitcountry.get(0));
                    organizationAddress.setCountry(isoCountry);
                }

                List<String> listOrgunitregion = employment
                        .get("affiliationorgunit.region");
                if (listOrgunitregion != null && !listOrgunitregion.isEmpty())
                {
                    organizationAddress.setRegion(listOrgunitregion.get(0));
                }

                organization.setName(stringOUname);
                organization.setAddress(organizationAddress);
                affiliation.setOrganization(organization);
                affiliations.add(wrapper);
            }
        }
        return affiliations;
    }

    private static List<WrapperEducation> buildEducations(
            ApplicationService applicationService, String crisId,
            Map<String, Map<String, List<String>>> mapResearcherMetadataToSend,
            Map<String, Map<String, List<Map<String, List<String>>>>> mapResearcherMetadataNestedToSend)
    {
        
        log.info(
                "Prepare Educations for ResearcherPage crisID:"
                        + crisId);

        Map<String, List<Map<String, List<String>>>> metadataNested = mapResearcherMetadataNestedToSend
                .get(crisId);

        List<Map<String, List<String>>> educations = metadataNested
                .get("affiliations-education");
        List<WrapperEducation> affiliations = new ArrayList<WrapperEducation>();

        if (educations != null)
        {
            for (Map<String, List<String>> education : educations)
            {
                WrapperEducation wrapper = new WrapperEducation();

                Education affiliation = new Education();

                wrapper.setEducation(affiliation);
                wrapper.setId(Integer.parseInt(education.get("id").get(0)));
                wrapper.setUuid(education.get("uuid").get(0));
                wrapper.setType(Integer.parseInt(education.get("type").get(0)));
                List<String> listStartDate = education
                        .get("educationstartdate");

                if (listStartDate != null && !listStartDate.isEmpty())
                {
                    String stringStartDate = listStartDate.get(0);
                    Date startDate;
                    Calendar cal1 = Calendar.getInstance();
                    try
                    {
                        startDate = df.parse(stringStartDate);
                        cal1.setTime(startDate);
                    }
                    catch (ParseException e)
                    {
                        log.error(e.getMessage());
                    }

                    FuzzyDate fuzzyStartDate = new FuzzyDate();

                    try
                    {
                        int yearSD = cal1.get(Calendar.YEAR);
                        Year year = new Year();
                        year.setValue(String.valueOf(yearSD));
                        fuzzyStartDate.setYear(year);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }

                    try
                    {
                        int monthSD = cal1.get(Calendar.MONTH);
                        Month month = new Month();
                        month.setValue(MessageFormat.format("{0,number,#00}",
                                new Object[] { new Integer(monthSD) }));
                        fuzzyStartDate.setMonth(month);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }

                    try
                    {
                        int daySD = cal1.get(Calendar.DAY_OF_MONTH);
                        Day day = new Day();
                        day.setValue(MessageFormat.format("{0,number,#00}",
                                new Object[] { new Integer(daySD) }));
                        fuzzyStartDate.setDay(day);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }
                    affiliation.setStartDate(fuzzyStartDate);
                }

                List<String> listEndDate = education.get("educationenddate");
                if (listEndDate != null && !listEndDate.isEmpty())
                {
                    String stringEndDate = listStartDate.get(0);
                    Date endDate;
                    Calendar cal2 = Calendar.getInstance();
                    try
                    {
                        endDate = df.parse(stringEndDate);
                        cal2.setTime(endDate);
                    }
                    catch (ParseException e)
                    {
                        log.error(e.getMessage());
                    }

                    FuzzyDate fuzzyEndDate = new FuzzyDate();

                    try
                    {
                        int yearED = cal2.get(Calendar.YEAR);
                        Year year = new Year();
                        year.setValue(String.valueOf(yearED));
                        fuzzyEndDate.setYear(year);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }

                    try
                    {
                        int monthED = cal2.get(Calendar.MONTH);
                        Month month = new Month();
                        month.setValue(MessageFormat.format("{0,number,#00}",
                                new Object[] { new Integer(monthED) }));
                        fuzzyEndDate.setMonth(month);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }

                    try
                    {
                        int dayED = cal2.get(Calendar.DAY_OF_MONTH);
                        Day day = new Day();
                        day.setValue(MessageFormat.format("{0,number,#00}",
                                new Object[] { new Integer(dayED) }));
                        fuzzyEndDate.setDay(day);
                    }
                    catch (Exception ex)
                    {
                        // nothing todo
                    }
                    affiliation.setEndDate(fuzzyEndDate);
                }

                List<String> listRole = education.get("educationrole");
                if (listRole != null && !listRole.isEmpty())
                {
                    String stringRole = listRole.get(0);
                    affiliation.setRoleTitle(stringRole);
                }

                List<String> listOrgUnitname = education
                        .get("educationorgunit.parentorgunit");
                Organization organization = new Organization();

                String stringOUname = "";
                if (listOrgUnitname != null && !listOrgUnitname.isEmpty())
                {
                    stringOUname = listOrgUnitname.get(0);
                    List<String> listDeptname = education
                            .get("educationorgunit");
                    if (listDeptname != null && !listDeptname.isEmpty())
                    {
                        String stringDeptname = listDeptname.get(0);
                        affiliation.setDepartmentName(stringDeptname);
                    }
                }
                else
                {
                    List<String> listDeptname = education
                            .get("educationorgunit");
                    if (listDeptname != null && !listDeptname.isEmpty())
                    {
                        stringOUname = listDeptname.get(0);
                    }
                }

                OrganizationAddress organizationAddress = new OrganizationAddress();
                List<String> listOrgunitcity = education
                        .get("educationorgunit.city");
                if (listOrgunitcity != null && !listOrgunitcity.isEmpty())
                {
                    organizationAddress.setCity(listOrgunitcity.get(0));
                }

                List<String> listOrgunitcountry = education
                        .get("educationorgunit.iso-country");
                if (listOrgunitcountry != null && !listOrgunitcountry.isEmpty())
                {
                    Iso3166Country isoCountry = Iso3166Country
                            .fromValue(listOrgunitcountry.get(0));
                    organizationAddress.setCountry(isoCountry);
                }

                List<String> listOrgunitregion = education
                        .get("educationorgunit.region");
                if (listOrgunitregion != null && !listOrgunitregion.isEmpty())
                {
                    organizationAddress.setRegion(listOrgunitregion.get(0));
                }

                organization.setName(stringOUname);
                organization.setAddress(organizationAddress);
                affiliation.setOrganization(organization);
                affiliations.add(wrapper);
            }
        }

        return affiliations;
    }
    
    //RETRIEVE METHODS
    private static void retrieveKeywords(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, SingleTimeStampInfo timestampAttempt, String currentSourceName)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        Keywords keywordsOrcid = orcidService.getKeywords(orcid, token);
        if(keywordsOrcid!=null) {
            for(KeywordCtype nctype : keywordsOrcid.getKeyword()) {
                String orcidSourceName = nctype.getSource().getSourceName().getContent();
                if(orcidSourceName.equals(currentSourceName)) {
                    String value = nctype.getContent();
                    Integer constantType = OrcidService.CONSTANT_PART_OF_RESEARCHER_TYPE;
                    registerHistoryStillInOrcid(applicationService, crisId, orcid,
                            timestampAttempt, value, nctype.getPutCode().toString(), constantType, OrcidService.CONSTANT_KEYWORD_UUID);
                }
            }
        }
    }

    private static void retrieveAddresses(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, SingleTimeStampInfo timestampAttempt, String currentSourceName)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        Addresses addressesOrcid = orcidService.getAddresses(orcid, token);
        if(addressesOrcid!=null) {
            for(AddressCtype nctype : addressesOrcid.getAddress()) {
                String orcidSourceName = nctype.getSource().getSourceName().getContent();
                if(orcidSourceName.equals(currentSourceName)) {
                    String value = nctype.getCountry();
                    Integer constantType = OrcidService.CONSTANT_PART_OF_RESEARCHER_TYPE;
                    registerHistoryStillInOrcid(applicationService, crisId, orcid,
                            timestampAttempt, value, nctype.getPutCode().toString(), constantType, OrcidService.CONSTANT_ADDRESS_UUID);
                }
            }
        }
    }

    private static void retrieveExternalIdentifiers(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, SingleTimeStampInfo timestampAttempt, String currentSourceName)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        ExternalIdentifiers externalIdentifiersOrcid = orcidService.getExternalIdentifiers(orcid, token);
        if(externalIdentifiersOrcid!=null) {
            for(ExternalId nctype : externalIdentifiersOrcid.getExternalIdentifier()) {
                String orcidSourceName = nctype.getSource().getSourceName().getContent();
                if(orcidSourceName.equals(currentSourceName)) {
                    String value = nctype.getExternalIdUrl();
                    Integer constantType = OrcidService.CONSTANT_PART_OF_RESEARCHER_TYPE;
                    registerHistoryStillInOrcid(applicationService, crisId, orcid,
                            timestampAttempt, value, nctype.getPutCode().toString(), constantType, OrcidService.CONSTANT_EXTERNALIDENTIFIER_UUID);
                }
            }
        }
    }

    private static void retrieveResearcherUrls(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, SingleTimeStampInfo timestampAttempt, String currentSourceName)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        ResearcherUrls researcherUrlsOrcid = orcidService.getResearcherUrls(orcid, token);
        if(researcherUrlsOrcid!=null) {
            for(ResearcherUrlCtype nctype : researcherUrlsOrcid.getResearcherUrl()) {
                String orcidSourceName = nctype.getSource().getSourceName().getContent();
                if(orcidSourceName.equals(currentSourceName)) {
                    String value = nctype.getUrl().getValue();
                    Integer constantType = OrcidService.CONSTANT_PART_OF_RESEARCHER_TYPE;
                    registerHistoryStillInOrcid(applicationService, crisId, orcid,
                            timestampAttempt, value, nctype.getPutCode().toString(), constantType, OrcidService.CONSTANT_RESEARCHERURL_UUID);
                }
            }
        }
    }

    private static void retrieveOtherNames(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, SingleTimeStampInfo timestampAttempt, String currentSourceName)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        OtherNames otherNamesOrcid = orcidService.getOtherNames(orcid, token);
        if(otherNamesOrcid!=null) {
            for(OtherNameCtype nctype : otherNamesOrcid.getOtherName()) {
                String orcidSourceName = nctype.getSource().getSourceName().getContent();
                if(orcidSourceName.equals(currentSourceName)) {
                    String value = nctype.getContent();
                    Integer constantType = OrcidService.CONSTANT_PART_OF_RESEARCHER_TYPE;                    
                    registerHistoryStillInOrcid(applicationService, crisId, orcid,
                            timestampAttempt, value, nctype.getPutCode().toString(), constantType, OrcidService.CONSTANT_OTHERNAME_UUID);
                }
            }
        }
    }

    private static void retrieveEmployments(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, SingleTimeStampInfo timestampAttemptToRetrieve,
            String currentSourceName)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        Employments employmentsOrcid = orcidService.getEmployments(orcid, token);
        if(employmentsOrcid!=null) {
            for(EmploymentSummary nctype : employmentsOrcid.getEmploymentSummary()) {
                String orcidSourceName = nctype.getSource().getSourceName().getContent();
                if(orcidSourceName.equals(currentSourceName)) {
                    String value = nctype.getOrganization().getName() + nctype.getStartDate().toString() + nctype.getEndDate().toString();
                    Integer constantType = OrcidService.CONSTANT_PART_OF_RESEARCHER_TYPE;
                    registerHistoryStillInOrcid(applicationService, crisId, orcid,
                            timestampAttemptToRetrieve, value, nctype.getPutCode().toString(), constantType, OrcidService.CONSTANT_EMPLOYMENT_UUID);
                }
            }
        }
    }

    private static void retrieveEducations(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, SingleTimeStampInfo timestampAttemptToRetrieve,
            String currentSourceName)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        Educations educationsOrcid = orcidService.getEducations(orcid, token);
        if(educationsOrcid!=null) {
            for(EducationSummary nctype : educationsOrcid.getEducationSummary()) {
                String orcidSourceName = nctype.getSource().getSourceName().getContent();
                if(orcidSourceName.equals(currentSourceName)) {
                    String value = nctype.getOrganization().getName() + nctype.getStartDate().toString() + nctype.getEndDate().toString();
                    Integer constantType = OrcidService.CONSTANT_PART_OF_RESEARCHER_TYPE;
                    registerHistoryStillInOrcid(applicationService, crisId, orcid,
                            timestampAttemptToRetrieve, value, nctype.getPutCode().toString(), constantType, OrcidService.CONSTANT_EDUCATION_UUID);
                }
            }
        }
    }

    private static void retrieveWork(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, SingleTimeStampInfo timestampAttemptToRetrieve,
            String currentSourceName, Item item)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        Works worksOrcid = orcidService.getWorks(orcid, token);
        if(worksOrcid!=null) {
            for(WorkGroup group : worksOrcid.getGroup()) {
                for (WorkSummary nctype : group.getWorkSummary())
                {
                    String orcidSourceName = nctype.getSource().getSourceName()
                            .getContent();
                    if (orcidSourceName.equals(currentSourceName))
                    {
                        String value = getInternalIdentifier(nctype);
                        if(StringUtils.isNotBlank(value)) {
                            if (item.getHandle().equals(value))
                            {
                                Integer constantType = Constants.ITEM;
                                registerHistoryStillInOrcid(applicationService,
                                        crisId, orcid,
                                        timestampAttemptToRetrieve, value,
                                        nctype.getPutCode().toString(),
                                        constantType, value);
                            }
                        }
                    }
                }
            }
        }
    }
    

    private static void retrieveFunding(OrcidService orcidService,
            ApplicationService applicationService, String crisId, String token,
            String orcid, SingleTimeStampInfo timestampAttemptToRetrieve,
            String currentSourceName, Project project)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        Fundings worksOrcid = orcidService.getFundings(orcid, token);
        if(worksOrcid!=null) {
            for(FundingGroup group : worksOrcid.getGroup()) {
                for (FundingSummary nctype : group.getFundingSummary())
                {
                    String orcidSourceName = nctype.getSource().getSourceName()
                            .getContent();
                    if (orcidSourceName.equals(currentSourceName))
                    {
                        String value = getInternalIdentifier(nctype);
                        if(StringUtils.isNotBlank(value)) {
                            if (value.equals(project.getUuid()))
                            {
                                Integer constantType = CrisConstants.PROJECT_TYPE_ID;
                                registerHistoryStillInOrcid(applicationService,
                                        crisId, orcid,
                                        timestampAttemptToRetrieve, value,
                                        nctype.getPutCode().toString(),
                                        constantType, value);
                            }
                        }
                    }
                }
            }
        }
    }    
    
    // REGISTER METHODS
    public static void registerHistoryStillInOrcid(ApplicationService applicationService,
            String crisId, String orcid, SingleTimeStampInfo timestampAttempt,
            String value, String putCode, Integer constantType, String constantUuid)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        String md5ContentPartOfResearcher = getMd5Hash(value);
        String constantUuidPartOf = constantUuid; 
        if(OrcidService.CONSTANT_PART_OF_RESEARCHER_TYPE.equals(constantType)) {        
            constantUuidPartOf += "::"+md5ContentPartOfResearcher ;
        }
        try
        {
            OrcidHistory orcidHistory = applicationService
                    .uniqueOrcidHistoryByOwnerAndEntityUUIDAndTypeId(crisId,
                            constantUuidPartOf, constantType);
            if (orcidHistory == null)
            {
                orcidHistory = new OrcidHistory();
            }
            orcidHistory.setEntityUuid(constantUuidPartOf);
            orcidHistory.setTypeId(constantType);
            orcidHistory.setOwner(crisId);
            orcidHistory.setPutCode(putCode);
            orcidHistory.setTimestampLastAttempt(timestampAttempt);
            orcidHistory.setOrcid(orcid);
            orcidHistory.setResponseMessage(null);
            orcidHistory.setTimestampSuccessAttempt(null);
            applicationService.saveOrUpdate(OrcidHistory.class, orcidHistory);                            
        }
        catch (Exception e)
        {
            log.error("ERROR!!! (E1)::PUSHORCID::RETRIEVE" + constantUuidPartOf
                    + " for ResearcherPage crisID:" + crisId);
        }
    }
   
    // PREPARE SECTION
    private static List<ExternalIdentifier> prepareExternalIdentifiers(
            ApplicationService applicationService,
            Map<String, String> orcidMetadataConfiguration,
            Map<String, List<String>> metadata)
    {
        List<ExternalIdentifier> externalIdentifiers = new ArrayList<ExternalIdentifier>();

        for (String key : metadata.keySet())
        {
            if (key.startsWith("external-identifier-"))
            {
                RPPropertiesDefinition rpPD = applicationService
                        .findPropertiesDefinitionByShortName(
                                RPPropertiesDefinition.class,
                                orcidMetadataConfiguration.get(key));

                for (String value : metadata.get(key))
                {
                    if (StringUtils.isNotBlank(value))
                    {
                        ExternalIdentifier externalIdentifier = new ExternalIdentifier();
                        externalIdentifier.setExternalIdRelationship(
                                RelationshipType.SELF);
                        externalIdentifier.setExternalIdUrl(value);
                        externalIdentifier.setExternalIdType(rpPD.getLabel());
                        externalIdentifier.setExternalIdValue(value);
                        externalIdentifiers.add(externalIdentifier);
                    }
                }
            }
        }
        return externalIdentifiers;
    }

    private static List<ResearcherUrl> prepareResearcherUrls(
            ApplicationService applicationService,
            Map<String, String> orcidMetadataConfiguration,
            Map<String, List<String>> metadata)
    {
        List<ResearcherUrl> researcherUrls = new ArrayList<ResearcherUrl>();

        if (metadata.containsKey("researcher-urls"))
        {
            List<String> links = metadata.get("researcher-urls");
            for (String l : links)
            {
                ResearcherUrl researcherUrl = new ResearcherUrl();
                researcherUrl.setUrlName(l.split("###")[0]);
                Url url = new Url();
                url.setValue(l.split("###")[1]);
                researcherUrl.setUrl(url);
                researcherUrls.add(researcherUrl);
            }
        }

        for (String key : metadata.keySet())
        {
            if (key.startsWith("researcher-url-"))
            {
                RPPropertiesDefinition rpPD = applicationService
                        .findPropertiesDefinitionByShortName(
                                RPPropertiesDefinition.class,
                                orcidMetadataConfiguration.get(key));

                for (String value : metadata.get(key))
                {
                    if (StringUtils.isNotBlank(value))
                    {
                        ResearcherUrl researcherUrl = new ResearcherUrl();
                        researcherUrl.setUrlName(rpPD.getLabel());
                        Url url = new Url();
                        url.setValue(value);
                        researcherUrl.setUrl(url);
                        researcherUrls.add(researcherUrl);
                    }
                }
            }
        }
        return researcherUrls;
    }

    private static List<OtherName> prepareOtherNames(
            Map<String, List<String>> metadata)
    {
        List<String> otherNames = new ArrayList<String>();

        // prepare names
             
        List<String> othernamesPlatform = metadata.get("other-names");
        if (othernamesPlatform != null && !othernamesPlatform.isEmpty())
        {
            otherNames.addAll(othernamesPlatform);
        }

        List<OtherName> otherNamesJAXB = new ArrayList<OtherName>();
        // fill name
        if (otherNames != null && !otherNames.isEmpty())
        {
            for (String otherName : otherNames)
            {
                OtherName otherNameJAXB = new OtherName();
                otherNameJAXB.setContent(otherName);
                otherNamesJAXB.add(otherNameJAXB);
            }
        }

        return otherNamesJAXB;
    }

    private static List<Address> prepareAddress(
            Map<String, List<String>> metadata)
    {
        List<Address> addresses = new ArrayList<Address>();

        List<String> isoCountryString = metadata.get("iso-3166-country");
        if (isoCountryString != null)
        {
            for (String country : isoCountryString)
            {
                if (StringUtils.isNotBlank(country))
                {
                    Address addressJAXB = new Address();
                    addressJAXB.setCountry(country);
                    addresses.add(addressJAXB);
                }
            }
        }
        return addresses;
    }

    private static List<Keyword> prepareKeywords(
            Map<String, List<String>> metadata)
    {
        List<String> keywords = metadata.get("keywords");

        List<Keyword> keywordsJAXB = new ArrayList<Keyword>();
        if (keywords != null)
        {
            for (String kk : keywords)
            {
                if (StringUtils.isNotBlank(kk))
                {
                    Keyword k = new Keyword();
                    k.setContent(kk);
                    keywordsJAXB.add(k);
                }
            }
        }
        return keywordsJAXB;
    }

    public static void prepareMetadataForFundings(ApplicationService applicationService,
            RelationPreferenceService relationPreferenceService,
            SearchService searchService,
            Map<String, List<Integer>> mapProjectsToSend,
            ResearcherPage researcher, String crisID,
            List<Integer> projectsIDsToSend, String projectsPrefs)
    {
        if (StringUtils.isNotBlank(projectsPrefs))
        {
            if (Integer
                    .parseInt(projectsPrefs) != ORCID_PROJECTS_PREFS_DISABLED)
            {
                if (Integer.parseInt(projectsPrefs) == ORCID_PROJECTS_PREFS_ALL)
                {
                    log.info("...prepare fundings...");
                    SolrQuery query = new SolrQuery("*:*");
                    query.addFilterQuery("{!field f=search.resourcetype}"
                            + CrisConstants.PROJECT_TYPE_ID);
                    query.addFilterQuery(
                            "crisproject.principalinvestigator_authority:" + crisID);
                    query.addFilterQuery("-withdrawn:true");
                    query.setFields("search.resourceid", "search.resourcetype");
                    query.setRows(Integer.MAX_VALUE);
                    try
                    {
                        QueryResponse response = searchService.search(query);
                        SolrDocumentList docList = response.getResults();
                        Iterator<SolrDocument> solrDoc = docList.iterator();
                        while (solrDoc.hasNext())
                        {
                            SolrDocument doc = solrDoc.next();
                            Integer rpId = (Integer) doc
                                    .getFirstValue("search.resourceid");
                            projectsIDsToSend.add(rpId);
                        }
                    }
                    catch (SearchServiceException e)
                    {
                        log.error("Error retrieving documents", e);
                    }
                }
                else
                {
                    if (Integer.parseInt(
                            projectsPrefs) == ORCID_PROJECTS_PREFS_SELECTED)
                    {
                        List<RelationPreference> selected = new ArrayList<RelationPreference>();
                        for (RelationPreferenceConfiguration configuration : relationPreferenceService
                                .getConfigurationService().getList())
                        {
                            if (configuration.getRelationConfiguration()
                                    .getRelationClass().equals(Project.class))
                            {
                                selected = relationPreferenceService
                                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                                researcher.getUuid(),
                                                configuration
                                                        .getRelationConfiguration()
                                                        .getRelationName(),
                                                RelationPreference.SELECTED);
                            }
                            for (RelationPreference sel : selected)
                            {
                                projectsIDsToSend.add(sel.getItemID());
                            }
                        }

                    }
                    else
                    {
                        log.warn(crisID
                                + " - projects preferences NOT recognized");
                    }
                }
            }
            else
            {
                log.info(crisID + " - DISABLED projects preferences");
            }
        }
        mapProjectsToSend.put(crisID, projectsIDsToSend);
    }

    public static void prepareMetadataForWorks(ApplicationService applicationService,
            RelationPreferenceService relationPreferenceService,
            SearchService searchService,
            Map<String, List<Integer>> mapPublicationsToSend,
            ResearcherPage researcher, String crisID,
            List<Integer> itemIDsToSend, String publicationsPrefs)
    {
        if (publicationsPrefs != null)
        {
            if (Integer.parseInt(
                    publicationsPrefs) != ORCID_PUBLICATION_PREFS_DISABLED)
            {
                if (Integer.parseInt(
                        publicationsPrefs) == ORCID_PUBLICATION_PREFS_ALL)
                {
                    log.info("...prepare works...");
                    SolrQuery query = new SolrQuery("*:*");
                    query.addFilterQuery(
                            "{!field f=search.resourcetype}" + Constants.ITEM);
                    query.addFilterQuery("author_authority:" + crisID);
                    query.addFilterQuery("-withdrawn:true");
                    query.setFields("search.resourceid", "search.resourcetype");
                    query.setRows(Integer.MAX_VALUE);
                    try
                    {
                        QueryResponse response = searchService.search(query);
                        SolrDocumentList docList = response.getResults();
                        Iterator<SolrDocument> solrDoc = docList.iterator();
                        while (solrDoc.hasNext())
                        {
                            SolrDocument doc = solrDoc.next();
                            Integer rpId = (Integer) doc
                                    .getFirstValue("search.resourceid");
                            itemIDsToSend.add(rpId);
                        }
                    }
                    catch (SearchServiceException e)
                    {
                        log.error("Error retrieving documents", e);
                    }
                }
                else
                {
                    if (Integer.parseInt(
                            publicationsPrefs) == ORCID_PUBLICATION_PREFS_SELECTED)
                    {
                        List<RelationPreference> selected = new ArrayList<RelationPreference>();
                        for (RelationPreferenceConfiguration configuration : relationPreferenceService
                                .getConfigurationService().getList())
                        {
                            if (configuration.getRelationConfiguration()
                                    .getRelationClass().equals(Item.class))
                            {
                                selected = relationPreferenceService
                                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                                researcher.getUuid(),
                                                configuration
                                                        .getRelationConfiguration()
                                                        .getRelationName(),
                                                RelationPreference.SELECTED);
                            }
                            for (RelationPreference sel : selected)
                            {
                                itemIDsToSend.add(sel.getItemID());
                            }
                        }

                    }
                    else
                    {
                        log.warn(crisID
                                + " - publications preferences NOT recognized");
                    }
                }
            }
            else
            {
                log.info(crisID + " - DISABLED publications preferences");
            }
        }
        mapPublicationsToSend.put(crisID, itemIDsToSend);
    }

    /**
     * Prepare data configuration for Researcher Profile, fill custom data
     * structure with values getted from the ResearcherPage. Manage both first
     * level metadata then nested metadata.
     * 
     * Rules for "First level metadata": add the values only if the related
     * {@link OrcidPreferencesUtils.PREFIX_ORCID_PROFILE_PREF} is enabled. Rules
     * for "Nested metadata": if first level metadata is not found then try to
     * retrieve data from nested, if the related
     * {@link OrcidPreferencesUtils.PREFIX_ORCID_PROFILE_PREF} is enabled then
     * expose all instances of the nested otherwise expose the preferred nested
     * or the most recent nested. If the preferred nested is not set up then try
     * to retrieve the instance with "enddate" greatest or null and "startdate"
     * greatest.
     * 
     * The parameter "force" is used to expose the metadata also if the settings
     * preferences is disabled (used to force metadata exposition at first
     * time). The parameter "force" is NOT used for "Nested metadata" mapping
     * configuration because if the related preference settings is disabled this
     * means that the value to send is the preferred one or the most recent.
     * 
     * @param applicationService
     * @param mapResearcherMetadataToSend
     * @param mapResearcherMetadataNestedToSend
     * @param researcher
     * @param force
     */
    public static void prepareMetadataForProfile(
            ApplicationService applicationService,
            Map<String, Map<String, List<String>>> mapResearcherMetadataToSend,
            Map<String, Map<String, List<Map<String, List<String>>>>> mapResearcherMetadataNestedToSend,
            ResearcherPage researcher, boolean force)
    {
        List<RPPropertiesDefinition> metadataDefinitions = applicationService
                .likePropertiesDefinitionsByShortName(
                        RPPropertiesDefinition.class,
                        OrcidPreferencesUtils.PREFIX_ORCID_PROFILE_PREF);
        Map<String, List<String>> mapMetadata = new HashMap<String, List<String>>();
        // this map contains e.g. affiliation-> for each nested
        // affiliation<nestedmetadata> -> list of nested metadata values
        Map<String, List<Map<String, List<String>>>> nestedMapValueInstances = new HashMap<String, List<Map<String, List<String>>>>();

        for (RPPropertiesDefinition rppd : metadataDefinitions)
        {
            String metadataShortnameINTERNAL = rppd.getShortName().replaceFirst(
                    OrcidPreferencesUtils.PREFIX_ORCID_PROFILE_PREF, "");
            String metadataShortnameORCID = rppd.getLabel();

            List<RPProperty> metadatas = researcher.getAnagrafica4view()
                    .get(metadataShortnameINTERNAL);

            if (metadatas == null || metadatas.isEmpty())
            {
                // manage mapping configuration on nested object
                List<RPNestedObject> nestedObjects = applicationService
                        .getNestedObjectsByParentIDAndShortname(
                                researcher.getID(), metadataShortnameINTERNAL,
                                researcher.getClassNested());

                if (nestedObjects != null && !nestedObjects.isEmpty())
                {

                    // this list contains for each nested the values (this list
                    // is the object entry in the above map)
                    List<Map<String, List<String>>> listMapMetadata = new ArrayList<Map<String, List<String>>>();

                    RPNestedObject selectedForDateRules = null;
                    Date selectedStartDate = null;
                    Date selectedEndDate = null;
                    boolean foundPreferred = false;
                    boolean sendOnlyGreatest = false;
                    for (RPNestedObject rpno : nestedObjects)
                    {
                        // organize all nested or only the preferred
                        if ((researcher.getProprietaDellaTipologia(rppd) != null
                                && !researcher.getProprietaDellaTipologia(rppd)
                                        .isEmpty()
                                && (Boolean) researcher
                                        .getProprietaDellaTipologia(rppd).get(0)
                                        .getObject()))
                        {
                            prepareNestedObjectConfiguration(rpno,
                                    listMapMetadata);

                        }
                        else
                        {
                            sendOnlyGreatest = true;
                            // found if have a preferred nested to send
                            if (rpno.getPreferred()!=null && rpno.getPreferred()==true)
                            {
                                foundPreferred = true;
                                break;
                            }
                        }
                    }

                    if (foundPreferred || sendOnlyGreatest)
                    {
                        for (RPNestedObject rpno : nestedObjects)
                        {
                            if (foundPreferred)
                            {
                                // send only the preferred or the nested with
                                // the
                                // greatest startdate and enddate null or
                                // greater
                                // then the others enddate
                                if (rpno.getPreferred()!=null && rpno.getPreferred()==true)
                                {
                                    prepareNestedObjectConfiguration(rpno,
                                            listMapMetadata);
                                    selectedForDateRules = null;
                                }
                            }
                            else
                            {
                                if (selectedForDateRules == null)
                                {
                                    selectedForDateRules = rpno;

                                    List<RPNestedProperty> listEndDate = rpno
                                            .getAnagrafica4view()
                                            .get("affiliationenddate");
                                    if (listEndDate != null
                                            && !listEndDate.isEmpty())
                                    {
                                        RPNestedProperty endDate = listEndDate
                                                .get(0);
                                        DateValue valED = (DateValue) endDate
                                                .getValue();
                                        selectedEndDate = valED.getObject();
                                    }

                                    List<RPNestedProperty> listStartDate = rpno
                                            .getAnagrafica4view()
                                            .get("affiliationstartdate");
                                    if (listStartDate != null
                                            && !listStartDate.isEmpty())
                                    {
                                        RPNestedProperty startDate = listStartDate
                                                .get(0);
                                        DateValue valSD = (DateValue) startDate
                                                .getValue();
                                        selectedStartDate = valSD.getObject();
                                    }
                                }
                                else
                                {

                                    List<RPNestedProperty> listEndDate = rpno
                                            .getAnagrafica4view()
                                            .get("affiliationenddate");

                                    if (listEndDate != null
                                            && !listEndDate.isEmpty())
                                    {
                                        RPNestedProperty endDate = listEndDate
                                                .get(0);
                                        DateValue valED = (DateValue) endDate
                                                .getValue();
                                        // is valED is after selectedEndDate?
                                        if (selectedEndDate != null
                                                && valED.getObject().compareTo(
                                                        selectedEndDate) >= 0)
                                        {

                                            List<RPNestedProperty> listStartDate = rpno
                                                    .getAnagrafica4view()
                                                    .get("affiliationstartdate");
                                            RPNestedProperty startDate = listStartDate
                                                    .get(0);
                                            DateValue valSD = (DateValue) startDate
                                                    .getValue();
                                            // is valSD is before
                                            // selectedStartDate?
                                            if (valSD.getObject().compareTo(
                                                    selectedStartDate) > 1)
                                            {
                                                selectedEndDate = valED
                                                        .getObject();
                                                selectedStartDate = valSD
                                                        .getObject();
                                                selectedForDateRules = rpno;
                                            }
                                        }
                                    }
                                    else
                                    {
                                        if (selectedEndDate == null)
                                        {
                                            // found it! done!
                                            selectedForDateRules = rpno;
                                            break;
                                        }
                                    }

                                }
                            }
                        }
                    }
                    if (selectedForDateRules != null)
                    {
                        prepareNestedObjectConfiguration(selectedForDateRules,
                                listMapMetadata);
                    }
                    nestedMapValueInstances.put(metadataShortnameORCID,
                            listMapMetadata);
                }
            }
            else
            {

                List<String> listMetadata = new ArrayList<String>();
                for (RPProperty metadata : metadatas)
                {
                    if (force || (researcher
                            .getProprietaDellaTipologia(rppd) != null
                            && !researcher.getProprietaDellaTipologia(rppd)
                                    .isEmpty()
                            && (Boolean) researcher
                                    .getProprietaDellaTipologia(rppd).get(0)
                                    .getObject()))
                    {
                        if (metadata.getTypo()
                                .getRendering() instanceof WidgetLink)
                        {
                            EmbeddedLinkValue link = (EmbeddedLinkValue) metadata
                                    .getObject();
                            listMetadata.add(link.getDescriptionLink() + "###"
                                    + link.getValueLink());
                        }
                        else
                        {
                            listMetadata.add(metadata.toString());
                        }
                    }
                }

                if (!listMetadata.isEmpty())
                {
                    mapMetadata.put(metadataShortnameORCID, listMetadata);
                }
            }
        }
        mapResearcherMetadataNestedToSend.put(researcher.getCrisID(),
                nestedMapValueInstances);
        mapResearcherMetadataToSend.put(researcher.getCrisID(), mapMetadata);
    }

    private static void prepareOrganizationAddress(
            List<String> listMetadataParentOrgunitCity,
            List<String> listMetadataParentOrgunitCountry,
            List<String> listMetadataParentOrgunitRegion, OrganizationUnit ou)
    {
        List<OUProperty> cityorgunits = ou.getAnagrafica4view().get("city");
        for (OUProperty pp : cityorgunits)
        {
            listMetadataParentOrgunitCity.add(pp.toString());
        }
        List<OUProperty> countryorgunits = ou.getAnagrafica4view()
                .get("iso-country");
        for (OUProperty pp : countryorgunits)
        {
            listMetadataParentOrgunitCountry.add(pp.toString());
        }
        List<OUProperty> regionorgunits = ou.getAnagrafica4view().get("region");
        for (OUProperty pp : regionorgunits)
        {
            listMetadataParentOrgunitRegion.add(pp.toString());
        }
    }

    private static String getMd5Hash(String value) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest digester = MessageDigest.getInstance("MD5");
        digester.update(value.getBytes("UTF-8"));
        byte[] signature = digester.digest();
        char[] arr = new char[signature.length << 1];
        for (int i = 0; i < signature.length; i++)
        {
            int b = signature[i];
            int idx = i << 1;
            arr[idx] = HEX_DIGITS[(b >> 4) & 0xf];
            arr[idx + 1] = HEX_DIGITS[b & 0xf];
        }
        String sigString = new String(arr);
        return sigString;
    }
    
    public static void sendEmail(Context context, ResearcherPage researcher,
            String crisID)
    {
        try
        {
            org.dspace.core.Email email = org.dspace.core.Email.getEmail(
                    I18nUtil.getEmailFilename(context.getCurrentLocale(),
                            PushToORCID.EMAIL_TEMPLATE_NAME));
            email.addRecipient(researcher.getEmail().getValue());
            email.addArgument(ConfigurationManager.getProperty("dspace.url")
                    + "/cris/rp/" + crisID);
            email.send();
        }
        catch (MessagingException | IOException e)
        {
            log.error("Email not send for:" + crisID);
        }
    }

    private static String getInternalIdentifier(Work obj)
    {
        for (ExternalId extid : obj.getExternalIds().getExternalId())
        {
            if (OrcidExternalIdentifierType.HANDLE.toString()
                    .equals(extid.getExternalIdType()))
            {
                return extid.getExternalIdValue();
            }
        }

        return null;
    }

    private static String getInternalIdentifier(WorkSummary obj)
    {
        for (ExternalId extid : obj.getExternalIds().getExternalId())
        {
            if (OrcidExternalIdentifierType.HANDLE.toString()
                    .equals(extid.getExternalIdType()))
            {
                return extid.getExternalIdValue();
            }
        }

        return null;
    }
    
    private static String getInternalIdentifier(Funding obj)
    {
        for (ExternalId extid : obj.getExternalIds().getExternalId())
        {
            if ("uuid".toString().equals(extid.getExternalIdType()))
            {
                return extid.getExternalIdValue();
            }
        }

        return null;
    }
    
    private static String getInternalIdentifier(FundingSummary obj)
    {
        for (ExternalId extid : obj.getExternalIds().getExternalId())
        {
            if ("uuid".toString().equals(extid.getExternalIdType()))
            {
                return extid.getExternalIdValue();
            }
        }

        return null;
    }

    /**
     * Check if manual mode is enable or we have the PUT signal for
     * works/fundings/profile
     * 
     * @param researcher
     * @param isManualMode
     * @return
     */
    public static boolean isManualModeEnable(ResearcherPage researcher)
    {
        boolean isManualMode = false;
        for (RPProperty pushManual : researcher.getAnagrafica4view()
                .get(ORCID_PUSH_MANUAL))
        {
            TextValue value = (TextValue) pushManual.getValue();
            if (value.getObject().equals("1"))
            {
                isManualMode = true;
            }
            break;
        }

        for (RPProperty pushManual : researcher.getAnagrafica4view()
                .get(OrcidPreferencesUtils.ORCID_PUSH_ITEM_ACTIVATE_PUT))
        {
            BooleanValue bval = (BooleanValue) (pushManual.getValue());
            if (bval.getObject())
            {
                isManualMode = true;
            }
            break;
        }

        for (RPProperty pushManual : researcher.getAnagrafica4view()
                .get(OrcidPreferencesUtils.ORCID_PUSH_CRISPJ_ACTIVATE_PUT))
        {
            BooleanValue bval = (BooleanValue) (pushManual.getValue());
            if (bval.getObject())
            {
                isManualMode = true;
            }
            break;
        }

        for (RPProperty pushManual : researcher.getAnagrafica4view()
                .get(OrcidPreferencesUtils.ORCID_PUSH_CRISRP_ACTIVATE_PUT))
        {
            BooleanValue bval = (BooleanValue) (pushManual.getValue());
            if (bval.getObject())
            {
                isManualMode = true;
            }
            break;
        }
        return isManualMode;
    }

    public static Map<String, String> prepareConfigurationMappingForProfile(
            ApplicationService applicationService)
    {
        // prepare configuration mapping ORCID->DSPACE-CRIS
        Map<String, String> orcidConfigurationMapping = new HashMap<String, String>();
        List<RPPropertiesDefinition> metadataDefinitions = applicationService
                .likePropertiesDefinitionsByShortName(
                        RPPropertiesDefinition.class,
                        OrcidPreferencesUtils.PREFIX_ORCID_PROFILE_PREF);
        for (RPPropertiesDefinition rppd : metadataDefinitions)
        {
            String metadataShortnameINTERNAL = rppd.getShortName().replaceFirst(
                    OrcidPreferencesUtils.PREFIX_ORCID_PROFILE_PREF, "");
            String metadataShortnameORCID = rppd.getLabel();
            orcidConfigurationMapping.put(metadataShortnameORCID,
                    metadataShortnameINTERNAL);
        }
        return orcidConfigurationMapping;
    }
    
    /**
     * Organize the maps to fill the values. In the case the metadata is a
     * pointer to the OrganizationUnit and is called with suffix 'orgunit' then
     * try to retrieve a 'parentorgunit' metadata. This is usefull when Orcid
     * expose information about Affiliation, infact in this case the string
     * value of the pointer with suffix 'orgunit' will be the department instead
     * the 'parentorgunit' will be mapped on the organization tag.
     * 
     * @param metadataShortnameORCID
     *            - the key of the result map
     * @param mapMetadataNested
     *            - the result map that contains mapping for metadata to values
     * @param rpno
     *            - the nested values
     * @param listMetadata
     *            - every instance of metadata
     * @param listMapMetadata
     *            - every instance of nested object
     */
    private static void prepareNestedObjectConfiguration(RPNestedObject rpno,
            List<Map<String, List<String>>> listMapMetadata)
    {

        // mapMetadata - map used temporary to fill mapMetadataNested
        Map<String, List<String>> mapMetadata = new HashMap<String, List<String>>();
        List<String> listId = new ArrayList<String>();
        listId.add(rpno.getId().toString());
        mapMetadata.put("id", listId);
        List<String> listUUID = new ArrayList<String>();
        listUUID.add(rpno.getUuid());
        mapMetadata.put("uuid", listUUID);
        List<String> listType = new ArrayList<String>();
        listType.add("" + rpno.getType());
        mapMetadata.put("type", listType);
        for (String metadataNestedShortname : rpno.getAnagrafica4view()
                .keySet())
        {

            List<RPNestedProperty> nestedMetadatas = rpno.getAnagrafica4view()
                    .get(metadataNestedShortname);
            List<String> listMetadataParentOrgunit = new ArrayList<String>();
            List<String> listMetadataParentOrgunitCity = new ArrayList<String>();
            List<String> listMetadataParentOrgunitCountry = new ArrayList<String>();
            List<String> listMetadataParentOrgunitRegion = new ArrayList<String>();
            List<String> listMetadata = new ArrayList<String>();
            for (RPNestedProperty metadata : nestedMetadatas)
            {

                if (metadata.getTypo().getRendering() instanceof WidgetLink)
                {
                    EmbeddedLinkValue link = (EmbeddedLinkValue) metadata
                            .getObject();
                    listMetadata.add(link.getDescriptionLink() + "###"
                            + link.getValueLink());
                }
                else if (metadata.getTypo()
                        .getRendering() instanceof WidgetPointer)
                {
                    try
                    {
                        if (metadataNestedShortname.endsWith("orgunit"))
                        {
                            OrganizationUnit ou = (OrganizationUnit) metadata
                                    .getObject();
                            List<OUProperty> parentorgunits = ou
                                    .getAnagrafica4view().get("parentorgunit");
                            OrganizationUnit parentOU = null;
                            boolean foundParent = false;
                            for (OUProperty pp : parentorgunits)
                            {
                                listMetadataParentOrgunit.add(pp.toString());
                                OUPointer pointer = (OUPointer) pp.getValue();
                                parentOU = pointer.getObject();
                                foundParent = true;
                            }
                            if (foundParent)
                            {
                                prepareOrganizationAddress(
                                        listMetadataParentOrgunitCity,
                                        listMetadataParentOrgunitCountry,
                                        listMetadataParentOrgunitRegion,
                                        parentOU);
                            }
                            else
                            {
                                prepareOrganizationAddress(
                                        listMetadataParentOrgunitCity,
                                        listMetadataParentOrgunitCountry,
                                        listMetadataParentOrgunitRegion, ou);
                            }
                            listMetadata.add(metadata.toString());
                            break;
                        }
                    }
                    catch (Exception ex)
                    {
                        log.warn(
                                "Orcid push affiliation do not able to retrieve value for parentorgunit");
                    }
                    listMetadata.add(metadata.toString());
                }
                else
                {
                    listMetadata.add(metadata.toString());
                }

            }
            if (!listMetadata.isEmpty())
            {
                mapMetadata.put(metadataNestedShortname, listMetadata);

                if (!listMetadataParentOrgunit.isEmpty())
                {
                    mapMetadata.put(metadataNestedShortname + ".parentorgunit",
                            listMetadataParentOrgunit);
                }
                if (!listMetadataParentOrgunitCountry.isEmpty())
                {
                    mapMetadata.put(
                            metadataNestedShortname + ".iso-country",
                            listMetadataParentOrgunitCountry);
                }
                if (!listMetadataParentOrgunitCity.isEmpty())
                {
                    mapMetadata.put(metadataNestedShortname + ".city",
                            listMetadataParentOrgunitCity);
                }
                if (!listMetadataParentOrgunitRegion.isEmpty())
                {
                    mapMetadata.put(metadataNestedShortname + ".region",
                            listMetadataParentOrgunitRegion);
                }
            }

        }
        listMapMetadata.add(mapMetadata);
    }
}