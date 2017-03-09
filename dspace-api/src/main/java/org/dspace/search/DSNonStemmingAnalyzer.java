/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import java.io.Reader;

import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.util.Version;

/**
 * Custom Lucene Analyzer that combines the standard filter, lowercase filter
 * and stopword filter. Intentionally omits the stemming filter (which is used
 * by DSAnalyzer)
 */
public class DSNonStemmingAnalyzer extends DSAnalyzer
{
    /**
     * Builds an analyzer
     *
     * @param matchVersion Lucene version to match
     */
    public DSNonStemmingAnalyzer(Version matchVersion) {
        super(matchVersion);
    }

    /**
     * Create a token stream for this analyzer.
     * This is identical to DSAnalyzer, except it omits the stemming filter
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        final Tokenizer source = new DSTokenizer(matchVersion,  reader);
        TokenStream result = new StandardFilter(matchVersion, source);

        result = new LowerCaseFilter(matchVersion, result);
        result = new StopFilter(matchVersion, result, stopSet);

        return new TokenStreamComponents(source, result);
    }
}
