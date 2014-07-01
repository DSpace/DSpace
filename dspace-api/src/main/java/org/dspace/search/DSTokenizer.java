/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import java.io.Reader;

import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.Version;

/**
 * Customized Lucene Tokenizer, since the standard one rejects numbers from
 * indexing/querying.
 * 
 * @deprecated Since DSpace 4 the system use an abstraction layer named
 *             Discovery to provide access to different search provider. The
 *             legacy system build upon Apache Lucene is likely to be removed in
 *             a future version. If you are interested in use Lucene as backend
 *             for the DSpace search system please consider to build a Lucene
 *             implementation of the Discovery interfaces
 */
@Deprecated
public final class DSTokenizer extends CharTokenizer
{
    /**
     * Construct a new LowerCaseTokenizer.
     * @param version Lucene version number
     */
    public DSTokenizer(Version version, Reader in)
    {
        super(version, in);
    }

    /**
     * Collects only characters which satisfy {@link Character#isLetter(char)}.
     */
    @Override
    protected int normalize(int c) {
        return super.normalize(Character.toLowerCase(c));
    }

    /**
     * Collects only characters which do not satisfy
     * {@link Character#isWhitespace(char)}.
     */
    @Override
    protected boolean isTokenChar(int c)
    {
        return Character.isLetterOrDigit(c);
    }
}
