package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.util.Util;
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

            for (String mm : metadataInputConfiguration.keySet())
            {

                List<String> values = new ArrayList<String>();

                T rivista = null;

                values.add(Util.normalizeISSN(getValue(rec, mm)));
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
                        if (result.getTotalSearchResults() == 1)
                        {
                            rivista = (T) result.getDspaceObjects().get(0);
                        }
                    }
                }

                if (rivista != null)
                {
                    for (String shortname : mappingOutputConfiguration.keySet())
                    {
                        List<String> valuesMetadata = rivista
                                .getMetadataValue(shortname);
                        if (valuesMetadata != null && !values.isEmpty())
                        {
                            rec.removeField(mappingOutputConfiguration
                                    .get(shortname));
                            for (String value : valuesMetadata)
                            {
                                if (mappingAuthorityConfiguration
                                        .containsKey(shortname))
                                {
                                    rec.addValue(
                                            mappingAuthorityConfiguration
                                                    .get(shortname),
                                            new StringValue(
                                                    value + SubmissionLookupService.SEPARATOR_VALUE_REGEX
                                                            + rivista
                                                                    .getCrisID()
                                                            + SubmissionLookupService.SEPARATOR_VALUE_REGEX
                                                            + "600"));
                                }
                                else
                                {
                                    rec.addValue(
                                            mappingOutputConfiguration
                                                    .get(shortname),
                                            new StringValue(value));
                                }
                            }
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

    private String getValue(MutableRecord rec, String md)
    {
        if (StringUtils.isNotBlank(md))
        {
            List<Value> vals = rec.getValues(md);
            if (vals != null && vals.size() > 0)
            {
                return vals.get(0).getAsString();
            }
        }

        return null;
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
