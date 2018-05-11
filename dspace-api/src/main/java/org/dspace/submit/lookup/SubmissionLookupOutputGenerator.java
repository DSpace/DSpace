/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import gr.ekt.bte.core.DataOutputSpec;
import gr.ekt.bte.core.OutputGenerator;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.submit.util.ItemSubmissionLookupDTO;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class SubmissionLookupOutputGenerator implements OutputGenerator
{
    protected List<ItemSubmissionLookupDTO> dtoList;

    protected final String DOI_FIELD = "doi";

    protected final String NOT_FOUND_DOI = "NOT-FOUND-DOI";

    public SubmissionLookupOutputGenerator()
    {

    }

    @Override
    public List<String> generateOutput(RecordSet records)
    {
        dtoList = new ArrayList<ItemSubmissionLookupDTO>();

        Map<String, List<Record>> record_sets = new HashMap<String, List<Record>>();
        int counter = 0;
        for (Record rec : records)
        {
            String current_doi = NOT_FOUND_DOI;
            List<Value> values = rec.getValues(DOI_FIELD);
            if (values != null && values.size() > 0)
            {
                current_doi = values.get(0).getAsString();
            }
            else
            {
                current_doi = NOT_FOUND_DOI + "_" + counter;
            }

            if (record_sets.keySet().contains(current_doi))
            {
                record_sets.get(current_doi).add(rec);
            }
            else
            {
                ArrayList<Record> publication = new ArrayList<Record>();
                publication.add(rec);
                record_sets.put(current_doi, publication);
            }

            counter++;
        }
        for (Map.Entry<String, List<Record>> entry : record_sets.entrySet())
        {
            ItemSubmissionLookupDTO dto = new ItemSubmissionLookupDTO(
                    entry.getValue());
            dtoList.add(dto);
        }

        return new ArrayList<String>();
    }

    @Override
    public List<String> generateOutput(RecordSet records, DataOutputSpec spec)
    {
        return generateOutput(records);
    }

    /**
     * @return the items
     */
    public List<ItemSubmissionLookupDTO> getDtoList()
    {
        return dtoList;
    }

    /**
     * @param items
     *            the items to set
     */
    public void setDtoList(List<ItemSubmissionLookupDTO> items)
    {
        this.dtoList = items;
    }
}
