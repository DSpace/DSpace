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

public class BarrChartStatisticDatasBean implements java.io.Serializable
{
    private String name;

    private int hits;

    private String[][] dataTable;

    // private String[] colLabels;
    // private String[] rowLabels;
    // private String[][] matrix;

    private String key1;

    private String key2;

    private String key3;

    private Collection<StatisticDatasBeanRow> jsData;

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

    public BarrChartStatisticDatasBean(String key1, String key2, String key3)
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

    /*
     * public String[] getColLabels() { return colLabels; }
     * 
     * public void setColLabels(String[] colLabels) { this.colLabels =
     * colLabels; }
     * 
     * public String[] getRowLabels() { return rowLabels; }
     * 
     * public void setRowLabels(String[] rowLabels) { this.rowLabels =
     * rowLabels; }
     */
    public String[][] getDataTable()
    {
        return dataTable;
    }

    public void setDataTable(String[][] dataTable)
    {
        this.dataTable = dataTable;
        this.jsData = new ArrayList<StatisticDatasBeanRow>();

        for (int i = 0; i < this.dataTable.length; i++)
        {
            if (this.dataTable[i].length == 2)
            {
                jsData.add(new StatisticDatasBeanRow(this.dataTable[i][0],
                        this.dataTable[i][1]));
            }
            else if (this.dataTable[i].length == 1)
            {
                jsData.add(new StatisticDatasBeanRow("", this.dataTable[i][0]));
            }
        }
    }

    public Collection<StatisticDatasBeanRow> getJsData()
    {
        return jsData;
    }

    public void setJsData(Collection<StatisticDatasBeanRow> jsData)
    {
        this.jsData = jsData;
    }

    /*
     * public String[][] getMatrix() { return matrix; }
     * 
     * public void setMatrix(String[][] matrix) { this.matrix = matrix; }
     */
    /*
     * public void addColLabel(String colLabel) { if (this.colLabels==null){
     * this.colLabels = new Vector<String>(); } this.colLabels.add(colLabel); }
     * 
     * public void addRowLabels(String rowLabel) { if (this.rowLabels==null){
     * this.rowLabels = new Vector<String>(); } this.rowLabels.add(rowLabel); }
     */
    /*
     * public String getColLabelComplete(int index) { String colLabel=""; if
     * (index>0 && index<colLabels.size()){ colLabel =
     * key1+"."+key2+"."+colLabels.get(index); } return colLabel; }
     * 
     * public String getRowLabelComplete(int index) { String rowLabel=""; if
     * (index>0 && index<rowLabels.size()){ rowLabel =
     * key1+"."+key2+"."+rowLabels.get(index); } return rowLabel; }
     */
}
