/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.bean;

import java.util.Collection;

public class PieStatisticBean extends StatisticDatasBean implements
        java.io.Serializable
{
    private String name;

    private int hits;

    private String key1;

    private String key2;

    private String key3;

    private Collection<StatisticDatasBeanRow> limitedDataTable;

    private Collection<StatisticDatasBeanRow> dataTable;

    public void setPercentages()
    {
        Integer total = 0;
        for (StatisticDatasBeanRow statisticDatasBeanRow : limitedDataTable)
        {
            total += (Integer) statisticDatasBeanRow.getValue();
        }
        for (StatisticDatasBeanRow statisticDatasBeanRow : limitedDataTable)
        {
            statisticDatasBeanRow
                    .setPercentage((((Integer) statisticDatasBeanRow.getValue())
                            .doubleValue() / total) * 100);
        }
    }

    public String getKey3()
    {
        return key3;
    }

    public void setKey3(String key3)
    {
        this.key3 = key3;
    }

    public String getKey1()
    {
        return key1;
    }

    public void setKey1(String key1)
    {
        this.key1 = key1;
    }

    public String getKey2()
    {
        return key2;
    }

    public void setKey2(String key2)
    {
        this.key2 = key2;
    }

    public PieStatisticBean(String key1, String key2, String key3)
    {
        this.key1 = key1;
        this.key2 = key2;
        this.key3 = key3;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public int getHits()
    {
        return this.hits;
    }

    public void setHits(int hits)
    {
        this.hits = hits;
    }

    public Collection<StatisticDatasBeanRow> getLimitedDataTable()
    {
        return limitedDataTable;
    }

    public void setLimitedDataTable(Collection<StatisticDatasBeanRow> dataTable)
    {
        this.limitedDataTable = dataTable;
    }

    public Collection<StatisticDatasBeanRow> getDataTable()
    {
        return dataTable;
    }

    public void setDataTable(Collection dataTable)
    {
        this.dataTable = dataTable;
    }

}
