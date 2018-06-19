/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.MetadataValue;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Try to look to an item metadata for the corresponding author name and email.
 * Failover to the RequestItemSubmitterStrategy
 * 
 * @author Andrea Bollini
 * 
 */
public class RequestItemMetadataStrategy extends RequestItemSubmitterStrategy {

    protected String emailMetadata;
    protected String fullNameMetadata;

    @Autowired(required = true)
    protected ItemService itemService;

	public RequestItemMetadataStrategy() {
	}

	@Override
	public RequestItemAuthor getRequestItemAuthor(Context context, Item item)
			throws SQLException {
		if (emailMetadata != null)
		{
			List<MetadataValue> vals = itemService.getMetadataByMetadataString(item, emailMetadata);
			if (vals.size() > 0)
			{
				String email = vals.iterator().next().getValue();
				String fullname = null;
				if (fullNameMetadata != null)
				{
                    List<MetadataValue> nameVals = itemService.getMetadataByMetadataString(item, fullNameMetadata);
					if (nameVals.size() > 0)
					{
						fullname = nameVals.iterator().next().getValue();
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

	public void setFullNameMetadata(String fullNameMetadata) {
		this.fullNameMetadata = fullNameMetadata;
	}

}
