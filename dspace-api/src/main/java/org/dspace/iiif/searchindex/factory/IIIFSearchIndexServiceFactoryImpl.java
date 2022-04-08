/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.searchindex.factory;

import org.dspace.iiif.searchindex.service.IIIFSearchIndexService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation for IIIFSearchIndexService.
 *
 *  @author Michael Spalti  mspalti@willamette.edu
 */
public class IIIFSearchIndexServiceFactoryImpl extends IIIFSearchIndexServiceFactory {

    @Autowired
    IIIFSearchIndexService iiifSearchIndexService;

    @Override
    public IIIFSearchIndexService getIiifSearchIndexService() {
        return iiifSearchIndexService;
    }
}
