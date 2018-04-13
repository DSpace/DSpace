/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.jaxb.model.message;

/**
 * 
 * This class is just for constants so can't be instantiated. It's not an
 * interface in case anyone is tempted to 'implement' it (see Effective Java,
 * Joshua Bloch), instead of statically importing the constants.
 * 
 * @author Will Simpson
 *
 */
final public class ScopeConstants {

    private ScopeConstants() {
    }

    public static final String AUTHENTICATE = "/authenticate";
    public static final String READ_PUBLIC = "/read-public";
    public static final String READ_LIMITED = "/read-limited";
    public static final String WEBHOOK = "/webhook";
    public static final String ORCID_BIO_READ_LIMITED = "/orcid-bio/read-limited";
    public static final String ORCID_PROFILE_READ_LIMITED = "/orcid-profile/read-limited";
    public static final String AFFILIATIONS_READ_LIMITED = "/affiliations/read-limited";
    public static final String ORCID_WORKS_READ_LIMITED = "/orcid-works/read-limited";
    public static final String FUNDING_READ_LIMITED = "/funding/read-limited";
    public static final String ORCID_PATENTS_READ_LIMITED = "/orcid-patents/read-limited";
    public static final String AFFILIATIONS_UPDATE = "/affiliations/update";
    public static final String ORCID_WORKS_UPDATE = "/orcid-works/update";
    public static final String FUNDING_UPDATE = "/funding/update";
    public static final String ORCID_PATENTS_UPDATE = "/orcid-patents/update";
    public static final String ORCID_BIO_EXTERNAL_IDENTIFIERS_CREATE = "/orcid-bio/external-identifiers/create";
    public static final String ORCID_BIO_UPDATE = "/orcid-bio/update";
    public static final String AFFILIATIONS_CREATE = "/affiliations/create";
    public static final String ORCID_WORKS_CREATE = "/orcid-works/create";
    public static final String FUNDING_CREATE = "/funding/create";
    public static final String ORCID_PATENTS_CREATE = "/orcid-patents/create";
    public static final String BASIC_NOTIFICATION = "/basic-notification";
    public static final String PREMIUM_NOTIFICATION = "/premium-notification";
    public static final String PEER_REVIEW_READ_LIMITED = "/peer-review/read-limited";
    public static final String PEER_REVIEW_UPDATE = "/peer-review/update";
    public static final String PEER_REVIEW_CREATE = "/peer-review/create";
    public static final String GROUP_ID_RECORD_READ = "/group-id-record/read";
    public static final String GROUP_ID_RECORD_UPDATE = "/group-id-record/update";
    public static final String EMAIL_READ_PRIVATE = "/email/read-private";

    // Per activity API
    public static final String ACTIVITIES_READ_LIMITED = "/activities/read-limited";
    public static final String ACTIVITIES_UPDATE = "/activities/update";
    public static final String PERSON_READ_LIMITED = "/person/read-limited";
    public static final String PERSON_UPDATE = "/person/update";
    public static final String ORCID_PROFILE_CREATE = "/orcid-profile/create";
    
    // Internal API
    public static final String INTERNAL_PERSON_LAST_MODIFIED = "/orcid-internal/person/last_modified";
    public static final String IDENTIFIER_TYPES_CREATE = "/identifier-types/create";
    
}
