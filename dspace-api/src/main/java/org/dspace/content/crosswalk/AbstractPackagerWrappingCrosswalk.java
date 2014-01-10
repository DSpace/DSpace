/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
