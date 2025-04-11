/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.app;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.cli.ParseException;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.util.UUIDUtils;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Script that allows to enhance items, also forcing the updating of the
 * calculated metadata with the enhancement.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * <p>
 * Extended to solr search to discover not-enhanced entities.
 * Some Query can be delivered,
 * additional some daterange filterquery on the lastModified field
 * and additional collection/entitytype queries as filterfacets.
 * @author florian.gantner@uni-bamberg.de
 */
public class ItemEnhancerByDateScript
        extends DSpaceRunnable<ItemEnhancerByDateScriptConfiguration<ItemEnhancerByDateScript>> {

    private ItemService itemService;
    private CollectionService collectionService;

    protected SolrSearchCore solrSearchCore;
    private boolean dryrun;
    private Map<UUID, List<MetadataValueDTO>> dryrunMetadata;
    private UUID collection;
    private String entitytype;

    private String query;

    private String dateupper;

    private String datelower;

    private Context context;

    private int max;

    private int limit;

    private EntityTypeService entityTypeService;

    private static final Logger log = LoggerFactory.getLogger(ItemEnhancerByDateScript.class);

    private List<ItemEnhancer> itemEnhancers;

    @Override
    public void setup() throws ParseException {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.collectionService = ContentServiceFactory.getInstance().getCollectionService();
        this.entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
        this.solrSearchCore = new DSpace().getSingletonService(SolrSearchCore.class);
        this.itemEnhancers = new DSpace().getServiceManager().getApplicationContext()
                .getBeansOfType(ItemEnhancer.class).values().stream().collect(Collectors.toList());
        if (commandLine.hasOption('c')) {
            this.collection = UUIDUtils.fromString(commandLine.getOptionValue('c'));
        }
        if (commandLine.hasOption('n')) {
            dryrun = true;
            dryrunMetadata = new HashMap<>();
        }
        if (commandLine.hasOption('e')) {
            this.entitytype = commandLine.getOptionValue('e');
        }
        if (commandLine.hasOption('q')) {
            this.query = commandLine.getOptionValue('q');
        }
        if (commandLine.hasOption('d')) {
            this.dateupper = commandLine.getOptionValue('d');
        }
        if (commandLine.hasOption('s')) {
            this.datelower = commandLine.getOptionValue('s');
        }
        if (commandLine.hasOption('m')) {
            try {
                this.max = Integer.parseInt(commandLine.getOptionValue('m'));
            } catch (Exception e) {
                //
            }
        }
        if (commandLine.hasOption('l')) {
            try {
                this.limit = Integer.parseInt(commandLine.getOptionValue('l'));
            } catch (Exception e) {
                //
            }
        }
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context();
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();
        if (commandLine.hasOption('e') && Objects.isNull(entityTypeService.findByEntityType(context, entitytype))) {
            throw new Exception("unknown entity " + entitytype);
        }
        if (commandLine.hasOption('c') && (Objects.isNull(collection) ||
                Objects.isNull(this.collectionService.find(context, collection)))) {
            throw new Exception("specified collection does not exist");
        }
        SolrPingResponse ping = solrSearchCore.getSolr().ping();
        if (ping.getStatus() > 299) {
            throw new Exception("Solr seems not to be available. Status" + ping.getStatus());
        }

        context.turnOffAuthorisationSystem();
        try {
            searchItems();
            commitOrRollback();
            context.complete();
            handler.logInfo("Enhancement completed with success");
        } catch (Exception e) {
            handler.handleException("An error occurs during enhancement. The process is aborted", e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private boolean removeMetadata(List<MetadataValueDTO> mvs, MetadataValueDTO mv) {
        for (MetadataValueDTO other : mvs) {
            boolean match = (Objects.equals(mv.getSchema(), other.getSchema()) &&
                    Objects.equals(mv.getElement(), other.getElement()) &&
                    Objects.equals(mv.getQualifier(), other.getQualifier()) &&
                    Objects.equals(mv.getValue(), other.getValue()) &&
                    Objects.equals(mv.getAuthority(), other.getAuthority()) &&
                    Objects.equals(mv.getLanguage(), other.getLanguage()));

            if (match) {
                mvs.remove(other);
                return true;
            }
        }

        return false;
    }

    private String describeMetadata(MetadataValueDTO mv) {
        return StringEscapeUtils.escapeCsv(mv.getValue());
    }

    private static class Stats {
        long totalItemsChanged = 0;
        long totalFieldsAdded = 0;
        long totalFieldsRemoved = 0;
    }

    Stats stats = new Stats();

    private void commitOrRollback() throws SQLException {
        if (dryrun) {
            for (UUID uuid : dryrunMetadata.keySet()) {
                DSpaceObject dso = itemService.find(context, uuid);
                if (dso != null) {
                    dryrunMetadata.put(uuid, dso.getMetadata().stream().map(mv -> new MetadataValueDTO(mv)).collect(Collectors.toList()));
                } else {
                    dryrunMetadata.put(uuid, Collections.emptyList());
                }
            }
            context.rollback();
            for (UUID uuid : dryrunMetadata.keySet()) {
                boolean counted = false;
                List<MetadataValueDTO> newMetadata = dryrunMetadata.get(uuid);
                DSpaceObject dso = itemService.find(context, uuid);
                List<MetadataValueDTO> oldMetadata = dso.getMetadata().stream().map(mv -> new MetadataValueDTO(mv))
                        .collect(Collectors.toList());
                for (MetadataValueDTO mv : oldMetadata) {
                    if (removeMetadata(newMetadata, mv)) {
                        continue;
                    }

                    if (!counted) {
                        stats.totalItemsChanged++;
                    }

                    counted = true;

                    log.warn(describeMetadata(mv) + " disappeared");
                    handler.logInfo(describeMetadata(mv) + " disappeared");

                    stats.totalFieldsRemoved++;
                }

                for (MetadataValueDTO mv : newMetadata) {
                    log.warn(describeMetadata(mv) + " was added");
                    handler.logInfo(describeMetadata(mv) + " was added");

                    if (!counted) {
                        stats.totalItemsChanged++;
                    }

                    counted = true;

                    stats.totalFieldsAdded++;
                }
            }
        } else {
            context.commit();
        }
    }

    private void searchItems() {
        List<Item> items = new ArrayList<>();
        try {
            SolrDocumentList results = searchItemsInSolr(this.query, this.dateupper, this.datelower);
            for (SolrDocument doc : results) {
                String resourceid = (String) doc.getFieldValue(SearchUtils.RESOURCE_ID_FIELD);
                if (Objects.nonNull(resourceid) && Objects.nonNull(UUIDUtils.fromString(resourceid))) {
                    Item item = itemService.findByIdOrLegacyId(context, resourceid);
                    if (item != null)
                        items.add(item);
                }
            }
        } catch (SQLException | SolrServerException | IOException e) {
            handler.logError(e.getMessage(), e);
            log.error(e.getMessage());
        }
        int total = items.size();
        if (total == 0) {
            handler.logInfo("No results in solr-Query");
            log.info("No results in solr-Query");
            return;
        }

        if (this.limit > 0) {
            // Split through every list
            int counter = 0;
            int start;
            int end;
            while (counter < total) {
                start = counter;
                end = counter + limit;
                if (end > total) {
                    end = total;
                }
                try {
                    items.subList(start, end).forEach(this::enhanceItem);
                    commitOrRollback();
                    counter += limit;
                } catch (SQLException e) {
                    handler.logError(e.getMessage());
                    counter += limit;
                }

            }
            handler.logInfo("enhanced " + total + " items");
            log.info("enhanced " + total + " items");

            if (dryrun) {
                handler.logInfo("stats: " + stats.totalItemsChanged + " items changed (" + stats.totalFieldsAdded + " fields added, " + stats.totalFieldsRemoved + " fields removed)");
                log.info("stats: " + stats.totalItemsChanged + " items changed (" + stats.totalFieldsAdded + " fields added, " + stats.totalFieldsRemoved + " fields removed)");
            }
        } else {
            items.forEach(this::enhanceItem);
            handler.logInfo("enhanced " + total + " items");
            log.info("enhanced " + total + " items");
        }
    }

    private SolrDocumentList searchItemsInSolr(String query, String datequeryupper, String datequerylower)
            throws SolrServerException, IOException {
        SolrQuery sQuery;
        if (Objects.nonNull(query)) {
            sQuery = new SolrQuery(query);
        } else {
            sQuery = new SolrQuery("*");
        }
        if (Objects.nonNull(datequeryupper) && Objects.nonNull(datequerylower)) {
            sQuery.addFilterQuery("lastModified:[" + datequerylower + " TO " + datequeryupper + "]");
        } else if (Objects.nonNull(datequeryupper)) {
            sQuery.addFilterQuery("lastModified:[* TO " + datequeryupper + "]");
        } else if (Objects.nonNull(datequerylower)) {
            sQuery.addFilterQuery("lastModified:[" + datequerylower + " TO *]");
        }
        if (Objects.nonNull(entitytype)) {
            sQuery.addFilterQuery("search.entitytype:" + entitytype);
        }
        sQuery.addFilterQuery(SearchUtils.RESOURCE_TYPE_FIELD + ":Item");
        if (Objects.nonNull(collection)) {
            sQuery.addFilterQuery("location.coll:" + UUIDUtils.toString(collection));
        }
        sQuery.addField(SearchUtils.RESOURCE_ID_FIELD);
        if (max > 0) {
            sQuery.setRows(this.max);
        } else {
            sQuery.setRows(Integer.MAX_VALUE);
        }
        sQuery.setSort("lastModified_dt", SolrQuery.ORDER.asc);
        handler.logInfo("Query Params:" + sQuery);
        QueryResponse qResp = solrSearchCore.getSolr().query(sQuery);
        return qResp.getResults();
    }

    private void enhanceItem(Item item) {
        if (dryrun) {
            dryrunMetadata.put(item.getID(), null);
        }
        try {
            enhance(context, item);
            uncacheItem(item);
        } catch (SQLException e) {
            // deliberately empty
        }
    }

    private void uncacheItem(Item item) throws SQLException {
        context.uncacheEntity(item);
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void enhance(Context context, Item item) {
        itemEnhancers.stream()
                .filter(itemEnhancer -> itemEnhancer.canEnhance(context, item))
                .forEach(itemEnhancer -> itemEnhancer.enhance(context, item));

        updateItem(context, item);
    }

    private void updateItem(Context context, Item item) {
        try {
            itemService.update(context, item);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }


    private void assignSpecialGroupsInContext() {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ItemEnhancerByDateScriptConfiguration<ItemEnhancerByDateScript> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("item-enhancer",
                ItemEnhancerByDateScriptConfiguration.class);
    }
}
