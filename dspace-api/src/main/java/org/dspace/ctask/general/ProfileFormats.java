/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;

/**
 * ProfileFormats is a task that creates a distribution table of Bitstream 
 * formats for it's passed object. Primarily a curation task demonstrator.
 *
 * @author richardrodgers
 */
@Distributive
public class ProfileFormats extends AbstractCurationTask
{
    // map of formats to occurrences
    private Map<String, Integer> fmtTable = new HashMap<String, Integer>();

    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException
    {
        fmtTable.clear();
        distribute(dso);
        formatResults();
        return Curator.CURATE_SUCCESS;
    }
    
    @Override
    protected void performItem(Item item) throws SQLException, IOException
    {
        for (Bundle bundle : item.getBundles())
        {
            for (Bitstream bs : bundle.getBitstreams())
            {
                String fmt = bs.getFormat().getShortDescription();
                Integer count = fmtTable.get(fmt);
                if (count == null)
                {
                    count = 1;
                }
                else
                {
                    count += 1;
                }
                fmtTable.put(fmt, count);
            }           
        }
    }
    
    private void formatResults() throws IOException
    {
        try
        {
            Context c = new Context();
            StringBuilder sb = new StringBuilder();
            for (String fmt : fmtTable.keySet())
            {
                BitstreamFormat bsf = BitstreamFormat.findByShortDescription(c, fmt);
                sb.append(String.format("%6d", fmtTable.get(fmt))).append(" (").
                append(bsf.getSupportLevelText().charAt(0)).append(") ").
                append(bsf.getDescription()).append("\n");
            }
            report(sb.toString());
            setResult(sb.toString());
            c.complete();
        }
        catch (SQLException sqlE)
        {
            throw new IOException(sqlE.getMessage(), sqlE);
        }
    }
}
