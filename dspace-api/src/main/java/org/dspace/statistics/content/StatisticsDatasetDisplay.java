/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.statistics.content;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.util.Util;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.dspace.statistics.util.LocationUtils;

public class StatisticsDatasetDisplay {
    protected final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    protected final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    public Map<String, String> getAttributes(String value, DSpaceObject dso, int dsoType,
                                             Context context) throws SQLException {
        HashMap<String, String> attrs = new HashMap<>();
        //Check if int
        String dsoId;
        try {
            dsoId = UUID.fromString(value).toString();
        } catch (Exception e) {
            try {
                //Legacy identifier support
                dsoId = String.valueOf(Integer.parseInt(value));
            } catch (NumberFormatException e1) {
                dsoId = null;
            }
        }
        if (dsoId == null && dso != null && value == null) {
            dsoId = dso.getID().toString();
        }

        if (dsoId != null && dsoType != -1) {
            // Store the UUID of the DSO as an attribute. Needed in particular for Bitstream download usage reports,
            // as the Bitstream itself won't be available when converting points to their REST representation
            attrs.put("id", dsoId);
            switch (dsoType) {
                case Constants.BITSTREAM:
                    Bitstream bit = bitstreamService.findByIdOrLegacyId(context, dsoId);
                    if (bit == null) {
                        break;
                    }
                    //Get our owning item
                    Item owningItem = null;
                    List<Bundle> bunds = bit.getBundles();
                    if (0 < bunds.size() && 0 < bunds.get(0).getItems().size()) {
                        owningItem = bunds.get(0).getItems().get(0);
                    }

                    // If possible reference this bitstream via a handle, however this may
                    // be null if a handle has not yet been assigned. In this case reference the
                    // item its internal id. In the last case where the bitstream is not associated
                    // with an item (such as a community logo) then reference the bitstreamID directly.
                    String identifier = null;
                    if (owningItem != null && owningItem.getHandle() != null) {
                        identifier = "handle/" + owningItem.getHandle();
                    } else if (owningItem != null) {
                        identifier = "item/" + owningItem.getID();
                    } else {
                        identifier = "id/" + bit.getID();
                    }


                    String url = configurationService.getProperty("dspace.ui.url") + "/bitstream/" + identifier + "/";

                    // If we can put the pretty name of the bitstream on the end of the URL
                    try {
                        if (bit.getName() != null) {
                            url += Util.encodeBitstreamName(bit.getName(), "UTF-8");
                        }
                    } catch (UnsupportedEncodingException uee) {
                        // Just ignore it:  we don't have to have a pretty
                        // name on the end of the URL because the sequence id will
                        // locate it. However it means that links in this file might
                        // not work....
                    }

                    url += "?sequence=" + bit.getSequenceID();

                    attrs.put("url", url);
                    break;

                case Constants.ITEM:
                    Item item = itemService.findByIdOrLegacyId(context, dsoId);
                    if (item == null || item.getHandle() == null) {
                        break;
                    }

                    attrs.put("url", handleService.resolveToURL(context, item.getHandle()));
                    break;

                case Constants.COLLECTION:
                    Collection coll = collectionService.findByIdOrLegacyId(context, dsoId);
                    if (coll == null || coll.getHandle() == null) {
                        break;
                    }

                    attrs.put("url", handleService.resolveToURL(context, coll.getHandle()));
                    break;

                case Constants.COMMUNITY:
                    Community comm = communityService.findByIdOrLegacyId(context, dsoId);
                    if (comm == null || comm.getHandle() == null) {
                        break;
                    }

                    attrs.put("url", handleService.resolveToURL(context, comm.getHandle()));
                    break;
                default:
                    break;
            }
        }
        return attrs;
    }

    public String getResultName(String queryName, String value, DSpaceObject dso,
                                int dsoType, int dsoLength, Context context) throws SQLException {
        if ("continent".equals(queryName)) {
            value = LocationUtils.getContinentName(value, context
                    .getCurrentLocale());
        } else if ("countryCode".equals(queryName)) {
            // no operations needed in case of countryCode

        } else {
            //TODO: CHANGE & THROW AWAY THIS ENTIRE METHOD
            //Check if int
            String dsoId;
            //DS 3602: Until all legacy stats records have been upgraded to using UUID,
            //duplicate reports may be presented for each DSO.  A note will be appended when reporting legacy counts.
            String legacyNote = "";
            try {
                dsoId = UUID.fromString(value).toString();
            } catch (Exception e) {
                try {
                    //Legacy identifier support
                    dsoId = String.valueOf(Integer.parseInt(value));
                    legacyNote = "(legacy)";
                } catch (NumberFormatException e1) {
                    dsoId = null;
                }
            }
            if (dsoId == null && dso != null && value == null) {
                dsoId = dso.getID().toString();
            }

            if (dsoId != null && dsoType != -1) {
                switch (dsoType) {
                    case Constants.BITSTREAM:
                        Bitstream bit = bitstreamService.findByIdOrLegacyId(context, dsoId);
                        if (bit == null) {
                            break;
                        }
                        return bit.getName() + legacyNote;
                    case Constants.ITEM:
                        Item item = itemService.findByIdOrLegacyId(context, dsoId);
                        if (item == null) {
                            break;
                        }
                        String name = "untitled";
                        List<MetadataValue> vals = itemService.getMetadata(item, "dc", "title", null, Item.ANY);
                        if (vals != null && 0 < vals.size()) {
                            name = vals.get(0).getValue();
                        }
                        if (dsoLength != -1 && name.length() > dsoLength) {
                            //Cut it off at the first space
                            int firstSpace = name.indexOf(' ', dsoLength);
                            if (firstSpace != -1) {
                                name = name.substring(0, firstSpace) + " ...";
                            }
                        }

                        return name + legacyNote;

                    case Constants.COLLECTION:
                        Collection coll = collectionService.findByIdOrLegacyId(context, dsoId);
                        if (coll == null) {
                            break;
                        }
                        name = coll.getName();

                        if (dsoLength != -1 && name.length() > dsoLength) {
                            //Cut it off at the first space
                            int firstSpace = name.indexOf(' ', dsoLength);
                            if (firstSpace != -1) {
                                name = name.substring(0, firstSpace) + " ...";
                            }
                        }
                        return name + legacyNote;

                    case Constants.COMMUNITY:
                        Community comm = communityService.findByIdOrLegacyId(context, dsoId);
                        if (comm == null) {
                            break;
                        }
                        name = comm.getName();

                        if (dsoLength != -1 && name.length() > dsoLength) {
                            //Cut it off at the first space
                            int firstSpace = name.indexOf(' ', dsoLength);
                            if (firstSpace != -1) {
                                name = name.substring(0, firstSpace) + " ...";
                            }
                        }
                        return name + legacyNote;
                    default:
                        break;
                }
            }
        }
        return value;
    }

    //create filter query for usage raport generator
    public String composeFilterQuery(String startDate, String endDate, boolean relation, int type) {
        StringBuilder filterQuery = new StringBuilder();
        //when generator has not inverse relation
        if (!relation) {
            filterQuery.append("(statistics_type:").append(SolrLoggerServiceImpl.StatisticsType.VIEW
                    .text()).append(")");
            filterQuery.append(setStartDateEndDateFilterQuery(startDate, endDate));
        } else {
            filterQuery.append("(statistics_type:").append(SolrLoggerServiceImpl.StatisticsType.VIEW
                    .text()).append(")");
            filterQuery.append(" AND type: ").append(type);
            // even generator has inverse relation dates limit are added
            filterQuery.append(setStartDateEndDateFilterQuery(startDate, endDate));
        }

        // statistics view reports must not include download (view of a bitstream)
        // of bitstream in bundle other than ORIGINAL
        if (type == Constants.BITSTREAM) {
            filterQuery.append(
                      " AND -bundleName:LICENSE AND -bundleName:THUMBNAIL AND -bundleName:SWORD AND -bundleName:TEXT");
        }

        // statistics view reports MUST not include view performed by bots (isBot=true in the SOLR Statistics document)
        boolean isBot = configurationService.getBooleanProperty("solr-statistics.query.filter.isBot", true);
        if (isBot) {
            filterQuery.append(" AND -isBot:true");
        }

        return filterQuery.toString();
    }

    /**
     * Creates the join between the statistics core and the search core used by the usage report generators: it
     * selects the statistics documents of the items (or of the bitstreams of the items) that satisfy the default
     * filter queries of a discovery configuration.
     * <p>
     * The join must be sent to Solr as a filter query (fq), not as the main query, so that it can be cached in the
     * filterCache. The main query of the report is then {@code *:*}.
     *
     * @param dSpaceObject    the object that replaces the {0} placeholder in the default queries, may be null
     * @param default_queries the default filter queries of the discovery configuration
     * @param type            the type of statistics documents to select, a BITSTREAM is joined by its owning item
     * @return the join filter query
     */
    public String composeJoinFilterQuery(DSpaceObject dSpaceObject, List<String> default_queries, int type) {
        StringBuilder query = new StringBuilder();
        if (type == Constants.BITSTREAM) {
            query.append("{!join from=search.resourceid to=owningItem fromIndex=");
        } else {
            query.append("{!join from=search.resourceid to=id fromIndex=");
        }

        query.append(configurationService.getProperty("solr.multicorePrefix"));
        query.append("search} ");
        query.append(composeJoinedItemsQuery(dSpaceObject, default_queries));

        return query.toString();
    }

    /**
     * Creates the query, to be run on the search core, that selects the items joined by
     * {@link #composeJoinFilterQuery(DSpaceObject, List, int)}. It is the part of the join query without the
     * {@code {!join ...}} prefix, so it can be run directly against the search core (e.g. to discover, with a single
     * query, which categories actually have items).
     *
     * @param dSpaceObject    the object that replaces the {0} placeholder in the default queries, may be null
     * @param default_queries the default filter queries of the discovery configuration
     * @return the query on the search core
     */
    public String composeJoinedItemsQuery(DSpaceObject dSpaceObject, List<String> default_queries) {
        StringBuilder query = new StringBuilder();

        // Build the filter queries
        StringBuilder defaultQueries = new StringBuilder();
        boolean isFirstDefaultQuery = true;
        for (String default_query : default_queries) {
            if (!isFirstDefaultQuery) {
                defaultQueries.append(" AND ");
            }
            if (dSpaceObject != null) {
                defaultQueries.append(replacePlaceholderIfPresent(default_query, dSpaceObject));
            } else {
                defaultQueries.append(default_query);
            }
            isFirstDefaultQuery = false;
        }
        query.append(defaultQueries);

        // to consider only related items
        query.append(" AND archived:true");
        if (!isWithdrawnAlreadyExcluded(defaultQueries.toString())) {
            query.append(" AND -withdrawn:true");
        }

        return query.toString();
    }

    /**
     * Tell if the given query already excludes the withdrawn items at its top level: the clause must be a prohibited
     * term of its own, in a query without sub-expressions and alternatives (an OR would make the clause optional).
     */
    private boolean isWithdrawnAlreadyExcluded(String query) {
        if (query.contains("(") || query.contains(" OR ") || query.contains("||")) {
            return false;
        }
        return Arrays.asList(query.split("\\s+")).contains("-withdrawn:true");
    }

    private String replacePlaceholderIfPresent(String defaultQuery, DSpaceObject dSpaceObject) {
        return new MessageFormat(defaultQuery).format(new String[] { dSpaceObject.getID().toString() });
    }

    // create filter in string format for start date and end date
    public String setStartDateEndDateFilterQuery(String startDate, String endDate) {
        if ((startDate != null && !startDate.equals("null")) || (endDate != null && !endDate.equals("null"))) {
            StringBuilder stringBuilder = new StringBuilder();
            if (startDate != null && !startDate.equals("null")) {
                stringBuilder.append(" AND time:[");
                stringBuilder.append(startDate);
                stringBuilder.append("T00:00:00.000Z TO ");
                if (endDate != null && !endDate.equals("null")) {
                    stringBuilder.append(endDate);
                    stringBuilder.append("T23:59:59.000Z]");
                } else {
                    stringBuilder.append(" * ]");
                }
            } else {
                //start date *
                stringBuilder.append(" AND time:[ * TO ");
                //end date
                stringBuilder.append(endDate);
                stringBuilder.append("T23:59:59.000Z]");
            }
            return stringBuilder.toString();
        }
        return "";
    }

}
