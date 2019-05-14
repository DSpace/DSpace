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

public class ConferenceCitation implements CoverpageCitationCrosswalk {

	@Override
	public String makeCitation(Item item) {
		String citation ="";
	    Metadatum[] dcvalues = item.getMetadata("dc","relation","ispartof",Item.ANY);

	    if (dcvalues.length == 1){
               citation = dcvalues[0].value;
	   }else{
               StringBuffer sb = new StringBuffer();
               for (int i=0, len=dcvalues.length; i<len; i++)
                   sb.append("; ").append(dcvalues[i].value);
               citation = sb.toString().substring(2);
           }
		return citation;
	}
	
}
