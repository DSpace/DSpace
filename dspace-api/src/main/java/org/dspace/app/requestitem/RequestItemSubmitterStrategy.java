/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Basic strategy that looks to the original submitter.
 * 
 * @author Andrea Bollini
 *
 */
public class RequestItemSubmitterStrategy implements RequestItemAuthorExtractor {

	public RequestItemSubmitterStrategy() {		
	}

	@Override
	public RequestItemAuthor getRequestItemAuthor(Context context, Item item)
			throws SQLException {
		EPerson submitter = item.getSubmitter();
		RequestItemAuthor author = new RequestItemAuthor(
				submitter.getFullName(), submitter.getEmail());
		return author;
	}

}
