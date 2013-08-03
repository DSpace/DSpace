/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

/**
 * This interface define the callback method that an Authority can implement to
 * receive notice when a potential match is rejected
 * 
 * @author cilea
 * 
 */
public interface NotificableAuthority
{
    /**
     * Callback method for notify the authority of a single rejected potential
     * match. For performance gain use the multiple reject form where applicable
     * 
     * @param itemID
     *            the item id
     * @param authorityKey
     *            the authority key
     */
    public void reject(int itemID, String authorityKey);
 
    /**
     * Callback method for notify the authority of multiple rejected potential
     * matches.
     * 
     * @param itemIDs
     *            an array of item id
     * @param authorityKey
     *            the authority key
     */
    public void reject(int[] itemIDs, String authorityKey);
}
