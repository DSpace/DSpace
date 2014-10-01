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
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.content.MetadataFieldDescriptor;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorProfileTextAreaField extends AuthorProfileOneBoxField {


    protected void addTextField(List form, AuthorProfileInput inputConfig, java.util.List<String> fieldsInError, java.util.Map<String,ValidationReport> invalidFields, String value,Request request) throws WingException {
        Item item = form.addItem();
        TextArea text = item.addTextArea(inputConfig.getId());
        text.setLabel(message(MESSAGE_PREFIX + inputConfig.getId()));
        text.setHelp(message(HELP_PREFIX+inputConfig.getId()));
        if(StringUtils.isNotBlank(value))
        {
            text.setValue(value);
        }
        addValidatorsToItem(inputConfig,item);
        if(fieldsInError.contains(inputConfig.getId()))
        {
            text.addError(message(ERROR_PREFIX + inputConfig.getId()));
        }else if (invalidFields.keySet().contains(inputConfig.getId())){
            text.addError(message(INVALID_PREFIX + inputConfig.getId()));
        }
    }

    @Override
    protected void addRepeatableField(MetadataFieldDescriptor metadataFieldDescriptor, Composite composite, AuthorProfileInput inputConfig, String value, int index, Request request, java.util.List<String> fieldsInError, java.util.Map<String,ValidationReport> invalidFields) throws WingException {

        TextArea text = composite.addTextArea(inputConfig.getId() + "_" + index);
        if(StringUtils.isNotBlank(value))
        {
            text.setValue(value);
        }

        text.setHelp(message(HELP_PREFIX+inputConfig.getId()));
        if(fieldsInError.contains(inputConfig.getId()))
        {
            text.addError(message(ERROR_PREFIX + inputConfig.getId()));
        }else if (invalidFields.keySet().contains(inputConfig.getId())){
            text.addError(message(INVALID_PREFIX + inputConfig.getId()));
        }

        addRepeatableButtons(metadataFieldDescriptor, composite);
    }
}
