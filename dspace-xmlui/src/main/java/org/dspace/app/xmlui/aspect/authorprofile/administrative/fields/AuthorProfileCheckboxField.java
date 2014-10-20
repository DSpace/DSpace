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
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.AuthorProfileInput;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.ValidationReport;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorProfileCheckboxField extends AuthorProfileField {

    private static final Logger log = Logger.getLogger(AuthorProfileCheckboxField.class);

    @Override
    public void renderField(Context context, List form, AuthorProfile authorProfile, AuthorProfileInput inputConfig, Request request, java.util.List<String> fieldsInError,java.util.Map<String,ValidationReport> invalidFields) throws WingException, AuthorizeException, SQLException {
        if(CollectionUtils.size(inputConfig.getMetadataFields()) == 1){
            MetadataFieldDescriptor metadataFieldDescriptor = inputConfig.getMetadataFields().get(0);
            MetadataField metadataField = metadataFieldDescriptor.getMetadataField(context);
            MetadataSchema metadataSchema = metadataFieldDescriptor.getMetadataSchema(context);

            //Attempt to get the value from our request
            String value = request.getParameter(inputConfig.getId());
            if(authorProfile != null){
                //No value present, attempt to retrieve it from our author profile
                if(StringUtils.isBlank(value)) {
                    value = authorProfile.getMetadataFirstValue(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), Item.ANY);
                }
            }

            boolean checked = BooleanUtils.toBoolean(value);

            CheckBox checkBox = form.addItem().addCheckBox(inputConfig.getId());
            checkBox.setLabel(message(MESSAGE_PREFIX + inputConfig.getId()));
            checkBox.setHelp(message(HELP_PREFIX+inputConfig.getId()));
            checkBox.addOption(checked, Boolean.TRUE.toString(), message(MESSAGE_PREFIX + inputConfig.getId() + "." + true));
        }else{
            log.warn("Error while rendering metadata field with id: " + inputConfig.getId() + " multiple metadata fields configured to a checkbox field.");
        }
    }

    @Override
    public void storeField(Request request, AuthorProfile authorProfile, AuthorProfileInput inputConfig, java.util.List<String> fieldsInError, java.util.Map<String,ValidationReport> invalidFields, Context context) throws AuthorizeException, SQLException, IOException {
        //Should just be a single metadata field !
        if(CollectionUtils.size(inputConfig.getMetadataFields()) == 1){
            boolean value = Util.getBoolParameter(request, inputConfig.getId());
            MetadataFieldDescriptor metadataFieldDescriptor = inputConfig.getMetadataFields().get(0);
            MetadataField metadataField = metadataFieldDescriptor.getMetadataField(context);
            MetadataSchema metadataSchema = metadataFieldDescriptor.getMetadataSchema(context);
            authorProfile.clearMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), Item.ANY);
            authorProfile.addMetadata(metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), null, String.valueOf(value));
        }
    }
}
