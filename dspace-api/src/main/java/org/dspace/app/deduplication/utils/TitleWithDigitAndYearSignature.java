/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.utils;

import java.util.Locale;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.ibm.icu.text.Normalizer;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;

/**
 * Signature extending MD5ValueSignature to match on title and issue date year.
 * (actually works on any string passed to the normalize method, not just title, but always adds year)
 * All non-alphanumeric characters are stripped from the signature value
 *
 * @author 4Science
 */
public class TitleWithDigitAndYearSignature extends MD5ValueSignature {

    /**
     * Normalise the value - strip non-alphanum characters, lowercase
     * @param item
     * @param value
     * @return
     */
    @Override
    protected String normalize(Context context, DSpaceObject item, String value) {
        if (value != null) {
            String temp = null;
            if (item != null) {
                temp = getYear(item);
            }
            String norm = Normalizer.normalize(value, Normalizer.NFD);
            CharsetDetector cd = new CharsetDetector();
            cd.setText(value.getBytes());
            CharsetMatch detect = cd.detect();
            if (detect != null && detect.getLanguage() != null) {
                norm = norm.replaceAll("[^\\p{L}^\\p{N}]", "").toLowerCase(new Locale(detect.getLanguage()));
            } else {
                norm = norm.replaceAll("[^\\p{L}^\\p{N}]", "").toLowerCase();
            }
            if (temp != null) {
                return temp + " " + norm;
            }
            return norm;
        } else {
            return "item:" + item.getID();
        }

    }

    /**
     * Get the year
     * @param item      Dspace item
     * @return          Issue date year as string
     */
    private String getYear(DSpaceObject item) {
        String year = null;
        String dcvalue = ContentServiceFactory.getInstance().getDSpaceObjectService(item).getMetadata(item,
                "dc.date.issued");
        if (StringUtils.isNotEmpty(dcvalue)) {
            year = StringUtils.substring(dcvalue, 0, 4);
        }
        return year;
    }

}
