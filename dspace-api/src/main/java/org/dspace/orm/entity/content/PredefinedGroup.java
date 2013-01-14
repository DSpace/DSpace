/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity.content;

public enum PredefinedGroup {
	ANONYMOUS(0),
	ADMIN(1);
	
	private int id;
	
	private PredefinedGroup (int id) {
		this.id = id;
	}
	
	public int getId () {
		return id;
	}
}
