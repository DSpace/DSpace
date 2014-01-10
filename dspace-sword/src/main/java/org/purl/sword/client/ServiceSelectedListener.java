/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

/**
 * Listener for any objects that want to be notified when a collection has been selected in the 
 * ServicePanel.  
 * 
 * @author Neil Taylor
 *
 */
public interface ServiceSelectedListener
{
   /**
    * Called to provide an update on whether the selected node is a Collection. 
    * 
    * @param collection The location of the collection. <code>null</code>, otherwise. 
    */
   public void selected(String collection); 
}
