package uk.ac.edina.datashare.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Metadatum;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;

/**
 * Checks the metadata of SWORD/batch deposit.
 */
public class MetadataChecker {
    //private static Logger LOG = Logger.getLogger(MetadataChecker.class);
    
    // map of metadata fields that users are allowed to populate 
    private Map<String, List<String>> allowedMap = new HashMap<String, List<String>>();
    
    // map of mandatory metadata fields
    private Map<String, List<String>> requiredMap = new HashMap<String, List<String>>();
    
    // map of non repeatable fields
    private Map<String, List<String>> singleValueMap = new HashMap<String, List<String>>();
    
    private static DCInputsReader reader = null;
    
    static{
        try{
            reader = new DCInputsReader();
        }
        catch (DCInputsReaderException e){
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Is the metadata in an item valid?
     * @param context DSpace context
     * @param item DSpace item
     * @throws IllegalStateException
     * @throws SQLException
     */
    public void isValid(Context context, Item item) throws IllegalStateException, SQLException{
        Collection collection = this.getCollection(context, item);
        
        if(collection == null){
            throw new IllegalStateException("No collection found. Does user " +
                    context.getCurrentUser().getEmail() + " have access to the collection?");
        }
        
        this.isValid(context, item, collection);
    }
    
    /**
     * Is the metadata in an item valid?
     * @param context DSpace context
     * @param item DSpace item
     * @param collection DSpace collection.
     * @throws IllegalStateException
     * @throws SQLException
     */
    public void isValid(Context context, Item item, Collection collection) throws IllegalStateException, SQLException{
        List<String> mandatory = this.getMandatoryList(collection.getHandle());
        
        for (String field : mandatory){
            Metadatum value[] = item.getMetadataByMetadataString(field);
            if(value.length == 0){
                if(field.equals(MetaDataUtil.DEPOSITOR_STR)){
                    // always ignore depositor
                }
                else if(field.equals(MetaDataUtil.PUBLISHER_STR) &&
                        item.getMetadataByMetadataString(MetaDataUtil.CREATOR_STR).length > 0){
                    // ignore publisher if data creator defined
                }
                else {
                    throw new IllegalStateException(field + " is a mandatory metadata field for DataShare");
                }
            }
            else{
                if(value[0].value.trim().length() == 0){
                    // we have an entry but does it have a value?
                    throw new IllegalStateException(field + " is a mandatory metadata field and must have a value");
                }
            }
        }
        
        List<String> unRepeatable = this.getSingleValueList(collection.getHandle());
        for (String field : unRepeatable){
            Metadatum value[] = item.getMetadataByMetadataString(field);
            if(value.length > 1){
                // we have a non repeatable field with more than one value  
                throw new IllegalStateException(field + " is a non repeatable metadata field and cannot have more than one value");
            }
        }
    }
    
    /**
     * Is the external user allowed to populate this metadata field? 
     * @param context DSpace context.
     * @param item DSpace item.
     * @param element Dublin core element.
     * @param qualifier Dublin core qualifier.
     * @return True if user is allowed to populate the metadata field.
     * @throws IllegalStateException
     * @throws SQLException
     */
    public boolean isAllowedMetadataField(
            Context context,
            Item item,
            String element,
            String qualifier) throws IllegalStateException, SQLException{
        return isAllowedMetadataField(context, item, MetadataSchema.DC_SCHEMA, element, qualifier);
    }
    
    /**
     * Is the external user allowed to populate this metadata field? 
     * @param context DSpace context.
     * @param item DSpace item.
     * @param schema Metadata schema.
     * @param element Dublin core element.
     * @param qualifier Dublin core qualifier.
     * @return True if user is allowed to populate the metadata field.
     * @throws IllegalStateException
     * @throws SQLException
     */
    public boolean isAllowedMetadataField(
            Context context,
            Item item,
            String schema,
            String element,
            String qualifier) throws IllegalStateException, SQLException{
        boolean allowed = false;
        
        Collection collection = this.getCollection(context, item);
        if(collection != null){
            List<String> allowedList = this.getAllowedList(collection.getHandle());
            String mdField = DSpaceUtils.getMdString(schema, element, qualifier);
            allowed = allowedList.contains(mdField);
        }

        return allowed;
    }
    
    /**
     * Get collection the item belongs to.
     * @param context DSpace context.
     * @param item DSpace item.
     * @return DSpace collection.
     * @throws SQLException
     */
    private Collection getCollection(Context context, Item item) throws SQLException{
        Collection collection = item.getOwningCollection();
        
        if(collection == null){
            WorkspaceItem wsi = WorkspaceItem.findByItem(context, item);
            
            if(wsi != null){
                collection = wsi.getCollection();
            }
        }
                
        return collection;        
    }
    
   /**
     * @param handle Collection handle.
     * @return List of mandatory fields in datashare.
     */
    private List<String> getMandatoryList(String handle)
    {
        List<String> required = this.requiredMap.get(handle);
        if(required == null){
            this.cacheMetadataList(handle);
            required = this.requiredMap.get(handle);
        }
        
        return required;
    }
    
    /**
     * @param handle Collection handle.
     * @return List of allowed fields in datashare.
     */
    private List<String> getAllowedList(String handle)
    {
        List<String> all = this.allowedMap.get(handle);
        if(all == null){
            this.cacheMetadataList(handle);
            all = this.allowedMap.get(handle);
        }
        
        return all;
    }
    /**
     * @param handle Collection handle.
     * @return List of un repeatable fields in datashare.
     */
    private List<String> getSingleValueList(String handle)
    {
        List<String> all = this.singleValueMap.get(handle);
        if(all == null){
            this.cacheMetadataList(handle);
            all = this.allowedMap.get(handle);
        }
        
        return all;
    }
    
    
    /**
     * Fetch list of mandatory and allowed fields in datashare.
     * @param handle Collection handle.
     */
    private void cacheMetadataList(String handle)
    {
        // exceptions are metadata fields defined in the configurable input that
        // we don't want people using (basically they are hijacked fields) 
        final List<String> exceptions = new ArrayList<String>(Arrays.asList(
                "dc.identifier.govdoc",
                "dc.identifier.isbn",
                "dc.identifier.ismn",
                "dc.identifier.issn",
                "dc.subject.ddc",
                "dc.language.iso"));
        
        List<String> allowed = new ArrayList<String>(100);
        List<String> required = new ArrayList<String>(10);
        List<String> notRepeatable = new ArrayList<String>(10);
        
        // rights is not a mandatory field in the user interface
        // but it must be with SWORD/batch ingest
        required.add(DSpaceUtils.getMdString("rights"));
        allowed.add(DSpaceUtils.getMdString("rights"));
        allowed.add(DSpaceUtils.getMdString("coverage", "temporal"));
        allowed.add(DSpaceUtils.getMdString("ds", "withdrawn", "showtombstone"));
    
        try{
            DCInputSet inputs = reader.getInputs(handle);

            for (int i = 0; i < inputs.getNumberPages(); i++)
            {
                for (DCInput input : inputs.getPageRows(i, true, true))
                {
                    String field = DSpaceUtils.getMdString(input.getSchema(), input.getElement(),  input.getQualifier());
                    if(input.isRequired()){
                        required.add(field);
                    }
                    
                    if(!exceptions.contains(field)){
                        allowed.add(field);
                    }
                    
                    if(!input.getRepeatable()){
                        notRepeatable.add(field);
                    }
                }
            }
            
            this.requiredMap.put(handle, required);
            this.allowedMap.put(handle, allowed);
            this.singleValueMap.put(handle, notRepeatable);
        }
        catch(DCInputsReaderException ex){
            throw new RuntimeException(ex);
        }
    }    
}
