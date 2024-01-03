/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * This class handles the checksums of a bitstream (local, S3, database)
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class BitstreamChecksum {
    CheckSumRest activeStore = null;
    CheckSumRest synchronizedStore = null;
    CheckSumRest databaseChecksum = null;

    public BitstreamChecksum() {
    }

    public CheckSumRest getActiveStore() {
        return activeStore;
    }

    public void setActiveStore(CheckSumRest activeStore) {
        this.activeStore = activeStore;
    }

    public CheckSumRest getSynchronizedStore() {
        return synchronizedStore;
    }

    public void setSynchronizedStore(CheckSumRest synchronizedStore) {
        this.synchronizedStore = synchronizedStore;
    }

    public CheckSumRest getDatabaseChecksum() {
        return databaseChecksum;
    }

    public void setDatabaseChecksum(CheckSumRest databaseChecksum) {
        this.databaseChecksum = databaseChecksum;
    }
}
