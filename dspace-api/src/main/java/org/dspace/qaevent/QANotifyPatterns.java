/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent;

/**
 * Constants for Quality Assurance configurations to be used into cfg and xml spring.
 * 
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.it)
 */
public class QANotifyPatterns {

    public static final String TOPIC_ENRICH_MORE_PROJECT = "ENRICH/MORE/PROJECT";
    public static final String TOPIC_ENRICH_MISSING_PROJECT = "ENRICH/MISSING/PROJECT";
    public static final String TOPIC_ENRICH_MISSING_ABSTRACT = "ENRICH/MISSING/ABSTRACT";
    public static final String TOPIC_ENRICH_MORE_REVIEW = "ENRICH/MORE/REVIEW";
    public static final String TOPIC_ENRICH_MORE_ENDORSEMENT = "ENRICH/MORE/ENDORSEMENT";
    public static final String TOPIC_ENRICH_MORE_PID = "ENRICH/MORE/PID";
    public static final String TOPIC_ENRICH_MISSING_PID = "ENRICH/MISSING/PID";
    public static final String TOPIC_ENRICH_MORE_LINK = "ENRICH/MORE/LINK";

    /**
     * Default constructor
     */
    private QANotifyPatterns() { }
}