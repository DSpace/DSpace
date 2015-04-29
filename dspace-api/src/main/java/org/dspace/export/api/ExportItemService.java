/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.export.api;

import java.util.List;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 * @version $Revision$
 */
public interface ExportItemService {
	List<ExportItemProvider> getProviders();
	ExportItemProvider getProvider (String id);
}
