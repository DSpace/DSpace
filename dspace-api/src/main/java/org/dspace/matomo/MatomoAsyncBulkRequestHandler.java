/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.matomo.client.MatomoClient;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.matomo.model.MatomoRequestDetailsBuilder;
import org.dspace.matomo.model.MatomoRequestDetailsSplitter;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class groups together {@code capacity} requests that will be sent as one bulk request
 * using the {@code MatomoClient}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoAsyncBulkRequestHandler extends MatomoAbstractHandler {

    private static final Logger log = LogManager.getLogger(MatomoAsyncBulkRequestHandler.class);

    private final LinkedBlockingDeque<MatomoRequestDetails> deque;
    private final Lock lock = new ReentrantLock();


    public MatomoAsyncBulkRequestHandler(
        @Autowired MatomoRequestDetailsBuilder builder,
        @Autowired MatomoClient matomoClient,
        int capacity
    ) {
        super(matomoClient, builder);
        this.deque = new LinkedBlockingDeque<>(capacity);
    }

    @Override
    public void handleEvent(UsageEvent usageEvent) {
        if (usageEvent == null) {
            log.error("Skipping UsageEvent is null");
            return;
        }

        lock.lock();
        try {
            this.deque.add(this.toDetails(usageEvent));
            if (this.deque.remainingCapacity() <= 1) {
                this.sendEvents();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void sendEvents() {
        lock.lock();
        try {
            ArrayList<MatomoRequestDetails> details = new ArrayList<>();
            deque.drainTo(details);
            MatomoRequestDetailsSplitter.split(details)
                                        .values()
                                        .forEach(this.matomoClient::sendDetails);
        } finally {
            lock.unlock();
        }
    }

}
