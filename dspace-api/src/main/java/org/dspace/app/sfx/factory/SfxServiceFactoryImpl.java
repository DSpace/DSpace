/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sfx.factory;

import org.dspace.app.sfx.service.SFXFileReaderService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the sfx package, use SfxServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class SfxServiceFactoryImpl extends SfxServiceFactory {

    @Autowired(required = true)
    private SFXFileReaderService sfxFileReaderService;

    @Override
    public SFXFileReaderService getSfxFileReaderService() {
        return sfxFileReaderService;
    }
}
