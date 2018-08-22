package org.dspace.app.cris.integration.orcid;

import java.io.File;
import java.io.IOException;
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
import org.dspace.authority.orcid.jaxb.activities.WorkGroup;
import org.dspace.authority.orcid.jaxb.activities.Works;
import org.dspace.authority.orcid.jaxb.common.CreditName;
import org.dspace.authority.orcid.jaxb.common.ExternalId;
import org.dspace.authority.orcid.jaxb.common.ExternalIds;
import org.dspace.authority.orcid.jaxb.common.FuzzyDate;
import org.dspace.authority.orcid.jaxb.common.LanguageCode;
import org.dspace.authority.orcid.jaxb.common.OrcidId;
import org.dspace.authority.orcid.jaxb.common.SourceType;
import org.dspace.authority.orcid.jaxb.common.Url;
import org.dspace.authority.orcid.jaxb.personaldetails.NameCtype;
import org.dspace.authority.orcid.jaxb.personaldetails.NameCtype.GivenNames;
import org.dspace.authority.orcid.jaxb.personaldetails.PersonalDetails;
import org.dspace.authority.orcid.jaxb.work.Citation;
import org.dspace.authority.orcid.jaxb.work.CitationType;
import org.dspace.authority.orcid.jaxb.work.Contributor;
import org.dspace.authority.orcid.jaxb.work.Work;
import org.dspace.authority.orcid.jaxb.work.WorkContributors;
import org.dspace.authority.orcid.jaxb.work.WorkSummary;
import org.dspace.authority.orcid.jaxb.work.WorkTitle;
import org.dspace.authority.orcid.jaxb.work.WorkType;
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
import gr.ekt.bte.core.Value;
import gr.ekt.bte.dataloader.FileDataLoader;

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
                    PersonalDetails profile = orcidService
                            .getPersonalDetails(orcid, null);
                    if (profile != null)
                    {
                        Works orcidWorks = orcidService.getWorks(orcid, null);
                        workgroup: for (WorkGroup orcidGroup : orcidWorks.getGroup())
                        {
                            int higher = orcidService.higherDisplayIndex(orcidGroup);
                            worksummary : for (WorkSummary orcidSummary : orcidGroup
                                    .getWorkSummary())
                            {
                                if (StringUtils.isNotBlank(orcidSummary.getDisplayIndex()))
                                {
                                    int current = Integer.parseInt(orcidSummary.getDisplayIndex());
                                    if (current < higher)
                                    {
                                        continue worksummary;
                                    }
                                }
                                SourceType source = orcidSummary.getSource();
                                String sourceNameWork = "";
                                if (source != null)
                                {
                                    sourceNameWork = source.getSourceName()
                                            .getContent();
                                }
                                if (StringUtils.isBlank(sourceNameWork)
                                        || !StringUtils.equals(sourceNameWork,
                                                sourceName))
                                {
                                    try
                                    {
                                        results.add(convertOrcidWorkToRecord(
                                                profile, orcid,
                                                orcidService.getWork(orcid,
                                                        null,
                                                        orcidSummary
                                                                .getPutCode()
                                                                .toString())));
                                    }
                                    catch (Exception e)
                                    {
                                        throw new IOException(e);
                                    }
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
            String orcid, Work orcidWork) throws Exception
    {
        MutableRecord record = new SubmissionLookupPublication("");

        Url url = orcidWork.getUrl();
        if (url != null)
        {
            record.addValue("url", new StringValue(url.getValue()));
        }
        WorkTitle workTitle = orcidWork.getTitle();
        if (workTitle != null)
        {
            record.addValue("title", new StringValue(workTitle.getTitle()));
        }

        WorkType workType = orcidWork.getType();
        if (workType != null && StringUtils.isNotBlank(workType.value()))
        {
            record.addValue("providerType", new StringValue(workType.value()));
        }

        ExternalIds identifiers = orcidWork.getExternalIds();
        if (identifiers != null)
        {
            for (ExternalId identifier : identifiers.getExternalId())
            {
                String extType = identifier.getExternalIdType();
                String extIdentifier = identifier.getExternalIdValue();
                record.addValue(extType, new StringValue(extIdentifier));
            }
        }

        String journalTitle = orcidWork.getJournalTitle();
        if (StringUtils.isNotBlank(journalTitle))
        {
            record.addValue("sourceTitle", new StringValue(journalTitle));
        }

        String abs = orcidWork.getShortDescription();
        if (StringUtils.isNotBlank(abs))
        {
            record.addValue("abstract", new StringValue(abs));
        }

        FuzzyDate issued = orcidWork.getPublicationDate();
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

        WorkContributors contributors = orcidWork.getContributors();
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
                    authOrcid.add(new StringValue(orcidid.getUriPath()));
                }
                else
                {
                    authOrcid.add(new StringValue(PLACEHOLER_NO_DATA));
                }
            }

        }
        if (authNames.isEmpty())
        {
            NameCtype name = personalDetails.getName();
            if (name != null)
            {
            	String value = "Undefined";
                CreditName cname = name.getCreditName();
                if (cname != null)
                {
                    value = cname.getValue();
					
                }
                else
                {
                    try {
						GivenNames givenNames = name.getGivenNames();
						value = name.getFamilyName().getValue()
								+ (givenNames != null ? ", " + givenNames.getValue() : "");
					} catch (NullPointerException e) {
						// the family name is missing! left it as Undefined
					}
                }
                authNames.add(new StringValue(value));
            }
            authOrcid.add(new StringValue(orcid));
        }
        record.addField("authors", authNames);
        record.addField("orcid", authOrcid);

        Citation citation = orcidWork.getCitation();
        if (citation != null)
        {
            CitationType citationType = citation.getCitationType();
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
                        Files.write(citation.getCitationValue(), file,
                                Charsets.UTF_8);
                        DataLoader dataLoader = dls.getDataLoaders()
                                .get(dataLoaderType);
                        if (dataLoader instanceof FileDataLoader)
                        {
                            FileDataLoader fdl = (FileDataLoader) dataLoader;
                            fdl.setFilename(file.getAbsolutePath());
                            try{
	                            RecordSet citationRecord = dataLoader.getRecords();
	                            for (Record rr : citationRecord.getRecords())
	                            {
	                                compare(rr, record);
	                            }
                            }catch(Exception e){
                            	
                            }catch(org.jbibtex.TokenMgrError t){
                            	
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
        for (String field : rr.getFields())
        {
            if (!record.hasField(field))
            {
                record.addField(field, rr.getValues(field));
            }
        }
    }

}
