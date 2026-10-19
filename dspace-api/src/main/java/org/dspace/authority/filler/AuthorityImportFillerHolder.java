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
 * <p>
 * The class enables the system to dynamically select the correct metadata filling
 * strategy at runtime based on the authority source associated with a metadata value.
 * </p>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class AuthorityImportFillerHolder {

    private Map<String, AuthorityImportFiller> fillers;

    /**
     * Retrieves the filler strategy associated with a specific authority source.
     * * @param authorityType The identifier for the authority source (e.g., "ORCID").
     * Matches the keys defined in the
     * org.dspace.authority.filler.AuthorityImportFillerHolder bean.
     * @return The specific {@link AuthorityImportFiller} for the type;
     * falls back to the internal filler if the specific type is not mapped.
     */
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
