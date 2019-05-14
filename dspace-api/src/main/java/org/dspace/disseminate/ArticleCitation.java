/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import org.dspace.content.Item;
import org.dspace.content.Metadatum;

public class ArticleCitation implements CoverpageCitationCrosswalk {

	@Override
	public String makeCitation(Item item) {
		String citation="";
	   // use this string buffer to build the merged citation dynamically depending on what data is available
	   StringBuffer mergedCitation = new StringBuffer();

	   // add dc.identifier.citation if available
	   Metadatum[] values = item.getMetadata("dc", "identifier", "citation", Item.ANY);
	   if(values.length > 0)
	      mergedCitation.append(values[0].value+", ");

	   // add dc.identifier.volume if available
	   values = item.getMetadata("dc", "identifier", "volume", Item.ANY);
	   if(values.length > 0)
	      mergedCitation.append(values[0].value+" ");

	   // add dc.identifier.issue (in brackets) if available
	   values = item.getMetadata("dc", "identifier", "issue", Item.ANY);
	   if(values.length > 0)
	      mergedCitation.append("("+values[0].value+")");

	   // add a colon as a seperator
	   mergedCitation.append(": ");

	   // add dc.identifier.startpage if available
	   values = item.getMetadata("dc", "identifier", "startpage", Item.ANY);
	   if(values.length > 0)
	      mergedCitation.append(values[0].value);

	   // add dc.identifier.endpage prefixed by a hyphen (-) if available
	   values = item.getMetadata("dc", "identifier", "endpage", Item.ANY);
	   if(values.length > 0)
	      mergedCitation.append("-"+values[0].value);

	   // add dc.identifier.other prefixed by a coma if available
	   // JG changed to dc.citation.other 2009-03-26
	   values = item.getMetadata("dc", "citation", "other", Item.ANY);
	   if(values.length > 0)
	      mergedCitation.append(", "+values[0].value);

	   // now print out the concatenated string

	   if(!mergedCitation.toString().equals(": ")) 
	      citation = mergedCitation.toString();
	   else
	      ; // do nothing
	
	return citation;
	}
}
