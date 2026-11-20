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
 * Converter to escape the json's special characters.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class JsonValueConverter implements Converter<String, String> {

    @Override
    public String convert(String source) {
        return StringEscapeUtils.escapeJson(source);
    }

}
