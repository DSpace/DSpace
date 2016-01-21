/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.util;

import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.Value;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.dspace.submit.lookup.SubmissionLookupService;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class ItemSubmissionLookupDTO implements Serializable
{
    private static final long serialVersionUID = 1;

    private static final String MERGED_PUBLICATION_PROVIDER = "merged";

    private static final String UNKNOWN_PROVIDER_STRING = "UNKNOWN-PROVIDER";

    private List<Record> publications;

    private String uuid;

    public ItemSubmissionLookupDTO(List<Record> publications)
    {
        this.uuid = UUID.randomUUID().toString();
        this.publications = publications;
    }

    public List<Record> getPublications()
    {
        return publications;
    }

    public Set<String> getProviders()
    {
        Set<String> orderedProviders = new LinkedHashSet<String>();
        for (Record p : publications)
        {
            orderedProviders.add(SubmissionLookupService.getProviderName(p));
        }
        return orderedProviders;
    }

    public String getUUID()
    {
        return uuid;
    }

    public Record getTotalPublication(List<DataLoader> providers)
    {
        if (publications == null)
        {
            return null;
        }
        else if (publications.size() == 1)
        {
            return publications.get(0);
        }
        else
        {
            MutableRecord pub = new SubmissionLookupPublication(
                    MERGED_PUBLICATION_PROVIDER);
            // for (SubmissionLookupProvider prov : providers)
            // {
            for (Record p : publications)
            {
                // if
                // (!SubmissionLookupService.getProviderName(p).equals(prov.getShortName()))
                // {
                // continue;
                // }
                for (String field : p.getFields())
                {
                    List<Value> values = p.getValues(field);
                    if (values != null && values.size() > 0)
                    {
                        if (!pub.getFields().contains(field))
                        {
                            for (Value v : values)
                            {
                                pub.addValue(field, v);
                            }
                        }
                    }
                }
            }
            // }
            return pub;
        }
    }
}
