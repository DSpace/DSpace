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
 */
public abstract class EmailParser {

	private static Logger LOGGER = LoggerFactory.getLogger(EmailParser.class);

	public abstract ParsingResult parseMessage(List<String> aMessage);

	protected static String getStrippedText(String aInputString) {
		SAXBuilder builder = new SAXBuilder();
		// We have to replace characters used for XML syntax with entity refs
		String text = aInputString.replace("&", "&amp;").replace("<", "&lt;")
				.replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&apos;");

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
        String[] authorArray = null;
        if (hasSemicolons){
            authorArray = authors.split(";");
            for (int i = 0; i< authorArray.length; i++){
                authorArray[i] = getStrippedText(StringUtils.stripToEmpty(authorArray[i]));
            }
        }
        else {
            authorArray = authors.split(",");
            for (int i = 0; i< authorArray.length-1; i++){
                authorArray[i] = flipName(getStrippedText(StringUtils.stripToEmpty(authorArray[i])));
            }
            int lastAuthor = authorArray.length-1;
            if (lastAuthor > 0 && authorArray[lastAuthor].startsWith("and "))
                authorArray[lastAuthor] =flipName(getStrippedText(StringUtils.stripToEmpty(authorArray[lastAuthor].substring(4))));
            else
                authorArray[lastAuthor] = StringUtils.stripToEmpty(flipName(getStrippedText(authorArray[lastAuthor])));       
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
