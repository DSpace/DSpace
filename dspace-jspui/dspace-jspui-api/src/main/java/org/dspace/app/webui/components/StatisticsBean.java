/**
 * $Id: $
 * $URL: $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */

package org.dspace.app.webui.components;
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
        return this.matrix;
    }
    public void setMatrix(final String[][] matrix)
    {
        this.matrix = matrix;
    }
}

