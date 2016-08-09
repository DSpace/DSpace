/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing;

import java.util.Map;

/**
 * The object manager is a class that must be implemented by each specific repository 
 * implementation that identifies referenced objects. Since the DRI document includes 
 * references to external resources implementers of this class must know how objects 
 * are referenced.
 *
 * <p>The specific implementation of ObjectManager that is used is determined by the
 * Wing component that is creating the reference.
 *
 * @author Scott Phillips
 */

public interface ObjectManager
{	
    /**
     * Determine if the supplied object is manageable by this implementation of 
     * ObjectManager. If the object is manageable then manage it, and return true.
     * 
     * @param object
     *            The object to be managed.
     * @return true if the object can be managed, otherwise false.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public boolean manageObject(Object object) throws WingException;  
	
	/**
	 * Return a URL referencing the object's metadata. If this is unavailable
	 * return null.
	 * 
	 * @param object The object being managed.
     * @return the URL.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
	 */
	public String getObjectURL(Object object) throws WingException;
	
	/**
	 * Return a descriptive, repository specific, type for the object. If
	 * this is unavailable return null.
	 * 
	 * @param object The object being managed.
     * @return name of the object's type.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
	 */
	public String getObjectType(Object object) throws WingException;
	
	/**
	 * Return a unique identifier of the repository this object is contained 
	 * in. If this is unavailable return null.
	 * 
	 * @param object The object being managed.
     * @return the repository identifier.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
	 */
	public String getRepositoryIdentifier(Object object) throws WingException;
	

	/**
	 * Return a list of all repositories managed by this manager. The 
	 * hash should be of the form repository identifier as the key, 
	 * and the value for each key is a metadata URL.
     * @return a collection of repositories.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
	 */
	public Map<String,String> getAllManagedRepositories() throws WingException;
}
