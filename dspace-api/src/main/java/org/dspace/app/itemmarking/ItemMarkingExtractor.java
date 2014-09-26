/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemmarking;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface to abstract the strategy for item signing
 * 
 * @author Kostas Stamatis
 * 
 */
public interface ItemMarkingExtractor {
	public ItemMarkingInfo getItemMarkingInfo(Context context, Item item)
			throws SQLException;
}
