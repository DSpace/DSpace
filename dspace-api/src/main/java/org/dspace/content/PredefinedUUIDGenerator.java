/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerator;

/**
 * Allows DSpaceObjects to provide a pre-determined UUID
 *
 * @author April Herron
 */
public class PredefinedUUIDGenerator extends UUIDGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        if (object instanceof DSpaceObject) {
            UUID uuid = ((DSpaceObject) object).getPredefinedUUID();
            if (uuid != null) {
                return uuid;
            }
        }
        return super.generate(session, object);
    }
}