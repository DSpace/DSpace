/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceItem;

import com.lyncode.xoai.common.dataprovider.data.AbstractItemIdentifier;
import com.lyncode.xoai.common.dataprovider.filter.AbstractFilter;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public abstract class DSpaceFilter extends AbstractFilter {
	private Context _ctx = null;

	public void initialize(Context ctx) {
		_ctx = ctx;
	}

	public Context getContext() {
		return _ctx;
	}

	/**
	* Returns null if no where given. Or non empty if some where is given.
	*
	* @param context
	* @param item
	* @return
	*/
	public abstract DatabaseFilterResult getWhere(Context context);
	public abstract SolrFilterResult getQuery();
	public abstract boolean isShown(DSpaceItem item);

	@Override
	public boolean isItemShown(AbstractItemIdentifier item) {
		if (item instanceof DSpaceItem) {
			return this.isShown((DSpaceItem) item);
		}
		return false;
	}
}
