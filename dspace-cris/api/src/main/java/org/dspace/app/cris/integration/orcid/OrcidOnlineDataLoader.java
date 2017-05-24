package org.dspace.app.cris.integration.orcid;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.dspace.app.itemimport.BTEBatchImportService;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.authority.orcid.jaxb.Citation;
import org.dspace.authority.orcid.jaxb.CitationType;
import org.dspace.authority.orcid.jaxb.Contributor;
import org.dspace.authority.orcid.jaxb.CreditName;
import org.dspace.authority.orcid.jaxb.JournalTitle;
import org.dspace.authority.orcid.jaxb.LanguageCode;
import org.dspace.authority.orcid.jaxb.OrcidId;
import org.dspace.authority.orcid.jaxb.OrcidProfile;
import org.dspace.authority.orcid.jaxb.OrcidWork;
import org.dspace.authority.orcid.jaxb.OrcidWorks;
import org.dspace.authority.orcid.jaxb.PersonalDetails;
import org.dspace.authority.orcid.jaxb.PublicationDate;
import org.dspace.authority.orcid.jaxb.Url;
import org.dspace.authority.orcid.jaxb.WorkContributors;
import org.dspace.authority.orcid.jaxb.WorkExternalIdentifier;
import org.dspace.authority.orcid.jaxb.WorkExternalIdentifiers;
import org.dspace.authority.orcid.jaxb.WorkTitle;
import org.dspace.core.Context;
import org.dspace.submit.lookup.NetworkSubmissionLookupDataLoader;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.dspace.utils.DSpace;

import com.google.api.client.util.Charsets;
import com.google.common.io.Files;

import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.TransformationEngine;
import gr.ekt.bte.core.TransformationResult;
import gr.ekt.bte.core.TransformationSpec;
import gr.ekt.bte.core.Value;
import gr.ekt.bte.dataloader.FileDataLoader;
import gr.ekt.bteio.generators.DSpaceOutputGenerator;

public class OrcidOnlineDataLoader extends NetworkSubmissionLookupDataLoader
{

    public final static String PLACEHOLER_NO_DATA = "#NODATA#";

    @Override
    public List<String> getSupportedIdentifiers()
    {
        return Arrays.asList(new String[] { ORCID });
    }

    @Override
    public boolean isSearchProvider()
    {
        return false;
    }

    @Override
    public List<Record> search(Context context, String title, String author,
            int year) throws HttpException, IOException
    {
        // TODO not supported yet
        return null;
    }

    @Override
    public List<Record> getByIdentifier(Context context,
            Map<String, Set<String>> keys) throws HttpException, IOException
    {
        Set<String> orcids = keys != null ? keys.get(ORCID) : null;
        List<Record> results = new ArrayList<Record>();

        OrcidService orcidService = OrcidService.getOrcid();
        String sourceName = orcidService.getSourceClientName();

        if (orcids != null)
        {
            for (String orcid : orcids)
            {

                try
                {
                    OrcidProfile profile = orcidService.getProfile(orcid);
                    if (profile != null)
                    {
                        OrcidWorks orcidWorks = profile.getOrcidActivities()
                                .getOrcidWorks();
                        for (OrcidWork orcidWork : orcidWorks.getOrcidWork())
                        {
                            if (!StringUtils.equals(orcidWork.getSource()
                                    .getSourceName().getContent(), sourceName))
                            {
                                PersonalDetails personalDetails = profile
                                        .getOrcidBio().getPersonalDetails();
                                try
                                {
                                    results.add(convertOrcidWorkToRecord(
                                            personalDetails, orcid, orcidWork));
                                }
                                catch (Exception e)
                                {
                                    throw new IOException(e);
                                }
                            }
                        }
                    }
                }
                catch (NotFoundException ex)
                {
                    return results;
                }
            }
        }
        return results;
    }

    private Record convertOrcidWorkToRecord(PersonalDetails personalDetails,
            String orcid, OrcidWork orcidWork) throws Exception
    {
        MutableRecord record = new SubmissionLookupPublication("");

        Url url = orcidWork.getUrl();
        if (url != null)
        {
            record.addValue("url", new StringValue(url.getValue()));
        }
        WorkTitle workTitle = orcidWork.getWorkTitle();
        if (workTitle != null)
        {
            record.addValue("title", new StringValue(workTitle.getTitle()));
        }

        String workType = orcidWork.getWorkType();
        if (StringUtils.isNotBlank(workType))
        {
            record.addValue("providerType", new StringValue(workType));
        }

        WorkExternalIdentifiers identifiers = orcidWork
                .getWorkExternalIdentifiers();
        if (identifiers != null)
        {
            for (WorkExternalIdentifier identifier : identifiers
                    .getWorkExternalIdentifier())
            {
                String extType = identifier.getWorkExternalIdentifierType();
                String extIdentifier = identifier.getWorkExternalIdentifierId();
                record.addValue(extType, new StringValue(extIdentifier));
            }
        }

        JournalTitle journalTitle = orcidWork.getJournalTitle();
        if (journalTitle != null)
        {
            record.addValue("sourceTitle",
                    new StringValue(journalTitle.getContent()));
        }

        String abs = orcidWork.getShortDescription();
        if (StringUtils.isNotBlank(abs))
        {
            record.addValue("abstract", new StringValue(abs));
        }

        PublicationDate issued = orcidWork.getPublicationDate();
        if (issued != null)
        {
            record.addValue("issued",
                    new StringValue(issued.getYear().getValue()));
        }

        LanguageCode language = orcidWork.getLanguageCode();
        if (language != null)
        {
            record.addValue("language", new StringValue(language.value()));
        }

        LinkedList<Value> authNames = new LinkedList<Value>();
        LinkedList<Value> authOrcid = new LinkedList<Value>();

        WorkContributors contributors = orcidWork.getWorkContributors();
        if (contributors != null)
        {
            for (Contributor contributor : contributors.getContributor())
            {
                CreditName cname = contributor.getCreditName();
                if (cname != null)
                {
                    authNames.add(new StringValue(cname.getValue()));
                }
                OrcidId orcidid = contributor.getContributorOrcid();
                if (orcidid != null)
                {
                    authOrcid.add(new StringValue(
                            orcidid.getContent().get(0).getValue()));
                }
                else
                {
                    authOrcid.add(new StringValue(PLACEHOLER_NO_DATA));
                }
            }

        }
        if(authNames.isEmpty()) {
            CreditName name = personalDetails.getCreditName();
            if (name != null)
            {
                authNames.add(new StringValue(name.getValue()));
            }
            else
            {
                authNames.add(new StringValue(personalDetails.getFamilyName()
                        + ", " + personalDetails.getGivenNames()));
            }
            authOrcid.add(new StringValue(orcid));
        }
        record.addField("authors", authNames);
        record.addField("orcid", authOrcid);

        Citation citation = orcidWork.getWorkCitation();
        if (citation != null)
        {
            CitationType citationType = citation.getWorkCitationType();
            if (citationType != null)
            {
                // Get all the possible data loaders from the Spring
                // configuration
                BTEBatchImportService dls = new DSpace()
                        .getSingletonService(BTEBatchImportService.class);
                List<String> dataLoaderTypes = dls.getFileDataLoaders();
                for (String dataLoaderType : dataLoaderTypes)
                {
                    if (dataLoaderType.equals(citationType.value()))
                    {
                        File file = File.createTempFile("tmp", ".json");
                        Files.write(citation.getCitation(), file, Charsets.UTF_8);
                        DataLoader dataLoader = dls.getDataLoaders()
                                .get(dataLoaderType);
                        if (dataLoader instanceof FileDataLoader)
                        {
                            FileDataLoader fdl = (FileDataLoader) dataLoader;
                            fdl.setFilename(file.getAbsolutePath());                            

                            RecordSet citationRecord = dataLoader.getRecords();
                            for (Record rr : citationRecord.getRecords())
                            {
                                compare(rr, record);
                            }

                        }
                        break;
                    }
                }
            }
        }
        return convertFields(record);
    }

    private void compare(Record rr, MutableRecord record)
    {
        for(String field : rr.getFields()) {
            if(!record.hasField(field)) {
                record.addField(field, rr.getValues(field));
            }
        }        
    }

}
