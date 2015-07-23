/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import org.apache.commons.lang.StringUtils;
import org.dspace.submit.lookup.MapConverterModifier;

public class SimpleMapConverter extends MapConverterModifier {

	public SimpleMapConverter(String name) {
		super(name);
	}

	public String getValue(String key) {
		String stringValue = key;

        String tmp = "";
        if (mapping.containsKey(stringValue))
        {
            tmp = mapping.get(stringValue);
        }
        else
        {
            tmp = defaultValue;
            for (String regex : regexConfig.keySet())
            {
                if (stringValue != null
                        && stringValue.matches(regex))
                {
                    tmp = stringValue.replaceAll(regex,
                            regexConfig.get(regex));
                }
            }
        }

        if ("@@ident@@".equals(tmp))
        {
            return stringValue;
        }
        else if (StringUtils.isNotBlank(tmp))
        {
            return tmp;
        }
        
        return stringValue;
	}

}