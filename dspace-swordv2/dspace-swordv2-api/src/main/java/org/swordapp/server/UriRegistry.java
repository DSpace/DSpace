package org.swordapp.server;

import javax.xml.namespace.QName;

public class UriRegistry
{
    // Namespaces
    public static String SWORD_TERMS_NAMESPACE = "http://purl.org/net/sword/terms/";
    public static String APP_NAMESPACE = "http://www.w3.org/2007/app";
    public static String DC_NAMESPACE = "http://purl.org/dc/terms/";
    public static String ORE_NAMESPACE = "http://www.openarchives.org/ore/terms/";
    public static String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";

    // QNames for Extension Elements
    public static QName SWORD_VERSION = new QName(SWORD_TERMS_NAMESPACE, "version");
    public static QName SWORD_MAX_UPLOAD_SIZE = new QName(SWORD_TERMS_NAMESPACE, "maxUploadSize");
    public static QName SWORD_COLLECTION_POLICY = new QName(SWORD_TERMS_NAMESPACE, "collectionPolicy");
    public static QName SWORD_MEDIATION = new QName(SWORD_TERMS_NAMESPACE, "mediation");
    public static QName SWORD_TREATMENT = new QName(SWORD_TERMS_NAMESPACE, "treatment");
    public static QName SWORD_ACCEPT_PACKAGING = new QName(SWORD_TERMS_NAMESPACE, "acceptPackaging");
    public static QName SWORD_SERVICE = new QName(SWORD_TERMS_NAMESPACE, "service");
    public static QName SWORD_PACKAGING = new QName(SWORD_TERMS_NAMESPACE, "packaging");
    public static QName SWORD_VERBOSE_DESCRIPTION = new QName(SWORD_TERMS_NAMESPACE, "verboseDescription");
    public static QName APP_ACCEPT = new QName(APP_NAMESPACE, "accept");
	public static QName DC_ABSTRACT = new QName(DC_NAMESPACE, "abstract");

    // URIs for the statement
    public static String SWORD_DEPOSITED_BY = SWORD_TERMS_NAMESPACE + "depositedBy";
    public static String SWORD_DEPOSITED_ON_BEHALF_OF = SWORD_TERMS_NAMESPACE + "depositedOnBehalfOf";
    public static String SWORD_DEPOSITED_ON = SWORD_TERMS_NAMESPACE + "depositedOn";
    public static String SWORD_ORIGINAL_DEPOSIT = SWORD_TERMS_NAMESPACE + "originalDeposit";
    public static String SWORD_STATE_DESCRIPTION = SWORD_TERMS_NAMESPACE + "stateDescription";
    public static String SWORD_STATE = SWORD_TERMS_NAMESPACE + "state";

    // rel values
    public static String REL_STATEMENT = "http://purl.org/net/sword/terms/statement";
    public static String REL_SWORD_EDIT = "http://purl.org/net/sword/terms/add";
    public static String REL_ORIGINAL_DEPOSIT = "http://purl.org/net/sword/terms/originalDeposit";
    public static String REL_DERIVED_RESOURCE = "http://purl.org/net/sword/terms/derivedResource";

    // Package Formats
    public static String PACKAGE_SIMPLE_ZIP = "http://purl.org/net/sword/package/SimpleZip";
    public static String PACKAGE_BINARY = "http://purl.org/net/sword/package/Binary";

    // Error Codes
    public static String ERROR_BAD_REQUEST = "http://purl.org/net/sword/error/ErrorBadRequest";
    public static String ERROR_CONTENT = "http://purl.org/net/sword/error/ErrorContent";
    public static String ERROR_CHECKSUM_MISMATCH = "http://purl.org/net/sword/error/ErrorChecksumMismatch";
    public static String ERROR_TARGET_OWNER_UNKNOWN = "http://purl.org/net/sword/error/TargetOwnerUnknown";
    public static String ERROR_MEDIATION_NOT_ALLOWED = "http://purl.org/net/sword/error/MediationNotAllowed";
    public static String ERROR_METHOD_NOT_ALLOWED = "http://purl.org/net/sword/error/MethodNotAllowed";
	public static String ERROR_MAX_UPLOAD_SIZE_EXCEEDED = "http://purl.org/net/sword/error/MaxUploadSizeExceeded";
}
