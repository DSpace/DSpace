/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.filler;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.MetadataValue;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service that provide methods to retrieve proper filler defined.
 *
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 *
 */
public class AuthorityImportFillerServiceImpl implements AuthorityImportFillerService {

    @Autowired(required = true)
    private AuthorityImportFillerHolder authorityImportFillerHolder;

    protected AuthorityImportFillerServiceImpl() {

    }

    public AuthorityImportFiller getAuthorityImportFillerByMetadata(MetadataValue metadata) {
        String authorityType = calculateAuthorityType(metadata);
        AuthorityImportFiller filler = authorityImportFillerHolder.getFiller(authorityType);
        return filler;
    }

    private String calculateAuthorityType(MetadataValue metadata) {
        String authority = metadata.getAuthority();
        if (StringUtils.isNotBlank(authority) && authority.startsWith(AuthorityValueService.GENERATE)) {
            String[] split = StringUtils.split(authority, AuthorityValueService.SPLIT);
            if (split.length > 1) {
                return split[1];
            }
        }
        return SOURCE_INTERNAL;
    }
}
