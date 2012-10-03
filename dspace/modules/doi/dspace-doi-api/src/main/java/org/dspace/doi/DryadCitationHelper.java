package org.dspace.doi;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.dspace.content.DCValue;
import org.dspace.content.Item;

/**
 * Convenience methods for outputting Dryad styled citations. Consolidates code
 * that I've rewritten in a several different places so far into one place.
 * 
 * @author Kevin S. Clarke <ksclarke@gmail.com>
 * 
 */
@SuppressWarnings("deprecation")
public class DryadCitationHelper {

	public static String[] getAuthors(Item aItem) {
		ArrayList<String> authors = new ArrayList<String>();

		authors.addAll(getAuthors(aItem.getMetadata("dc.contributor.author")));
		authors.addAll(getAuthors(aItem.getMetadata("dc.creator")));
		authors.addAll(getAuthors(aItem.getMetadata("dc.contributor")));

		return authors.toArray(new String[authors.size()]);
	}

	public static String getYear(Item aItem) {
		for (DCValue date : aItem.getMetadata("dc.date.issued")) {
			return date.value.substring(0, 4);
		}
		
		return null;
	}
	
	public static String[] getKeywords(Item aItem) {
		ArrayList<String> keywordList = new ArrayList<String>();
		
		for (DCValue keyword : aItem.getMetadata("dc.subject")) {
			if (keyword.value.length() < 255) {
				keywordList.add(keyword.value);
			}
		}
		
		for (DCValue keyword : aItem.getMetadata("dwc.ScientificName")) {
			if (keyword.value.length() < 255) {
				keywordList.add(keyword.value);
			}
		}
		
		return keywordList.toArray(new String[keywordList.size()]);
	}
	
	public static String getJournalName(Item aItem) {
		for (DCValue name : aItem.getMetadata("prism.publicationName")) {
			return name.value;
		}
		
		return null;
	}
	
	public static String[] getDate(Item aItem) {
		StringTokenizer tokenizer;
		
		for (DCValue date : aItem.getMetadata("dc.date.issued")) {
			tokenizer = new StringTokenizer(date.value, "-/ T");
			String[] dateParts = new String[tokenizer.countTokens()];
			
			for (int index = 0; index < dateParts.length; index++) {
				dateParts[index] = tokenizer.nextToken();
			}
			
			return dateParts;
		}
		
		return new String[0];
	}
	
	public static String getAbstract(Item aItem) {
		for (DCValue abstrakt : aItem.getMetadata("dc.description")) {
			return abstrakt.value;
		}
		
		return null;
	}
	
	public static String getTitle(Item aItem) {
		for (DCValue title : aItem.getMetadata("dc.title")) {
			if (title.value.startsWith("Data from: ")) {
				return title.value;
			}
		}
		
		return null;
	}
	
	public static String getDOI(Item aItem) {
		for (DCValue id : aItem.getMetadata("dc.identifier")) {
			if (id.value.startsWith("doi:") && id.value.contains("dryad.")) {
				return id.value;
			}
		}
		
		return null;
	}
	
	private static List<String> getAuthors(DCValue[] aMetadata) {
		ArrayList<String> authors = new ArrayList<String>();
		StringTokenizer tokenizer;

		for (DCValue metadata : aMetadata) {
			StringBuilder builder = new StringBuilder();

			if (metadata.value.indexOf(",") != -1) {
				String[] parts = metadata.value.split(",");

				if (parts.length > 1) {
					tokenizer = new StringTokenizer(parts[1], ". ");
					builder.append(parts[0]).append(" ");

					while (tokenizer.hasMoreTokens()) {
						builder.append(tokenizer.nextToken().charAt(0));
					}
				}
				else {
					builder.append(metadata.value);
				}

				authors.add(builder.toString());
			}
			// Now the minority case (as we've cleaned up data and input method)
			else {
				String[] parts = metadata.value.split("\\s+|\\.");
				String name = parts[parts.length - 1].replace("\\s+|\\.", "");

				builder.append(name).append(" ");

				for (int index = 0; index < parts.length - 1; index++) {
					if (parts[index].length() > 0) {
						name = parts[index].replace("\\s+|\\.", "");
						builder.append(name.charAt(0));
					}
				}
			}
		}

		return authors;
	}
}
