/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.statistics;

import static org.dspace.core.Constants.BITSTREAM;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.model.UsageReportPointDsoTotalVisitsRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.content.StatisticsDatasetDisplay;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

/**
 * This report generator provides usage data by top children
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class TopItemsGenerator extends AbstractUsageReportGenerator {
    private static final String OWNING_ITEM_FIELD = "owningItem";
    @Autowired
    private DiscoveryConfigurationService discoveryConfigurationService;
    protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
    protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    private int dsoType = Constants.ITEM;

    /**
     * Create stat usage report of the items most popular over the entire site or a
     * specific community, collection
     *
     * @param context   DSpace context
     * @param root      DSO we want the stats dataset of
     * @param startDate String to filter the start date of statistic
     * @param endDate   String to filter the end date of statistic
     * @return Usage report with top most popular items
     */
    public UsageReportRest createUsageReport(Context context, DSpaceObject root, String startDate, String endDate) {
        try {
            StatisticsDatasetDisplay statisticsDatasetDisplay = new StatisticsDatasetDisplay();
            boolean hasValidRelation = false;
            String query = "";
            //if no relation property for generator
            if (getRelation() != null) {
                DiscoveryConfiguration discoveryConfiguration  = discoveryConfigurationService
                                                                     .getDiscoveryConfigurationByName(getRelation());
                if (discoveryConfiguration == null) {
                    // not valid because not found bean with this relation configuration name
                    hasValidRelation = false;

                } else {
                    hasValidRelation = true;
                    query = statisticsDatasetDisplay.composeQueryWithInverseRelation(root,
                        discoveryConfiguration.getDefaultFilterQueries(), getDsoType());
                }
            }
            if (!hasValidRelation) {
                query += "type: " + dsoType;
                if (root != null) {
                    if (!(root instanceof Site)) {
                        query += " AND ";
                        query += "id:" + root.getID() ;
                    }
                }
            }
            String filter_query = statisticsDatasetDisplay.composeFilterQuery(startDate, endDate, hasValidRelation,
                dsoType);
            String facetField = calculateFacetField(root);
            ObjectCount[] topCounts = solrLoggerService.queryFacetField(query, filter_query, facetField,
                    getMaxResults(), false, null, 1);
            UsageReportRest usageReportRest = new UsageReportRest();
            // if no data
            if (topCounts.length == 0) {
                UsageReportPointDsoTotalVisitsRest totalVisitPoint = new UsageReportPointDsoTotalVisitsRest();
                totalVisitPoint.addValue("views", 0);
                totalVisitPoint.setType(getType(facetField));
                usageReportRest.addPoint(totalVisitPoint);
            }
            for (ObjectCount count : topCounts) {
                String legacyNote = "";
                String dsoId;
                dsoId = count.getValue();
                if (dsoId == null && root != null && !(root instanceof Site) && count.getValue() == null) {
                    dsoId = root.getID().toString();
                }

                Pair<String, String> idAndName = getIdAndName(context, dsoId, facetField);
                if (idAndName == null) {
                    continue;
                }

                UsageReportPointDsoTotalVisitsRest totalVisitPoint = new UsageReportPointDsoTotalVisitsRest();
                totalVisitPoint.setType(getType(facetField));
                totalVisitPoint.setId(idAndName.getFirst());
                totalVisitPoint.setLabel(idAndName.getSecond() + legacyNote);
                totalVisitPoint.addValue("views", (int) count.getCount());
                usageReportRest.addPoint(totalVisitPoint);

            }
            return usageReportRest;
        } catch (SQLException | SolrServerException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String calculateFacetField(DSpaceObject root) {
        return getDsoType() == BITSTREAM && root.getType() != Constants.ITEM ? OWNING_ITEM_FIELD : "id";
    }

    private Pair<String, String> getIdAndName(Context context, String dsoId, String facetField) throws SQLException {
        if (getDsoType() == Constants.ITEM || facetField.equals(OWNING_ITEM_FIELD)) {
            Item item = itemService.findByIdOrLegacyId(context, dsoId);
            if (item == null) {
                return null;
            }
            String name = "untitled";
            List<MetadataValue> vals = itemService.getMetadata(item, "dc", "title", null, Item.ANY);
            if (vals != null && 0 < vals.size()) {
                name = vals.get(0).getValue();
            }
            return Pair.of(item.getID().toString(), name);
        } else if (getDsoType() == Constants.BITSTREAM) {
            Bitstream bitstream = bitstreamService.find(context, UUIDUtils.fromString(dsoId));
            if (bitstream == null) {
                return null;
            }

            return Pair.of(bitstream.getID().toString(), bitstream.getName() != null ? bitstream.getName() : "N/A");
        }

        throw new IllegalStateException("Not supported dspace object type: " + getDsoType());
    }

    public int getDsoType() {
        return dsoType;
    }

    public void setDsoType(int dsoType) {
        this.dsoType = dsoType;
    }

    private String getType(String facetField) {
        return getDsoType() == Constants.ITEM || OWNING_ITEM_FIELD.equals(facetField) ? "item" : "bitstream";
    }

    @Override
    public String getReportType() {
        return UsageReportUtils.TOP_ITEMS_REPORT_ID;
    }


}
