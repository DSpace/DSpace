package uk.ac.edina.datashare.utils;

import java.io.File;

import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;

/**
 * DataShare constants.
 */
public interface Consts
{
    /** Invisible spam prevention subject field name */
    public static final String SUBJECT_FIELD = "Subject";
    
    /** Name of the multiple titles check box */
    public static final String MULTIPLE_TITLES_CHECK_FIELD = "multiple_titles";
    
    /** The datashare login URL */
    public static final String LOGIN_PAGE = "ease-login";
    
    // ************ spatial coverage ******************* //
    
    /** The hijacked DC element for spatial coverage */
    public static final String SPATIAL_HIJACKED_ELEMENT =
        MetaDataUtil.SUBJECT_DDC_ELEMENT;
    
    /** The hijacked DC identifier for spatial coverage */
    public static final String SPATIAL_HIJACKED_IDENTIFIER =
        MetaDataUtil.SUBJECT_DDC_QUALIFIER;
    
    /** The full hijacked DC name for spatial coverage */
    public static final String SPATIAL_HIJACKED_NAME =
        MetadataSchema.DC_SCHEMA + "_" +
        SPATIAL_HIJACKED_ELEMENT + "_" +
        SPATIAL_HIJACKED_IDENTIFIER;
    
    // ************ EMARGO Stuff **********************//
    
    /** Should enbargo functionality be enabled string */
    public static final String ENABLE_EMBARGO     = "enable_embargo";
    
    /** Embargo composite control field name */ 
    public static final String EMBARGO_FIELD_NAME = "dc_date_embargo";
    
    /** Embargo control year field name */ 
    public static final String EMBARGO_YEAR       = EMBARGO_FIELD_NAME + "_year";
    
    /** Embargo control month field name */ 
    public static final String EMBARGO_MONTH      = EMBARGO_FIELD_NAME + "_month";
    
    /** Embargo control month day name */ 
    public static final String EMBARGO_DAY        = EMBARGO_FIELD_NAME + "_day";
    
    //  
    public static final int INVALID_EMBARGO_STRING = 9025;
    public static final int EMBARGO_IN_THE_PAST = 9026;
    public static final int EMBARGO_TOO_FAR_IN_FUTURE = 9027;

    // ************ License consts **********************//
    
    /** Full path and name of ODC Attribution license file. */
    public static final String CREATIVE_COMMONS_BY_LICENCE =
        ConfigurationManager.getProperty("dspace.dir") + File.separator +
        "config" + File.separator + "cc-by.license";
    /** Full path and name of ODC Attribution license file. */
    public static final String ODC_ATTRIBUTION_LICENCE =
        ConfigurationManager.getProperty("dspace.dir") + File.separator +
        "config" + File.separator + "odc-attribution.license";
    /** Full path and name of Open Data license file. */
    public static final String OPEN_DATA_LICENCE =
        ConfigurationManager.getProperty("dspace.dir") + File.separator +
        "config" + File.separator + "open-data.license";
    
    /** User license control name */
    public static final String USER_LICENSE_CONTROL = "license-options";
    /** Right statement control name */
    public static final String RIGHTS_STATEMENT     = "right-statement";
    /** License checkbox statement control name */
    public static final String LICENSE_CHECKBOX     = "license-options";
    /** Confirm license accept string */
    public static final String LICENSE_ACCEPT       = "yes";
    
    /** Manditory field error */
    public static final int MANDITORY_FIELD = 1;
    /** No license error */
    public static final int NO_LICENSE  = 2;
    
    //  ************ Time Period **********************//
    
    /** Temporal coverage field name */
    public static final String TEMPORAL_FIELD_NAME = "dc_coverage_temporal";
    
    /** Temporal coverage start year field name */ 
    public static final String START_YEAR  = TEMPORAL_FIELD_NAME + "_start_year";
    
    /** Temporal coverage start month field name */ 
    public static final String START_MONTH = TEMPORAL_FIELD_NAME + "_start_month";
    
    /** Temporal coverage start month day name */ 
    public static final String START_DAY   = TEMPORAL_FIELD_NAME + "_start_day";
    
    /** Temporal coverage end year field name */ 
    public static final String END_YEAR    = TEMPORAL_FIELD_NAME + "_end_year";
    
    /** Temporal coverage end month field name */ 
    public static final String END_MONTH   = TEMPORAL_FIELD_NAME + "_end_month";
    
    /** Temporal coverage end day field name */
    public static final String END_DAY     = TEMPORAL_FIELD_NAME + "_end_day";
    
    /** End time period date missing */
    public static final int TIME_PERIOD_END_MISSING      = 9021;
    
    /** Start time period date missing */
    public static final int TIME_PERIOD_START_BEFORE_END = 9022;
    
    /** Date day is invalid, e.g > 31 in January */
    public static final int TIME_PERIOD_INVALID_DAY = 9023;
    
    /** Future date is used */
    public static final int TIME_PERIOD_FUTURE_DATE = 9024;
    
    
    //  ************ Time Period ********************** //
    
    /** Publisher should be mandatory error message */
    public static final int MAKE_PUBLISHER_MANDITORY = 9025;
    
    /** Virus checking failed error */
    public static final int VIRUS_CHECK_FAILED = 1223;
    
    // misc
    public static final String PUBLISHER_FIELD_NAME = "dc_publisher";
    public static final String ALTERNATIVE_TITLE_FIELD_NAME = "dc_title_alternative";    
}
