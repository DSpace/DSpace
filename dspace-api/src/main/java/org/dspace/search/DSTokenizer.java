/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;

/**
 * Customized Lucene Tokenizer, since the standard one rejects numbers from
 * indexing/querying.
 */
public final class DSTokenizer extends CharTokenizer
{
    /**
     * Construct a new LowerCaseTokenizer.
     */
    public DSTokenizer(Reader in)
    {
        super(in);
    }

    /**
     * Collects only characters which satisfy {@link Character#isLetter(char)}.
     */
    protected char normalize(char c)
    {
        return Character.toLowerCase(c);
    }

    /**
     * Collects only characters which do not satisfy
     * {@link Character#isWhitespace(char)}.
     */
    protected boolean isTokenChar(char c)
    {
        return Character.isLetterOrDigit(c);
    }
}
