/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.submit.lookup;

import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.DataLoadingSpec;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.exceptions.MalformedSourceException;

import java.util.List;

import org.dspace.submit.util.ItemSubmissionLookupDTO;


/**
 * @author Panagiotis Koutsourakis
 */
public class SubmissionItemDataLoader implements DataLoader {
    private List<ItemSubmissionLookupDTO> dtoList;
    List<SubmissionLookupProvider> providers;


    public SubmissionItemDataLoader() {
        dtoList = null;
        providers = null;
    }

    @Override
    public RecordSet getRecords() throws MalformedSourceException {
        if (dtoList == null) {
            throw new MalformedSourceException("dtoList not initialized");
        }
        RecordSet ret = new RecordSet();

        for (ItemSubmissionLookupDTO dto : dtoList) {
            Record rec = dto.getTotalPublication(providers);
            ret.addRecord(rec);
        }

        return ret;
    }

    @Override
    public RecordSet getRecords(DataLoadingSpec spec) throws MalformedSourceException {
        if(spec.getOffset() > 0) {
            return new RecordSet();
        }

        return getRecords();
    }

    /**
     * @return the dtoList
     */
    public List<ItemSubmissionLookupDTO> getDtoList() {
        return dtoList;
    }

    /**
     * @param dtoList the dtoList to set
     */
    public void setDtoList(List<ItemSubmissionLookupDTO> dtoList) {
        this.dtoList = dtoList;
    }

    /**
     * @return the providers
     */
    public List<SubmissionLookupProvider> getProviders() {
        return providers;
    }

    /**
     * @param providers the providers to set
     */
    public void setProviders(List<SubmissionLookupProvider> providers) {
        this.providers = providers;
    }
}
