/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Community;

public class CollectionsTree implements Comparable<CollectionsTree> {
	private Community current;
	private List<CollectionsTree> subTree;
	private List<Collection> collections;

	public Community getCurrent() {
		return current;
	}

	public void setCurrent(Community current) {
		this.current = current;
	}

	public List<CollectionsTree> getSubTree() {
		return subTree;
	}

	public void setSubTree(List<CollectionsTree> subTree) {
		this.subTree = subTree;
	}

	public List<Collection> getCollections() {
		return collections;
	}

	public void setCollections(List<Collection> collections) {
		this.collections = collections;
	}

    @Override
    public int compareTo(CollectionsTree o)
    {
        if(o != null) {
            if(o.getCurrent()!=null) {
                if(this.getCurrent()!=null) {
                    return this.getCurrent().getName().compareTo(o.getCurrent().getName());
                }
                return 1;
            }             
        }
        return -1;
    }
}
