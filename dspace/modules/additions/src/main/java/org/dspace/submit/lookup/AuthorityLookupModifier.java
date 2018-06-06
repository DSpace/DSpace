package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;

import gr.ekt.bte.core.AbstractModifier;
import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;

public class AuthorityLookupModifier<T extends ACrisObject>
        extends AbstractModifier
{
    private static Logger log = Logger.getLogger(AuthorityLookupModifier.class);

    private Map<String, String> metadataInputConfiguration;

    private Map<String, String> mappingOutputConfiguration;

    private Map<String, String> mappingAuthorityConfiguration;

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
            Map<String, Map<String, T>> results = new HashMap<String, Map<String, T>>();
            for (String mm : metadataInputConfiguration.keySet())
            {
                Map<String, T> internalResults = new HashMap<String, T>();
                List<String> values = new ArrayList<String>();

                values.addAll(normalize(getValue(rec, mm)));
                for (String value : values)
                {
                    if (StringUtils.isNotBlank(value))
                    {
                        DiscoverQuery query = new DiscoverQuery();
                        query.setQuery("search.resourcetype:" + resourceTypeID
                                + " AND " + metadataInputConfiguration.get(mm)
                                + ":\"" + value + "\"");
                        DiscoverResult result = searchService.search(null,
                                query, true);
                        
                        T rivista = null;
                        if (result.getTotalSearchResults() == 1)
                        {
                            rivista = (T) result.getDspaceObjects().get(0);
                        }
                        internalResults.put(value, rivista);
                    }
                    else {
                        internalResults.put(value, null);
                    }
                }
                results.put(mm, internalResults);
            }
            
            for (String shortname : mappingOutputConfiguration.keySet())
            {
                rec.removeField(mappingOutputConfiguration.get(shortname));
                
                for(Map<String, T> mapInternal : results.values()) {
                    
                    for(String key : mapInternal.keySet()) {
                        T object = mapInternal.get(key); 
                        if(object!=null) {
                            List<String> valuesMetadata = object
                                    .getMetadataValue(shortname);
                            if (valuesMetadata != null)
                            {

                                for (String valueMetadata : valuesMetadata)
                                {
                                    if (mappingAuthorityConfiguration
                                            .containsKey(shortname))
                                    {
                                        rec.addValue(
                                                mappingAuthorityConfiguration
                                                        .get(shortname),
                                                new StringValue(valueMetadata
                                                        + SubmissionLookupService.SEPARATOR_VALUE_REGEX
                                                        + object.getCrisID()
                                                        + SubmissionLookupService.SEPARATOR_VALUE_REGEX
                                                        + "600"));
                                    }
                                    else
                                    {
                                        rec.addValue(
                                                mappingOutputConfiguration
                                                        .get(shortname),
                                                new StringValue(valueMetadata));
                                    }
                                }
                            }
                        }
                        else
                        {
                            rec.addValue(mappingOutputConfiguration.get(shortname),
                                    new StringValue(key));
                        }
                    }
                    
                }

            }

            return rec;
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            return null;
        }
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
            Map<String, String> mappingAuthorityConfiguration)
    {
        this.mappingAuthorityConfiguration = mappingAuthorityConfiguration;
    }

    public void setResourceTypeID(Integer resourceTypeID)
    {
        this.resourceTypeID = resourceTypeID;
    }

}
