/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.deduplication.model.DuplicateDecisionType;
import org.dspace.app.deduplication.service.DedupService;
import org.dspace.app.deduplication.service.SearchDeduplication;
import org.dspace.app.deduplication.service.SolrDedupServiceIndexPlugin;
import org.dspace.app.deduplication.utils.Signature;
import org.dspace.app.util.Util;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.deduplication.Deduplication;
import org.dspace.deduplication.service.DeduplicationService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SolrDedupServiceImpl implements DedupService {

    private static final Logger log = Logger.getLogger(SolrDedupServiceImpl.class);

    public static final String COLUMN_ADMIN_NOTE = "note";

    public static final String COLUMN_READER_NOTE = "reader_note";

    public static final String COLUMN_ADMIN_DECISION = "admin_decision";

    public static final String COLUMN_WORKFLOW_DECISION = "workflow_decision";

    public static final String COLUMN_SUBMITTER_DECISION = "submitter_decision";

    public static final String LAST_INDEXED_FIELD = "SolrIndexer.lastIndexed";

    public static final String UNIQUE_ID_FIELD = "dedup.uniqueid";

    public static final String RESOURCE_RESOURCETYPE_FIELD = "dedup.resourcetype";

    public static final String RESOURCE_SIGNATURETYPE_FIELD = "dedup.signaturetype";

    public static final String RESOURCE_SIGNATURE_FIELD = "dedup.signature";

    public static final String RESOURCE_ID_FIELD = "dedup.id";

    /***
     * Identify the couple of UUID.
     */
    public static final String RESOURCE_IDS_FIELD = "dedup.ids";

    /**
     * Identify the deduplication status
     * 
     * @See DeduplicationFlag
     */
    public static final String RESOURCE_FLAG_FIELD = "dedup.flag";

    public static final String RESOURCE_NOTE_FIELD = "dedup.note";

    public static final String RESOURCE_WITHDRAWN_FIELD = "dedup.withdrawn";

    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    protected SolrClient solr = null;

    private DSpace dspace = new DSpace();

    public static final String SUBQUERY_NOT_IN_REJECTED = "-({!join from=" + RESOURCE_ID_FIELD + " to="
            + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":reject_admin)";

    public static final String SUBQUERY_IN_REJECTEDWS = "-({!join from=" + RESOURCE_ID_FIELD + " to="
            + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":reject*)";

    public static final String SUBQUERY_IN_REJECTEDWF = "-({!join from=" + RESOURCE_ID_FIELD + " to="
            + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":reject_admin) OR " + "-({!join from="
            + RESOURCE_ID_FIELD + " to=" + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":reject_wf)";

    public static final String SUBQUERY_NOT_IN_REJECTED_OR_VERIFY = "-({!join from=" + RESOURCE_ID_FIELD + " to="
            + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":verify*) OR " + "-({!join from=" + RESOURCE_ID_FIELD
            + " to=" + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":reject*)";

    public static final String SUBQUERY_WF_MATCH_OR_REJECTED_OR_VERIFY = "(({!join from=" + RESOURCE_ID_FIELD + " to="
            + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":verify_wf) OR " + "({!join from=" + RESOURCE_ID_FIELD
            + " to=" + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":reject_wf) OR dedup.flag:match)";
    public static final String SUBQUERY_WS_MATCH_OR_REJECTED_OR_VERIFY = "(({!join from=" + RESOURCE_ID_FIELD + " to="
            + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":verify_ws) OR " + "({!join from=" + RESOURCE_ID_FIELD
            + " to=" + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":reject_ws) OR dedup.flag:match)";

    public static final String SUBQUERY_MATCH_NOT_IN_REJECTED_OR_VERIFY = "(-({!join from=" + RESOURCE_ID_FIELD + " to="
            + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":verify*) OR " + "-({!join from=" + RESOURCE_ID_FIELD
            + " to=" + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":reject*) OR dedup.flag:match)";
    /*
     * (({!join from=dedup.id to=dedup.id}dedup.flag:verify* AND -dedup.flag:match)
     * OR ({!join from=dedup.id to=dedup.id}dedup.flag:reject* AND
     * -dedup.flag:match))
     */
    public static final String SUBQUERY_IN_REJECTED_OR_VERIFY = "(({!join from=" + RESOURCE_ID_FIELD + " to="
            + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":verify* AND -dedup.flag:match) OR " + "({!join from="
            + RESOURCE_ID_FIELD + " to=" + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD
            + ":reject* AND -dedup.flag:match))";

    public static final String SUBQUERY_NOT_IN_REJECTED_OR_VERIFYWF = "-({!join from=" + RESOURCE_ID_FIELD + " to="
            + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":verify_wf) OR " + "-({!join from=" + RESOURCE_ID_FIELD
            + " to=" + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":reject_wf) OR " + "-({!join from="
            + RESOURCE_ID_FIELD + " to=" + RESOURCE_ID_FIELD + "}" + RESOURCE_FLAG_FIELD + ":reject_admin)";

    public static final String SUBQUERY_STORED_DECISION = UNIQUE_ID_FIELD + ":{0}-*_{1}" + " AND " + "-("
            + UNIQUE_ID_FIELD + "{0}-match)";

    public static final String QUERY_REMOVE = RESOURCE_IDS_FIELD + ":{0}" + " AND " + RESOURCE_RESOURCETYPE_FIELD
            + ":{1}";

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired(required = true)
    private DeduplicationService deduplicationService;

    /***
     * Deduplication status
     * <p>
     * MATCH, there is a match between dedup.ids items. REJECTWS and REJECTWF, the
     * match was rejected by the user. REJECTADMIN, the match was rejected by the
     * administrator. VERIFYWS and VERIFYWF, the match has to be verified.
     */
    public enum DeduplicationFlag {

        FAKE("fake", 0), MATCH("match", 1), REJECTWS("reject_ws", 2), REJECTWF("reject_wf", 3),
        REJECTADMIN("reject_admin", 4), VERIFYWS("verify_ws", 5), VERIFYWF("verify_wf", 6);

        String description;

        int identifier;

        private DeduplicationFlag(String desc, int identifier) {
            this.description = desc;
            this.identifier = identifier;
        }

        public String getDescription() {
            return description;
        }

        public int getIdentifier() {
            return identifier;
        }

        public static DeduplicationFlag getEnum(String description) {
            switch (description) {
                case "reject_admin":
                    return DeduplicationFlag.REJECTADMIN;
                case "reject_ws":
                    return DeduplicationFlag.REJECTWS;
                case "reject_wf":
                    return DeduplicationFlag.REJECTWF;
                case "match":
                    return DeduplicationFlag.MATCH;
                case "verify_ws":
                    return DeduplicationFlag.VERIFYWS;
                case "verify_wf":
                    return DeduplicationFlag.VERIFYWF;
                default:
                    return DeduplicationFlag.FAKE;
            }
        }
    }

    protected SolrClient getSolr() {
        if (solr == null) {
            String solrService = DSpaceServicesFactory.getInstance().getConfigurationService()
                    .getProperty("deduplication.search.server");

            UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
            if (urlValidator.isValid(solrService)
                    || ConfigurationManager.getBooleanProperty("deduplication", "solr.url.validation.enabled", true)) {
                try {
                    log.debug("Solr URL: " + solrService);
                    solr = new HttpSolrClient.Builder(solrService).build();

                    ((HttpSolrClient) solr).setBaseURL(solrService);

                    // Dummy/test query to search for Item (type=2) of ID=1
                    SolrQuery solrQuery = new SolrQuery()
                            .setQuery(RESOURCE_RESOURCETYPE_FIELD + ":2 AND " + RESOURCE_ID_FIELD + ":1");
                    // Only return obj identifier fields in result doc
                    solrQuery.setFields(RESOURCE_RESOURCETYPE_FIELD, RESOURCE_ID_FIELD);
                    solr.query(solrQuery);

                } catch (Exception e) {
                    log.error("Error while initializing solr server", e);
                }
            } else {
                log.error("Error while initializing solr, invalid url: " + solrService);
            }
        }

        return solr;
    }

    @Override
    public void indexContent(Context ctx, Item iu, boolean force) throws SearchServiceException {

        Map<String, List<String>> tmpMapFilter = new HashMap<String, List<String>>();
        List<String> tmpFilter = new ArrayList<String>();

        fillSignature(ctx, iu, tmpMapFilter, tmpFilter);

        if (tmpFilter.isEmpty()) {
            return;
        }

        // the FAKE identifier
        String dedupID = iu.getID() + "-" + iu.getID();

        // retrieve all search plugin to build search document in the same index
        SearchDeduplication searchSignature = dspace.getServiceManager()
                .getServiceByName("item".toUpperCase() + "SearchDeduplication", SearchDeduplication.class);

        // build the dedup reject in the dedup index core
        buildFromDedupReject(ctx, iu, tmpMapFilter, tmpFilter, searchSignature);

        // clean FAKE documents related to this identifier
        removeFake(dedupID, iu.getType());

        // build the FAKE document
        build(ctx, iu.getID(), iu.getID(), DeduplicationFlag.FAKE, tmpMapFilter, searchSignature, null);

        // remove previous potential match
        removeMatch(iu.getID(), iu.getType());

        // build the new ones
        buildPotentialMatch(ctx, iu, tmpMapFilter, tmpFilter, searchSignature);

    }

    private void fillSignature(Context ctx, DSpaceObject iu, Map<String, List<String>> tmpMapFilter,
            List<String> tmpFilter) {
        // get all algorithms to build signature
        List<Signature> signAlgo = dspace.getServiceManager().getServicesByType(Signature.class);
        for (Signature algo : signAlgo) {
            if (iu.getType() == algo.getResourceTypeID()) {
                List<String> signatures = algo.getSignature(iu, ctx);
                for (String signature : signatures) {
                    if (StringUtils.isNotEmpty(signature)) {
                        String key = algo.getSignatureType() + "_signature";
                        if (tmpMapFilter.containsKey(key)) {
                            List<String> obj = tmpMapFilter.get(key);
                            obj.add(signature);
                            tmpMapFilter.put(key, obj);
                        } else {
                            List<String> obj = new ArrayList<String>();
                            obj.add(signature);
                            tmpMapFilter.put(key, obj);
                        }
                    }
                }
            }
        }

        String result = "";
        int index = 0;
        for (String tmpF : tmpMapFilter.keySet()) {
            if (index > 0) {
                result += " OR ";
            }

            result += tmpF + ":(";
            int jindex = 0;
            for (String s : tmpMapFilter.get(tmpF)) {
                if (jindex > 0) {
                    result += " OR ";
                }
                result += s;
                jindex++;
            }
            result += ")";
            index++;
        }

        if (StringUtils.isNotBlank(result)) {
            tmpFilter.add(result);
        }
    }

    private void buildPotentialMatch(Context ctx, DSpaceObject iu, Map<String, List<String>> tmpMapFilter,
            List<String> tmpFilter, SearchDeduplication searchSignature) throws SearchServiceException {
        tmpFilter.add("+" + RESOURCE_FLAG_FIELD + ":" + DeduplicationFlag.FAKE.getDescription());
        // select all fake not in reject and build the potential match
        String[] tmpArrayFilter = new String[tmpFilter.size()];
        QueryResponse response = find("*:*", tmpFilter.toArray(tmpArrayFilter));
        SolrDocumentList list = response.getResults();
        external: for (SolrDocument resultDoc : list) {

            // build the MATCH identifier
            Collection<Object> matchIds = (Collection<Object>) resultDoc.getFieldValues(RESOURCE_IDS_FIELD);
            UUID matchId = iu.getID();

            internal: for (Object matchIdObj : matchIds) {
                try {
                    matchId = UUID.fromString((String) matchIdObj);

                    if (!iu.getID().equals(matchId)) {
                        break internal;
                    }
                } catch (IllegalArgumentException ie) {
                    log.error("Match ids: " + matchId + ". Id " + matchId + " is not an UUID");
                }
            }

            // this check manage fake node
            if (matchId.equals(iu.getID())) {
                continue external;
            }

            Map<String, List<String>> tmp = new HashMap<String, List<String>>();

            for (String field : resultDoc.getFieldNames()) {
                List<String> valueResult = new ArrayList<String>();
                if (field.endsWith("_signature")) {

                    List<String> valueCurrentSignature = tmpMapFilter.get(field);
                    Collection<Object> valuesSignature = (Collection<Object>) resultDoc.getFieldValues(field);
                    if (valueCurrentSignature != null && !valueCurrentSignature.isEmpty()) {
                        for (Object valSign : valuesSignature) {
                            if (valueCurrentSignature.contains((String) valSign)) {
                                valueResult.add((String) valSign);
                            }
                        }
                    }
                }
                if (!valueResult.isEmpty()) {
                    tmp.put(field, valueResult);
                }
            }

            build(ctx, iu.getID(), matchId, DeduplicationFlag.MATCH, tmp, searchSignature, null);

        }
    }

    private void removeFake(String dedupID, Integer type) throws SearchServiceException {
        // remove all FAKE related this deduplication item
        String queryDeleteFake = RESOURCE_RESOURCETYPE_FIELD + ":" + type + " AND " + RESOURCE_FLAG_FIELD + ":"
                + DeduplicationFlag.FAKE.description + " AND " + RESOURCE_ID_FIELD + ":\"" + dedupID + "\"";
        delete(queryDeleteFake);
    }

    private void removeMatch(UUID id, Integer type) throws SearchServiceException {
        // remove all MATCH related to deduplication item
        String queryDeleteMatch = RESOURCE_RESOURCETYPE_FIELD + ":" + type + " AND " + RESOURCE_FLAG_FIELD + ":"
                + DeduplicationFlag.MATCH.description + " AND " + RESOURCE_IDS_FIELD + ":" + id;
        delete(queryDeleteMatch);
    }

    @Override
    public void removeStoredDecision(UUID firstId, UUID secondId, DuplicateDecisionType type)
            throws SearchServiceException {
        QueryResponse response = findDecisions(firstId, secondId, type);

        SolrDocumentList solrDocumentList = response.getResults();
        for (SolrDocument solrDocument : solrDocumentList) {
            String duplicateID = solrDocument.getFieldValue(UNIQUE_ID_FIELD).toString();
            String queryDelete = UNIQUE_ID_FIELD + ":" + duplicateID;
            delete(queryDelete);
        }
    }

    public void build(Context ctx, UUID firstId, UUID secondId, DeduplicationFlag flag,
            Map<String, List<String>> signatures, SearchDeduplication searchSignature, String note) {
        SolrInputDocument doc = new SolrInputDocument();

        // build upgraded document
        doc.addField(LAST_INDEXED_FIELD, new Date());

        UUID[] sortedIds = new UUID[] { firstId, secondId };
        Arrays.sort(sortedIds);

        String dedupID = sortedIds[0] + "-" + sortedIds[1];

        doc.addField(UNIQUE_ID_FIELD, dedupID + "-" + flag.getDescription());
        doc.addField(RESOURCE_ID_FIELD, dedupID);
        doc.addField(RESOURCE_IDS_FIELD, sortedIds[0].toString());
        if (!firstId.equals(secondId)) {
            doc.addField(RESOURCE_IDS_FIELD, sortedIds[1].toString());
        }
        doc.addField(RESOURCE_RESOURCETYPE_FIELD, Constants.ITEM);
        doc.addField(RESOURCE_FLAG_FIELD, flag.getDescription());

        if (signatures != null) {
            for (String key : signatures.keySet()) {
                for (String ss : signatures.get(key)) {
                    doc.addField(key, ss);
                }
                doc.addField(RESOURCE_SIGNATURETYPE_FIELD, key);
            }
        }

        if (StringUtils.isNotBlank(note)) {
            doc.addField(RESOURCE_NOTE_FIELD, note);
        }

        if (searchSignature != null) {
            for (SolrDedupServiceIndexPlugin solrServiceIndexPlugin : searchSignature.getSolrIndexPlugin()) {
                solrServiceIndexPlugin.additionalIndex(ctx, sortedIds[0], sortedIds[1], doc);
            }

        }

        // write the document to the index
        try {
            writeDocument(doc);
            log.info("Wrote " + flag.description + " duplicate: " + dedupID + " to Index");
        } catch (RuntimeException e) {
            log.error("Error while writing a " + flag.description + " to deduplication index: " + dedupID + " message:"
                    + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Error while writing a " + flag.description + " to deduplication index: " + dedupID + " message:"
                    + e.getMessage(), e);
        }
    }

    protected void writeDocument(SolrInputDocument doc) throws IOException {

        try {
            if (getSolr() != null) {
                getSolr().add(doc);

            }
        } catch (SolrServerException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void unIndexContent(Context context, Item item) {
        try {
            delete(MessageFormat.format(QUERY_REMOVE, item.getID(), item.getType()));

        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public QueryResponse search(SolrQuery solrQuery) throws SearchServiceException {
        try {
            return getSolr().query(solrQuery);
        } catch (Exception e) {
            throw new org.dspace.discovery.SearchServiceException(e.getMessage(), e);
        }
    }

    @Override
    public QueryResponse findDecisions(UUID firstItemID, UUID secondItemID, DuplicateDecisionType type)
            throws SearchServiceException {

        String append = "";
        switch (type) {
            case WORKSPACE:
                append = "ws";
                break;
            case WORKFLOW:
                append = "ws";
                break;
            case ADMIN:
                append = "admin";
                break;
            default:
                // no-action
                break;
        }
        String[] sortedIds = new String[] { firstItemID.toString(), secondItemID.toString() };
        Arrays.sort(sortedIds);

        String dedupID = sortedIds[0] + "-" + sortedIds[1];

        String findQuery = MessageFormat.format(SolrDedupServiceImpl.SUBQUERY_STORED_DECISION, dedupID, append);

        QueryResponse response = find(findQuery);

        return response;
    }

    @Override
    public QueryResponse find(String query, String... filters) throws SearchServiceException {
        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.addFilterQuery(filters);

            return getSolr().query(solrQuery);
        } catch (Exception e) {
            throw new org.dspace.discovery.SearchServiceException(e.getMessage(), e);
        }
    }

    @Override
    public UpdateResponse delete(String query) throws SearchServiceException {
        try {
            return getSolr().deleteByQuery(query);
        } catch (Exception e) {
            throw new org.dspace.discovery.SearchServiceException(e.getMessage(), e);
        }
    }

    @Override
    public void cleanIndex(boolean force) throws IOException, SQLException, SearchServiceException {
        if (force) {
            try {
                getSolr().deleteByQuery("*:*");
            } catch (Exception e) {
                throw new SearchServiceException(e.getMessage(), e);
            }
        } else {
            Context context = new Context();
            context.turnOffAuthorisationSystem();

            try {
                if (getSolr() == null) {
                    return;
                }
                SolrQuery query = new SolrQuery();
                // Query for all indexed Items, Collections and Communities,
                // returning just their handle
                query.setFields(RESOURCE_IDS_FIELD);
                query.setQuery(RESOURCE_RESOURCETYPE_FIELD + ":" + Constants.ITEM);
                QueryResponse rsp = getSolr().query(query);
                SolrDocumentList docs = rsp.getResults();

                Iterator iter = docs.iterator();
                while (iter.hasNext()) {

                    SolrDocument doc = (SolrDocument) iter.next();

                    Collection<Object> ids = doc.getFieldValues(RESOURCE_IDS_FIELD);

                    for (Object id : ids) {
                        Item i = ContentServiceFactory.getInstance().getItemService()
                                /* getDSpaceObjectService(type) */.find(context, UUID.fromString((String) id));

                        if (i == null) {
                            log.info("Deleting: " + id);
                            /*
                             * Use IndexWriter to delete, its easier to manage write.lock
                             */
                            unIndexContent(context, i);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error cleaning cris deduplication index: " + e.getMessage(), e);
            } finally {
                context.abort();
            }
        }
    }

    @Override
    public void indexContent(Context context, List<UUID> ids, boolean force) {
        try {
            startMultiThreadIndex(context, force, ids);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void updateIndex(Context context, boolean force) {
        try {
            startMultiThreadIndex(context, true, null);
            commit();
            startMultiThreadIndex(context, false, null);
            commit();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void optimize() {
        try {
            if (getSolr() == null) {
                return;
            }
            long start = System.currentTimeMillis();
            System.out.println("SOLR Search Optimize -- Process Started:" + start);
            getSolr().optimize();
            long finish = System.currentTimeMillis();
            System.out.println("SOLR Search Optimize -- Process Finished:" + finish);
            System.out.println("SOLR Search Optimize -- Total time taken:" + (finish - start) + " (ms).");
        } catch (SolrServerException sse) {
            System.err.println(sse.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    @Override
    public void unIndexContent(Context context, String handleOrUuid) throws IllegalStateException, SQLException {
        Item item = null;
        if (StringUtils.isNotEmpty(handleOrUuid)) {
            String handlePrefix = ConfigurationManager.getProperty("handle.prefix");

            item = (Item) HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, handleOrUuid);
        }
        if (item != null) {
            unIndexContent(context, item);
        }
    }

    private void startMultiThreadIndex(Context context, boolean onlyFake, List<UUID> ids) throws SQLException {
        int numThreads = ConfigurationManager.getIntProperty("deduplication", "indexer.items.threads", 5);

        if (ids == null) {
            ids = new ArrayList<>();
            Iterator<Item> items = itemService.findAllUnfiltered(context);
            for (Item item : ImmutableList.copyOf(items)) {
                ids.add(item.getID());
            }
        }
        List<UUID>[] arrayIDList = Util.splitList(ids, numThreads);
        List<IndexerThread> threads = new ArrayList<IndexerThread>();
        for (List<UUID> hl : arrayIDList) {
            IndexerThread thread = new IndexerThread(hl, onlyFake);
            thread.start();
            threads.add(thread);
        }
        boolean finished = false;
        while (!finished) {
            finished = true;
            for (IndexerThread thread : threads) {
                finished = finished && !thread.isAlive();
            }
        }
    }

    class IndexerThread extends Thread {
        private boolean onlyFake;

        private List<UUID> itemids;

        public IndexerThread(List<UUID> itemids, boolean onlyFake) {
            this.onlyFake = onlyFake;
            this.itemids = itemids;
        }

        @Override
        public void run() {
            Context context = null;
            try {
                context = new Context();
                context.turnOffAuthorisationSystem();
                int idx = 1;
                final String head = this.getName() + "#" + this.getId();
                final int size = itemids.size();
                for (UUID id : itemids) {
                    try {
                        Item item = ContentServiceFactory.getInstance().getItemService().find(context, id);
                        Map<String, List<String>> tmpMapFilter = new HashMap<String, List<String>>();
                        List<String> tmpFilter = new ArrayList<String>();
                        fillSignature(context, (DSpaceObject) item, tmpMapFilter, tmpFilter);
                        if (!tmpFilter.isEmpty()) {
                            // retrieve all search plugin to build search document in the same index
                            SearchDeduplication searchSignature = dspace.getServiceManager().getServiceByName(
                                    "item".toUpperCase() + "SearchDeduplication", SearchDeduplication.class);
                            if (onlyFake) {
                                buildFromDedupReject(context, item, tmpMapFilter, tmpFilter, searchSignature);
                                build(context, item.getID(), item.getID(), DeduplicationFlag.FAKE, tmpMapFilter,
                                        searchSignature, null);
                            } else {
                                buildPotentialMatch(context, item, tmpMapFilter, tmpFilter, searchSignature);
                            }
                        }
                    } catch (Exception ex) {
                        System.out.println("ERROR: identifier item:" + id + " identifier thread:" + head + " error:"
                                + ex.getMessage());
                    }
                    System.out.println(head + ":" + (idx++) + " / " + size);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (context != null) {
                    context.abort();
                }
            }
        }
    }

    private void buildFromDedupReject(Context ctx, DSpaceObject iu, Map<String, List<String>> tmpMapFilter,
            List<String> tmpFilter, SearchDeduplication searchSignature) {

        try {
            List<Deduplication> tri = deduplicationService.getDeduplicationByFirstAndSecond(ctx, iu.getID(),
                    iu.getID());

            for (Deduplication row : tri) {

                String submitterDecision = row.getSubmitterDecision();
                String workflowDecision = row.getWorkflowDecision();
                String adminDecision = row.getAdminDecision();
                String readerNote = row.getReaderNote();
                String adminNote = row.getNote();

                UUID firstId = row.getFirstItemId();
                UUID secondId = row.getSecondItemId();
                if (StringUtils.isNotBlank(submitterDecision)) {
                    buildDecision(ctx, firstId, secondId, DeduplicationFlag.getEnum(submitterDecision), readerNote);
                }

                if (StringUtils.isNotBlank(workflowDecision)) {
                    buildDecision(ctx, firstId, secondId, DeduplicationFlag.getEnum(workflowDecision), readerNote);
                }

                if (StringUtils.isNotBlank(adminDecision)) {
                    buildDecision(ctx, firstId, secondId, DeduplicationFlag.getEnum(adminDecision), adminNote);
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

    }

    @Override
    public void buildDecision(Context context, UUID firstId, UUID secondId, DeduplicationFlag flag, String note) {
        build(context, firstId, secondId, flag, null, null, note);
    }

    @Override
    public void commit() {
        if (getSolr() != null) {
            try {
                getSolr().commit();
            } catch (SolrServerException | IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void unIndexContent(Context context, UUID id) throws IllegalStateException, SQLException {
        try {
            delete(MessageFormat.format(QUERY_REMOVE, id, Constants.ITEM));

        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }

    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }
}
