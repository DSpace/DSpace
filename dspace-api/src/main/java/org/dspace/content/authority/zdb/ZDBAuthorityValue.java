/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.zdb;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.AuthorityValue;

/**
 * Authority value representation for ZDB (Zeitschriftendatenbank) entries.
 *
 * <p>Extends {@link AuthorityValue} with a ZDB-specific authority string format
 * ({@code will be generated::zdb::<serviceId>}) and a fixed authority type of
 * {@code "zdb"}.</p>
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ZDBAuthorityValue extends AuthorityValue {

    public static final String SPLIT = "::";
    public static final String GENERATE = "will be generated" + SPLIT;
    /** {@inheritDoc} */
    @Override
    public String generateString() {
        String generateString = GENERATE + getAuthorityType() + SPLIT;
        if (StringUtils.isNotBlank(getServiceId())) {
            generateString += getServiceId();
        }
        return generateString;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthorityType() {
        return "zdb";
    }

}