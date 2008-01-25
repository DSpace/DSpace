/*
 * InitialArticleWord.java
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

/**
 * Abstract class for implementing initial article word filters
 * Allows you to create new classes with their own rules for mapping
 * languages to article word lists.
 * 
 * @author Graham Triggs
 */
public abstract class InitialArticleWord implements TextFilter
{
    /**
     * When no language is passed, use null and let implementation decide what to do
     */
    public String filter(String str)
    {
        return filter(str, null);
    }
    
    /**
     * Do an initial definite/indefinite article filter on the passed string.
     * On matching an initial word, can strip or move to the end, depending on the 
     * configuration of the implementing class.
     * 
     * @param str  The string to parse
     * @param lang The language of the passed string
     * @return String The filtered string
     */
    public String filter(String str, String lang)
    {
        // Get the list of article words for this language
        String[] articleWordArr = getArticleWords(lang);

        // If we have an article word array, process the string
        if (articleWordArr != null && articleWordArr.length > 0)
        {
            String initialArticleWord = null;
            int curPos =  0;
            int initialStart = -1;
            int initialEnd   = -1;
            
            // Iterate through the characters until we find something significant, or hit the end
            while (initialEnd < 0 && curPos < str.length())
            {
                // Have we found a significant character
                if (Character.isLetterOrDigit(str.charAt(curPos)))
                {
                    // Mark this as the cut point for the initial word
                    initialStart = curPos;
                    
                    // Loop through the article words looking for a match
                    for (int idx = 0; initialEnd < 0 && idx < articleWordArr.length; idx++)
                    {
                        // Extract a fragment from the string to test
                        // Must be same length as the article word
                        if (idx > 1 && initialArticleWord != null)
                        {
                            // Only need to do so if we haven't already got one
                            // of the right length
                            if (initialArticleWord.length() != articleWordArr[idx].length())
                                initialArticleWord = extractText(str, curPos, articleWordArr[idx].length());
                        }
                        else
                        {
                            initialArticleWord = extractText(str, curPos, articleWordArr[idx].length());
                        }

                        // Does the fragment match an article word?
                        if (initialArticleWord!= null && initialArticleWord.equalsIgnoreCase(articleWordArr[idx]))
                        {
                            // Check to see if the next character in the source
                            // is a whitespace
                            boolean isNextWhitespace = Character.isWhitespace(
                                    str.charAt(curPos + articleWordArr[idx].length())
                                );
                            
                            // Check to see if the last character of the article word is a letter or digit
                            boolean endsLetterOrDigit = Character.isLetterOrDigit(initialArticleWord.charAt(initialArticleWord.length() - 1));
                         
                            // If the last character of the article word is  a letter or digit,
                            // then it must be followed by whitespace, if not, it can be anything
                            // Setting endPos signifies that we have found an article word
                            if (endsLetterOrDigit && isNextWhitespace)
                                initialEnd = curPos + initialArticleWord.length();
                            else if (!endsLetterOrDigit)
                                initialEnd = curPos + initialArticleWord.length();
                        }
                    }

                    // Quit the loop, as we have a significant character
                    break;
                }
                
                // Keep going
                curPos++;
            }
            
            // If endPos is positive, then we've found an article word
            if (initialEnd > 0)
            {
                // Find a cut point in the source string, removing any whitespace after the article word
                int cutPos = initialEnd;
                while (cutPos < str.length() && Character.isWhitespace(str.charAt(cutPos)))
                    cutPos++;
                
                // Are we stripping the article word?
                if (stripInitialArticle)
                {
                    // Yes, simply return everything after the cut
                    return str.substring(cutPos);
                }
                else
                {
                    // No - move the initial article word to the end
                    return new StringBuffer(str.substring(cutPos))
                                        .append(wordSeperator)
                                        .append(str.substring(initialStart, initialEnd))
                                        .toString();
                }
            }
        }
        
        // Didn't do any processing, or didn't find an initial article word
        // Return the original string
        return str;
    }
    
    protected InitialArticleWord(boolean stripWord)
    {
        stripInitialArticle = stripWord;
    }
    
    protected InitialArticleWord()
    {
        stripInitialArticle = false;
    }

    /**
     * Abstract method to get the list of words to use in the initial word filter
     * 
     * @param lang The language to retrieve article words for
     * @return An array of definite/indefinite article words
     */
    protected abstract String[] getArticleWords(String lang);

    // Seperator to use when appending article to end
    private String wordSeperator = ", ";

    // Flag to signify initial article word should be removed
    // If false, then the initial article word is appended to the end
    private boolean stripInitialArticle = false;
    
    /**
     * Helper method to extract text from a string.
     * Ensures that there is significant data (ie. non-whitespace)
     * after the segment requested.
     * 
     * @param str
     * @param pos
     * @param len
     * @return
     */
    private String extractText(String str, int pos, int len)
    {
        int testPos = pos + len;
        while (testPos < str.length() && Character.isWhitespace(str.charAt(testPos)))
            testPos++;
        
        if (testPos < str.length())
            return str.substring(pos, pos + len);
        
        return null;
    }
}
