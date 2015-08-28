/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter.factory;

import org.dspace.app.mediafilter.service.MediaFilterService;
import org.dspace.utils.DSpace;

/**
 * Abstract factory to get services for the mediafilter package, use MediaFilterServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class MediaFilterServiceFactory {

    public abstract MediaFilterService getMediaFilterService();

    public static MediaFilterServiceFactory getInstance(){
        return new DSpace().getServiceManager().getServiceByName("mediaFilterServiceFactory", MediaFilterServiceFactory.class);
    }
}
