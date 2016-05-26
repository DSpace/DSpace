/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sfx.factory;

import org.dspace.app.sfx.service.SFXFileReaderService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the sfx package, use SfxServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class SfxServiceFactory {

    public abstract SFXFileReaderService getSfxFileReaderService();

    public static SfxServiceFactory getInstance(){
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("sfxServiceFactory", SfxServiceFactory.class);
    }
}
