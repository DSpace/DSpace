/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;
import org.apache.commons.lang.ArrayUtils;

import java.util.List;
/**
 *
 * @author Kim Shepherd
 */
public class StatisticsBean implements java.io.Serializable
{

    private String name;
    private int hits;
    private String[][] matrix;
    private List<String> colLabels;
    private List<String> rowLabels;
    
    
    public StatisticsBean()
    {
        
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
    public void setHits(final int hits)
    {
        this.hits = hits;
    }
    public List<String> getColLabels()
    {
        return this.colLabels;
    }
    public void setColLabels(final List<String> colLabels)
    {
        this.colLabels = colLabels;
    }
    public List<String> getRowLabels()
    {
        return this.rowLabels;
    }
    public void setRowLabels(final List<String> rowLabels)
    {
        this.rowLabels = rowLabels;
    }
    public String[][] getMatrix()
    {
        return (String[][]) ArrayUtils.clone(this.matrix);
    }
    public void setMatrix(final String[][] matrix)
    {
        this.matrix = (String[][]) ArrayUtils.clone(matrix);
    }
}

