/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter.factory;

import org.dspace.app.mediafilter.service.MediaFilterService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the mediafilter package, use MediaFilterServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class MediaFilterServiceFactoryImpl extends MediaFilterServiceFactory {

    @Autowired(required = true)
    private MediaFilterService mediaFilterService;

    @Override
    public MediaFilterService getMediaFilterService() {
        return mediaFilterService;
    }
}
