/*
 * AbstractPackagerWrappingCrosswalk.java
 *
 * Version: $Revision: 3761 $
 *
 * Date: $Date: 2009-05-06 23:18:02 -0500 (Wed, 06 May 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.content.crosswalk;

import org.dspace.content.packager.PackageParameters;

/**
 * Packager Wrapping Crosswalk plugin
 * <p>
 * A Packager Wrapping Crosswalk is a crosswalk which works with or wraps
 * an existing Packager for all or some of its crosswalking functionality.
 * <p>
 * Crosswalks which utilize Packagers for ingestion/dissemination do not always
 * have enough information to call a Packager (org.dspace.content.packager.*)
 * with the proper parameters.  This abstract class allows the crosswalk to
 * store its own PackageParameters and deposit license, which can then be
 * used by the Crosswalk to call a PackageIngester or PackagerDisseminator
 * with all the proper parameters.
 *
 * @author Tim Donohue
 * @version $Revision: 3761 $
 * @see IngestionCrosswalk
 * @see DisseminationCrosswalk
 * @see org.dspace.content.packager.PackageIngester
 * @see org.dspace.content.packager.PackageDisseminator
 * @see org.dspace.content.packager.PackageParameters
 */
public abstract class AbstractPackagerWrappingCrosswalk 
{
    // Crosswalk's PackageParameters, which can be used when calling/initializing a Packager during ingestion/dissemination
    private PackageParameters packageParameters = null;
    
    // Crosswalk's Ingestion License, which can be used when calling a Packager during ingestion
    private String ingestionLicense = null;

    /**
     * Set custom packaging parameters for this Crosswalk.
     * <p>
     * These PackageParameters can be used to pass on to a Packager Plugin
     * to actually perform the dissemination/ingestion required
     * by this crosswalk.
     * <p>
     * The crosswalk itself can choose how to utilize or obey these
     * PackageParameters. this method just provides the crosswalk
     * access to these parameters, so that it can make smarter decisions
     * about how to call a particular Packager.
     *
     * @param pparams PackageParameters to make available to the Crosswalk
     */
    public void setPackagingParameters(PackageParameters pparams)
    {
        this.packageParameters = pparams;
    }

    /**
     * Get custom packaging parameters for this Crosswalk.
     * <p>
     * These PackageParameters can be used to pass on to a Packager Plugin
     * to actually perform the dissemination/ingestion required
     * by this crosswalk.
     * <p>
     * The crosswalk itself can choose how to utilize or obey these
     * PackageParameters. this method just provides the crosswalk
     * access to these parameters, so that it can make smarter decisions
     * about how to call a particular Packager.
     *
     * @return PackageParameters previously made available to the Crosswalk or null
     */
    public PackageParameters getPackagingParameters()
    {
        return this.packageParameters;
    }
    
    
    /**
     * Set custom ingestion license for this Crosswalk.
     * <p>
     * This license can be used by the crosswalk when calling a PackageIngester
     * 
     * @param license the full text of the ingestion license
     * @see org.dspace.content.packager.PackageIngester
     */
    public void setIngestionLicense(String license)
    {
        this.ingestionLicense = license;
    }
    
    /**
     * Get custom ingestion license for this Crosswalk.
     * <p>
     * This license can be used by the crosswalk when calling a PackageIngester
     * 
     * @return the full text of the ingestion license as a String
     * @see org.dspace.content.packager.PackageIngester
     */
    public String getIngestionLicense()
    {
        return this.ingestionLicense;
    }
    
}
