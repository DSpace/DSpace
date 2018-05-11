/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.TransformationEngine;
import gr.ekt.bte.dataloader.FileDataLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.submit.util.SubmissionLookupDTO;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class SubmissionLookupService
{
    public static final String CFG_MODULE = "submission-lookup";

    public static final String SL_NAMESPACE_PREFIX = "http://www.dspace.org/sl/";

    public static final String MANUAL_USER_INPUT = "manual";

    public static final String PROVIDER_NAME_FIELD = "provider_name_field";

    private static Logger log = Logger.getLogger(SubmissionLookupService.class);

    public static final String SEPARATOR_VALUE = "#######";

    public static final String SEPARATOR_VALUE_REGEX = SEPARATOR_VALUE;

    protected List<DataLoader> providers;

    protected Map<String, List<String>> idents2provs;

    protected List<String> searchProviders;

    protected List<String> fileProviders;

    protected TransformationEngine phase1TransformationEngine;

    protected TransformationEngine phase2TransformationEngine;
    
    protected List<String> detailFields = null;

    public void setPhase2TransformationEngine(
            TransformationEngine phase2TransformationEngine)
    {
        this.phase2TransformationEngine = phase2TransformationEngine;
    }

    public void setPhase1TransformationEngine(
            TransformationEngine phase1TransformationEngine)
    {
        this.phase1TransformationEngine = phase1TransformationEngine;

        MultipleSubmissionLookupDataLoader dataLoader = (MultipleSubmissionLookupDataLoader) phase1TransformationEngine
                .getDataLoader();

        this.idents2provs = new HashMap<String, List<String>>();
        this.searchProviders = new ArrayList<String>();
        this.fileProviders = new ArrayList<String>();

        if (providers == null)
        {
            this.providers = new ArrayList<DataLoader>();

            for (String providerName : dataLoader.getProvidersMap().keySet())
            {
                DataLoader p = dataLoader.getProvidersMap().get(providerName);

                this.providers.add(p);

                // Do not do that for file providers
                if (p instanceof FileDataLoader)
                {
                    this.fileProviders.add(providerName);
                }
                else if (p instanceof NetworkSubmissionLookupDataLoader)
                {

                    NetworkSubmissionLookupDataLoader p2 = (NetworkSubmissionLookupDataLoader) p;

                    p2.setProviderName(providerName);

                    if (p2.isSearchProvider())
                    {
                        searchProviders.add(providerName);
                    }
                    List<String> suppIdentifiers = p2.getSupportedIdentifiers();
                    if (suppIdentifiers != null)
                    {
                        for (String ident : suppIdentifiers)
                        {
                            List<String> tmp = idents2provs.get(ident);
                            if (tmp == null)
                            {
                                tmp = new ArrayList<String>();
                                idents2provs.put(ident, tmp);
                            }
                            tmp.add(providerName);
                        }
                    }
                }
            }
        }
    }

    public TransformationEngine getPhase1TransformationEngine()
    {
        return phase1TransformationEngine;
    }

    public TransformationEngine getPhase2TransformationEngine()
    {
        return phase2TransformationEngine;
    }

    public List<String> getIdentifiers()
    {

        List<String> allSupportedIdentifiers = new ArrayList<String>();
        MultipleSubmissionLookupDataLoader dataLoader = (MultipleSubmissionLookupDataLoader) phase1TransformationEngine
                .getDataLoader();
        for (String providerName : dataLoader.getProvidersMap().keySet())
        {
            DataLoader provider = dataLoader.getProvidersMap()
                    .get(providerName);
            if (provider instanceof SubmissionLookupDataLoader)
            {
                for (String identifier : ((SubmissionLookupDataLoader) provider)
                        .getSupportedIdentifiers())
                {
                    if (!allSupportedIdentifiers.contains(identifier))
                    {
                        allSupportedIdentifiers.add(identifier);
                    }
                }
            }
        }

        return allSupportedIdentifiers;
    }

    public Map<String, List<String>> getProvidersIdentifiersMap()
    {
        return idents2provs;
    }

    public SubmissionLookupDTO getSubmissionLookupDTO(
            HttpServletRequest request, String uuidSubmission)
    {
        SubmissionLookupDTO dto = (SubmissionLookupDTO) request.getSession()
                .getAttribute("submission_lookup_" + uuidSubmission);
        if (dto == null)
        {
            dto = new SubmissionLookupDTO();
            storeDTOs(request, uuidSubmission, dto);
        }
        return dto;
    }

    public void invalidateDTOs(HttpServletRequest request, String uuidSubmission)
    {
        request.getSession().removeAttribute(
                "submission_lookup_" + uuidSubmission);
    }

    public void storeDTOs(HttpServletRequest request, String uuidSubmission,
            SubmissionLookupDTO dto)
    {
        request.getSession().setAttribute(
                "submission_lookup_" + uuidSubmission, dto);
    }

    public List<String> getSearchProviders()
    {
        return searchProviders;
    }

    public List<DataLoader> getProviders()
    {
        return providers;
    }

    public static String getProviderName(Record rec)
    {
        return SubmissionLookupUtils.getFirstValue(rec,
                SubmissionLookupService.PROVIDER_NAME_FIELD);
    }

    public static String getType(Record rec)
    {
        return SubmissionLookupUtils.getFirstValue(rec,
                SubmissionLookupDataLoader.TYPE);
    }

    public List<String> getFileProviders()
    {
        return this.fileProviders;
    }

	public List<String> getDetailFields() {
		return detailFields;
	}

	public void setDetailFields(List<String> detailFields) {
		this.detailFields = detailFields;
	}
}
