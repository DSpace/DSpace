/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.text.filter;

import java.util.HashMap;
import java.util.Map;

/**
 * Define languages - both as IANA and ISO639-2 codes
 * 
 * @author Graham Triggs
 */
public class Language
{
    public final String IANA;
    public final String ISO639_1;
    public final String ISO639_2;

    public static final Language AFRIKAANS          = Language.create("af",  "af", "afr");
    public static final Language ALBANIAN           = Language.create("sq",  "sq", "alb");
    public static final Language ARABIC             = Language.create("ar",  "ar", "ara");
    public static final Language BALUCHI            = Language.create("bal", "",   "bal");
    public static final Language BASQUE             = Language.create("eu",  "",   "baq");
    public static final Language BRAHUI             = Language.create("",    "",   "");
    public static final Language CATALAN            = Language.create("ca",  "ca", "cat");
    public static final Language CLASSICAL_GREEK    = Language.create("grc", "",   "grc");
    public static final Language DANISH             = Language.create("da",  "da", "dan");
    public static final Language DUTCH              = Language.create("nl",  "ni", "dut");
    public static final Language ENGLISH            = Language.create("en",  "en", "eng");
    public static final Language ESPERANTO          = Language.create("eo",  "eo", "epo");
    public static final Language FRENCH             = Language.create("fr",  "fr", "fre");
    public static final Language FRISIAN            = Language.create("fy",  "fy", "fri");
    public static final Language GALICIAN           = Language.create("gl",  "gl", "glg");
    public static final Language GERMAN             = Language.create("de",  "de", "ger");
    public static final Language GREEK              = Language.create("el",  "el", "gre");
    public static final Language HAWAIIAN           = Language.create("haw", "",   "haw");
    public static final Language HEBREW             = Language.create("he",  "he", "heb");
    public static final Language HUNGARIAN          = Language.create("hu",  "hu", "hun");
    public static final Language ICELANDIC          = Language.create("is",  "is", "ice");
    public static final Language IRISH              = Language.create("ga",  "ga", "gle");
    public static final Language ITALIAN            = Language.create("it",  "it", "ita");
    public static final Language MALAGASY           = Language.create("mg",  "mg", "mlg");
    public static final Language MALTESE            = Language.create("mt",  "mt", "mlt");
    public static final Language NEAPOLITAN_ITALIAN = Language.create("nap", "",   "nap");
    public static final Language NORWEGIAN          = Language.create("no",  "no", "nor");
    public static final Language PORTUGUESE         = Language.create("pt",  "pt", "por");
    public static final Language PANJABI            = Language.create("pa",  "pa", "pan");
    public static final Language PERSIAN            = Language.create("fa",  "fa", "per");
    public static final Language PROVENCAL          = Language.create("pro", "",   "pro");
    public static final Language PROVENCAL_OCCITAN  = Language.create("oc",  "oc", "oci");
    public static final Language ROMANIAN           = Language.create("ro",  "ro", "rum");
    public static final Language SCOTS              = Language.create("sco", "",   "sco");
    public static final Language SCOTTISH_GAELIC    = Language.create("gd",  "gd", "gae");
    public static final Language SHETLAND_ENGLISH   = Language.create("",    "",   "");
    public static final Language SPANISH            = Language.create("es",  "es", "spa");
    public static final Language SWEDISH            = Language.create("sv",  "sv", "swe");
    public static final Language TAGALOG            = Language.create("tl",  "tl", "tgl");
    public static final Language TURKISH            = Language.create("tr",  "tr", "tur");
    public static final Language URDU               = Language.create("ur",  "ur", "urd");
    public static final Language WALLOON            = Language.create("wa",  "wa", "wln");
    public static final Language WELSH              = Language.create("cy",  "cy", "wel");
    public static final Language YIDDISH            = Language.create("yi",  "yi", "yid");

    public static Language getLanguage(String lang)
    {
        return LanguageMaps.getLanguage(lang);
    }

    public static Language getLanguageForIANA(String iana)
    {
        return LanguageMaps.getLanguageForIANA(iana);
    }
    
    public static Language getLanguageForISO639_2(String iso)
    {
        return LanguageMaps.getLanguageForISO639_2(iso);
    }
    
    private static synchronized Language create(String iana, String iso639_1, String iso639_2)
    {
        Language lang = LanguageMaps.getLanguageForIANA(iana);
        
        lang = (lang != null ? lang : LanguageMaps.getLanguageForISO639_1(iso639_1));
        lang = (lang != null ? lang : LanguageMaps.getLanguageForISO639_2(iso639_2));
        
        return (lang != null ? lang : new Language(iana, iso639_1, iso639_2));
    }
    
    private static class LanguageMaps
    {
        private static final Map<String, Language> langMapIANA     = new HashMap<String, Language>();
        private static final Map<String, Language> langMapISO639_1 = new HashMap<String, Language>();
        private static final Map<String, Language> langMapISO639_2 = new HashMap<String, Language>();

        static void add(Language l)
        {
            if (l.IANA != null && l.IANA.length() > 0 && !langMapIANA.containsKey(l.IANA))
            {
                langMapIANA.put(l.IANA, l);
            }
            
            if (l.ISO639_1 != null && l.ISO639_1.length() > 0 && !langMapISO639_1.containsKey(l.ISO639_1))
            {
                langMapISO639_1.put(l.ISO639_1, l);
            }

            if (l.ISO639_2 != null && l.ISO639_2.length() > 0 && !langMapISO639_2.containsKey(l.ISO639_2))
            {
                langMapISO639_2.put(l.ISO639_2, l);
            }
        }

        public static Language getLanguage(String lang)
        {
            if (langMapIANA.containsKey(lang))
            {
                return langMapIANA.get(lang);
            }

            return langMapISO639_2.get(lang);
        }

        public static Language getLanguageForIANA(String iana)
        {
            return langMapIANA.get(iana);
        }
        
        public static Language getLanguageForISO639_1(String iso)
        {
            return langMapISO639_1.get(iso);
        }

        public static Language getLanguageForISO639_2(String iso)
        {
            return langMapISO639_2.get(iso);
        }
    }
    
    private Language(String iana, String iso639_1, String iso639_2)
    {
        IANA     = iana;
        ISO639_1 = iso639_1;
        ISO639_2 = iso639_2;
        
        LanguageMaps.add(this);
    }
    
    private Language()
    {
        IANA     = null;
        ISO639_1 = null;
        ISO639_2 = null;
    }
}
