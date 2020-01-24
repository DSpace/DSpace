/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.deduplication.service.impl;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.deduplication.service.SolrDedupServiceIndexPlugin;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.util.ItemUtils;

public class ItemStatusDedupServiceIndexPlugin implements SolrDedupServiceIndexPlugin {

	private static final Logger log = Logger.getLogger(ItemStatusDedupServiceIndexPlugin.class);

	@Override
	public void additionalIndex(Context context, UUID firstId, UUID secondId, Integer type,
			SolrInputDocument document) {

		if (type == Constants.ITEM) {
			internal(context, firstId, document);
			if (firstId != secondId) {
				internal(context, secondId, document);
			}
		}

	}

	private void internal(Context context, UUID itemId, SolrInputDocument document) {
		try {
			Item item = ContentServiceFactory.getInstance().getItemService().find(context, itemId);

			if (item == null) {
				// found a zombie reference in solr, ignore it
				return;
			}

			document.addField("itemstatus_i", ItemUtils.getItemStatus(context, item));
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		}
	}

}
