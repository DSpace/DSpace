/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

/**
 * Some URIs for DSpace specific errors which may be reported through the SWORDErrorException
 */
public interface DSpaceSWORDErrorCodes
{
    /** if unpackaging the package fails */
    public static final String UNPACKAGE_FAIL =
            SWORDProperties.SOFTWARE_URI + "/errors/UnpackageFail";

    /** if the url of the request does not resolve to something meaningful */
    public static final String BAD_URL =
            SWORDProperties.SOFTWARE_URI + "/errors/BadUrl";

    /** if the media requested is unavailable */
    public static final String MEDIA_UNAVAILABLE =
            SWORDProperties.SOFTWARE_URI + "/errors/MediaUnavailable";

    /* additional codes */

    /** Invalid package */
    public static final String PACKAGE_ERROR =
            SWORDProperties.SOFTWARE_URI + "/errors/PackageError";

    /** Missing resources in package */
    public static final String PACKAGE_VALIDATION_ERROR =
            SWORDProperties.SOFTWARE_URI + "/errors/PackageValidationError";

    /** Crosswalk error */
    public static final String CROSSWALK_ERROR =
            SWORDProperties.SOFTWARE_URI + "/errors/CrosswalkError";

    /** Invalid collection for linking */
    public static final String COLLECTION_LINK_ERROR =
            SWORDProperties.SOFTWARE_URI + "/errors/CollectionLinkError";

    /** Database or IO Error when installing new item */
    public static final String REPOSITORY_ERROR =
            SWORDProperties.SOFTWARE_URI + "/errors/RepositoryError";

}
