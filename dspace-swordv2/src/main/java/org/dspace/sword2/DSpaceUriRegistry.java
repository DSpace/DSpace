/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

public class DSpaceUriRegistry
{
    public static final String DSPACE_SWORD_NS = "http://www.dspace.org/ns/sword/2.0/";

    /** if unpackaging the package fails */
    public static final String UNPACKAGE_FAIL =
            DSPACE_SWORD_NS + "errors/UnpackageFail";

    /** if the url of the request does not resolve to something meaningful */
    public static final String BAD_URL = DSPACE_SWORD_NS + "errors/BadUrl";

    /** if the media requested is unavailable */
    public static final String MEDIA_UNAVAILABLE =
            DSPACE_SWORD_NS + "errors/MediaUnavailable";

    /* additional codes */

    /** Invalid package */
    public static final String PACKAGE_ERROR =
            DSPACE_SWORD_NS + "errors/PackageError";

    /** Missing resources in package */
    public static final String PACKAGE_VALIDATION_ERROR =
            DSPACE_SWORD_NS + "errors/PackageValidationError";

    /** Crosswalk error */
    public static final String CROSSWALK_ERROR =
            DSPACE_SWORD_NS + "errors/CrosswalkError";

    /** Invalid collection for linking */
    public static final String COLLECTION_LINK_ERROR =
            DSPACE_SWORD_NS + "errors/CollectionLinkError";

    /** Database or IO Error when installing new item */
    public static final String REPOSITORY_ERROR =
            DSPACE_SWORD_NS + "errors/RepositoryError";

    // FIXME: this is being withdrawn from all 406 responses for the time being, in preference
    // for ErrorContent as per the spec (whether that is right or wrong)
    public static final String NOT_ACCEPTABLE =
            DSPACE_SWORD_NS + "errors/NotAcceptable";
}
