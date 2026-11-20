/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.converter;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.core.convert.converter.Converter;

/**
 * Converter to escape the xml's special characters.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class XmlValueConverter implements Converter<String, String> {

    @Override
    public String convert(String source) {
        if (source != null && source.contains("fo:block")) {
            return source;
        }
        return StringEscapeUtils.escapeXml11(source);
    }

}
