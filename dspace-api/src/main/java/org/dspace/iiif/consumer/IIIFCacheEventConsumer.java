/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.consumer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;


/**
 * This consumer is used to evict modified items from the manifests cache.
 */
public class IIIFCacheEventConsumer implements Consumer {

    private final static Logger log = org.apache.logging.log4j.LogManager.getLogger(IIIFCacheEventConsumer.class);

    // When true all entries will be cleared from cache.
    private boolean clearAll = false;

    // Collects modified items for individual removal from cache.
    private final Set<DSpaceObject> toEvictFromManifestCache = new HashSet<>();

    // Collects modified bitstreams for individual removal from canvas dimension cache.
    private final Set<DSpaceObject> toEvictFromCanvasCache = new HashSet<>();

    @Override
    public void initialize() throws Exception {
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();
        if (!(st == Constants.BUNDLE || st == Constants.ITEM || st == Constants.BITSTREAM)) {
            return;
        }
        // This subject may become a reference to the parent Item that will be evicted from
        // the manifests cache.
        DSpaceObject subject = event.getSubject(ctx);
        DSpaceObject unmodifiedSubject = event.getSubject(ctx);

        int et = event.getEventType();

        if (et == Event.DELETE || et == Event.REMOVE) {
            log.warn("IIIF event consumer cannot remove a single item from the cache when " +
                "a bundle is deleted. The entire cache will be cleared.");
            clearAll = true;
        }

        if (st == Constants.BUNDLE) {
            if ((et == Event.ADD || et == Event.MODIFY || et == Event.MODIFY_METADATA || et == Event.REMOVE
                || et == Event.DELETE) && subject != null) {
                // set subject to be the parent Item.
                subject = ((Bundle) subject).getItems().get(0);
                if (log.isDebugEnabled()) {
                    log.debug("Transforming Bundle event into Item event for "
                        + subject.getID());
                }
            } else {
                return;
            }
        }

        if (st == Constants.BITSTREAM) {
            if (et == Event.DELETE || et == Event.REMOVE) {
                log.warn("IIIF event consumer cannot remove a single item from the cache when " +
                    "a bitstream is deleted. The entire cache will be cleared.");
                clearAll = true;
            }

            if ((et == Event.ADD || et == Event.MODIFY_METADATA  ) && subject != null
                && ((Bitstream) subject).getBundles().size() > 0) {
                // set subject to be the parent Item.
                Bundle bundle = ((Bitstream) subject).getBundles().get(0);
                subject = bundle.getItems().get(0);
                if (log.isDebugEnabled()) {
                    log.debug("Transforming Bitstream event into Item event for "
                        + subject.getID());
                }
            } else {
                return;
            }
        }

        if (st == Constants.ITEM && et == Event.ADD) {
            // nothing to evict from cache.
            return;
        }

        switch (et) {
            case Event.ADD:
                addToCacheEviction(subject, unmodifiedSubject, st);
                break;
            case Event.MODIFY:
                addToCacheEviction(subject, unmodifiedSubject, st);
                break;
            case Event.MODIFY_METADATA:
                addToCacheEviction(subject, unmodifiedSubject, st);
                break;
            case Event.REMOVE:
                addToCacheEviction(subject, unmodifiedSubject, st);
                break;
            case Event.DELETE:
                addToCacheEviction(subject, unmodifiedSubject, st);
                break;
            default: {
                log.warn("ManifestsCacheEventConsumer should not have been given this kind of "
                    + "subject in an event, skipping: " + event);
            }
        }
    }

    private void addToCacheEviction(DSpaceObject subject, DSpaceObject subject2, int type) {
        if (type == Constants.BITSTREAM) {
            toEvictFromCanvasCache.add(subject2);
        }
        toEvictFromManifestCache.add(subject);
    }

    @Override
    public void end(Context ctx) throws Exception {
        // Get the eviction service beans.
        ManifestsCacheEvictService manifestsCacheEvictService = CacheEvictBeanLocator.getManifestsCacheEvictService();
        CanvasCacheEvictService canvasCacheEvictService = CacheEvictBeanLocator.getCanvasCacheEvictService();

        if (manifestsCacheEvictService != null) {
            if (clearAll) {
                manifestsCacheEvictService.evictAllCacheValues();
            } else {
                for (DSpaceObject dso : toEvictFromManifestCache) {
                    UUID uuid = dso.getID();
                    manifestsCacheEvictService.evictSingleCacheValue(uuid.toString());
                }
            }
        }
        if (canvasCacheEvictService != null) {
            for (DSpaceObject dso : toEvictFromCanvasCache) {
                UUID uuid = dso.getID();
                canvasCacheEvictService.evictSingleCacheValue(uuid.toString());
            }
        }

        clearAll = false;
        toEvictFromManifestCache.clear();
        toEvictFromCanvasCache.clear();
    }

    @Override
    public void finish(Context ctx) throws Exception {

    }

}
