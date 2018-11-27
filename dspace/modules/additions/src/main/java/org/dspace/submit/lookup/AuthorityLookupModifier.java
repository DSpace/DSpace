package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.authority.Choices;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;

import gr.ekt.bte.core.AbstractModifier;
import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;

/**
 * This class enrich a BTE record with information extracted from a CRIS
 * authority. The metadataInput map contains the mapping between the information
 * potentially available in the incoming BTE record and the SOLR field to use to
 * lookup for the CRIS entity (i.e using the jissn field of the BTE make a solr
 * search on the crisjournal.journalissn field). When multiple inputs are
 * provided they will be used in the exact order and only the first match will
 * be returned
 * 
 * The mappingOutput map contains the information to add/override to/in the BTE
 * record if a CRIS entity is found. The mapptingAuthorityConfiguration contains
 * the list of BTE field to enrich with the CRIS ID as authority if the match is
 * found
 *
 */
public class AuthorityLookupModifier<T extends ACrisObject>
        extends AbstractModifier
{
    private static Logger log = Logger.getLogger(AuthorityLookupModifier.class);

    /**
     * the key is the BTE field, the value the SOLR field
     */
    private Map<String, String> metadataInputConfiguration;

    /**
     * the key is the CRIS object prop name, the value the BTE field
     */
    private Map<String, String> mappingOutputConfiguration;

	// the list of BTE field that should be linked to the CRIS object if found via
	// the authority framework. They need to appear in the enhanced fields map as
	// well
    private List<String> mappingAuthorityConfiguration;

    private Integer resourceTypeID;

    private SearchService searchService;

    private Class<T> clazzCrisObject;

    public AuthorityLookupModifier()
    {
        super("AuthorityLookupModifier");
    }

    @Override
    public Record modify(MutableRecord rec)
    {
        try
        {
        	List<T> crisObjects = new ArrayList<T>();
        	
            for (String mm : metadataInputConfiguration.keySet())
            {
            	// lookup for the cris object using the preferred field
                List<String> values = new ArrayList<String>();
                values.addAll(normalize(getValue(rec, mm)));
                int pos = 0;
                for (String value : values)
                {
					// if we have already identified the cris object using another field is not
					// necessary to make a second lookup
                	if (crisObjects.size() > pos && crisObjects.get(pos) != null) {
                		continue;
                	}
                    if (StringUtils.isNotBlank(value))
                    {
                        DiscoverQuery query = new DiscoverQuery();
                        query.setQuery("search.resourcetype:" + resourceTypeID
                                + " AND " + metadataInputConfiguration.get(mm)
                                + ":\"" + value + "\"");
                        DiscoverResult result = searchService.search(null,
                                query, true);
                        
                        T cris = null;
                        if (result.getTotalSearchResults() == 1)
                        {
                            cris = (T) result.getDspaceObjects().get(0);
                        }
                        if (crisObjects.size() > pos) {
                        	crisObjects.set(pos, cris);
                        }
                        else {
                        	crisObjects.add(cris);
                        }
                    }
                    pos++;                    
                }
            }
            
            int pos = 0;
            for (T cris : crisObjects) {
				if (cris != null) {
					// only process the additional information if a CRIS object has been found
					for (String propShortname : mappingOutputConfiguration.keySet()) {
						String bteField = mappingOutputConfiguration.get(propShortname);
						List<Value> exValues = rec.getValues(bteField);
						List<Value> newValues = new ArrayList<Value>();
						rec.removeField(bteField);
						
						// add back all the existing values as is 
						for (int iPos = 0; iPos < pos; iPos++) {
							newValues.add(exValues.size() > iPos ? exValues.get(iPos) : new StringValue(""));
						}

						if (ResearcherPageUtils.getStringValue(cris, propShortname) != null) {
							if (mappingAuthorityConfiguration.contains(bteField)) {
								newValues.add(new StringValue(ResearcherPageUtils.getStringValue(cris, propShortname)
										+ SubmissionLookupService.SEPARATOR_VALUE_REGEX + cris.getCrisID()
										+ SubmissionLookupService.SEPARATOR_VALUE_REGEX + Choices.CF_ACCEPTED));
							} else {
								newValues.add(new StringValue(ResearcherPageUtils.getStringValue(cris, propShortname)));
							}
						}
						else {
							newValues.add(new StringValue(""));
						}
						
						// add back all the existing values not yet processed
						for (int iPos = pos + 1; iPos < exValues.size(); iPos++) {
							newValues.add(exValues.get(iPos));
						}
						
						rec.addField(bteField, newValues);
					}
				}
            	pos++;
            }
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            return null;
        }
        return rec;

    }

    public List<String> normalize(List<String> values)
    {
        // overrided this method to perform any normalization
        return values;
    }

    private List<String> getValue(MutableRecord rec, String md)
    {
        List<String> result = new ArrayList<String>();
        if (StringUtils.isNotBlank(md))
        {
            List<Value> vals = rec.getValues(md);
            if (vals != null && vals.size() > 0)
            {
                for (Value val : vals)
                {
                    result.add(val.getAsString());
                }
            }
        }

        return result;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public Class<T> getClazzCrisObject()
    {
        return clazzCrisObject;
    }

    public void setClazzCrisObject(Class<T> clazzCrisObject)
    {
        this.clazzCrisObject = clazzCrisObject;
    }

    public void setMetadataInputConfiguration(
            Map<String, String> metadataInputConfiguration)
    {
        this.metadataInputConfiguration = metadataInputConfiguration;
    }

    public void setMappingOutputConfiguration(
            Map<String, String> mappingOutputConfiguration)
    {
        this.mappingOutputConfiguration = mappingOutputConfiguration;
    }

    public void setMappingAuthorityConfiguration(
            List<String> mappingAuthorityConfiguration)
    {
        this.mappingAuthorityConfiguration = mappingAuthorityConfiguration;
    }

    public void setResourceTypeID(Integer resourceTypeID)
    {
        this.resourceTypeID = resourceTypeID;
    }

}
