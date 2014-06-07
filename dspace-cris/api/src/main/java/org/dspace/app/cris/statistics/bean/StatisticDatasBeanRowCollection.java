/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.bean;

import java.util.ArrayList;
import java.util.Collection;

public class StatisticDatasBeanRowCollection
{
    // private Hashtable<StatisticDatasBeanRow,Float> percentages = new
    // Hashtable<StatisticDatasBeanRow, Float>();
    private Collection<StatisticDatasBeanRow> list = new ArrayList<StatisticDatasBeanRow>();

    public StatisticDatasBeanRowCollection()
    {

    }

    public void add(StatisticDatasBeanRow statisticDatasBeanRow)
    {

        if (!list.contains(statisticDatasBeanRow))
        {
            list.add(statisticDatasBeanRow);
        }
        updatePercentages();
    }

    private void updatePercentages()
    {
        Integer total = 0;
        for (StatisticDatasBeanRow statisticDatasBeanRow : list)
        {
            total += (Integer) statisticDatasBeanRow.getValue();
        }
        for (StatisticDatasBeanRow statisticDatasBeanRow : list)
        {
            statisticDatasBeanRow.setPercentage((Double) statisticDatasBeanRow
                    .getValue() / total);
        }
    }
}
