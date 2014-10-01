/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration;

import org.dspace.app.xmlui.aspect.authorprofile.administrative.fields.AuthorProfileField;
import org.dspace.content.AuthorProfile;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldDescriptor;
import org.dspace.core.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorProfileInput {

    private final RegexValidator regexValidator = new RegexValidator();
    private String id;
    private boolean required;
    private boolean repeatable;
    private boolean lockAfterWrite = false;
    private AuthorProfileField inputType;
    private String defaultValue;
    private List<MetadataFieldDescriptor> metadataFields;
    private FieldDisplayer displayer = null;
    private HashMap<String,InputCleaner> inputCleanerHashMap;
    private List<Validator> validators= Collections.emptyList();

    public AuthorProfileInput(String id) {
        this.id = id;
        inputCleanerHashMap = new HashMap<String, InputCleaner>();
    }

    public boolean isLockAfterWrite() {
        return lockAfterWrite;
    }

    public void setLockAfterWrite(boolean lockAfterWrite) {
        this.lockAfterWrite = lockAfterWrite;
    }

    public FieldDisplayer getDisplayer() {
        return displayer;
    }

    public void setDisplayer(FieldDisplayer displayer) {
        this.displayer = displayer;
    }

    public List<Validator> getValidators() {
        return validators;
    }

    public void setValidators(List<Validator> validators) {
        this.validators = validators;
    }

    public List<InputCleaner> getInputCleaners(){
        return new ArrayList<InputCleaner>(inputCleanerHashMap.values());
    }

    public void setInputCleaners(List<InputCleaner> inputCleaners){
        for(InputCleaner ic:inputCleaners){
            for(MetadataFieldDescriptor rmf:ic.getMetadataFields()){
                inputCleanerHashMap.put(rmf.toString(),ic);
            }
        }

    }

   public Validator validate(AuthorProfile ap,Context context,String input){
       for(Validator validator:validators){
           if(!validator.validate(ap,context,input)){
               return validator;
           }
       }
       return null;
   }

    public String clean(String value, MetadataField field){
        if(inputCleanerHashMap.containsKey(field.toString())){
            return inputCleanerHashMap.get(field.toString()).cleanup(value);
        } else return value;
    }

    public boolean getRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public AuthorProfileField getInputType() {
        return inputType;
    }

    public void setInputType(AuthorProfileField inputType) {
        this.inputType = inputType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<MetadataFieldDescriptor> getMetadataFields(){
        return metadataFields;
    }

    public void setMetadataFields(List<MetadataFieldDescriptor> metadataFields) {
        this.metadataFields = metadataFields;
    }

    public String getId() {
        return id;
    }
}
