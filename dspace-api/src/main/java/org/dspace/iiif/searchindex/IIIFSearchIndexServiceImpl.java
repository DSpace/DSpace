/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.searchindex;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.iiif.searchindex.service.IIIFSearchIndexService;
import org.dspace.iiif.util.IIIFSharedUtils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This service adds and removes OCR files from the word_highlighting
 * solr index. The file processing is done by a service running on the solr host.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 */
public class IIIFSearchIndexServiceImpl implements IIIFSearchIndexService {

    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(IIIFSearchIndexServiceImpl.class);


    @Autowired
    ItemService itemService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    List<String> skipList;
    private int max2Process = Integer.MAX_VALUE;
    private int processed = 0;
    boolean isQuiet = true;

    @Override
    public boolean checkStatus() {
        String indexingService = configurationService.getProperty("iiif.search.index.service");
        HttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(indexingService + "/status");
        try {
            HttpResponse response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }
        // Should not reach this point
        return false;
    }

    @Override
    public void setSkipList(List<String> skipList) {
        this.skipList = skipList;
    }

    @Override
    public void setMax2Process(int max) {
        this.max2Process = max;
    }

    @Override
    public void setIsQuiet(boolean quiet) {
        this.isQuiet = quiet;
    }

    @Override
    public int processCommunity(Context context, Community community, String action) throws Exception {
        if (!inSkipList(community.getHandle())) {
            List<Community> subcommunities = community.getSubcommunities();
            for (Community subcommunity : subcommunities) {
                processCommunity(context, subcommunity, action);
            }
            List<Collection> collections = community.getCollections();
            for (Collection collection : collections) {
                processCollection(context, collection, action);
            }
        }
        return processed;
    }

    @Override
    public int processCollection(Context context, Collection collection, String action) throws Exception {
        if (!inSkipList(collection.getHandle())) {
            Iterator<Item> itemIterator = itemService.findAllByCollection(context, collection);
            while (itemIterator.hasNext() && processed < max2Process) {
                processItem(context, itemIterator.next(), action);
            }
        }
        return processed;
    }

    @Override
    public void processItem(Context context, Item dso, String action) {
        String indexingService = configurationService.getProperty("iiif.search.index.service");
        String url = indexingService + "/item/" + dso.getID().toString();
        boolean isIIIFItem = IIIFSharedUtils.isIIIFItem(dso);
        boolean isIIIFSearchable = IIIFSharedUtils.isIIIFSearchable(dso);
        if (isIIIFSearchable && isIIIFItem && !inSkipList(dso.getHandle())) {
            if (action == "add") {
                if (!checkIndex(url)) {
                    post(url, dso.getID().toString());
                    ++processed;
                } else {
                    System.out.println("An index entry already exists. Skipping: " + dso.getID());
                }
            } else if (action == "delete") {
                delete(url, dso.getID().toString());
                ++processed;
            }
        }
    }

    private boolean checkIndex(String url) {
        HttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                return true;
            } else if (response.getStatusLine().getStatusCode() == 404) {
                return false;
            }
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }
        // Should not reach this point
        return false;
    }

    private void post(String url, String id)  {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);
        try {
            HttpResponse response = httpclient.execute(httppost);
            if (response.getStatusLine().getStatusCode() != 200) {
                log.warn("Post to OCR processor failed with status code: "
                    + response.getStatusLine().getStatusCode());
                System.out.println("Indexing failed for: " + id);
                System.out.println("Check the DSpace log and the Solr OCR processor log for details.");
            }
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void delete(String url, String id) {
        HttpClient httpclient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        try {
            HttpResponse response = httpclient.execute(httpDelete);
            if (response.getStatusLine().getStatusCode() != 200) {
                log.warn("Solr deletion failed with status code: "
                    + response.getStatusLine().getStatusCode());
                System.out.println("Deletion failed for: " + id);
            }
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }
    }


    /**
     * Tests whether the identifier is in the skip list.
     * @param identifier
     * @return
     */
    private boolean inSkipList(String identifier) {
        if (skipList != null && skipList.contains(identifier)) {
            if (!isQuiet) {
                System.out.println("SKIP-LIST: skipped bitstreams within identifier " + identifier);
            }
            return true;
        } else {
            return false;
        }
    }

}
