/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.share;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public interface ShareProvider {
	boolean isAvailable (ShareItem item);
	String generateUrl (ShareItem item);
	String getImage ();
	String getId ();
}
