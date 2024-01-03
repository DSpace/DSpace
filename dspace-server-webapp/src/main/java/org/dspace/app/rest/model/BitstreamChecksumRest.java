/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The BitstreamChecksum REST Resource.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class BitstreamChecksumRest extends RestAddressableModel implements RestModel {
    private static final long serialVersionUID = -3415049466402327251L;
    public static final String NAME = "bitstreamchecksum";
    CheckSumRest activeStore = null;
    CheckSumRest synchronizedStore = null;
    CheckSumRest databaseChecksum = null;

    public BitstreamChecksumRest() {
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
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

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public Class getController() {
        return null;
    }
}
