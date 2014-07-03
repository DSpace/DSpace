/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rest.search;

import java.util.ArrayList;
import java.util.HashMap;

import org.dspace.core.Context;

public interface Search {
	
	public ArrayList<org.dspace.rest.common.Item> search(Context context, HashMap<String,String>searchTerms, String expand, Integer limit, Integer offset, String sortfield, String sortorder);
	public long getTotalCount();
	public ArrayList<org.dspace.rest.common.Item> searchAll(Context context, String query, String expand, Integer limit, Integer offset, String sortfield, String sortorder);
	public ArrayList<Integer> searchIdAll(Context context, String query);
	public ArrayList<Integer> searchId(Context context, HashMap<String,String>searchTerms);
}
