/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.filler;

import static org.dspace.authority.filler.AuthorityImportFillerService.SOURCE_INTERNAL;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;

/**
 * Holder that handle all the authorities import filler defined.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class AuthorityImportFillerHolder {

    private Map<String, AuthorityImportFiller> fillers;

    public AuthorityImportFiller getFiller(String authorityType) {
        if (MapUtils.isEmpty(fillers)) {
            return null;
        }

        return fillers.getOrDefault(authorityType, fillers.get(SOURCE_INTERNAL));
    }

    public void setFillers(Map<String, AuthorityImportFiller> fillers) {
        this.fillers = fillers;
    }
}
