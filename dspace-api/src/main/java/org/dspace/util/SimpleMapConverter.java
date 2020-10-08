/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import org.apache.commons.lang3.StringUtils;
import org.dspace.submit.lookup.MapConverterModifier;

/**
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SimpleMapConverter extends MapConverterModifier {

    public SimpleMapConverter(String name) {
        super(name);
    }

    public String getValue(String key) {
        boolean matchEmpty = false;
        String stringValue = key;

        String tmp = "";
        if (mapping.containsKey(stringValue)) {
            tmp = mapping.get(stringValue);
        } else {
            tmp = defaultValue;
            for (String regex : regexConfig.keySet()) {
                if (stringValue != null && stringValue.matches(regex)) {
                    tmp = stringValue.replaceAll(regex, regexConfig.get(regex));
                    if (StringUtils.isBlank(tmp)) {
                        matchEmpty = true;
                    }
                }
            }
        }

        if ("@@ident@@".equals(tmp)) {
            return stringValue;
        } else if (StringUtils.isNotBlank(tmp) || (StringUtils.isBlank(tmp) && matchEmpty)) {
            return tmp;
        }

        return stringValue;
    }

}