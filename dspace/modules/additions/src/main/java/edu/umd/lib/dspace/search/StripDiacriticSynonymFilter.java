/*
 * Copyright (c) 2006 The University of Maryland. All Rights Reserved.
 * 
 

package edu.umd.lib.dspace.search;

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import sun.text.normalizer.NormalizerImpl;

import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;

 *********************************************************************
 * If a term contains diacritics make a synonym for the term with the diacritics
 * stripped.
 * 
 * @author Ben Wallberg
 ********************************************************************

public class StripDiacriticSynonymFilter extends TokenFilter
{

    public static final String TYPE = "synonym-nodiacritic";

    private Token save = null;

    private final UnicodeSet nx = NormalizerImpl.getNX(0);

 ******************************************* StripDiacriticSynonymFilter *
 *
 * Constructor.
     

    public StripDiacriticSynonymFilter(TokenStream in)
    {
        super(in);
    }

 ***************************************************************** next 
    /**
 * Get the next token.
     

    public final Token next() throws IOException
    {
        // Check for synonym needing injection into the stream
        if (save != null)
        {
            Token ret = save;
            save = null;
            return ret;
        }

        // Get the next token
        Token token = input.next();

        // Check for end of stream
        if (token == null)
        {
            return null;
        }

        // Get a decomposed form of the text
        StringBuffer sb = new StringBuffer(Normalizer.normalize(
                token.termText(), Normalizer.NFD));
        // StringBuffer sb = decompose(token.termText());

        // Check for diacritics and strip them
        boolean bDiacritic = false;
        for (int i = 0; i < sb.length();)
        {
            char ch = sb.charAt(i);

            if (Character.UnicodeBlock.of(ch).equals(
                    Character.UnicodeBlock.COMBINING_DIACRITICAL_MARKS))
            {
                bDiacritic = true;
                sb.deleteCharAt(i);
            }
            else
            {
                i++;
            }
        }

        // Make a synonym with diacritics stripped
        if (bDiacritic)
        {
            save = new Token(sb.toString(), token.startOffset(),
                    token.endOffset(), TYPE);
            save.setPositionIncrement(0);
        }

        return token;
    }

 ************************************************************ decompose *
    
 * Decompose Unicode characters in composed form. This code does what
 * Normalizer.normalize() does but bypasses several layers of method call.
 * It's not clear the performance improvement is significant but it's here
 * so let's use it.
     

    private final StringBuffer decompose(String str)
    {

        char[] dest = new char[str.length() * 3];
        int[] trailCC = new int[1];
        int destSize = 0;
        for (;;)
        {
            destSize = NormalizerImpl.decompose(str.toCharArray(), 0,
                    str.length(), dest, 0, dest.length, false, trailCC, nx);
            if (destSize <= dest.length)
            {
                StringBuffer sb = new StringBuffer(destSize);
                sb.append(dest, 0, destSize);
                return sb;
            }
            else
            {
                dest = new char[destSize];
            }
        }
    }

    @Override
    public boolean incrementToken() throws IOException
    {
        // TODO Auto-generated method stub
        return false;
    }

}

 */
