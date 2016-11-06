/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.utils.DSpace;

public class CollectionUtils {
	public static CollectionsTree getCollectionsTree(Collection[] collections, boolean skipCollection)
			throws SQLException {
		DSpace dspace = new DSpace();
		if (collections == null || collections.length == 0) {
			return null;
		}
		
		String skipHandles= ConfigurationManager.getProperty("submission.skip.handle");
		//TODO: Should we create a plugin?
		

		Map<Community, List<Collection>> map = new HashMap<Community, List<Collection>>();
		for (Collection col : collections) {
		
			String handle = col.getHandle();
			if (skipCollection && StringUtils.contains(handle, skipHandles)) {
				continue;
			} 
			Community com = (Community) col.getParentObject();
			if (map.containsKey(com)) {
				map.get(com).add(col);
			} else {
				List<Collection> cols = new ArrayList<>();
				cols.add(col);
				map.put(com, cols);
			}
		}
		List<CollectionsTree> trees = new ArrayList<CollectionsTree>();
		for (Community com : map.keySet()) {
			CollectionsTree tree = new CollectionsTree();
			tree.setCurrent(com);
			tree.setCollections(map.get(com));
			trees.add(tree);
		}
		Collections.sort(trees);
		return getCollectionsTree(trees);
	}

	private static CollectionsTree getCollectionsTree(
			List<CollectionsTree> trees) throws SQLException {
		if (trees.size() == 1) {
			return trees.get(0);
		}
		Map<Community, CollectionsTree> map = new HashMap<Community, CollectionsTree>();
		for (CollectionsTree tree : trees) {
			Community current = tree.getCurrent();
			Community com = null;
			if (current != null)
			{
				com = current.getParentCommunity();
			}
			if (map.containsKey(com)) {
				map.get(com).getSubTree().add(tree);
			} else {
				List<CollectionsTree> subTree;
				if (current == null) {
					subTree = tree.getSubTree();	
				}
				else
				{
					subTree = new ArrayList<>();
					subTree.add(tree);
				}
				CollectionsTree currTree = new CollectionsTree();
				currTree.setCurrent(com);
				currTree.setSubTree(subTree);
				map.put(com, currTree);
			}
		}

		List<CollectionsTree> result = new ArrayList<CollectionsTree>();
		for (CollectionsTree t : map.values()) {
			result.add(t);
		}

		return getCollectionsTree(result);
	}
}
