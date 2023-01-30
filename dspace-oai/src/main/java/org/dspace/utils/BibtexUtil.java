/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/* Created for LINDAT/CLARIAH-CZ (UFAL) */
package org.dspace.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;


/**
 * Escaping from the old oai-bibtex crosswalk
 * Meant to be called from xsl
 *
 *  Class is copied from the LINDAT/CLARIAH-CZ (https://github.com/ufal/clarin-dspace) and modified by
 *  @author Marian Berger (marian.berger at dataquest.sk)
 */
public class BibtexUtil {

    private BibtexUtil() {}

    /**
     * Make the string bibtex friendly by escaping etc. See
     * http://www.bibtex.org/SpecialSymbols/
     * https://java-bibtex.googlecode.com/svn
     * /trunk/src/main/java/org/jbibtex/LaTeXPrinter.java
     */
    public static String bibtexify(String s) {
        return new BibtexString(s).toString().replaceAll(",\\s+$", "").replaceAll("(?m)[\\r\\n]+", "");
    }

    public static String format(String s) {
        return s.replaceAll("\\s+", " ").replaceAll(" $", "").replaceAll("[,;]\\s*}", "}") + "\n";
    }

    public static void main(String[] args) {
        System.out.println(bibtexify("Î"));
        System.out.println(bibtexify("ơ"));
        System.out.println(bibtexify("PříÎliš žluťoučký kůň úpěl ďábelské ódy"));
        System.out.println(bibtexify(""));
        System.out.println(bibtexify("Add some \n\n\n\n new lines\n to the mix."));
        //Lower
        for (String ch : BibtexString.accents) {
            String actual_ch = ch.substring(0, 1);
            System.out.println(actual_ch + " : " + bibtexify(actual_ch));

        }
        //Upper
        for (String ch : BibtexString.accents) {
            String actual_ch = ch.substring(0, 1);
            System.out.println(actual_ch.toUpperCase() + " : " + bibtexify(actual_ch.toUpperCase()));

        }
        System.out.println(bibtexify("Cătălina"));
    }
}

class BibtexString {

    private String s_;

    BibtexString(String s) {
        s_ = _escape(s);
    }

    @Override
    public String toString() {
        return s_;
    }

    private static String _escape(String s) {

        // change escape characters first (we will introduce those in the next
        // replacements)
        for (String ch : to_escape) {
            s = s.replaceAll("\\" + ch, "\\\\" + ch);
        }

        String news = "";
        StringTokenizer stwords = new StringTokenizer(s, " \t\n\r\f", true);

        // first remove urls from {}ification
        //
        while (stwords.hasMoreTokens()) {
            String word = stwords.nextToken();
            if (1 < word.length()
                    && (!word.startsWith("http") && !word.startsWith("ftp"))) {
                // then, go throught all word parts long enough
                // there could still be problems with (http://P123)
                //
                String newword = "";
                StringTokenizer st = new StringTokenizer(word,
                        " \t\n\r\f().!?:;<>_\"'~=+-@#$%^*/\\|,", true);
                while (st.hasMoreTokens()) {
                    String wordpart = st.nextToken();
                    // if it is long
                    // and not url
                    // and lowercase does not match
                    if (1 < word.length()) {
                        String ww = wordpart.substring(1);
                        if (!ww.toLowerCase().equals(ww)) {
                            wordpart = "{" + wordpart + "}";
                        }
                    }
                    newword += wordpart;
                } //
                word = newword;

            }
            news += word;
        }
        s = news;

        // change accents with uppercase too
        for (String ch : getAccentsWithUpper()) {
            String to_find = ch.substring(0, 1);
            String to_change_with = ch.substring(1).replaceAll("\\\\",
                    "\\\\\\\\");
            s = s.replaceAll(to_find, to_change_with);
        }

        for (String ch : symbols_final) {
            String to_find = ch.substring(0, 1);
            String to_change_with = ch.substring(1).replaceAll("\\\\",
                    "\\\\\\\\");
            s = s.replaceAll(to_find, to_change_with);
        }

        return s;
    }

    private static List<String> getAccentsWithUpper() {
        List<String> accentsWithUpper = new ArrayList<String>(accents.length * 2);
        for (String ch : accents) {
            accentsWithUpper.add(ch);
            String to_find = ch.substring(0, 1);
            String to_change_with = ch.substring(1);
            // uppercase only chars before } without space
            int lbr_idx = to_change_with.length() - 1;
            for (; 0 < lbr_idx; --lbr_idx) {
                char c = to_change_with.charAt(lbr_idx);
                if (' ' == c || '\\' == c) {
                    break;
                }
            }
            // or only the last char
            String to_change_with_upper = to_change_with.substring(0, lbr_idx)
                    + to_change_with.substring(lbr_idx).toUpperCase();
            // we don't need/want certain upper case
            // esp. in strings containing {\\"{\\I}} (or similar) don't replace the I with {\\I}
            if (!blackListedUpperAccents.contains(to_find.toUpperCase())) {
                if (to_change_with_upper.matches(".*\\{\\\\[A-Z]\\}}")) {
                    to_change_with_upper = to_change_with_upper.replaceFirst("\\{\\\\([A-Z]\\})}", "$1");
                }
                accentsWithUpper.add(to_find.toUpperCase() + to_change_with_upper);
            }
        }
        return accentsWithUpper;
    }

    //CZ - Příliš žluťoučký kůň úpěl ďábelské ódy
    // lower case, will do uppercase automatically
    public static final String[] accents = new String[] {
        //&aogon;               &aacute;   &acirc;                           &auml;     &abreve;
        "ą{\\c a}", "à{\\`a}", "á{\\'a}", "â{\\^a}", "ã{\\~a}", "ā{\\=a}", "ä{\\\"a}", "ă{\\u a}",
        //         acute                                       uml         caron       ogon       ecircumflexgrave
        "è{\\`e}", "é{\\'e}", "ê{\\^e}", "ẽ{\\~e}", "ē{\\=e}", "ë{\\\"e}", "ě{\\v e}", "ȩ{\\c e}", "ề{\\`{\\^e}}",

        "ễ{\\~{\\^e}}", "ė{\\.e}",
        //         acute      circ
        "ì{\\`{\\i}}", "í{\\'{\\i}}", "î{\\^{\\i}}", "ĩ{\\~{\\i}}", "ī{\\={\\i}}", "ï{\\\"{\\i}}", "ı{\\i}", "ị{\\d i}",
        //         acute      circ                             uml                                           &odblac;
        "ò{\\`o}", "ó{\\'o}", "ô{\\^o}", "õ{\\~o}", "ō{\\=o}", "ö{\\\"o}", "ø{\\o}", "ọ{\\d o}", "ŏ{\\v o}", "ő{\\H o}",

        "ồ{\\`{\\^o}}", "ỗ{\\~{\\^o}}", "ȯ{\\.o}",
        //&lacute; &lstrok;  &lcaron;
        "ĺ{\\'l}", "ł{\\l}", "ľ{\\v l}",
        //         acute                                       uml         uring       udblac
        "ù{\\`u}", "ú{\\'u}", "û{\\^u}", "ũ{\\~u}", "ū{\\=u}", "ü{\\\"u}", "ů{\\r u}", "ű{\\H u}",
        //acute
        "ý{\\'y}", "ÿ{\\\"y}", "ỳ{\\`y}", "ŷ{\\^y}", "ỹ{\\~y}",
        //         acute      caron
        "ñ{\\~n}", "ń{\\'n}", "ň{\\v n}", "ņ{\\c n}",
        //acute   caron       cedil
        "ś{\\'s}", "š{\\v s}", "ş{\\c s}",
        //caron     cedil
        "ť{\\v t}", "ţ{\\c t}",
        //cedil     acute      caron
        "ç{\\c c}", "ć{\\'c}", "č{\\v c}",
        //acute     caron       dot
        "ź{\\'z}", "ž{\\v z}", "ż{\\.z}",
        //caron     strok
        "ď{\\v d}", "đ{\\d}",
        //caron     acute
        "ř{\\v r}", "ŕ{\\'r}",
        //
        "ĵ{\\^{\\j}}",
        //
        "ğ{\\u g}",

        "œ{\\oe}", "æ{\\ae}", "å{\\aa}",  "þ{\\t h}", };

    // without automatic uppercase
    public static final String[] symbols_final = new String[]{"ß{\\ss}",
        "£{\\pounds}", "§{\\S}", "©{\\textcopyright}", "ª{\\textordfeminine}",
        "®{\\textregistered}", "¶{\\P}", "·{\\textperiodcentered}",
        "º{\\textordmasculine}", "¿{?`} ",};

    public static final String[] to_escape = new String[] { "?", "&", "$", "{",
        "}", "%", "_", "#", };

    private static final Set<String> blackListedUpperAccents = new HashSet<>(Arrays.asList("I"));

} // class BibtexString

