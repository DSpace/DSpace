/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

/**
 * Constants for LDN metadata fields
 * 
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.com)
 */
public final class LDNMetadataFields {

    // schema and element are the same for each metadata of LDN coar-notify
    public static final String SCHEMA = "coar";
    public static final String ELEMENT = "notify";

    // qualifiers
    public static final String INITIALIZE = "initialize";
    public static final String REQUEST_REVIEW = "requestreview";
    public static final String REQUEST_ENDORSEMENT = "requestendorsement";
    public static final String EXAMINATION = "examination";
    public static final String REFUSED = "refused";
    public static final String REVIEW = "review";
    public static final String ENDORSMENT = "endorsement";
    public static final String RELEASE = "release";

    /**
     * 
     */
    private LDNMetadataFields() {

    }

}