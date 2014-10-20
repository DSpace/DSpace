/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.fields;

import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.AuthorProfileInput;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.ValidationReport;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.Validator;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.*;
import java.util.Collection;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorProfileMultiMetadataField extends AuthorProfileField {

    private String separator = ":##:";

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    @Override
    public void renderField(Context context, List form, AuthorProfile authorProfile, AuthorProfileInput inputConfig, Request request, java.util.List<String> fieldsInError, Map<String, ValidationReport> invalidFields) throws WingException, AuthorizeException, SQLException {

        form.addLabel(message(MESSAGE_PREFIX + inputConfig.getId()));
        MetadataFieldDescriptor firstMetadataFieldDescriptor = inputConfig.getMetadataFields().get(0);
        MetadataField firstMetadataField = firstMetadataFieldDescriptor.getMetadataField(context);
        MetadataSchema firstMetadataFieldSchema = firstMetadataFieldDescriptor.getMetadataSchema(context);
        if (inputConfig.getRepeatable()) {
            Item wingItem = form.addItem(null, "clearfix");



            LinkedHashSet<String> values = new LinkedHashSet<String>(getRepeatableValues(request, inputConfig.getId(), inputConfig.getMetadataFields()));
            if (authorProfile != null) {
                DCValue[] dcValues = authorProfile.getMetadata(firstMetadataFieldSchema.getName(), firstMetadataField.getElement(), firstMetadataField.getQualifier(), org.dspace.content.Item.ANY);
                for (DCValue dcValue : dcValues) {
                    values.add(dcValue.value);
                }
            }
            //Check if we have ANY values at all
            int renderedValues = 0;
            if (0 < values.size()) {
                //Render our used values !
                for (String value : values) {
                    Composite composite = wingItem.addComposite(inputConfig.getId() + "-composite-" + renderedValues, "clearfix");
                    String decomposedValues[] = value.split(separator);
                    int index = 0;
                    for (MetadataFieldDescriptor metadataFieldDescriptor : inputConfig.getMetadataFields()) {
                        addRepeatableField(composite, inputConfig, metadataFieldDescriptor, index >= decomposedValues.length ? null : decomposedValues[index], renderedValues, fieldsInError, invalidFields);
                        index++;
                    }
                    addRepeatableButtons(inputConfig.getMetadataFields().iterator().next(), composite);
                    renderedValues++;
                }
            }

            //Add a new field
            Composite composite = wingItem.addComposite(inputConfig.getId() + "-composite-" + renderedValues, "clearfix");


            addRepeatableField(composite, inputConfig, firstMetadataFieldDescriptor, null, renderedValues, fieldsInError, invalidFields);
            for (int j = 1; j < inputConfig.getMetadataFields().size(); j++) {
                MetadataFieldDescriptor metadataFieldDescriptor = inputConfig.getMetadataFields().get(j);
                addRepeatableField(composite, inputConfig, metadataFieldDescriptor, null, renderedValues, fieldsInError, invalidFields);
            }
            addRepeatableButtons(inputConfig.getMetadataFields().iterator().next(), composite);
            addValidatorsToItem(inputConfig, wingItem);


        } else {
            Item item = form.addItem();
            Composite composite = item.addComposite(inputConfig.getId() + "-composite", "actual");
            addValidatorsToItem(inputConfig,item);
            java.util.List<MetadataFieldDescriptor> metadataFieldDescriptors = inputConfig.getMetadataFields();
            for (MetadataFieldDescriptor metadataFieldDescriptor : metadataFieldDescriptors) {
                MetadataField metadataField = metadataFieldDescriptor.getMetadataField(context);
                MetadataSchema metadataSchema = metadataFieldDescriptor.getMetadataSchema(context);
                Text text = composite.addText(inputConfig.getId() + metadataField.getFieldID());
                text.setLabel(message(MESSAGE_PREFIX + metadataField.toString()));

                if (authorProfile != null) {
                    String value = authorProfile.getMetadataFirstValue(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), org.dspace.content.Item.ANY);
                    if (StringUtils.isNotBlank(value)) {
                        text.setValue(value);
                    }
                }
            }
        }
    }

    protected void addRepeatableField(Composite composite, AuthorProfileInput inputConfig, MetadataFieldDescriptor metadataFieldDescriptor, String value, int index, java.util.List<String> fieldsInError, Map<String, ValidationReport> invalidFields) throws WingException, SQLException {
        Text text = null;
        MetadataField metadataField = metadataFieldDescriptor.getMetadataField(context);
        if (inputConfig.getRequired()) {
            text = composite.addText(inputConfig.getId() + metadataField.getFieldID() + "_" + index, "required actual");
        } else {
            text = composite.addText(inputConfig.getId() + metadataField.getFieldID() + "_" + index, "actual");
        }
        text.setLabel(message(MESSAGE_PREFIX + metadataFieldDescriptor.toString()));

        //text.setRequired(inputConfig.getRequired());
        if (index == 0){
            composite.setHelp(message(HELP_PREFIX + inputConfig.getId()));
        }

        if (StringUtils.isNotBlank(value)) {
            text.setValue(value);
        }
        if (fieldsInError.contains(inputConfig.getId())) {
            text.addError(message(ERROR_PREFIX + inputConfig.getId()));
        } else if (invalidFields.keySet().contains(inputConfig.getId())) {
            text.addError(message(INVALID_PREFIX + inputConfig.getId()));
        }
    }


    public void storeField(Request request, AuthorProfile authorProfile, AuthorProfileInput inputConfig, java.util.List<String> fieldsInError, Map<String, ValidationReport> invalidFields, Context ctx) throws AuthorizeException, SQLException {
        java.util.List<MetadataFieldDescriptor> metadataFieldDescriptors = inputConfig.getMetadataFields();

        if (inputConfig.getRepeatable()) {

            boolean validValuesAdded = false;

            //Clear our values
            for (MetadataFieldDescriptor metadataFieldDescriptor : metadataFieldDescriptors) {
                MetadataField metadataField = metadataFieldDescriptor.getMetadataField(context);
                MetadataSchema metadataSchema = metadataFieldDescriptor.getMetadataSchema(context);


                authorProfile.clearMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), org.dspace.content.Item.ANY);
                MetadataField composite = MetadataField.findByElement(ctx, metadataField.getSchemaID(), metadataField.getElement(), metadataField.getQualifier());
                //Store our values
                Collection<String> values = getRepeatableValues(request, inputConfig.getId(), inputConfig.getMetadataFields());
                for (String string : values) {

                    validValuesAdded = true;
                    if (!"".equals(string.replace(separator, "").trim())) {
                        authorProfile.addMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), null, inputConfig.clean(string, composite));
                    }


                }
                if (inputConfig.getRequired() && !validValuesAdded) {
                    fieldsInError.add(inputConfig.getId());
                }
            }

        } else {
            StringBuffer sb = new StringBuffer();
            boolean error = false;
            for (MetadataFieldDescriptor metadataFieldDescriptor : metadataFieldDescriptors) {
                MetadataField metadataField = metadataFieldDescriptor.getMetadataField(context);
                MetadataSchema metadataSchema = metadataFieldDescriptor.getMetadataSchema(context);

                String value = request.getParameter(inputConfig.getId() + metadataField.getFieldID());
                if (StringUtils.isBlank(value)) {
                    if (inputConfig.getRequired()) {
                        fieldsInError.add(inputConfig.getId());
                        error = true;
                    }
                } else {
                    Validator validator=inputConfig.validate(authorProfile,context,value);
                    if (validator==null) {
                        sb.append(value);
                        sb.append(separator);
                    } else {
                        invalidFields.put(inputConfig.getId(), ValidationReport.create(validator,value));
                        error = true;
                    }
                }
                MetadataField composite = MetadataField.findByElement(context, metadataSchema.getSchemaID(), metadataField.getElement(), metadataField.getQualifier());
                if (!error) {
                    if (!"".equals(sb.toString().replace(separator, "").trim())) {
                        authorProfile.clearMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), org.dspace.content.Item.ANY);
                        authorProfile.addMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), null, inputConfig.clean(sb.toString(), composite));
                    }
                }
            }
        }
    }

    public java.util.List<String> getRepeatableValues(Request request, String prefix, java.util.List<MetadataFieldDescriptor> metadataFieldDescriptors) throws SQLException {

        java.util.List<String> result = new LinkedList<String>();

        MetadataFieldDescriptor initialMetadataFieldDescriptor = metadataFieldDescriptors.get(0);
        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameter = (String) parameterNames.nextElement();
            if (parameter.startsWith(prefix + initialMetadataFieldDescriptor.getMetadataField(context).getFieldID())) {
                int index = Integer.parseInt(StringUtils.substringAfter(parameter, "_"));
                //Check that the first value is present, this is good enough !
                boolean isValid = StringUtils.isNotBlank(request.getParameter(parameter));
                StringBuffer vl = new StringBuffer();


                //We have one value, iterate over em again but this time store them
                for (MetadataFieldDescriptor metadataField : metadataFieldDescriptors) {
                    String value = request.getParameter(prefix + metadataField.getMetadataField(context).getFieldID() + "_" + index);
                    vl.append(value);
                    vl.append(separator);


                }
                if (!"".equals(vl.toString().replace(separator, "").trim()))
                    result.add(vl.toString());

            }
        }

        return result;
    }

}
