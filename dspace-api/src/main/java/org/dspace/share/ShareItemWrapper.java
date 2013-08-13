/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.share;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.services.share.ShareItem;

/**
 * DSpace share item wrapper
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
@SuppressWarnings("deprecation")
public class ShareItemWrapper implements ShareItem {
	private static String titleField = null;
	private static String urlField = null;
	private static String descriptionField = null;

	private static String getTitleField () {
		if (titleField == null) {
			titleField = ConfigurationManager.getProperty("sharingbar", "field.title");
			if (titleField == null)
				titleField = "dc.title";
		}
		return titleField;
	}
	private static String getUrlField () {
		if (urlField == null) {
			urlField = ConfigurationManager.getProperty("sharingbar", "field.url");
			if (urlField == null)
				urlField = "dc.identifier.uri";
		}
		return urlField;
	}

	private static String getDescriptionField () {
		if (descriptionField == null) {
			descriptionField = ConfigurationManager.getProperty("sharingbar", "field.description");
			if (descriptionField == null)
				descriptionField = "dc.description.abstract";
		}
		return descriptionField;
	}
	
	private Item item;
	 
	
	public ShareItemWrapper (Item i) {
		item = i;
	}
	
	private String getMetadata (String md) {
		DCValue[] values = this.item.getMetadata(md);
		if (values != null && values.length > 0)
			return values[0].value;
		else
			return null;
	}
	
	@Override
	public String getTitle() {
		return this.getMetadata(getTitleField());
	}

	@Override
	public String getUrl() {
		return this.getMetadata(getUrlField());
	}

	@Override
	public String getDescription() {
		return this.getMetadata(getDescriptionField());
	}

}
