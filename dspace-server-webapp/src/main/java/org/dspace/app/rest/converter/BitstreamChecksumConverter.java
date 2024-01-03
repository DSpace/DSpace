/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.BitstreamChecksum;
import org.dspace.app.rest.model.BitstreamChecksumRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * Convert the BitstreamChecksum to appropriate REST data model
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component
public class BitstreamChecksumConverter implements DSpaceConverter<BitstreamChecksum, BitstreamChecksumRest> {
    @Override
    public BitstreamChecksumRest convert(BitstreamChecksum modelObject, Projection projection) {
        BitstreamChecksumRest bitstreamChecksumRest = new BitstreamChecksumRest();
        bitstreamChecksumRest.setActiveStore(modelObject.getActiveStore());
        bitstreamChecksumRest.setDatabaseChecksum(modelObject.getDatabaseChecksum());
        bitstreamChecksumRest.setSynchronizedStore(modelObject.getSynchronizedStore());
        return bitstreamChecksumRest;
    }

    @Override
    public Class<BitstreamChecksum> getModelClass() {
        return BitstreamChecksum.class;
    }
}
