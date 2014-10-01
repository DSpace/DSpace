/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.fields;

import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.AuthorProfileInput;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.ValidationReport;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.Validator;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.AuthorProfile;
import org.dspace.content.MetadataFieldDescriptor;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TreeMap;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class AuthorProfileField extends AbstractDSpaceTransformer {

    protected static final String MESSAGE_PREFIX = "xmlui.authorprofile.administrative.field.head.";
    protected static final String ERROR_PREFIX = "xmlui.authorprofile.administrative.field.error.";
    protected static final String INVALID_PREFIX = "xmlui.authorprofile.administrative.field.invalid.";
    protected static final String HELP_PREFIX = "xmlui.authorprofile.administrative.field.help.";
    private static final Message T_button_add = message("xmlui.general.add");
    private static final Message T_button_remove = message("xmlui.general.remove");

    public abstract void renderField(Context context, List form, AuthorProfile authorProfile, AuthorProfileInput inputConfig, Request request, java.util.List<String> fieldsInError, java.util.Map<String,ValidationReport> invalidFields) throws WingException, AuthorizeException, SQLException;

    public abstract void storeField(Request request, AuthorProfile authorProfile, AuthorProfileInput inputConfig, java.util.List<String> fieldsInError, java.util.Map<String,ValidationReport> invalidFields, Context context) throws AuthorizeException, SQLException, IOException;



    public java.util.List<String> getRepeatableValues(Request request, String prefix)
    {
        TreeMap<String, String> result = new TreeMap<String, String>();

        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameter = (String) parameterNames.nextElement();
            if(parameter.startsWith(prefix)){
                result.put(parameter, request.getParameter(parameter));
            }
        }
        return new ArrayList<String>(result.values());
    }

    protected final void addRepeatableButtons(MetadataFieldDescriptor metadataFieldDescriptor, Composite composite) throws WingException {
        //Since our composite cannot add figures we add a text box & use the classes to pick this up in xsl
        //And transform it to the images
//        composite.addText(request.getContextPath() + "/static/images/del.png", "author-profile-img del");
        composite.addButton(metadataFieldDescriptor.toString() + "-author-profile-add", "author-profile-add").setValue(T_button_add);
        composite.addButton(metadataFieldDescriptor.toString() + "-author-profile-remove", "author-profile-remove").setValue(T_button_remove);
    }

    protected void addValidatorsToItem(AuthorProfileInput inputConfig, Item wingItem) throws WingException {
        for (Validator vl : inputConfig.getValidators())
            if (vl.hasClientComponent())
                wingItem.addHidden(vl.getClientComponentClass()).setValue(vl.getClientComponent());
    }
}
