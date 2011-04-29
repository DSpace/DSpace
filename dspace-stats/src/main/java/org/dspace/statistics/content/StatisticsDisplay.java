/**
 * $Id: StatisticsDisplay.java 4440 2009-10-10 19:03:27Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/content/StatisticsDisplay.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.content;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.filter.StatisticsFilter;

/**
 * Encapsulates all data to render the statistics
 *
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 9:27:09
 * 
 */
public abstract class StatisticsDisplay {
    private String id;
    private StatisticsData statisticsData;
    private String title;

    /** css information used to position the display object in a html page**/
    private List<String> css;



    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    protected StatisticsDisplay(StatisticsData statisticsData){
        this.statisticsData = statisticsData;
    }



    public List<DatasetGenerator> getDatasetGenerators() {
        return statisticsData.getDatasetGenerators();
    }

    public void addDatasetGenerator(DatasetGenerator set){
        statisticsData.addDatasetGenerator(set);
    }

    public void addFilter(StatisticsFilter filter){
        statisticsData.addFilters(filter);
    }

    public List<StatisticsFilter> getFilters(){
        return statisticsData.getFilters();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDataset(Dataset dataset) {
        statisticsData.setDataset(dataset);
    }

    public abstract String getType();
    
    public Dataset getDataset() {
        return statisticsData.getDataset();
    }

    public Dataset getDataset(Context context) throws SQLException, SolrServerException, IOException, ParseException {
        return statisticsData.createDataset(context);
    }

    public void addCss(String style){
        if (style != null) {
            if (css == null)
                css = new ArrayList<String>();
            css.add(style.trim());
        }
    }

    public String getCss() {
        if (css != null) {
            String result = "";
            for (String s : css) {
                if (!s.endsWith(";"))
                    s += ";";
                result += s;
            }
            return result;
        }
        else
            return "";
    }
}


