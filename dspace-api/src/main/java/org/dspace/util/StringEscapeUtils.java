/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.translate.AggregateTranslator;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.EntityArrays;
import org.apache.commons.text.translate.LookupTranslator;

public class StringEscapeUtils extends org.apache.commons.text.StringEscapeUtils {
    public static final CharSequenceTranslator ESCAPE_MAIL;
    static {
        final Map<CharSequence, CharSequence> escapeMailMap = new HashMap<>();
        escapeMailMap.put("#", "&#35");
        ESCAPE_MAIL = new AggregateTranslator(
                new LookupTranslator(EntityArrays.BASIC_ESCAPE),
                new LookupTranslator(EntityArrays.APOS_ESCAPE),
                new LookupTranslator(Collections.unmodifiableMap(escapeMailMap))
        );
    }

     /**
     * Escapes the characters in a {@code String} using custom rules to avoid XSS attacks.
     *
     * <p>Escapes user-entered text that is sent with mail to avoid possible XSS attacks.
     * It escapes double-quote, ampersand, less-than, greater-than, apostrophe, number sign (", &, <, >,',#) </p>
     *
     * <p>Example:</p>
     * <pre>
     * input string: <div attr="*x" onblur="alert(1)*"> lá lé lí ló LÚ pingüino & yo # </div>!!"
     * output string: &lt;div attr=&quot;*x&quot; onblur=&quot;alert(1)*&quot;&gt; lá lé lí ló LÚ
     *  pingüino &amp; yo &#35 &lt;/div&gt;!!
     * </pre>
     *
     * @param input  String to escape values in, may be null
     * @return String with escaped values, {@code null} if null string input
     */
    public static final String escapeMail(final String input) {
        return ESCAPE_MAIL.translate(input);
    }
}
