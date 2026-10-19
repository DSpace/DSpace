/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.authority.factory;

import java.util.Map;

import org.dspace.content.authority.service.ItemAuthorityService;

/**
 * Factory implementation to get services for the content.authority package, use
 * ItemAuthorityServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 * @author Stefano Maffei 4Science.com
 */
public class ItemAuthorityServiceFactory {

    private Map<String, ItemAuthorityService> authorityServiceImplMap;

    public Map<String, ItemAuthorityService> getAuthorityServiceImplMap() {
        return authorityServiceImplMap;
    }

    public void setAuthorityServiceImplMap(Map<String, ItemAuthorityService> authorityServiceImplMap) {
        this.authorityServiceImplMap = authorityServiceImplMap;
    }

    public ItemAuthorityService getInstance(String authorityName) {
        return (authorityName != null && authorityServiceImplMap.containsKey(authorityName))
            ? authorityServiceImplMap.get(authorityName)
            : authorityServiceImplMap.get("default");
    }
}
