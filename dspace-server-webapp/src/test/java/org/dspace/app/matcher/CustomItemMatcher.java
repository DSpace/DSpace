/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.matcher;

import java.util.UUID;

import org.dspace.content.Item;
import org.mockito.ArgumentMatcher;

public class CustomItemMatcher implements ArgumentMatcher<Item> {
    private final UUID expectedUUID;

    public CustomItemMatcher(UUID expectedUUID) {
        this.expectedUUID = expectedUUID;
    }

    @Override
    public boolean matches(Item actual) {
        return actual != null && actual.getID().equals(expectedUUID);
    }
}
