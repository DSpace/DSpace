/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.dto.CrisAnagraficaObjectDTO;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.ChoiceAuthorityDetails;
import org.dspace.content.authority.Choices;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.util.AnagraficaUtils;

/**
 * This class is the main point of integration beetween the Projects and DSpace.
 * It implements the contract of the Authority Control Framework.
 * 
 * @author cilea
 */
public abstract class CRISAuthority<T extends ACrisObject> implements ChoiceAuthority, ChoiceAuthorityDetails
{
    /** The logger */
    private static Logger log = Logger.getLogger(CRISAuthority.class);

    /** The RPs database service layer */
    private ApplicationService applicationService;

    /** The RPs search service layer */
    private SearchService searchService;

    private ConfigurationService configurationService;

    /**
     * Make sure that the class is fully initialized before use it
     */
    private void init()
    {
        if (applicationService == null && searchService == null)
        {
            DSpace dspace = new DSpace();
            applicationService = dspace.getServiceManager().getServiceByName(
                    "applicationService", ApplicationService.class);
            searchService = dspace.getServiceManager().getServiceByName(
                    "org.dspace.discovery.SearchService", SearchService.class);

            configurationService = dspace.getServiceManager().getServiceByName(
                    "org.dspace.services.ConfigurationService",
                    ConfigurationService.class);
        }
    }

    /**
     * Empty constructor needed by the DSpace plugin facility.
     */
    public CRISAuthority()
    {
        // nothing to do, initialization is provided by the IoC Spring Frameword
    }

    /**
     * Return a list of choices performing a lucene query on the RPs names index
     * appending the wildchar to every word in the query (i.e. if the query
     * string is Chan Tse the search will be perfomed with Chan* Tse*). For any
     * matching RP will be returned choices for every variants form.
     * 
     * {@link ChoiceAuthority#getMatches(String, int, int, int, String)}
     * 
     * @param query
     *            the lookup string
     * @param collection
     *            (not used by this Authority)
     * @param locale
     *            (not used by this Authority)
     * @param start
     *            (not used by this Authority)
     * @param limit
     *            (not used by this Authority)
     * @param locale
     *            (not used by this Authority)
     * 
     * @return a Choices of RPs where a name form match the query string
     */
    public Choices getMatches(String field, String query, int collection,
            int start, int limit, String locale)
    {
        try
        {
            init();
            if (query == null || query.length() > 2)
            {
                DiscoverQuery discoverQuery = new DiscoverQuery();
                discoverQuery.setDSpaceObjectFilter(getCRISTargetTypeID());
                String filter = configurationService.getProperty("cris."
                        + getPluginName() + ((field!=null && !field.isEmpty())?"." + field:"") + ".filter");
                if (filter != null)
                {
                    discoverQuery.addFilterQueries(filter);
                }
                else {
                    if(field.contains("_authority_") && getPluginName().equals(DOAuthority.class.getSimpleName())) {
                        filter = configurationService.getProperty("cris."
                                + getPluginName()
                                + ".dc_authority_default.filter");
                        if(filter==null) {
                            throw new RuntimeException("You have to define a default dc_authority_default filter");
                        }
                        discoverQuery.addFilterQueries(MessageFormat.format(filter,  field.substring(field.lastIndexOf("_") + 1)));
                    }
                }
                if (query == null) {
                	discoverQuery.setQuery("*:*");
                	discoverQuery.setMaxResults(500);
                }
                else {
                	String luceneQuery = ClientUtils.escapeQueryChars(query.toLowerCase()) + "*";
	                luceneQuery = luceneQuery.replaceAll("\\\\ "," ");
	                discoverQuery
	                        .setQuery("{!lucene q.op=AND df=crisauthoritylookup}("
	                                + luceneQuery
	                                + ") OR (\""
	                                + luceneQuery.substring(0,
	                                        luceneQuery.length() - 1) + "\")");
	                discoverQuery.setMaxResults(50);
                }
                discoverQuery.setSortField("crisauthoritylookup_sort", SORT_ORDER.asc);
                
                DiscoverResult result = searchService.search(null,
                        discoverQuery, true);

                List<Choice> choiceList = new ArrayList<Choice>();

                for (DSpaceObject dso : result.getDspaceObjects())
                {
                    T cris = (T) dso;
                    choiceList.add(new Choice(ResearcherPageUtils
                            .getPersistentIdentifier(cris), cris.getName(),
                            getDisplayEntry(cris)));
                }

                Choice[] results = new Choice[choiceList.size()];
                results = choiceList.toArray(results);
                return new Choices(results, 0, results.length,
                        Choices.CF_AMBIGUOUS, false, 0);
            }
            return new Choices(false);
        }
        catch (Exception e)
        {
            log.error("Error quering the RPAuthority - " + e.getMessage(), e);
            return new Choices(true);
        }
    }

	protected String getDisplayEntry(T cris) {
		return cris.getName();
	}    
    
    public abstract int getCRISTargetTypeID();

    public String getPluginName()
    {
        return this.getClass().getSimpleName();
    }

    /**
     * This authority doesn't actual implements this method, an empty choices
     * object is always returned. This method is used by unattended submssion
     * only, interactive submission will use the
     * {@link CRISAuthority#getMatches(String, String, int, int, int, String)}.
     * 
     * The confidence value of the returned Choices will be
     * {@link Choices#CF_UNCERTAIN} if there is only an object that match with the
     * lookup string or {@link Choices#CF_AMBIGUOUS} if there are more objects.
     * 
     * {@link ChoiceAuthority#getMatches(String, String, int, int, int, String)}
     * 
     * @param field
     *            (not used by this Authority)
     * @param text
     *            the lookup string
     * @param collection
     *            (not used by this Authority)
     * @param locale
     *            (not used by this Authority)
     *            
     * @return a Choices of CrisObject that have an exact string match between a name
     *         forms and the text lookup string
     * 
     * @return an empty Choices
     */
    public Choices getBestMatch(String field, String text, int collection,
            String locale)
    {
        try
        {
            init();
            List<Choice> choiceList = new ArrayList<Choice>();
            int totalResult = 0;
            if (text != null && text.length() > 2)
            {
                text = text.replaceAll("\\.", "");
                DiscoverQuery discoverQuery = new DiscoverQuery();
                discoverQuery.setDSpaceObjectFilter(getCRISTargetTypeID());
                String filter = configurationService.getProperty("cris."
                        + getPluginName() + ((field!=null && !field.isEmpty())?"." + field:"") + ".filter");
                if (filter != null)
                {
                    discoverQuery.addFilterQueries(filter);
                }
                else {
                    if(field.contains("_authority_") && getPluginName().equals(DOAuthority.class.getSimpleName())) {
                        filter = configurationService.getProperty("cris."
                                + getPluginName()
                                + ".dc_authority_default.filter");
                        if(filter==null) {
                            throw new RuntimeException("You have to define a default dc_authority_default filter");
                        }
                        discoverQuery.addFilterQueries(MessageFormat.format(filter,  field.substring(field.lastIndexOf("_") + 1)));
                    }
                }

                discoverQuery
                        .setQuery("{!lucene q.op=AND df=crisauthoritylookup}\""
                                + ClientUtils.escapeQueryChars(text.trim())
                                + "\"");
                discoverQuery.setMaxResults(50);
                DiscoverResult result = searchService.search(null,
                        discoverQuery, true);
                totalResult = (int) result.getTotalSearchResults();
                for (DSpaceObject dso : result.getDspaceObjects())
                {
                    T cris = (T) dso;
                    choiceList.add(new Choice(ResearcherPageUtils
                            .getPersistentIdentifier(cris), cris.getName(),
                            getDisplayEntry(cris)));
                }
            }

            Choice[] results = new Choice[choiceList.size()];
            if (choiceList.size() > 0)
            {
                results = choiceList.toArray(results);

                if (totalResult == 1)
                {
                    return new Choices(results, 0, totalResult,
                            Choices.CF_UNCERTAIN, false, 0);
                }
                else
                {
                    return new Choices(results, 0, totalResult,
                            Choices.CF_AMBIGUOUS, false, 0);
                }
            }
            else
            {
                return new Choices(false);
            }
        }
        catch (Exception e)
        {
            log.error("Error quering the CRISAuthority - " + e.getMessage(), e);
            return new Choices(true);
        }
    }

    /**
     * Will return the name of the CRIS Object FullName value
     * 
     * @param key
     *            the identifier (i.e. 00024)
     * @param locale     *            
     * 
     * @return the rp fullname
     */
    public String getLabel(String field, String key, String locale)
    {
        init();
        Integer id = ResearcherPageUtils.getRealPersistentIdentifier(key, getCRISTargetClass());
        if (id == null)
        {
            log.error(LogManager.getHeader(null, "getLabel",
                    "invalid key for authority key " + key));
            return null;
        }
        T cris = applicationService.get(getCRISTargetClass(), id);
        if (cris != null)
        {

            boolean isMultilanguage = new DSpace()
                    .getConfigurationService().getPropertyAsType(
                            "discovery.authority.multilanguage." + field,
                            false);

            if (isMultilanguage)
            {
                if (StringUtils.isNotBlank(locale))
                {
                    String metadata = cris
                            .getMetadataFieldName(new Locale(locale));
                    String value = cris.getMetadata(metadata);
                    if (StringUtils.isNotBlank(value))
                    {
                        return value;
                    }
                }
            }
            return cris.getName();
        }
        return null;
    }

    public abstract Class<T> getCRISTargetClass();
  
    public abstract String getPublicPath();
    
    @Override
    public Object getDetailsInfo(String field, String key, String locale) {
    	init();
    	T cris = applicationService.getEntityByCrisId(key, getCRISTargetClass());
    	if (cris != null)
    	{
	    	CrisAnagraficaObjectDTO dto = new CrisAnagraficaObjectDTO(cris);
			AnagraficaUtils
					.fillDTO(dto, cris, applicationService.getList(cris
							.getClassPropertiesDefinition()));
			return dto;
    	}
    	return null;
    }
    
    public abstract T getNewCrisObject();
}
