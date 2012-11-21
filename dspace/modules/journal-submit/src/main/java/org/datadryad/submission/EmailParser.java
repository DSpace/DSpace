package org.datadryad.submission;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The interface of submission e-mail parsing classes
 * 
 * @author Akio Sone
 * @author Kevin S. Clarke
 * @author Peter E. Midford
 * @author Dan Leehr
 */
public abstract class EmailParser {

	private static Logger LOGGER = LoggerFactory.getLogger(EmailParser.class);

	public abstract ParsingResult parseMessage(List<String> aMessage);

        /**
         * Strips characters that can't be output to XML.  Implementation from
         * http://blog.mark-mclaren.info/2007/02/invalid-xml-characters-when-valid-utf8_5873.html.
         * In 
         * @param inputString string to sanitize
         * @return String after removing certain control characters and other 
         * characters that cause XML building to fail.
         * 
         * Prior to stripping the non valid XML characters, issues were seen when 
         * decoding special characters from quoted-printable encoded messages.  
         * The control characters (e.g. 0xB) that prefix the specials were still
         * in the String.
         * 
         * This method strips out these control characters, as well as others
         * that may prevent XML output.
         */
        private static String stripNonValidXMLCharacters(String inputString) {
            if(inputString == null || inputString.length() == 0) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            char c;
            // Check for XML-valid characters
            for (int i=0; i < inputString.length(); i++) {
                c = inputString.charAt(i);
                if ((c == 0x9) ||
                    (c == 0xA) ||
                    (c == 0xD) ||
                    ((c >= 0x20) && (c <= 0xD7FF)) ||
                    ((c >= 0xE000) && (c <= 0xFFFD)) ||
                    ((c >= 0x10000) && (c <= 0x10FFFF))) {
                    builder.append(c);
                }            
            }
            return builder.toString();
        }
        
	protected static String getStrippedText(String aInputString) {
		SAXBuilder builder = new SAXBuilder();
		// We have to replace characters used for XML syntax with entity refs
		String text = aInputString.replace("&", "&amp;").replace("<", "&lt;")
				.replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&apos;");
                
                text = stripNonValidXMLCharacters(text);

		// Check that we have well-formed XML
		try {
			Reader reader = new StringReader("<s>" + text + "</s>");
			builder.build(reader).getRootElement().getValue();
			
			// If we do, though, use our text string which has the entity refs;
			// they don't come out of our getValue() call above...
			return text.replaceAll("\\s+", " ");
		}
		catch (Exception details) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("The following couldn't be parsed: \n" + text
                                    + "\n" + details.getMessage());
                    }

                    return aInputString; // just return what we're given if problems
		}
	}

	public static String flipName(String aName) {
		if (aName == null || aName.trim().equals("") || aName.contains(",")) {
			return aName;
		}

		// a very simplistic first pass at this... you'd think there'd be a
		// OSS name parser out there already, but I'm not finding one...
		String[] parts = aName.split("\\s+");
		StringBuilder builder = new StringBuilder(parts[parts.length - 1]);

		builder.append(", ");

		for (int index = 0; index < parts.length - 1; index++) {
			builder.append(parts[index]).append(' ');
		}

		return builder.toString();
	}
	
	/**
     * A couple of higher level processing methods that handle common variants of keyword, author and corresponding authors
	 */

	/**
	 * Keyword lists generally come as either keyword phrase, keyword phrase,... 
	 * or as Major phrase, minor phrase; Major phrase, minor phrase
	 * but occasionally keyword phrase; keyword phrase
	 */
	public static String[] processKeywordList(String keywords){
	    final boolean hasSemicolons = keywords.contains(";");
	    String[] keywordArray = null;
	    if (hasSemicolons){
            keywordArray = keywords.split(";");
        }
        else //is this sufficient - will either split at commas or return singleton string
            keywordArray = keywords.split(",");
	    for (int i = 0; i< keywordArray.length; i++)
	        keywordArray[i] = getStrippedText(StringUtils.stripToEmpty(keywordArray[i]));
	    return keywordArray;
	}
	
	/**
	 * Author lists general come as either last, first; last, first
	 * or as first last, first last and first last (sometimes with the Oxford comma before the 'and' token)
	 */
	public static String[] processAuthorList(String authors){
	    final boolean hasSemicolons = authors.contains(";");
	    final boolean hasCommas = authors.contains(",");
	    final boolean hasLineBreaks = authors.contains("\n");
        String[] authorArray = null;
        if (hasSemicolons){
            authorArray = authors.split(";");
            if (hasCommas){
                for (int i = 0; i< authorArray.length; i++){
                    authorArray[i] = getStrippedText(StringUtils.stripToEmpty(authorArray[i]));
                }
            }
            else{
                for (int i = 0; i< authorArray.length; i++){
                    authorArray[i] = StringUtils.stripToEmpty(flipName(getStrippedText(StringUtils.stripToEmpty(authorArray[i]))));
                }
            }
        }
        else if (hasCommas){
            authorArray = authors.split(",");
            // distinguish single author from author list
            if (authorArray.length > 2 || StringUtils.stripToEmpty(authorArray[0]).contains(" ")){
                for (int i = 0; i< authorArray.length-1; i++){
                    authorArray[i] = StringUtils.stripToEmpty(flipName(getStrippedText(StringUtils.stripToEmpty(authorArray[i]))));
                }
                int lastAuthor = authorArray.length-1;
                if (lastAuthor > 0 && StringUtils.stripToEmpty(authorArray[lastAuthor]).startsWith("and "))
                    authorArray[lastAuthor] = StringUtils.stripToEmpty(flipName(getStrippedText(StringUtils.stripToEmpty(authorArray[lastAuthor]).substring(4))));
                else
                    authorArray[lastAuthor] = StringUtils.stripToEmpty(flipName(getStrippedText(authorArray[lastAuthor])));  
            }
            else{
                authorArray = new String[1];
                authorArray[0] = authors;
            }
        }
        else if (hasLineBreaks){
            authorArray = authors.split("\n");
            for (int i = 0; i< authorArray.length; i++){
                authorArray[i] = StringUtils.stripToEmpty(flipName(getStrippedText(StringUtils.stripToEmpty(authorArray[i]))));
            }
            authorArray[0] = "found line breaks";
        }
        else {  //either a single author (first last) or two authors (first last and first last)
            authorArray = authors.split(" and ");
            for (int i = 0; i< authorArray.length; i++){
                authorArray[i] = StringUtils.stripToEmpty(flipName(getStrippedText(StringUtils.stripToEmpty(authorArray[i]))));
            }
        }
        return authorArray;
	}
	
	/**
	 * Corresponding author - the element is fairly involved, but perhaps keeping the name field unaltered will suffice?
	 */

	public static String processCorrespondingAuthorName(String authorName){
	    return StringUtils.stripToEmpty(authorName);
	}

}
