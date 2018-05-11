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

import org.apache.log4j.Logger;
import org.dspace.submit.util.ItemSubmissionLookupDTO;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class SubmissionItemDataLoader implements DataLoader
{
    protected List<ItemSubmissionLookupDTO> dtoList;

    List<DataLoader> providers;

    private static Logger log = Logger
            .getLogger(SubmissionItemDataLoader.class);

    public SubmissionItemDataLoader()
    {
        dtoList = null;
        providers = null;
    }

    @Override
    public RecordSet getRecords() throws MalformedSourceException
    {
        if (dtoList == null)
        {
            throw new MalformedSourceException("dtoList not initialized");
        }
        RecordSet ret = new RecordSet();

        for (ItemSubmissionLookupDTO dto : dtoList)
        {
            Record rec = dto.getTotalPublication(providers);
            ret.addRecord(rec);
        }

        log.info("BTE DataLoader finished. Items loaded: "
                + ret.getRecords().size());

        // Printing debug message
        String totalString = "";
        for (Record record : ret.getRecords())
        {
            totalString += SubmissionLookupUtils.getPrintableString(record)
                    + "\n";
        }
        log.debug("Records loaded:\n" + totalString);

        return ret;
    }

    @Override
    public RecordSet getRecords(DataLoadingSpec spec)
            throws MalformedSourceException
    {
        if (spec.getOffset() > 0)
        {
            return new RecordSet();
        }

        return getRecords();
    }

    /**
     * @return the dtoList
     */
    public List<ItemSubmissionLookupDTO> getDtoList()
    {
        return dtoList;
    }

    /**
     * @param dtoList
     *            the dtoList to set
     */
    public void setDtoList(List<ItemSubmissionLookupDTO> dtoList)
    {
        this.dtoList = dtoList;
    }

    /**
     * @return the providers
     */
    public List<DataLoader> getProviders()
    {
        return providers;
    }

    /**
     * @param providers
     *            the providers to set
     */
    public void setProviders(List<DataLoader> providers)
    {
        this.providers = providers;
    }
}
