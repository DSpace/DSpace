/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.app.cris.integration.RPAuthorityExtraMetadataGenerator;
import org.dspace.app.cris.integration.NameResearcherPage;
import org.dspace.app.cris.integration.RPAuthority;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DCPersonName;
import org.dspace.content.DSpaceObject;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.model.ANestedObject;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.AValue;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.value.EmbeddedLinkValue;
import it.cilea.osd.jdyna.value.LinkValue;
import it.cilea.osd.jdyna.value.TextValue;
import it.cilea.osd.jdyna.widget.WidgetLink;
import it.cilea.osd.jdyna.widget.WidgetTesto;


/**
 * This class provides some static utility methods to extract information from
 * the rp identifier quering the RPs database.
 * 
 * @author cilea
 * 
 */
public class ResearcherPageUtils
{
	
	/** Maximum query results*/
	public static final int MAX_RESULTS = 20;
	
	/** Handler dspace service */
	private static DSpace dspace = new DSpace();
		
    /** log4j logger */
    @Transient
    private static Logger log = Logger.getLogger(ResearcherPageUtils.class);
    
    /**
     * Formatter to build the rp identifier
     */
    private static DecimalFormat persIdentifierFormat = new DecimalFormat(
            "00000");

    /**
     * the applicationService to query the RP database, injected by Spring IoC
     */
    private static ApplicationService applicationService;

    /**
     * Constructor use by Spring IoC
     * 
     * @param applicationService
     *            the applicationService to query the RP database, injected by
     *            Spring IoC
     */
    public ResearcherPageUtils(ApplicationService applicationService)
    {
        ResearcherPageUtils.applicationService = applicationService;
    }

    /**
     * Build the public identifier (authority) of the supplied CRIS object
     * 
     * @param cris
     *            the cris object
     * @return the public identifier of the supplied CRIS object
     */
    public static String getPersistentIdentifier(ACrisObject cris)
    {
		if (cris.getCrisID() != null) {
			return cris.getCrisID();
		}
		return formatIdentifier(cris.getId(), cris.getAuthorityPrefix());
    }

    
    /**
     * Build the cris identifier starting from the db internal primary key
    */
    public static <T extends ACrisObject> String getPersistentIdentifier(
            Integer id, Class<T> clazz)
    {
		ACrisObject crisObject = applicationService.get(clazz, id, false);
		return getPersistentIdentifier(crisObject);
    }
    
    
    /**
     * Format the cris suffix identifier starting from the db internal primary key
     * key
    */
	private static <T extends ACrisObject> String formatIdentifier(Integer rp, String prefix)
    {
		return prefix + persIdentifierFormat.format(rp);
    }

    /**
     * Extract the db primary key from a cris identifier
     * 
     * @param authorityKey
     *            the cris identifier
     * @return the db primary key
     */
    public static Integer getRealPersistentIdentifier(String authorityKey,
            Class className)
    {
        try
        {
            String id = authorityKey.substring(2);
            ACrisObject crisObject = applicationService.getEntityByCrisId(
                    authorityKey, className);
            if (crisObject != null)
            {
                 return crisObject.getId();
            }
            else {
            	crisObject = applicationService.getEntityByUUID(authorityKey);
                if (crisObject != null)
                {
                     return crisObject.getId();
                }
            }
            return Integer.parseInt(id); 
        }
        catch (NumberFormatException e)
        {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Return a label to use with a specific form of the name of the researcher.
     * 
     * @param alternativeName
     *            the form of the name to use
     * @param rp
     *            the researcher page
     * @return the label to use
     */
    public static String getLabel(String alternativeName, ResearcherPage rp)
    {
    	IResearcherPageLabelDecorator decorator = dspace
				  .getServiceManager()
				  .getServiceByName("org.dspace.app.cris.util.IResearcherPageLabelDecorator", IResearcherPageLabelDecorator.class);
return decorator.generateDisplayValue(alternativeName, rp);    
    }

    /**
     * Return a label to use with a specific form of the name of the researcher.
     * 
     * @param alternativeName
     *            the form of the name to use
     * @param rpKey
     *            the rp identifier of the ResearcherPage
     * @return the label to use
     */
    public static String getLabel(String alternativeName, String rpkey)
    {
        if (rpkey != null)
        {
            ResearcherPage rp = applicationService.get(ResearcherPage.class,
                    getRealPersistentIdentifier(rpkey, ResearcherPage.class),
                    true);
            return getLabel(alternativeName, rp);
        }
        return alternativeName;
    }

    /**
     * Check if the supplied form is the fullname of the ResearcherPage
     * 
     * @param alternativeName
     *            the string to check
     * @param rpkey
     *            the rp identifier
     * @return true, if the form is the fullname of the ResearcherPage with the
     *         supplied rp identifier
     */
    public static boolean isFullName(String alternativeName, String rpkey)
    {
        if (alternativeName != null && rpkey != null)
        {
            ResearcherPage rp = applicationService.get(ResearcherPage.class,
                    getRealPersistentIdentifier(rpkey, ResearcherPage.class),
                    true);
            return alternativeName.equals(rp.getFullName());
        }
        return false;
    }

    /**
     * Check if the supplied form is the Chinese name of the ResearcherPage
     * 
     * @param alternativeName
     *            the string to check
     * @param rpkey
     *            the rp identifier
     * @return true, if the form is the Chinese name of the ResearcherPage with
     *         the supplied rp identifier
     */
    public static boolean isChineseName(String alternativeName, String rpkey)
    {
        if (alternativeName != null && rpkey != null)
        {
            ResearcherPage rp = applicationService.get(ResearcherPage.class,
                    getRealPersistentIdentifier(rpkey, ResearcherPage.class),
                    true);
            return alternativeName.equals(rp.getTranslatedName().getValue());
        }
        return false;
    }

    /**
     * Get the fullname of the ResearcherPage
     * 
     * @param rpkey
     *            the rp identifier
     * @return the fullname of the ResearcherPage
     */
    public static String getFullName(String rpkey)
    {
        if (rpkey != null)
        {
            ResearcherPage rp = applicationService.get(ResearcherPage.class,
                    getRealPersistentIdentifier(rpkey, ResearcherPage.class),
                    true);
            return rp.getFullName();
        }
        return null;
    }

    /**
     * Get the staff number of the ResearcherPage
     * 
     * @param rpkey
     *            the rp identifier
     * @return the staff number of the ResearcherPage
     */
    public static String getStaffNumber(String rpkey)
    {
        if (rpkey != null)
        {
            ResearcherPage rp = applicationService.get(ResearcherPage.class,
                    getRealPersistentIdentifier(rpkey, ResearcherPage.class),
                    true);
            return rp != null ? rp.getSourceID() : null;
        }
        return null;
    }

    /**
     * Get the rp identifier of the ResearcherPage with a given staffno
     * 
     * @param staffno
     *            the staffno
     * @return the rp identifier of the ResearcherPage or null
     */
    public static String getRPIdentifierByStaffno(String staffno, String sourceref)
    {
        if (staffno != null)
        {
            ResearcherPage rp = applicationService
                    .getEntityBySourceId(sourceref,  staffno, ResearcherPage.class);
            if (rp != null)
            {
                return getPersistentIdentifier(rp);
            }
        }
        return null;
    }

    /**
     * Get the ChineseName of the ResearcherPage
     * 
     * @param rpkey
     *            the rp identifier
     * @return the Chinese name of the ResearcherPage
     */
    public static String getChineseName(String rpkey)
    {
        if (rpkey != null)
        {
            ResearcherPage rp = applicationService.get(ResearcherPage.class,
                    getRealPersistentIdentifier(rpkey, ResearcherPage.class),
                    true);
            return VisibilityConstants.PUBLIC == rp.getTranslatedName()
                    .getVisibility() ? rp.getTranslatedName().getValue() : "";
        }
        return null;
    }
  
    /**
     * Get the AcademicName of the ResearcherPage
     * 
     * @param rpkey
     *            the rp identifier
     * @return the Chinese name of the ResearcherPage
     */
    public static String getAcademicName(String rpkey)
    {
        if (rpkey != null)
        {
            ResearcherPage rp = applicationService.get(ResearcherPage.class,
                    getRealPersistentIdentifier(rpkey, ResearcherPage.class),
                    true);
            return VisibilityConstants.PUBLIC == rp.getPreferredName()
                    .getVisibility() ? rp.getPreferredName().getValue() : "";
        }
        return null;
    }

    public static Integer getNestedMaxPosition(ANestedObject nested)
    {
        return applicationService.getNestedMaxPosition(nested);
    }

    public static <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> List<ACNO> getNestedObjectsByParentIDAndShortname(Class<ACNO> modelClazz, Integer parentID, String typeShortname)
    {    	
    	return applicationService.getNestedObjectsByParentIDAndShortname(parentID, typeShortname, modelClazz);
    }
    
    public static <T extends ACrisObject> T getCrisObject(
            Integer id, Class<T> clazz)
    {
        return applicationService.get(clazz, id);
    }

    public static Choices doGetMatches(String field, String query) throws SearchServiceException
	{
    	DSpace dspace = new DSpace();
    	SearchService searchService = dspace.getServiceManager().getServiceByName(
                "org.dspace.discovery.SearchService", SearchService.class);

    	ConfigurationService configurationService = dspace.getServiceManager().getServiceByName(
                "org.dspace.services.ConfigurationService",
                ConfigurationService.class);
    	
    	return doGetMatches(field, query, configurationService, searchService);
	}

	public static void applyCustomFilter(String field, DiscoverQuery discoverQuery,
			ConfigurationService _configurationService) {
		String filter = _configurationService.getPropertyAsType("cris." + RPAuthority.RP_AUTHORITY_NAME
				+ ((field != null && !field.isEmpty()) ? "." + field : "") + ".filter", _configurationService
				.getPropertyAsType("cris." + RPAuthority.RP_AUTHORITY_NAME + ".filter", String.class));
		if (filter != null) {
			discoverQuery.addFilterQueries(filter);
		}
	}
    
    private static List<Choice> choiceResults(DiscoverResult result){
    	List<Choice> choiceList = new LinkedList<Choice>();
		for (DSpaceObject dso : result.getDspaceObjects()) {
			ResearcherPage rp = (ResearcherPage) dso;
			Map<String, String> extras = buildExtra(rp);

			choiceList.add(new Choice(getPersistentIdentifier(rp), rp.getFullName(),getLabel(rp.getFullName(), rp), extras));
			if (rp.getTranslatedName() != null
					&& rp.getTranslatedName().getVisibility() == VisibilityConstants.PUBLIC
					&& rp.getTranslatedName().getValue() != null) {
				choiceList.add(new Choice(getPersistentIdentifier(rp), rp
						.getTranslatedName().getValue(),
						getLabel(rp.getTranslatedName()
								.getValue(), rp), extras));
			}
			for (RestrictedField variant : rp.getVariants()) {
				if (variant.getValue() != null
						&& variant.getVisibility() == VisibilityConstants.PUBLIC) {
					choiceList.add(new Choice(getPersistentIdentifier(rp), variant
							.getValue(), getLabel(
							variant.getValue(), rp), extras));
				}
			}
	    }
    	return choiceList;
    }
    
    
	public static Map<String, String> buildExtra(ResearcherPage rp)
    {
	    Map<String, String> extras = new HashMap<String,String>();
	    List<RPAuthorityExtraMetadataGenerator> generators = dspace.getServiceManager().getServicesByType(RPAuthorityExtraMetadataGenerator.class);
	    if(generators!=null) {
	        for(RPAuthorityExtraMetadataGenerator gg : generators) {
	            Map<String, String> extrasTmp = gg.build(rp);
	            extras.putAll(extrasTmp);
	        }
	    }
        return extras;
    }

    public static Choices doGetMatches(String field, String query, ConfigurationService _configurationService,
			SearchService _searchService) throws SearchServiceException
	{
		Choices choicesResult;
		if (query != null && query.length() > 1)
		{
		    DCPersonName tmpPersonName = new DCPersonName(
		            query.toLowerCase());
	
		    String luceneQuery = "";
		    if (StringUtils.isNotBlank(tmpPersonName.getLastName()))
		    {
		        luceneQuery += ClientUtils.escapeQueryChars(tmpPersonName
		                .getLastName().trim())
		                + (StringUtils.isNotBlank(tmpPersonName
		                        .getFirstNames()) ? "" : "*");
		    }
	
		    if (StringUtils.isNotBlank(tmpPersonName.getFirstNames()))
		    {
		        luceneQuery += (luceneQuery.length() > 0 ? " " : "")
		                + ClientUtils.escapeQueryChars(tmpPersonName
		                        .getFirstNames().trim()) + "*";
		    }
		    luceneQuery = luceneQuery.replaceAll("\\\\ ", " ");
		    DiscoverQuery discoverQuery = new DiscoverQuery();
		    applyCustomFilter(field, discoverQuery,_configurationService);
		    discoverQuery.setSortField("crisrp.fullName_sort", SORT_ORDER.asc);		    
		    discoverQuery.setDSpaceObjectFilter(CrisConstants.RP_TYPE_ID);
		    String surnameQuery = "{!lucene q.op=AND df=rpsurnames}("
    			    + luceneQuery
    			    + ") OR ("
    			    // no need for a phrase search, the default operator is now AND and we want to match surnames in any order
    			    + luceneQuery.substring(0,luceneQuery.length() - 1) + ")";
		    
		    discoverQuery.setQuery(surnameQuery);
		    discoverQuery.setMaxResults(MAX_RESULTS);
		    
		    DiscoverResult result = _searchService.search(null, discoverQuery, true);
			
			List<Choice> choiceList = choiceResults(result);
			int surnamesResult = choiceList.size();
		    
			if (surnamesResult<MAX_RESULTS){
		    	int difference = MAX_RESULTS - surnamesResult;
		    	discoverQuery.setMaxResults(difference);
		    	String crisauthoritylookup = "{!lucene q.op=AND df=crisauthoritylookup}("
		    								 + luceneQuery
		    								 + ") OR (\""
		    								 + luceneQuery.substring(0,luceneQuery.length() - 1) + "\")";
		    	
		    	discoverQuery.setQuery(crisauthoritylookup);
				String negativeFilters = "-rpsurnames:(" + luceneQuery.substring(0,luceneQuery.length() - 1) + ")";
				String negativeFiltersStar = "-rpsurnames:(" + luceneQuery + ")";
				discoverQuery.addFilterQueries(negativeFilters);
				discoverQuery.addFilterQueries(negativeFiltersStar);
		    	result = _searchService.search(null, discoverQuery, true);
		    	List<Choice> authorityLookupList = choiceResults(result);
		    	if (authorityLookupList.size()>0){
		    		choiceList.addAll(authorityLookupList);
		    	}
		    }
		    
			int foundSize = choiceList.size();
		    Choice[] results = new Choice[foundSize];
		    results = choiceList.toArray(results);
		    choicesResult = new Choices(results, 0, foundSize,
		            Choices.CF_AMBIGUOUS, false, 0);
		} else {
			choicesResult = new Choices(false);
		}
		return choicesResult;
	}
	
	public static List<String> getAllNamesForm(String crisID) {

        ResearcherPage rp = applicationService.getEntityByCrisId(crisID,
                ResearcherPage.class);

        if (rp == null) {
            log.error("researcher=" + crisID + " not found");
            return null;
        }

        return getAbbreviations(getAllVariantsName(null, rp));
	}
	
	
    public static List<NameResearcherPage> getAllVariantsName(
            Set<Integer> invalidIds, ResearcherPage researcher)
    {
        String authority = researcher.getCrisID();
        Integer id = researcher.getId();
        List<NameResearcherPage> names = new LinkedList<NameResearcherPage>();
        NameResearcherPage name = new NameResearcherPage(
                researcher.getFullName(), authority, id, invalidIds);
        names.add(name);
        RestrictedField field = researcher.getPreferredName();
        if (field != null && field.getValue() != null
                && !field.getValue().isEmpty())
        {
            NameResearcherPage name_1 = new NameResearcherPage(
                    field.getValue(), authority, id, invalidIds);
            names.add(name_1);
        }
        field = researcher.getTranslatedName();
        if (field != null && field.getValue() != null
                && !field.getValue().isEmpty())
        {
            NameResearcherPage name_2 = new NameResearcherPage(
                    field.getValue(), authority, id, invalidIds);
            names.add(name_2);
        }
        for (RestrictedField r : researcher.getVariants())
        {
            if (r != null && r.getValue() != null && !r.getValue().isEmpty())
            {
                NameResearcherPage name_3 = new NameResearcherPage(
                        r.getValue(), authority, id, invalidIds);
                names.add(name_3);
            }
        }

        return names;
    }
	   
	public static List<String> getAbbreviations(List<NameResearcherPage> names)
    {
        List<String> result = new ArrayList<String>();
        for (NameResearcherPage rpn : names)
        {

            String rawValue = rpn.getName();
            
            // oggetto dcpersona composto solo da cognome e nome
            DCPersonName dcpersona = new DCPersonName(rawValue);
            String firstname = "";
            //firstNames contiene una lista di iniziali del nome delle persone
            List<String> firstNames = new ArrayList<String>();
            String[] tmpStr;
            int tmp = dcpersona.getFirstNames().indexOf(" ");
			if (tmp == -1) {
				tmp = dcpersona.getFirstNames().indexOf(".");
			}
			
            if(tmp > -1){
				tmpStr = dcpersona.getFirstNames().split("[ \\.]");
                for(int h = 0; h < tmpStr.length; h++){
					if (StringUtils.isNotBlank(tmpStr[h])) {
                        firstname += tmpStr[h].substring(0, 1);
                        firstNames.add(firstname);
                    }
                }
				if (tmpStr.length > 1) {
					firstNames.add(tmpStr[0]);
					firstname = tmpStr[0] + " ";
					for (int h = 1; h < tmpStr.length; h++) {
						if (StringUtils.isNotBlank(tmpStr[h])) {
							firstname += tmpStr[h].substring(0, 1);
							firstNames.add(firstname);
						}
					}
				}
                for (int h = 0; h < tmpStr.length; h++) {
                	if (StringUtils.isNotBlank(tmpStr[h])) {
                        firstNames.add(tmpStr[h].substring(0, 1));
                    }
                }
            } else if (dcpersona.getFirstNames().length() > 0) {
                firstname = dcpersona.getFirstNames().substring(0, 1);
                firstNames.add(firstname);
            }
            
            String lastname = dcpersona.getLastName();
			result.add((dcpersona.getFirstNames() + " " + lastname).trim());
			result.add((lastname + " " + dcpersona.getFirstNames()).trim());
            for(String first : firstNames) {
				result.add((first + " " + lastname).trim());
				result.add((lastname + " " + first).trim());               
            }
            
        }
        return result;
    }

	public static Date getDateValue(ACrisObject ro, String key) {
		List<? extends Property> dpList = (List<? extends Property>) ro.getAnagrafica4view().get(key);
		if (dpList != null && dpList.size() > 0) {
			return (Date) dpList.get(0).getObject();
		}
		return null;
	}

	public static String getStringValue(ACrisObject ro, String key) {
		List<? extends Property> dpList = (List<? extends Property>) ro.getAnagrafica4view().get(key);
		if (dpList != null && dpList.size() > 0)
		{
			return dpList.get(0).toString();
		}
		return null;
	}
	
	public static Boolean getBooleanValue(ACrisObject ro, String key) {
		List<? extends Property> dpList = (List<? extends Property>) ro.getAnagrafica4view().get(key);
		if (dpList != null && dpList.size() > 0)
		{
			return (Boolean) dpList.get(0).getObject();
		}
		return null;
	}
	
	public static <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, 
		ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void buildTextValue(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> ro, 
				String valueToSet, String pdefKey) {
		buildTextValue(ro, valueToSet, pdefKey, VisibilityConstants.PUBLIC);
	}

	public static <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void buildGenericValue(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> ro, Object valueToSet, String pdefKey, Integer visibility) {
		if (valueToSet == null) {
			return;
		}
		TP pdef = applicationService.findPropertiesDefinitionByShortName(ro.getClassPropertiesDefinition(), pdefKey);
        if (pdef == null) {
        	log.warn("Property "+pdefKey+ " not found");
        	return;
        }
        AValue avalue = pdef.getRendering().getInstanceValore();

        avalue.setOggetto(valueToSet);                 
        P prop = ro.createProprieta(pdef);
        prop.setValue(avalue);
        prop.setVisibility(visibility);
	}
	
	public static <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void buildTextValue(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> ro, String valueToSet, String pdefKey, Integer visibility) {

		TP pdef = applicationService.findPropertiesDefinitionByShortName(ro.getClassPropertiesDefinition(), pdefKey);
        if (pdef == null || !(pdef.getRendering() instanceof WidgetTesto)) {
        	log.warn("Property "+pdefKey+ " not found or not a text");
        	return;
        }
        TextValue text = new TextValue();
        text.setOggetto(valueToSet);                 
        P prop = ro.createProprieta(pdef);
        prop.setValue(text);
        prop.setVisibility(visibility);
	}

	public static <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, 
		ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> 
		void buildLinkValue(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> ro, String linkDescr, String linkURL, String pdefKey, Integer visibility) {

		TP pdef = applicationService.findPropertiesDefinitionByShortName(ro.getClassPropertiesDefinition(), pdefKey);
        if (pdef == null || !(pdef.getRendering() instanceof WidgetLink)) {
        	log.warn("Property "+pdefKey+ " not found or not a link");
        	return;
        }
        LinkValue link = new LinkValue();
        EmbeddedLinkValue linkValue = new EmbeddedLinkValue();
        linkValue.setDescriptionLink(linkDescr);
        linkValue.setValueLink(linkURL);
        link.setOggetto(linkValue);                 
        P prop = ro.createProprieta(pdef);
        prop.setValue(link);
        prop.setVisibility(visibility);
	}

	
    public static <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void cleanPropertyByPropertyDefinition(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> ro, String pdefKey)
    {
        ArrayList<P> toRemove = new ArrayList<P>();
        List<P> rppp = ro.getAnagrafica4view().get(pdefKey);
        if (rppp != null && !rppp.isEmpty())
        {
            for (P rpppp : rppp)
            {
                toRemove.add(rpppp);
            }
        }
        
        for(P remove : toRemove) {
            ro.removeProprieta(remove);
        }
    }

	public static void copyNestedObject(ACrisObject targetCrisObject, ACrisNestedObject no) {
		ACrisNestedObject copy = null;
		try {
			copy = (ACrisNestedObject) targetCrisObject.getClassNested().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
		}
		copy.setParent(targetCrisObject);
		copy.setTypo(no.getTypo());
		copy.setPositionDef(no.getPositionDef());
		copy.setPreferred(no.getPreferred());
		copy.setScopeDef(no.getScopeDef());
		copy.setSourceReference(no.getSourceReference());
		copy.setAvailabilityInfo(no.getAvailabilityInfo());
		
		for (Property p : (List<Property>) no.getAnagrafica()) {
			AValue avalue = p.getTypo().getRendering().getInstanceValore();
	        avalue.setOggetto(p.getObject());
	        Property pc = copy.createProprieta(p.getTypo());
	        pc.setValue(avalue);
	        pc.setVisibility(p.getVisibility());
		}
		applicationService.saveOrUpdate(targetCrisObject.getClassNested(), copy);
	}

	public static void cleanNestedObjectByShortname(ACrisObject targetCrisObject, String propName) {
		List<? extends ACrisNestedObject> nestedObjects = applicationService.getNestedObjectsByParentIDAndShortname(
				targetCrisObject.getId(), propName, targetCrisObject.getClassNested());
		for (ACrisNestedObject acno : nestedObjects) {
			applicationService.delete(targetCrisObject.getClassNested(), acno.getId());
		}
	}
}
