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
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 *
 * Signature extending MD5ValueSignature to match on title string. (actually, works on any string)
 * All non-alphanumeric characters are stripped from the signature value
 *
 * @author 4Science
 */
public class TitleWithDigitSignature extends MD5ValueSignature {

    /**
     * Normalise the text value. Strip all non-alphanumeric characters.
     *
     * @param context   DSpace context
     * @param item      DSpace item
     * @param value     Text to normalise
     * @return
     */
    @Override
    protected String normalize(Context context, DSpaceObject item, String value) {
        if (value != null) {

            String norm = Normalizer.normalize(value, Normalizer.NFD);
            CharsetDetector cd = new CharsetDetector();
            cd.setText(value.getBytes());
            CharsetMatch detect = cd.detect();
            if (detect != null && detect.getLanguage() != null) {
                norm = norm.replaceAll("[^\\p{L}^\\p{N}]", "").toLowerCase(new Locale(detect.getLanguage()));
            } else {
                norm = norm.replaceAll("[^\\p{L}^\\p{N}]", "").toLowerCase();
            }
            return norm;
        } else {
            return "item:" + item.getID();
        }

    }
}
