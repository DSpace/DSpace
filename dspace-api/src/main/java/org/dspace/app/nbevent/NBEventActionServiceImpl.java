/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import java.util.Map;

public class NBEventActionServiceImpl implements NBEventActionService {
    private Map<String, NBAction> topicsToActions;

    public void setTopicsToActions(Map<String, NBAction> topicsToActions) {
        this.topicsToActions = topicsToActions;
    }

    public Map<String, NBAction> getTopicsToActions() {
        return topicsToActions;
    }
}
