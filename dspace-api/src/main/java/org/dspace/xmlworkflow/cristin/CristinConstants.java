/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.cristin;

import javax.xml.namespace.QName;

public class CristinConstants
{
    /** metadata namespace of the StudentWeb incoming metadata */
    public static String FS_NAMESPACE = "http://studentweb.no/terms/";

    /** QName object representing the metadata element the StudentWeb grade is held in */
    public static QName GRADE_QNAME = new QName(FS_NAMESPACE, "grade");

    /** QName object representing the metadata element the StudentWeb Embargo End Date is held in */
    public static QName EMBARGO_END_DATE_QNAME = new QName(FS_NAMESPACE, "embargoEndDate");

    /** QName object representing the metadata element the StudentWeb Embargo Type is held in */
    public static QName EMBARGO_TYPE_QNAME = new QName(FS_NAMESPACE, "embargoType");

    /** Name of the DSpace ORIGINAL bundle */
    public static String ORIGINAL_BUNDLE = "ORIGINAL";

    /** Name of the DSpace SECONDARY bundle */
    public static String SECONDARY_BUNDLE = "SECONDARY";

    /** Name of the DSpace SECONDARY_RESTRICED bundle */
    public static String SECONDARY_RESTRICTED_BUNDLE = "SECONDARY_CLOSED"; // must be under 16 characters

    /** Name of the DSpace METADATA bundle */
    public static String METADATA_BUNDLE = "METADATA";

    /** Name of th DSpace LICENSE bundle */
    public static String LICENSE_BUNDLE = "LICENSE";

    /** Name of the file where metadata should be stored */
    public static String METADATA_FILE = "metadata.xml";

    /** "open" access condition for items coming from StudentWeb */
    public static String OPEN = "open";

    /** "closed" access condition for items coming from StudentWeb */
    public static String CLOSED = "closed";

    /** Name of the DSpace administrator group */
    public static String ADMIN_GROUP = "Administrator";

    /** Name of the DSpace Anonymous group */
    public static String ANON_GROUP = "Anonymous";
}
