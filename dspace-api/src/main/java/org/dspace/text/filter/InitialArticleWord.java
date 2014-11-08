/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
    @Override
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
    @Override
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
                            {
                                initialArticleWord = extractText(str, curPos, articleWordArr[idx].length());
                            }
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
                            {
                                initialEnd = curPos + initialArticleWord.length();
                            }
                            else if (!endsLetterOrDigit)
                            {
                                initialEnd = curPos + initialArticleWord.length();
                            }
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
                {
                    cutPos++;
                }
                
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
                                        .append(wordSeparator)
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

    // Separator to use when appending article to end
    private String wordSeparator = ", ";

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
        {
            testPos++;
        }
        
        if (testPos < str.length())
        {
            return str.substring(pos, pos + len);
        }
        
        return null;
    }
}
