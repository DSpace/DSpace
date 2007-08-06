/*
 * LanguageCodes.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2007/03/02 11:22:13 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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

    public static Language AFRIKAANS          = Language.create("af",  "af", "afr");
    public static Language ALBANIAN           = Language.create("sq",  "sq", "alb");
    public static Language ARABIC             = Language.create("ar",  "ar", "ara");
    public static Language BALUCHI            = Language.create("bal", "",   "bal");
    public static Language BASQUE             = Language.create("eu",  "",   "baq");
    public static Language BRAHUI             = Language.create("",    "",   "");
    public static Language CATALAN            = Language.create("ca",  "ca", "cat");
    public static Language CLASSICAL_GREEK    = Language.create("grc", "",   "grc");
    public static Language DANISH             = Language.create("da",  "da", "dan");
    public static Language DUTCH              = Language.create("nl",  "ni", "dut");
    public static Language ENGLISH            = Language.create("en",  "en", "eng");
    public static Language ESPERANTO          = Language.create("eo",  "eo", "epo");
    public static Language FRENCH             = Language.create("fr",  "fr", "fre");
    public static Language FRISIAN            = Language.create("fy",  "fy", "fri");
    public static Language GALICIAN           = Language.create("gl",  "gl", "glg");
    public static Language GERMAN             = Language.create("de",  "de", "ger");
    public static Language GREEK              = Language.create("el",  "el", "gre");
    public static Language HAWAIIAN           = Language.create("haw", "",   "haw");
    public static Language HEBREW             = Language.create("he",  "he", "heb");
    public static Language HUNGARIAN          = Language.create("hu",  "hu", "hun");
    public static Language ICELANDIC          = Language.create("is",  "is", "ice");
    public static Language IRISH              = Language.create("ga",  "ga", "gle");
    public static Language ITALIAN            = Language.create("it",  "it", "ita");
    public static Language MALAGASY           = Language.create("mg",  "mg", "mlg");
    public static Language MALTESE            = Language.create("mt",  "mt", "mlt");
    public static Language NEAPOLITAN_ITALIAN = Language.create("nap", "",   "nap");
    public static Language NORWEGIAN          = Language.create("no",  "no", "nor");
    public static Language PORTUGUESE         = Language.create("pt",  "pt", "por");
    public static Language PANJABI            = Language.create("pa",  "pa", "pan");
    public static Language PERSIAN            = Language.create("fa",  "fa", "per");
    public static Language PROVENCAL          = Language.create("pro", "",   "pro");
    public static Language PROVENCAL_OCCITAN  = Language.create("oc",  "oc", "oci");
    public static Language ROMANIAN           = Language.create("ro",  "ro", "rum");
    public static Language SCOTS              = Language.create("sco", "",   "sco");
    public static Language SCOTTISH_GAELIC    = Language.create("gd",  "gd", "gae");
    public static Language SHETLAND_ENGLISH   = Language.create("",    "",   "");
    public static Language SPANISH            = Language.create("es",  "es", "spa");
    public static Language SWEDISH            = Language.create("sv",  "sv", "swe");
    public static Language TAGALOG            = Language.create("tl",  "tl", "tgl");
    public static Language TURKISH            = Language.create("tr",  "tr", "tur");
    public static Language URDU               = Language.create("ur",  "ur", "urd");
    public static Language WALLOON            = Language.create("wa",  "wa", "wln");
    public static Language WELSH              = Language.create("cy",  "cy", "wel");
    public static Language YIDDISH            = Language.create("yi",  "yi", "yid");

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
        Language lang = null;
        
        lang = (lang != null ? lang : LanguageMaps.getLanguageForIANA(iana));
        lang = (lang != null ? lang : LanguageMaps.getLanguageForISO639_1(iso639_1));
        lang = (lang != null ? lang : LanguageMaps.getLanguageForISO639_2(iso639_2));
        
        return (lang != null ? lang : new Language(iana, iso639_1, iso639_2));
    }
    
    private static class LanguageMaps
    {
        private static final Map langMapIANA     = new HashMap();
        private static final Map langMapISO639_1 = new HashMap();
        private static final Map langMapISO639_2 = new HashMap();

        static void add(Language l)
        {
            if (l.IANA != null && l.IANA.length() > 0 && !langMapIANA.containsKey(l.IANA))
                langMapIANA.put(l.IANA, l);
            
            if (l.ISO639_1 != null && l.ISO639_1.length() > 0 && !langMapISO639_1.containsKey(l.ISO639_1))
                langMapISO639_1.put(l.ISO639_1, l);

            if (l.ISO639_2 != null && l.ISO639_2.length() > 0 && !langMapISO639_2.containsKey(l.ISO639_2))
                langMapISO639_2.put(l.ISO639_2, l);
        }

        public static Language getLanguage(String lang)
        {
            if (langMapIANA.containsKey(lang))
                return (Language)langMapIANA.get(lang);

            return (Language)langMapISO639_2.get(lang);
        }

        public static Language getLanguageForIANA(String iana)
        {
            return (Language)langMapIANA.get(iana);
        }
        
        public static Language getLanguageForISO639_1(String iso)
        {
            return (Language)langMapISO639_1.get(iso);
        }

        public static Language getLanguageForISO639_2(String iso)
        {
            return (Language)langMapISO639_2.get(iso);
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
