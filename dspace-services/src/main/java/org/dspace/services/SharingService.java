/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services;

import java.util.List;

import org.dspace.services.share.ShareProvider;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public interface SharingService {
	List<ShareProvider> getProviders();
}
