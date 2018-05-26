/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.UUID;

public interface RootObject {

    int getType();

    String getName();

    UUID getID();

    String getHandle();
}
