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
