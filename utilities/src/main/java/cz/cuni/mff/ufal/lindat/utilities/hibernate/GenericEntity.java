package cz.cuni.mff.ufal.lindat.utilities.hibernate;

import java.io.Serializable;

public abstract class GenericEntity implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public abstract int getID(); 
	
}
