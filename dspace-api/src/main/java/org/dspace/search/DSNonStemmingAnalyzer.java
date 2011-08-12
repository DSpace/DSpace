/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import java.io.Reader;

import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;

/**
 * Custom Lucene Analyzer that combines the standard filter, lowercase filter
 * and stopword filter. Intentionally omits the stemming filter (which is used
 * by DSAnalyzer)
 */
public class DSNonStemmingAnalyzer extends DSAnalyzer
{
    /**
     * Create a token stream for this analyzer.
     * This is identical to DSAnalyzer, except it omits the stemming filter
     */
    @Override
    public TokenStream tokenStream(String fieldName, final Reader reader)
    {
        TokenStream result = new DSTokenizer(reader);

        result = new StandardFilter(result);
        result = new LowerCaseFilter(result);
        result = new StopFilter(result, stopSet);

        return result;
    }
}
