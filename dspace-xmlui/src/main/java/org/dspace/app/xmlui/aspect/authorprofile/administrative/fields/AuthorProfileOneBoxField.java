/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.fields;

import org.apache.cocoon.environment.Request;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.AuthorProfileInput;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.ValidationReport;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.Validator;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorProfileOneBoxField extends AuthorProfileField {

    private static final Logger log = Logger.getLogger(AuthorProfileOneBoxField.class);

    @Override
    public void renderField(Context context, List form, AuthorProfile authorProfile, AuthorProfileInput inputConfig, Request request, java.util.List<String> fieldsInError, java.util.Map<String,ValidationReport> invalidFields) throws WingException, AuthorizeException, SQLException {
        if(CollectionUtils.size(inputConfig.getMetadataFields()) == 1){
            MetadataFieldDescriptor metadataFieldDescriptor = inputConfig.getMetadataFields().get(0);
            MetadataField metadataField = metadataFieldDescriptor.getMetadataField(context);
            MetadataSchema metadataSchema = metadataFieldDescriptor.getMetadataSchema(context);

            if(inputConfig.getRepeatable())
            {
                form.addLabel(message(MESSAGE_PREFIX + inputConfig.getId()));

                org.dspace.app.xmlui.wing.element.Item wingItem = form.addItem();
                java.util.List<String> values = new ArrayList<String>();

                if(authorProfile != null&&request.getParameter("submit_edit")==null)
                {
                    DCValue[] dcValues = authorProfile.getMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), Item.ANY);
                    for (DCValue dcValue : dcValues) {
                        values.add(dcValue.value);
                    }
                }else{
                    values = getRepeatableValues(request, inputConfig.getId() + "_");
                }

                //Render previously filled in values
                for (int i = 0; i < values.size(); i++) {
                    String value = values.get(i);
                    if(StringUtils.isNotBlank(value))
                    {
                        addRepeatableField(metadataFieldDescriptor, wingItem.addComposite(inputConfig.getId()/* + "-composite-" + i*/, "clearfix"), inputConfig, value, i, request, fieldsInError,invalidFields);
                    }
                }

                //Add a new field
                addRepeatableField(metadataFieldDescriptor, wingItem.addComposite(inputConfig.getId()/* + "-composite-" + values.size()*/, "clearfix"), inputConfig, null, values.size(), request, fieldsInError,invalidFields);

            }else{
                //Attempt to get the value from our request
                String value = request.getParameter(inputConfig.getId());
                if(authorProfile != null&&request.getParameter("submit_edit")==null){
                    //Attempt to retrieve it from our author profile
                    value = authorProfile.getMetadataFirstValue(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), Item.ANY);
                }
                addTextField(form, inputConfig, fieldsInError,invalidFields, value,request);
            }

        }else{
            log.warn("Error while rendering metadata field with id: " + inputConfig.getId() + " multiple metadata fields configured to a onebox field.");
        }
    }

    protected void addTextField(List form, AuthorProfileInput inputConfig, java.util.List<String> fieldsInError, java.util.Map<String,ValidationReport> invalidFields, String value,Request request) throws WingException {
        Text text;
        java.util.List<MetadataFieldDescriptor> metadataFieldList=inputConfig.getMetadataFields();
        String type = "";
        if(metadataFieldList!=null&&metadataFieldList.size()==1){
            type=metadataFieldList.get(0).getElement()+"."+metadataFieldList.get(0).getQualifier();
        }

        org.dspace.app.xmlui.wing.element.Item item=form.addItem();
        if (inputConfig.getRequired()) {
            text = item.addText(inputConfig.getId(),"required "+type+" regex actual");
        } else {
            text = item.addText(inputConfig.getId(),type+" regex actual");
        }
        text.setLabel(message(MESSAGE_PREFIX + inputConfig.getId()));

        text.setHelp(message(HELP_PREFIX+inputConfig.getId()));



        addValidatorsToItem(inputConfig,item);
        if(StringUtils.isNotBlank(value))
        {
            text.setValue(value);
            text.setDisabled(inputConfig.isLockAfterWrite());
            if(inputConfig.getDisplayer()!=null){
                item.addXref(inputConfig.getDisplayer().displayValue(request,value),inputConfig.getDisplayer().displayValue(request,value));
            }
        }
        if(fieldsInError.contains(inputConfig.getId()))
        {
            text.addError(message(ERROR_PREFIX + inputConfig.getId()));
        } else if (invalidFields.keySet().contains(inputConfig.getId())){
            for(Message message:invalidFields.get(inputConfig.getId()).getErrorMessage(INVALID_PREFIX,inputConfig.getId(),value))
                text.addError(message);
        }
    }

    protected void addRepeatableField(MetadataFieldDescriptor metadataFieldDescriptor, Composite composite, AuthorProfileInput inputConfig, String value, int index, Request request, java.util.List<String> fieldsInError,java.util.Map<String,ValidationReport> invalidFields) throws WingException {

        Text text;
        if (inputConfig.getRequired()&&index==0) {
            text = composite.addText(inputConfig.getId() + "_" + index,"required regex actual");
        } else {
            text = composite.addText(inputConfig.getId() + "_" + index,"regex actual");
        }
        if (index==0)composite.setHelp(message(HELP_PREFIX+inputConfig.getId()));
        for(Validator validator:inputConfig.getValidators()){
            if(validator.hasClientComponent())
                composite.addText(validator.getClientComponentClass(),"ds-hidden-field").setValue(validator.getClientComponent());
        }

        if(StringUtils.isNotBlank(value))
        {
            text.setValue(value);
        }
        if(fieldsInError.contains(inputConfig.getId()))
        {
            text.addError(message(ERROR_PREFIX + inputConfig.getId()));
        }else if (invalidFields.keySet().contains(inputConfig.getId())){
            for(Message message:invalidFields.get(inputConfig.getId()).getErrorMessage(INVALID_PREFIX, inputConfig.getId(), value))
                text.addError(message);
        }

        addRepeatableButtons(metadataFieldDescriptor, composite);
    }



    @Override
    public void storeField(Request request, AuthorProfile authorProfile, AuthorProfileInput inputConfig, java.util.List<String> fieldsInError, java.util.Map<String,ValidationReport> invalidFields, Context context) throws AuthorizeException, SQLException {
        //Should just be a single metadata field !
        if(CollectionUtils.size(inputConfig.getMetadataFields()) == 1){
            MetadataFieldDescriptor metadataFieldDescriptor = inputConfig.getMetadataFields().get(0);
            MetadataField metadataField = metadataFieldDescriptor.getMetadataField(context);
            MetadataSchema metadataSchema = metadataFieldDescriptor.getMetadataSchema(context);


            DCValue oldValue[] = authorProfile.getMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), Item.ANY);
            java.util.List<String> oldValues=new LinkedList<String>();
            for(DCValue value:oldValue)
                oldValues.add(value.value);
            authorProfile.clearMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), Item.ANY);

            if(inputConfig.getRepeatable())
            {
                boolean validValuesAdded = false;
                java.util.List<String> values = getRepeatableValues(request, inputConfig.getId() + "_");
                for (String value : values) {
                    if (StringUtils.isNotBlank(value)) {
                        Validator validator=inputConfig.validate(authorProfile,context,value);
                        validValuesAdded = true;
                        if(validator==null){
                            authorProfile.addMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), null, inputConfig.clean(value, metadataField));
                            oldValues.remove(value);
                        } else {
                           if(invalidFields.containsKey(inputConfig.getId())){
                               invalidFields.get(inputConfig.getId()).add(validator,value);
                           } else {
                               invalidFields.put(inputConfig.getId(), ValidationReport.create(validator, value));
                           }
                        }
                    }
                }
                if(!validValuesAdded && inputConfig.getRequired())
                {
                    fieldsInError.add(inputConfig.getId());
                }

                if(invalidFields.containsKey(inputConfig.getId())){
                    for(String value:oldValues)
                        authorProfile.addMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), null, value);
                }

            }else{
                String metadataValue = request.getParameter(inputConfig.getId());
                if(oldValue.length!=0&&inputConfig.isLockAfterWrite()) return;

                if(StringUtils.isEmpty(metadataValue))
                {
                    if(inputConfig.getRequired())
                    {
                        fieldsInError.add(inputConfig.getId());
                    }
                }else{
                    Validator validator=inputConfig.validate(authorProfile,context,metadataValue);
                    if(validator==null)
                        authorProfile.addMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), null, inputConfig.clean(metadataValue, metadataField));
                    else {
                       invalidFields.put(inputConfig.getId(), ValidationReport.create(validator, metadataValue));
                    }
                }
            }
        }
    }
}
