/**
 * $Id: StatisticsData.java 4405 2009-10-07 08:35:32Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/content/StatisticsData.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.content;

import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.core.Context;
import org.apache.solr.client.solrj.SolrServerException;

import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;
import java.io.IOException;
import java.text.ParseException;

/**
 * 
 * @author kevinvandevelde at atmire.com
 * Date: 23-feb-2009
 * Time: 12:37:04
 * 
 */
public abstract class StatisticsData {

    private Dataset dataset;
    private List<DatasetGenerator> datasetgenerators;

    private List<StatisticsFilter> filters;

    protected StatisticsData() {
        datasetgenerators = new ArrayList<DatasetGenerator>(2);
        filters = new ArrayList<StatisticsFilter>();
    }

    protected StatisticsData(Dataset dataset) {
        this.dataset = dataset;
        datasetgenerators = new ArrayList<DatasetGenerator>(2);
        filters = new ArrayList<StatisticsFilter>();
    }

    public void addDatasetGenerator(DatasetGenerator set){
        datasetgenerators.add(set);
    }

    public void addFilters(StatisticsFilter filter){
        filters.add(filter);
    }


    public List<DatasetGenerator> getDatasetGenerators() {
        return datasetgenerators;
    }

    public List<StatisticsFilter> getFilters() {
        return filters;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }


    public abstract Dataset createDataset(Context context) throws SQLException, SolrServerException, IOException, ParseException;

}
