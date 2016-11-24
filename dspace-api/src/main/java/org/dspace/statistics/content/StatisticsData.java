/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content;

import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.core.Context;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;

import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;
import java.io.IOException;
import java.text.ParseException;

/**
 * Abstract "factory" for statistical queries.
 * @author kevinvandevelde at atmire.com
 * Date: 23-feb-2009
 * Time: 12:37:04
 */
public abstract class StatisticsData {

    private Dataset dataset;
    private List<DatasetGenerator> datasetgenerators;

    private List<StatisticsFilter> filters;
    protected final SolrLoggerService solrLoggerService;


    /** Construct a blank query factory. */
    protected StatisticsData() {
        datasetgenerators = new ArrayList<DatasetGenerator>(2);
        filters = new ArrayList<StatisticsFilter>();
        solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
    }

    /**
     * Wrap an existing Dataset in an unconfigured query factory.
     *
     * @param dataset
     *     statistics dataset
     */
    protected StatisticsData(Dataset dataset) {
        this.dataset = dataset;
        datasetgenerators = new ArrayList<DatasetGenerator>(2);
        filters = new ArrayList<StatisticsFilter>();
        solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
    }

    /** Augment the list of facets (generators).
     *
     * @param set
     *     generator of statistics datasets
     */
    public void addDatasetGenerator(DatasetGenerator set){
        datasetgenerators.add(set);
    }

    /** Augment the list of filters.
     *
     * @param filter
     *     statistics filter
     */
    public void addFilters(StatisticsFilter filter){
        filters.add(filter);
    }

    /** Return the current list of generators.
     *
     * @return list of dataset generators
     */
    public List<DatasetGenerator> getDatasetGenerators() {
        return datasetgenerators;
    }

    /** Return the current list of filters.
     *
     * @return list of dataset filters
     */
    public List<StatisticsFilter> getFilters() {
        return filters;
    }

    /** Return the existing query result if there is one.
     *
     * @return dataset existing query result dataset
     */
    public Dataset getDataset() {
        return dataset;
    }

    /** Jam an existing query result in.
     *
     * @param dataset
     *     statistics dataset
     */
    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    /** Run the accumulated query and return its results.
     *
     * @param context
     *     The relevant DSpace Context.
     * @return accumulated query results
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws SolrServerException
     *     Exception from the Solr server to the solrj Java client.
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws ParseException if the dataset cannot be parsed
     */
    public abstract Dataset createDataset(Context context) throws SQLException,
            SolrServerException, IOException, ParseException;

}
