/*
 * MARC21InitialArticleWord.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dspace.core.ConfigurationManager;

/**
 * Implements MARC 21 standards to disregard initial
 * definite or indefinite article in sorting.
 * 
 * Note: This only works for languages defined with IANA code entries.
 * 
 * @author Graham Triggs
 */
public class MARC21InitialArticleWord extends InitialArticleWord
{
    public MARC21InitialArticleWord()
    {
        // Default behaviour is to strip the initial word completely
        super(true);
    }

    public MARC21InitialArticleWord(boolean stripWord)
    {
        super(stripWord);
    }

    /**
     * Return the list of definite and indefinite article codes
     * for this language.
     */
    protected String[] getArticleWords(String lang)
    {
        // No language - no words
        if (StringUtils.isEmpty(lang))
            return defaultWords;

        Language l = Language.getLanguage(lang);
        
        // Is the lanugage in our map?
        if (l != null && ianaArticleMap.containsKey(l.IANA))
        {
            // Get the list of words for this language
            ArticlesForLang articles = (ArticlesForLang)ianaArticleMap.get(l.IANA);
            
            if (articles != null)
                return articles.words;
        }
        
        return null;
    }
    
    // Mapping of IANA codes to article word lists
    private static Map ianaArticleMap = new HashMap();

    private static String[] defaultWords = null;

    // Static initialisation - convert word -> languages map 
    // into language -> words map
    static
    {
        /* Define a mapping for article words to the languages that have them.
         * Take from: http://www.loc.gov/marc/bibliographic/bdapp-e.html
         */
        Object[][] articleWordArray = {
            { "a",          Language.ENGLISH, Language.GALICIAN, Language.HUNGARIAN, Language.PORTUGUESE, Language.ROMANIAN, Language.SCOTS, Language.YIDDISH },
            { "a'",         Language.SCOTTISH_GAELIC },
            { "al",         Language.ROMANIAN },
            { "al-",        Language.ARABIC, Language.BALUCHI, Language.BRAHUI, Language.PANJABI, Language.PERSIAN, Language.TURKISH, Language.URDU },
            { "am",         Language.SCOTTISH_GAELIC },
            { "an",         Language.ENGLISH, Language.IRISH, Language.SCOTS, Language.SCOTTISH_GAELIC, Language.YIDDISH },
            { "an t-",      Language.IRISH, Language.SCOTTISH_GAELIC },
            { "ane",        Language.SCOTS },
            { "ang",        Language.TAGALOG },
            { "ang mga",    Language.TAGALOG },
            { "as",         Language.GALICIAN, Language.PORTUGUESE },
            { "az",         Language.HUNGARIAN },
            { "bat",        Language.BASQUE },
            { "bir",        Language.TURKISH },
            { "d'",         Language.ENGLISH },
            { "da",         Language.SHETLAND_ENGLISH },
            { "das",        Language.GERMAN },
            { "de",         Language.DANISH, Language.DUTCH, Language.ENGLISH, Language.FRISIAN, Language.NORWEGIAN, Language.SWEDISH },
            { "dei",        Language.NORWEGIAN },
            { "dem",        Language.GERMAN },
            { "den",        Language.DANISH, Language.GERMAN, Language.NORWEGIAN, Language.SWEDISH },
            { "der",        Language.GERMAN, Language.YIDDISH },
            { "des",        Language.GERMAN, Language.WALLOON },
            { "det",        Language.DANISH, Language.NORWEGIAN, Language.SWEDISH },
            { "di",         Language.YIDDISH },
            { "die",        Language.AFRIKAANS, Language.GERMAN, Language.YIDDISH },
            { "dos",        Language.YIDDISH },
            { "e",          Language.NORWEGIAN },
            { "e",          Language.FRISIAN },         // should be 'e - leading apostrophes are ignored
            { "een",        Language.DUTCH },
            { "eene",       Language.DUTCH },
            { "egy",        Language.HUNGARIAN },
            { "ei",         Language.NORWEGIAN },
            { "ein",        Language.GERMAN, Language.NORWEGIAN, Language.WALLOON },
            { "eine",       Language.GERMAN },
            { "einem",      Language.GERMAN },
            { "einen",      Language.GERMAN },
            { "einer",      Language.GERMAN },
            { "eines",      Language.GERMAN },
            { "eit",        Language.NORWEGIAN },
            { "el",         Language.CATALAN, Language.SPANISH },
            { "el-",        Language.ARABIC },
            { "els",        Language.CATALAN },
            { "en",         Language.CATALAN, Language.DANISH, Language.NORWEGIAN, Language.SWEDISH },
            { "enne",       Language.WALLOON },
            { "et",         Language.DANISH, Language.NORWEGIAN },
            { "ett",        Language.SWEDISH },
            { "eyn",        Language.YIDDISH },
            { "eyne",       Language.YIDDISH },
            { "gl'",        Language.ITALIAN },
            { "gli",        Language.PROVENCAL },
            { "ha-",        Language.HEBREW },
            { "hai",        Language.CLASSICAL_GREEK, Language.GREEK },
            { "he",         Language.HAWAIIAN },
            { "h\u0113",    Language.CLASSICAL_GREEK, Language.GREEK }, // e macron
            { "he-",        Language.HEBREW },
            { "heis",       Language.GREEK },
            { "hen",        Language.GREEK },
            { "hena",       Language.GREEK },
            { "henas",      Language.GREEK },
            { "het",        Language.DUTCH },
            { "hin",        Language.ICELANDIC },
            { "hina",       Language.ICELANDIC },
            { "hinar",      Language.ICELANDIC },
            { "hinir",      Language.ICELANDIC },
            { "hinn",       Language.ICELANDIC },
            { "hinna",      Language.ICELANDIC },
            { "hinnar",     Language.ICELANDIC },
            { "hinni",      Language.ICELANDIC },
            { "hins",       Language.ICELANDIC },
            { "hinu",       Language.ICELANDIC },
            { "hinum",      Language.ICELANDIC },
            { "hi\u01d2",   Language.ICELANDIC },
            { "ho",         Language.CLASSICAL_GREEK, Language.GREEK },
            { "hoi",        Language.CLASSICAL_GREEK, Language.GREEK },
            { "i",          Language.ITALIAN },
            { "ih'",        Language.PROVENCAL },
            { "il",         Language.ITALIAN, Language.PROVENCAL_OCCITAN },
            { "il-",        Language.MALTESE },
            { "in",         Language.FRISIAN },
            { "it",         Language.FRISIAN },
            { "ka",         Language.HAWAIIAN },
            { "ke",         Language.HAWAIIAN },
            { "l'",         Language.CATALAN, Language.FRENCH, Language.ITALIAN, Language.PROVENCAL_OCCITAN, Language.WALLOON },
            { "l-",         Language.MALTESE },
            { "la",         Language.CATALAN, Language.ESPERANTO, Language.FRENCH, Language.ITALIAN, Language.PROVENCAL_OCCITAN, Language.SPANISH },
            { "las",        Language.PROVENCAL_OCCITAN, Language.SPANISH },
            { "le",         Language.FRENCH, Language.ITALIAN, Language.PROVENCAL_OCCITAN },
            { "les",        Language.CATALAN, Language.FRENCH, Language.PROVENCAL_OCCITAN, Language.WALLOON },
            { "lh",         Language.PROVENCAL_OCCITAN },
            { "lhi",        Language.PROVENCAL_OCCITAN },
            { "li",         Language.PROVENCAL_OCCITAN },
            { "lis",        Language.PROVENCAL_OCCITAN },
            { "lo",         Language.ITALIAN, Language.PROVENCAL_OCCITAN, Language.SPANISH },
            { "los",        Language.PROVENCAL_OCCITAN, Language.SPANISH },
            { "lou",        Language.PROVENCAL_OCCITAN },
            { "lu",         Language.PROVENCAL_OCCITAN },
            { "mga",        Language.TAGALOG },
            { "m\u0303ga",  Language.TAGALOG },
            { "mia",        Language.GREEK },
            { "n",          Language.AFRIKAANS, Language.DUTCH, Language.FRISIAN },              // should be 'n - leading apostrophes are ignored
            { "na",         Language.HAWAIIAN, Language.IRISH, Language.SCOTTISH_GAELIC },
            { "na h-",      Language.IRISH, Language.SCOTTISH_GAELIC },
            { "nje",        Language.ALBANIAN },
            { "ny",         Language.MALAGASY },
            { "o",          Language.NEAPOLITAN_ITALIAN },               // should be 'o - leading apostrophes are ignored
            { "o",          Language.GALICIAN, Language.HAWAIIAN, Language.PORTUGUESE, Language.ROMANIAN },
            { "os",         Language.PORTUGUESE },
            { "r",          Language.ICELANDIC },                // should be 'r - leading apostrophes are ignored
            { "s",          Language.GERMAN },                   // should be 's - leading apostrophes are ignored
            { "sa",         Language.TAGALOG },
            { "sa mga",     Language.TAGALOG },
            { "si",         Language.TAGALOG },
            { "sin\u00e1",  Language.TAGALOG },
            { "t",          Language.DUTCH, Language.FRISIAN },          // should be 't - leading apostrophes are ignored
            { "ta",         Language.CLASSICAL_GREEK, Language.GREEK },
            { "tais",       Language.CLASSICAL_GREEK },
            { "tas",        Language.CLASSICAL_GREEK },
            { "t\u0113",    Language.CLASSICAL_GREEK },    // e macron
            { "t\u0113n",   Language.CLASSICAL_GREEK, Language.GREEK },   // e macron
            { "t\u0113s",   Language.CLASSICAL_GREEK, Language.GREEK },   // e macron
            { "the",        Language.ENGLISH },
            { "t\u014d",    Language.CLASSICAL_GREEK, Language.GREEK }, // o macron
            { "tois",       Language.CLASSICAL_GREEK },
            { "t\u014dn",   Language.CLASSICAL_GREEK, Language.GREEK }, // o macron
            { "tou",        Language.CLASSICAL_GREEK, Language.GREEK },
            { "um",         Language.PORTUGUESE },
            { "uma",        Language.PORTUGUESE },
            { "un",         Language.CATALAN, Language.FRENCH, Language.ITALIAN, Language.PROVENCAL_OCCITAN, Language.ROMANIAN, Language.SPANISH },
            { "un'",        Language.ITALIAN },
            { "una",        Language.CATALAN, Language.ITALIAN, Language.PROVENCAL_OCCITAN, Language.SPANISH },
            { "une",        Language.FRENCH },
            { "unei",       Language.ROMANIAN },
            { "unha",       Language.GALICIAN },
            { "uno",        Language.ITALIAN, Language.PROVENCAL_OCCITAN },
            { "uns",        Language.PROVENCAL_OCCITAN },
            { "unui",       Language.ROMANIAN },
            { "us",         Language.PROVENCAL_OCCITAN },
            { "y",          Language.WELSH },
            { "ye",         Language.ENGLISH },
            { "yr",         Language.WELSH }
        };

        // Initialize the lang -> article map
        ianaArticleMap = new HashMap();

        int wordIdx = 0;
        int langIdx = 0;

        // Iterate through word/language array
        // Generate temporary language map
        Map langWordMap = new HashMap();
        for (wordIdx = 0; wordIdx < articleWordArray.length; wordIdx++)
        {
            for (langIdx = 1; langIdx < articleWordArray[wordIdx].length; langIdx++)
            {
                Language lang = (Language)articleWordArray[wordIdx][langIdx];

                if (lang != null && lang.IANA.length() > 0)
                {
                    List words = (List)langWordMap.get(lang);
                    
                    if (words == null)
                    {
                        words = new ArrayList();
                        langWordMap.put(lang, words);
                    }
                    
                    // Add language to list if we haven't done so already
                    if (!words.contains(articleWordArray[wordIdx][0]))
                        words.add(articleWordArray[wordIdx][0]);
                }
            }
        }
        
        // Iterate through languages
        Iterator langIter = langWordMap.keySet().iterator();
        while (langIter.hasNext())
        {
            Language lang = (Language)langIter.next();
            List wordList = (List)langWordMap.get(lang);

            // Convert the list into an array of strings
            String[] words = new String[wordList.size()];
            
            for (int idx = 0; idx < wordList.size(); idx++)
                words[idx] = (String)wordList.get(idx);

            // Sort the array into length order - longest to shortest
            // This ensures maximal matching on the article words
            Arrays.sort(words, new MARC21InitialArticleWord.InverseLengthComparator() );
            
            // Add language/article entry to map
            ianaArticleMap.put(lang.IANA, new MARC21InitialArticleWord.ArticlesForLang(lang, words));
        }

        // Setup default stop words for null languages
        String defaultLangs = ConfigurationManager.getProperty("marc21wordfilter.defaultlang");
        if (!StringUtils.isEmpty(defaultLangs))
        {
            String[] langArr = defaultLangs.split("[, ]+");
            if (langArr != null && langArr.length > 0)
            {
                int wordCount = 0;
                ArticlesForLang[] afl = new ArticlesForLang[langArr.length];

                for (int idx = 0; idx < afl.length; idx++)
                {
                    Language l = Language.getLanguage(langArr[idx]);
                    if (l != null && ianaArticleMap.containsKey(l.IANA))
                    {
                        afl[idx] = (ArticlesForLang)ianaArticleMap.get(l.IANA);
                        if (afl[idx] != null)
                        {
                            wordCount += afl[idx].words.length;
                        }
                    }
                }

                if (wordCount > 0)
                {
                    int destPos = 0;
                    defaultWords = new String[wordCount];
                    for (int idx = 0; idx < afl.length; idx++)
                    {
                        if (afl[idx] != null)
                        {
                            System.arraycopy(afl[idx].words, 0, defaultWords, destPos, afl[idx].words.length);
                            destPos += afl[idx].words.length;
                        }
                    }
                }
            }
        }
    }

    // Wrapper class for inserting word arrays into a map
    private static class ArticlesForLang
    {
        final Language lang;
        final String[] words;
        
        ArticlesForLang(Language lang, String[] words)
        {
           this.lang  = lang;
           this.words = words;
        }
    }
    
    // Compare strings according to their length - longest to shortest
    private static class InverseLengthComparator implements Comparator
    {
        public int compare(Object arg0, Object arg1)
        {
            return ((String)arg1).length() - ((String)arg0).length(); 
        };
        
    };
}
