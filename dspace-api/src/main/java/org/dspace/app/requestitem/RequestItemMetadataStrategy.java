/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;

/**
 * Try to look to an item metadata for the corresponding author name and email.
 * Failover to the RequestItemSubmitterStrategy
 * 
 * @author Andrea Bollini
 * 
 */
public class RequestItemMetadataStrategy extends RequestItemSubmitterStrategy {

	private String emailMetadata;
	private String fullNameMatadata;

	public RequestItemMetadataStrategy() {
	}

	@Override
	public RequestItemAuthor getRequestItemAuthor(Context context, Item item)
			throws SQLException {
		if (emailMetadata != null)
		{
			DCValue[] vals = item.getMetadata(emailMetadata);
			if (vals.length > 0)
			{
				String email = vals[0].value;
				String fullname = null;
				if (fullNameMatadata != null)
				{
					DCValue[] nameVals = item.getMetadata(fullNameMatadata); 
					if (nameVals.length > 0)
					{
						fullname = nameVals[0].value;
					}
				}
				
				if (StringUtils.isBlank(fullname))
				{
					fullname = I18nUtil
							.getMessage(
									"org.dspace.app.requestitem.RequestItemMetadataStrategy.unnamed",
									context);					
				}
				RequestItemAuthor author = new RequestItemAuthor(
						fullname, email);
				return author;
			}
		}
		return super.getRequestItemAuthor(context, item);
	}

	public void setEmailMetadata(String emailMetadata) {
		this.emailMetadata = emailMetadata;
	}

	public void setFullNameMatadata(String fullNameMatadata) {
		this.fullNameMatadata = fullNameMatadata;
	}

}
