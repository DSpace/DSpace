/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Extremely simple filter that always returns true!
 * Useful to pass to methods that expect a filter, in order to effectively say "all items".
 * This could be configured in Spring XML but it is more stable and reliable to have it hard-coded here
 * so that any broken configuration doesn't silently break parts of DSpace that expect it to work.
 *
 * @author Kim Shepherd
 */
public class TrueFilter implements Filter {
    private String name;
    private final static Logger log = LogManager.getLogger();

    public boolean getResult(Context context, Item item) throws LogicalStatementException {
        return true;
    }

    @Override
    public void setBeanName(String name) {
        log.debug("Initialize bean " + name);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
