/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.function.Function;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.BrowseEntryRest;
import org.springframework.stereotype.Component;

/**
 * This is the converter from String array returned by the Browse engine for
 * metadata browse to the BrowseEntryRest DTO
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class BrowseEntryConverter implements Function<String[], BrowseEntryRest> {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(BrowseEntryConverter.class);

    @Override
    public BrowseEntryRest apply(String[] source) {
        BrowseEntryRest entry = new BrowseEntryRest();
        entry.setValue(source[0]);
        entry.setAuthority(source[1]);
        if (source.length == 3 && source[2] != null) {
            entry.setCount(Long.valueOf(source[2]));
        }
        return entry;
    }
}
