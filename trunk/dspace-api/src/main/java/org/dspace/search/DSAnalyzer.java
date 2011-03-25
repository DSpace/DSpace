/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.dspace.core.ConfigurationManager;

/**
 * Custom Lucene Analyzer that combines the standard filter, lowercase filter,
 * stemming and stopword filters.
 */
public class DSAnalyzer extends Analyzer
{
    /*
     * An array containing some common words that are not usually useful for
     * searching.
     */
    private static final String[] STOP_WORDS =
    {

    // new stopwords (per MargretB)
            "a", "am", "and", "are", "as", "at", "be", "but", "by", "for",
            "if", "in", "into", "is", "it", "no", "not", "of", "on", "or",
            "the", "to", "was"
    // old stopwords (Lucene default)
    /*
     * "a", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in",
     * "into", "is", "it", "no", "not", "of", "on", "or", "s", "such", "t",
     * "that", "the", "their","then", "there","these", "they", "this", "to",
     * "was", "will", "with"
     */
    };

    /*
     * Stop table
     */
    private static final Set stopSet = StopFilter.makeStopSet(STOP_WORDS);

    /*
     * Create a token stream for this analyzer.
     */
    public final TokenStream tokenStream(String fieldName, final Reader reader)
    {
        TokenStream result = new DSTokenizer(reader);

        result = new StandardFilter(result);
        result = new LowerCaseFilter(result);
        result = new StopFilter(result, stopSet);
        result = new PorterStemFilter(result);

        return result;
    }

    public int getPositionIncrementGap(String fieldName)
    {
        // If it is the default field, or bounded fields is turned off in the config, return the default value
        if ("default".equalsIgnoreCase(fieldName) || !ConfigurationManager.getBooleanProperty("search.boundedfields", false))
        {
            return super.getPositionIncrementGap(fieldName);
        }

        // Not the default field, and we want bounded fields, so return an large gap increment
        return 10;
    }
}
