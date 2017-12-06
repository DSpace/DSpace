/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class EnumUtils {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(EnumUtils.class);

    private static String getEnumName(String value) {
        return StringUtils.isNotBlank(value) ?
                value.toUpperCase().trim().replaceAll("[^a-zA-Z]", "_")
                : null;
    }

    public static <E extends Enum<E>> E lookup(Class<E> enumClass, String enumName) {
        try {
            return Enum.valueOf(enumClass, getEnumName(enumName));
        } catch (Exception ex) {
            log.warn("Did not find an "+enumClass.getSimpleName()+" for value '"+enumName+"'");
            return null;
        }
    }
}
