/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;

/**
 * Encapsulates all data to render the statistics
 *
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 9:27:09
 */
public abstract class StatisticsDisplay {
    private String id;
    private StatisticsData statisticsData;
    private String title;
    protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
    protected final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();
    /**
     * css information used to position the display object in a html page
     **/
    private List<String> css;

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    protected StatisticsDisplay(StatisticsData statisticsData) {
        this.statisticsData = statisticsData;
    }


    public List<DatasetGenerator> getDatasetGenerators() {
        return statisticsData.getDatasetGenerators();
    }

    public void addDatasetGenerator(DatasetGenerator set) {
        statisticsData.addDatasetGenerator(set);
    }

    public void addFilter(StatisticsFilter filter) {
        statisticsData.addFilters(filter);
    }

    public List<StatisticsFilter> getFilters() {
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

    public Dataset getDataset(Context context, int facetMinCount) throws SQLException, SolrServerException, IOException,
            ParseException {
        return statisticsData.createDataset(context, facetMinCount);
    }


    public void addCss(String style) {
        if (style != null) {
            if (css == null) {
                css = new ArrayList<String>();
            }
            css.add(style.trim());
        }
    }

    public String getCss() {
        if (css != null) {
            StringBuilder result = new StringBuilder();
            for (String s : css) {
                result.append(s);
                if (!s.endsWith(";")) {
                    result.append(";");
                }
            }
            return result.toString();
        }

        return "";
    }
}


