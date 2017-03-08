package uk.ac.edina.datashare.utils;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Metadatum;

/**
 * Meta data utility. This wraps the getting and setting of getting DSpace
 * metadata.
 */
public class MetaDataUtil
{
    //private static final Logger LOG = Logger.getLogger(MetaDataUtil.class);
    
    private static final String CITATION_ELEMENT   = "identifier";
    private static final String CITATION_QUALIFIER = "citation";
    
    private static final String CONTRIBUTOR_ELEMENT         = "contributor";
    private static final String CONTRIBUTOR_OTHER_QUALIFIER = "other";
    
    private static final String CREATOR_ELEMENT = "creator";
    static final String         CREATOR_STR = MetadataSchema.DC_SCHEMA + "." +
            CREATOR_ELEMENT; 
    
    private static final String DATASHARE_SCHEMA = "ds";
    private static final String DATE_ELEMENT             = "date";
    private static final String DATE_AVAILABLE_QUALIFIER = "available";
    private static final String DATE_COPYRIGHT_QUALIFIER = "copyright";
    static final String         DEPOSITOR_STR = MetadataSchema.DC_SCHEMA + "." +
            CONTRIBUTOR_ELEMENT;
    
    private static final String FORMAT_ELEMENT = "format";
    
    private static final String IDENTIFIER_ELEMENT = "identifier";
    private static final String IDENTIFIER_URI_ELEMENT = "uri";
    
    private static final String PUBLISHER_ELEMENT = "publisher";
    static final String         PUBLISHER_STR = MetadataSchema.DC_SCHEMA + "." + PUBLISHER_ELEMENT;
    
    private static final String RELATION_IS_FORMAT_ELEMENT   = "relation";
    private static final String RELATION_IS_FORMAT_QUALIFIER = "isformatof";
    
    private static final String RIGHTS_ELEMENT     = "rights";
    private static final String RIGHTS_URI_ELEMENT = "uri";
    
    private static final String SPATIAL_ELEMENT   = "coverage";
    private static final String SPATIAL_QUALIFIER = "spatial";
    
    private static final String SOURCE_ELEMENT = "source";
    
    public static final  String SUBJECT_DDC_ELEMENT   = "subject";
    public static final  String SUBJECT_DDC_QUALIFIER = "ddc";
    
    private static final String TEMPORAL_ELEMENT   = SPATIAL_ELEMENT;
    private static final String TEMPORAL_QUALIFIER = "temporal";
    
    private static final String TITLE_ELEMENT = "title";
    private static final String TITLE_ALTERNATIVE_QUALIFIER = "alternative";
    
    private static final String TYPE_ELEMENT = "type";
    
    private static final String TOMBSTONE_ELEMENT = "withdrawn";
    private static final String TOMBSTONE_SHOW_QUALIFIER = "showtombstone";
    
    private static final String DEFAULT_LANG = "en";
    
    /**
     * Clear metadata values for specified element using a null qualifier and
     * any language
     * @param item DSpace item.
     * @param element DSpace dublin core element.
     */
    public static void clearMetaData(
            Item item,
            String element)
    {
        clearMetaData(item, element, null, Item.ANY);
    }
    
    /**
     * Clear metadata values for specified element and qualifier and any
     * language.
     * @param item DSpace item.
     * @param element DSpace dublin core element.
     * @param qualifier DSpace dublin core qualifier.
     */
    public static void clearMetaData(
            Item item,
            String element,
            String qualifier)
    {
        clearMetaData(item, element, qualifier, Item.ANY);
    }
    
    /**
     * Clear metadata values for specified element, qualifier and language.
     * @param item DSpace item.
     * @param element DSpace dublin core element.
     * @param qualifier DSpace dublin core qualifier.
     * @param language Text language used.
     */
    public static void clearMetaData(
            Item item,
            String element,
            String qualifier,
            String lang)
    {
        item.clearMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang);
    }
    
    /**
     * Clear dublin core Citation metadata value.
     * @param item DSpace item object.
     */
    public static void clearCitation(Item item)
    {
        clearMetaData(item, CITATION_ELEMENT, CITATION_QUALIFIER);
    }
    
    /**
     * Clear dublin core Contributor metadata value.
     * @param info DSpace item object.
     */
    public static void clearContributor(Item item)
    {
        clearMetaData(item, CONTRIBUTOR_ELEMENT, null, Item.ANY);
    }

    /**
     * Clear dublin core Date Available metadata value.
     * @param info DSpace item object.
     */
    public static void clearDateAvailable(Item item)
    {
        clearMetaData(item, DATE_ELEMENT, DATE_AVAILABLE_QUALIFIER);
    }
    
    /**
     * Clear dublin core Date Copyright metadata value.
     * @param info DSpace submission object.
     */
    public static void clearDateCopyright(SubmissionInfo info)
    {
        clearDateCopyright(info);
    }
    
    /**
     * Clear dublin core Date Copyright metadata value.
     * @param item The DSpace item.
     */
    public static void clearDateCopyright(Item item)
    {
        clearMetaData(
                item,
                DATE_ELEMENT,
                DATE_COPYRIGHT_QUALIFIER);
    }
    
    /**
     * Clear dublin core is format of value.
     * @param info DSpace submission object.
     */
    public static void clearIsFormatOf(SubmissionInfo info)
    {
        clearMetaData(
                info.getSubmissionItem().getItem(),
                RELATION_IS_FORMAT_ELEMENT,
                RELATION_IS_FORMAT_QUALIFIER);
    }
    
    /**
     * Clear dublin core publisher value.
     * @param info DSpace submission object.
     */
    public static void clearPublisher(SubmissionInfo info)
    {
    	clearMetaData(info.getSubmissionItem().getItem(), PUBLISHER_ELEMENT);
    }
    
    /**
     * Clear dublin core rights.uri value.
     * @param item DSpace item.
     */
    public static void clearRights(Item item)
    {
        clearMetaData(item, RIGHTS_ELEMENT);
    }
    
    /**
     * Clear dublin core rights.uri value.
     * @param item DSpace item.
     */
    public static void clearRightsUri(Item item)
    {
        clearMetaData(item, RIGHTS_ELEMENT, RIGHTS_URI_ELEMENT);
    }
    
    /**
     * Clear dublin core rights.uri value.
     * @param info DSpace submission object.
     */
    public static void clearRightsUri(SubmissionInfo info)
    {
        clearRightsUri(info.getSubmissionItem().getItem());
    }
       
    /**
     * Clear dublin core Subject DCC metadata value.
     * @param info DSpace submission object.
     */
    public static void clearSubjectDcc(SubmissionInfo info)
    {
        clearSubjectDcc(info.getSubmissionItem().getItem());
    }
    
    /**
     * Clear dublin core Subject DCC metadata value.
     * @param item The DSpace item.
     */
    public static void clearSubjectDcc(Item item)
    {
        clearMetaData(
                item,
                SUBJECT_DDC_ELEMENT,
                SUBJECT_DDC_QUALIFIER);
    }
    
    /**
     * Clear dublin core Coverage Temporal metadata value.
     * @param info DSpace submission object.
     */
    public static void clearTemporal(Item item)
    {
        clearMetaData(item, TEMPORAL_ELEMENT, TEMPORAL_QUALIFIER);
    }
    
    /**
     * Clear dublin core Alternative Title metadata value.
     * @param info DSpace submission object.
     */
    public static void clearTitleAlternative(SubmissionInfo info)
    {
    	clearMetaData(
        		info.getSubmissionItem().getItem(),
                TITLE_ELEMENT,
                TITLE_ALTERNATIVE_QUALIFIER);
    }

    /**
     * Get dublin core identifier.citation value.
     * @param info DSpace submission object.
     * @return DSpace dublin core identifier.citation value.
     */
    public static String getCitation(SubmissionInfo info)
    {
        return getCitation(info.getSubmissionItem().getItem());
    }
    
    /**
     * Get dublin core identifier.citation value.
     * @param item DSpace item.
     * @return DSpace dublin core identifier.citation value.
     */
    public static String getCitation(Item item)
    {
        return getUnique(
                item,
                CITATION_ELEMENT,
                CITATION_QUALIFIER);
    }

    /**
     * Get array of dublin core contributor values.
     * @param info DSpace submission object.
     * @return DSpace Item contributors.
     */
    public static Metadatum[] getContributors(
            SubmissionInfo info)
    {
        return getContributors(info.getSubmissionItem().getItem());
    }
    
    /**
     * Get array of dublin core contributor values.
     * @param item DSpace item.
     * @return DSpace Item contributors.
     */
    public static Metadatum[] getContributors(Item item)
    {
        return item.getMetadata(
                MetadataSchema.DC_SCHEMA,
                CONTRIBUTOR_ELEMENT,
                null,
                Item.ANY);
    }
    
    /**
     * Get dublin core contributor.other value.
     * @param info DSpace submission object.
     * @return DSpace dublin core contributor.other value.
     */
    public static String getContributorOther(SubmissionInfo info)
    {
        return getUnique(
                info.getSubmissionItem().getItem(),
                CONTRIBUTOR_ELEMENT,
                CONTRIBUTOR_OTHER_QUALIFIER);
    }
    
    /**
     * Get first dublin core creator value.
     * @param info DSpace submission object.
     * @return First dublin core creator value.
     */
    public static String getCreator(SubmissionInfo info)
    {
        return getUnique(
                info.getSubmissionItem().getItem(),
                CREATOR_ELEMENT);
    }
    
    /**
     * Get all dublin core creator values.
     * @param info DSpace submission object.
     * @return All dublin core creator values.
     */
    public static Metadatum[] getCreators(
            SubmissionInfo info)
    {
        return getCreators(info.getSubmissionItem().getItem());
    }
    
    /**
     * Get all dublin core creator values.
     * @param item DSpace item.
     * @return All dublin core creator values.
     */
    public static Metadatum[] getCreators(Item item)
    {
        return item.getMetadata(
                MetadataSchema.DC_SCHEMA,
                CREATOR_ELEMENT,
                Item.ANY,
                Item.ANY);
    }
    
    /**
     * Get earliest date available.
     * @param item DSpace item.
     * @return Earliest date available.
     */
    public static DCDate getDateAvailable(Item item)
    {
        DCDate date = null;
        
        // get unique gets the earliest date
        String value = getUnique(
                item,
                DATE_ELEMENT,
                DATE_AVAILABLE_QUALIFIER);
        
        if(value != null)
        {
            date = new DCDate(value); 
        }
    
        return date;
    }
    
    /**
     * Get array of data available values.
     * @param item DSpace item.
     * @return Array of data available values.
     */
    public static Metadatum[] getDateAvailables(Item item)
    {
        return item.getMetadata(
                MetadataSchema.DC_SCHEMA,
                DATE_ELEMENT, DATE_AVAILABLE_QUALIFIER,
                Item.ANY);
    }

    /**
     * Get dublin core Date Copyright value.
     * @param info DSpace submission object.
     * @return Dublin core Date Copyright value.
     */
    public static String getDateCopyright(SubmissionInfo info)
    {
        return getUnique(
                info.getSubmissionItem().getItem(),
                DATE_ELEMENT,
                DATE_COPYRIGHT_QUALIFIER);
    }
    
    /**
     * Get dublin core Identifier value.
     * @param item DSpace item.
     * @return Dublin core Identifier value.
     */
    public static String getIdentifier(Item item)
    {
        return getUnique(item, IDENTIFIER_ELEMENT, null, DEFAULT_LANG);
    }
    
    /**
     * Get array of identifer uri values.
     * @param item DSpace item.
     * @return All dublin identifier uri values.
     */
    public static Metadatum[] getIdentifierUris(
            Item item)
    {
        return item.getMetadata(
            MetadataSchema.DC_SCHEMA,
            IDENTIFIER_ELEMENT,
            IDENTIFIER_URI_ELEMENT,
            Item.ANY);
    }

    /**
     * Get dublin core Relation Is Format Of value.
     * @param info DSpace submission object.
     * @return Dublin core Relation Is Format value.
     */
    public static String getIsFormatOf(SubmissionInfo info)
    {
        return getUnique(
                info.getSubmissionItem().getItem(),
                RELATION_IS_FORMAT_ELEMENT,
                RELATION_IS_FORMAT_QUALIFIER);
    }
    /**
     * Get dublin core publisher value.
     * @param info DSpace submission object.
     * @return Dublin core publisher value.
     */
    public static String getPublisher(SubmissionInfo info)
    {
        return getPublisher(info.getSubmissionItem().getItem());
    }
    
    /**
     * Get dublin core publisher value. 
     * @param item DSpace item.
     * @return Dublin core publisher value.
     */
    public static String getPublisher(Item item)
    {
        return getUnique(item, PUBLISHER_ELEMENT);
    }
    
    /**
     * @param info DSpace submission object.
     * @return DSpace Item rights statement.
     */
    public static String getRights(SubmissionInfo info)
    {
        return getRights(info.getSubmissionItem().getItem());
    }
    
    /**
     * @param item DSpace submission object.
     * @return DSpace DSpace item.
     */
    public static String getRights(Item item)
    {
        return getUnique(
                item,
                RIGHTS_ELEMENT,
                null);
    }
    
    /**
     * Get first rights URI of an item.
     * @param info DSpace submission object.
     * @return The first rights URI.
     */
    public static String getRightsUri(SubmissionInfo info)
    {
        return getUnique(
                info.getSubmissionItem().getItem(),
                RIGHTS_ELEMENT,
                RIGHTS_URI_ELEMENT);
    }
    
    /**
     * Get array of dublin core rights.uri values.
     * @param info DSpace submission object.
     * @return Dublin core rights.uri values.
     */
    public static Metadatum[] getRightsUris(
            SubmissionInfo info)
    {
        return info.getSubmissionItem().getItem().getMetadata(
            MetadataSchema.DC_SCHEMA,
            RIGHTS_ELEMENT,
            RIGHTS_URI_ELEMENT,
            Item.ANY);
    }
    
    /**
     * Get dublin core source value of a submission item.
     * @param info DSpace submission object.
     * @return Dublin core source value. 
     */
    public static String getSource(SubmissionInfo info)
    {
        return getUnique(info.getSubmissionItem().getItem(), SOURCE_ELEMENT);
    }
    
    /**
     * Get dublin core value for spatial coverage from a DSpace item. 
     * @param item DSpace item.
     * @return Spatial coverage dublin core value.
     */
    public static Metadatum[] getSpatial(Item item)
    {
        return item.getMetadata(
                MetadataSchema.DC_SCHEMA, 
                SPATIAL_ELEMENT,
                SPATIAL_QUALIFIER,
                Item.ANY);
    }
    
    /**
     * Get the first dublin core value for spatial coverage from a DSpace item.
     * @param item DSpace item.
     * @return The first spatial coverage dublin core value.
     */
    public static String getSpatialFirst(Item item)
    {
        return getUnique(item, SPATIAL_ELEMENT, SPATIAL_QUALIFIER, Item.ANY);
    }
    
    /**
     * @param item DSpace item.
     * @return Get show tombsomstone metadata value.
     */
    public static String getShowThombstone(Item item)
    {
        return getUnique(
                item,
                TOMBSTONE_ELEMENT,
                TOMBSTONE_SHOW_QUALIFIER,
                Item.ANY, DATASHARE_SCHEMA);
    }
    
    /**
     * Get dublin core value for Subject DCC from a submission item. 
     * @param item DSpace submission item.
     * @return subject dcc dublin core value.
     */
    public static String getSubjectDcc(SubmissionInfo info)
    {
        return getUnique(
                info.getSubmissionItem().getItem(),
                SUBJECT_DDC_ELEMENT,
                SUBJECT_DDC_QUALIFIER);
    }
    
    /**
     * Get dublin core values for Subject DCC from a submission item. 
     * @param item DSpace submission item.
     * @return subject dcc dublin core values.
     */
	public static Metadatum[] getSubjectDccs(Item item)
    {
        return item.getMetadata(
        		MetadataSchema.DC_SCHEMA,
        		SUBJECT_DDC_ELEMENT,
                SUBJECT_DDC_QUALIFIER,
                Item.ANY);
    }

    /**
     * Get dublin core value for temporal coverage from a submission item.
     * @param item DSpace submission item.
     * @return Temporal coverage dublin core value.
     */
    public static String getTemporal(SubmissionInfo info)
    {
        return getTemporal(info.getSubmissionItem().getItem());
    }
    
    /**
     * Get dublin core value for temporal coverage from a submission item.
     * @param item DSpace item.
     * @return Temporal coverage dublin core value.
     */
    public static String getTemporal(Item item)
    {
        return getUnique(
                item,
                TEMPORAL_ELEMENT,
                TEMPORAL_QUALIFIER);
    }
    
    /**
     * Get type dublin core value.
     * @param info DSpace submission object.
     * @return DSpace Item type.
     */
    public static String getType(SubmissionInfo info)
    {
        return getType(info.getSubmissionItem().getItem());
    }
    
    /**
     * Get type dublin core value.
     * @param item DSpace item.
     * @return DSpace Item type.
     */
    public static String getType(Item item)
    {
        return getUnique(item, TYPE_ELEMENT);
    }
    
    /**
     * Get dublin core title.
     * @param info DSpace submission object.
     * @return DSpace Item title.
     */
    public static String getTitle(SubmissionInfo info)
    {
        return getTitle(info.getSubmissionItem().getItem());
    }
    
    /**
     * Get dublin core title.
     * @param item DSpace item.
     * @return DSpace Item title.
     */
    public static String getTitle(Item item)
    {
        return getUnique(item, TITLE_ELEMENT, null);
    }
    
    /**
     * Get unique metadata value from DSpace item using any qualifier and
     * any language.
     * @param item DSpace item.
     * @param element Metadata element.
     * @return Metadata value.
     */
    public static String getUnique(Item item, String element)
    {
        return getUnique(item, element, Item.ANY, Item.ANY);
    }
    
    /**
     * Get unique metadata value from DSpace item using any language.
     * @param item DSpace item.
     * @param element Metadata element.
     * @param qualifier Metadata qualifier.
     * @return Metadata value.
     */
    public static String getUnique(Item item, String element, String qualifier)
    {
        return getUnique(item, element, qualifier, Item.ANY);
    }
    
    /**
     * Get unique metadata value from DSpace item.
     * @param item DSpace item.
     * @param element Metadata element.
     * @param qualifier Metadata qualifier.
     * @param lang Metadata language.
     * @return Metadata value.
     */
    public static String getUnique(
            Item item,
            String element,
            String qualifier,
            String lang)
    {
        return getUnique(item, element, qualifier, lang, MetadataSchema.DC_SCHEMA);
    }
   
    /**
     * Get unique metadata value from DSpace item.
     * @param item DSpace item.
     * @param element Metadata element.
     * @param qualifier Metadata qualifier.
     * @param lang Metadata language.
     * @param schema Metadata schema.
     * @return Metadata value.
     */
    public static String getUnique(
            Item item,
            String element,
            String qualifier,
            String lang,
            String schema)
    {
        String value = null;
        
        Metadatum[] values = item.getMetadata(
                schema,
                element,
                qualifier,
                lang);
        
        if(values != null && values.length > 0)
        {
            value = values[0].value;
        }
        
        return value;  
    }
    
    /**
     * Set DSpace item citation. Any existing value will first be deleted. 
     * @param info DSpace submission object.
     * @param value Item citation.
     */
    public static void setCitation(SubmissionInfo info, String value)
    {
        // rights is unique ensure entries are first removed
        setCitation(info.getSubmissionItem().getItem(), value);
    }
    
    /**
     * Set DSpace item citation. 
     * @param item DSpace item.
     * @param value New citation value.
     */
    public static void setCitation(Item item, String value)
    {
        // rights is unique ensure entries are first removed
        setUnique(
                item,
                CITATION_ELEMENT,
                CITATION_QUALIFIER,
                value);
    }
   
    /**
     * Add a DSpace item contributor. 
     * @param info DSpace submission object.
     * @param value Item contributor.
     */
    public static void setContributor(SubmissionInfo info, String value){
    	setContributor(info.getSubmissionItem().getItem(), value);
    }

    /**
     * Add a DSpace item contributor. 
     * @param item DSpace item.
     * @param value New contributor value.
     */
    public static void setContributor(Item item, String value){
    	item.addMetadata(
                MetadataSchema.DC_SCHEMA,
                CONTRIBUTOR_ELEMENT,
                null,
                DEFAULT_LANG,
                value);
    }
    
    /**
     * Set a DSpace item contributor.other value. Any existing value will first
     * be deleted. 
     * @param info DSpace submission object.
     * @param value New contributor.other value.
     */
    public static void setContributorOther(SubmissionInfo info, String value)
    {
        setUnique(
                info.getSubmissionItem().getItem(),
                CONTRIBUTOR_ELEMENT,
                CONTRIBUTOR_OTHER_QUALIFIER,
                value);
    }
    
    /**
     * Set a DSpace item date available value. Any existing value will first
     * be deleted.
     * @param info DSpace submission object.
     * @param value New date available value.
     */
    public static void setDateAvailable(Item item, DCDate value)
    {
        setDateAvailable(item, value.toString());
    }
    
    /**
     * Set a DSpace item date available value. Any existing value will first
     * be deleted.
     * @param info DSpace submission object.
     * @param value New date available string value.
     */
    public static void setDateAvailable(Item item, String value)
    {
        setUnique(
                item,
                DATE_ELEMENT,
                DATE_AVAILABLE_QUALIFIER,
                Item.ANY,
                value);
    }
    
    /**
     * Set a DSpace item Date Copyright value. Any existing value will first be
     * deleted.
     * @param item DSpace submission object.
     * @param value New format value.
     */
    public static void setDateCopyright(SubmissionInfo info, String value)
    {
        setUnique(
                info.getSubmissionItem().getItem(),
                DATE_ELEMENT,
                DATE_COPYRIGHT_QUALIFIER,
                value);
    }
    
    /**
     * Set a DSpace item format value. Any existing value will first be deleted. 
     * @param item DSpace submission object.
     * @param value New format value.
     */
    public static void setFormat(Item item, String value)
    {
        setUnique(item, FORMAT_ELEMENT, value);
    }
   
    /**
     * Set a DSpace item Identifier value.
     * @param item DSpace item object.
     * @param value The new value.
     */
    public static void setIdentifier(Item item, String value)
    {
        setIdentifier(item, value, DEFAULT_LANG);
    }
    
    /**
     * Set a DSpace item Identifier value.
     * @param item DSpace item object.
     * @param value The new value.
     * @param lang The value language.
     */
    public static void setIdentifier(Item item, String value, String lang)
    {
        item.addMetadata(
                MetadataSchema.DC_SCHEMA,
                IDENTIFIER_ELEMENT,
                null,
                lang,
                value);
    }
    
    /**
     * Set a DSpace item Relation Is Format Of value. Any existing value will
     * first be deleted. 
     * @param item DSpace submission object.
     * @param value New format value.
     */
    public static void setIsFormatOf(SubmissionInfo info, String value)
    {
        setUnique(
                info.getSubmissionItem().getItem(),
                RELATION_IS_FORMAT_ELEMENT,
                RELATION_IS_FORMAT_QUALIFIER,
                value); 
    }
    
    /**
     * Set a DSpace item Publisher value. Any existing value will first be
     * deleted.
     * @param info DSpace submission object.
     * @param value New publisher value.
     */
    public static void setPublisher(SubmissionInfo info, String value)
    {
        // rights is unique ensure entries are first removed
        setUnique(info.getSubmissionItem().getItem(), PUBLISHER_ELEMENT, value);
    }
    
    /**
     * Set DSpace item rights statement. Any existing value will first be
     * deleted.
     * @param info DSpace submission object.
     * @param value Item rights statement.
     */
    public static void setRights(SubmissionInfo info, String value)
    {
        // rights is unique ensure entries are first removed
        setUnique(info.getSubmissionItem().getItem(), RIGHTS_ELEMENT, value);
    }
    
    /**
     * Set/add DSpace item rights uri.
     * @param info DSpace submission object.
     * @param value The new right.uri value.
     * @param unique If true any existing values will first be deleted.
     */
    public static void setRightsUri(SubmissionInfo info, String value, boolean unique)
    {
        Item item = info.getSubmissionItem().getItem();
        
        if(unique)
        {
            clearRightsUri(item);
        }
        
        setRightsUri(item, value);
    }
    
    /**
     * Add DSpace item rights.uri using default language.
     * @param item DSpace submission object.
     * @param value The new right.uri value.
     */
    public static void setRightsUri(Item item, String value)
    {
        item.addMetadata(
                MetadataSchema.DC_SCHEMA,
                RIGHTS_ELEMENT,
                RIGHTS_URI_ELEMENT,
                DEFAULT_LANG,
                value);
    }
    
    /**
     * Set DSpace dublin core source value. Any existing source value will first
     * be deleted.
     * @param info DSpace submission object.
     * @param value The new source value. 
     */
    public static void setSource(SubmissionInfo info, String value)
    {
        setUnique(info.getSubmissionItem().getItem(), SOURCE_ELEMENT, value);
    }
    
    /**
     * Set DSpace dublin core coverage spatial value.
     * @param info DSpace submission object.
     * @param value The new spatial value.
     * @param unique Detemines whether any existing values are deleted. If
     * unique is true all existing values will be deleted.
     */
    public static void setSpatial(Item item, String value, boolean unique)
    {
        if(unique)
        {
            setSpatial(item, value);
        }
        else
        {
            item.addMetadata(
                    MetadataSchema.DC_SCHEMA,
                    SPATIAL_ELEMENT,
                    SPATIAL_QUALIFIER,
                    DEFAULT_LANG,
                    value);
        }
    }
    
    /**
     * Set DSpace dublin core coverage.spatial value. Any existing source value
     * will first be deleted.
     * @param item DSpace item.
     * @param value The new coverage.spatial value.
     */
    public static void setSpatial(Item item, String value)
    {
        setUnique(item, SPATIAL_ELEMENT, SPATIAL_QUALIFIER, value);
    }
    
    /**
     * Set DSpace dublin core subject.dcc value.
     * @param item DSpace item.
     * @param value The new subject.dcc value.
     */
    public static void setSubjectDcc(Item item, String value)
    {
    	 item.addMetadata(
                 MetadataSchema.DC_SCHEMA,
                 SUBJECT_DDC_ELEMENT,
                 SUBJECT_DDC_QUALIFIER,
                 DEFAULT_LANG,
                 value);
    }

    /**
     * Set the dublin core temporal coverage value for a DSpace submission item.
     * Any existing value will first be deleted. 
     * @param info DSpace submission item. 
     * @param value The new temporal coverage value.
     */
    public static void setTemporal(SubmissionInfo info, String value)
    {
        setTemporal(
                info.getSubmissionItem().getItem(),
                value);
    }
    
    /**
     * Set the dublin core temporal coverage value for a DSpace item.  Any
     * existing value will first be deleted. 
     * @param item DSpace item. 
     * @param value The new temporal coverage value.
     */
    public static void setTemporal(Item item, String value)
    {
        setUnique(
                item,
                TEMPORAL_ELEMENT,
                TEMPORAL_QUALIFIER,
                value);
    }
    
    /**
     * Set the dublin core alternative title value for a DSpace item.
     * @param info DSpace submission item. 
     * @param value The new alternative title value.
     */    
    public static void setTitleAlternative(SubmissionInfo info, String value)
    {
		info.getSubmissionItem().getItem().addMetadata(
                MetadataSchema.DC_SCHEMA,
                TITLE_ELEMENT,
                TITLE_ALTERNATIVE_QUALIFIER,
                DEFAULT_LANG,
                value);
    }
    
    /**
     * Set unique DSpace metadata item using a null qualifier and english
     * language. Unique means if a value current exists it will be deleted and
     * updated to be current value. 
     * @param item DSpace item.
     * @param element Metadata element.
     * @param value The new value.
     */
    public static void setUnique(Item item, String element, String value)
    {
        setUnique(item, element, null, DEFAULT_LANG, value);
    }
    
    /**
     * Set unique DSpace metadata item using the default language. 
     * @param item DSpace item.
     * @param element Metadata element.
     * @param qualifier Metadata qualifier.
     * @param value The new value.
     */
    public static void setUnique(
            Item item,
            String element,
            String qualifier,
            String value)
    {
        setUnique(item, element, qualifier, DEFAULT_LANG, value);
    }
    
    /**
     * Set unique DSpace metadata item. 
     * @param item DSpace item.
     * @param element Metadata element.
     * @param qualifier Metadata qualifier.
     * @param lang Metadata language.
     * @param value The new value.
     */
    public static void setUnique(
            Item item,
            String element,
            String qualifier,
            String lang,
            String value)
    {
        item.clearMetadata(
                MetadataSchema.DC_SCHEMA,
                element,
                qualifier,
                lang);
        
        if(value != null && value.length() > 0)
        {
            item.addMetadata(
                    MetadataSchema.DC_SCHEMA,
                    element,
                    qualifier,
                    lang,
                    value);
        }
    }
}
