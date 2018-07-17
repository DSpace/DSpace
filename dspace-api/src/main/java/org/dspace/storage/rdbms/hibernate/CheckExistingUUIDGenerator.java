/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.hibernate;

import org.dspace.content.DSpaceObject;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.UUIDGenerator;

import java.io.Serializable;
import java.util.UUID;

/**
 * Allows DSpaceObjects to provide a pre-determined UUID
 *
 * @author Chris Herron
 */
public class CheckExistingUUIDGenerator extends UUIDGenerator {

    @Override
    public Serializable generate(SessionImplementor session, Object object) {
        if (object instanceof DSpaceObject) {
            UUID uuid = ((DSpaceObject) object).getPredefinedUUID();
            if (uuid != null) {
                return uuid;
            }
        }
        return super.generate(session, object);
    }
}
