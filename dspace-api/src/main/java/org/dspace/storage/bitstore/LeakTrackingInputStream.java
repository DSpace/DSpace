/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.Logger;

/**
 * Wrapper Class for InputStream
 * Keeps track of open InputStreams and adds information to be able to track down the origins of the InputStream
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public final class LeakTrackingInputStream extends FilterInputStream {

    private static final Cleaner CLEANER = Cleaner.create();
    private static final ConcurrentMap<UUID, BitstreamInputStreamOpenInfo> OPEN = new ConcurrentHashMap<>();

    private final UUID id = UUID.randomUUID();
    private final Cleaner.Cleanable cleanable;
    private volatile boolean closed = false;

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LeakTrackingInputStream.class);

    /**
     * Constructor of LeakTrackingInputStream
     * Wraps an InputStream with information to keep track of open InputStreams
     *
     * @param in    the InputStream to be wrapped
     * @param kind  where the InputStream was opened or retrieved
     */
    public LeakTrackingInputStream(InputStream in, String kind) {
        super(in);
        BitstreamInputStreamOpenInfo info =
                new BitstreamInputStreamOpenInfo(id, kind, Instant.now(), new Exception("Opened here"));
        OPEN.put(id, info);
        this.cleanable = CLEANER.register(this, () -> {
            BitstreamInputStreamOpenInfo leaked = OPEN.remove(id);
            if (leaked != null) {
                log.error("LEAKED {} stream {}", leaked.kind(), leaked.id(), leaked.openedAtThrowable());
            }
        });
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            OPEN.remove(id);
            cleanable.clean();
        }
        super.close();
    }

    public static Map<UUID, BitstreamInputStreamOpenInfo> snapshotOpenStreams() {
        return Map.copyOf(OPEN);
    }
}
