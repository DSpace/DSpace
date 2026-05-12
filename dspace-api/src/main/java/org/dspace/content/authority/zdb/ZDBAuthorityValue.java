/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.zdb;
import org.apache.commons.lang.StringUtils;
import org.dspace.authority.AuthorityValue;

public class ZDBAuthorityValue extends AuthorityValue {

    public static final String SPLIT = "::";
    public static final String GENERATE = "will be generated" + SPLIT;
    @Override
    public String generateString() {
        String generateString = GENERATE + getAuthorityType() + SPLIT;
        if (StringUtils.isNotBlank(getServiceId())) {
            generateString += getServiceId();
        }
        return generateString;
    }

    @Override
    public String getAuthorityType() {
        return "zdb";
    }

}