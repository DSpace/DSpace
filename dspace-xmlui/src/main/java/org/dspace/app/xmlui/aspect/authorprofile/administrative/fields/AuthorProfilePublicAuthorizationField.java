/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.fields;

import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.AuthorProfileInput;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.ValidationReport;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.AuthorProfile;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorProfilePublicAuthorizationField extends AuthorProfileField{

    @Override
    public void renderField(Context context, List form, AuthorProfile authorProfile, AuthorProfileInput inputConfig, Request request, java.util.List<String> fieldsInError, Map<String, ValidationReport> invalidFields) throws WingException, AuthorizeException, SQLException {
        boolean checked = false;
        String value = request.getParameter(inputConfig.getId());
        if(authorProfile != null){
            //No value present, attempt to retrieve it from our author profile
            if(StringUtils.isBlank(value)) {
                checked = AuthorizeManager.findByTypeIdGroupAction(context, authorProfile.getType(), authorProfile.getID(), Group.ANONYMOUS_ID, Constants.READ, -1) != null;
            }else{
                checked = BooleanUtils.toBoolean(value);
            }
        }

        CheckBox checkBox = form.addItem().addCheckBox(inputConfig.getId());
        checkBox.setLabel(message(MESSAGE_PREFIX + inputConfig.getId()));
        checkBox.setHelp(message(HELP_PREFIX+inputConfig.getId()));
        checkBox.addOption(checked, Boolean.TRUE.toString(), message(MESSAGE_PREFIX + inputConfig.getId() + "." + true));
    }

    @Override
    public void storeField(Request request, AuthorProfile authorProfile, AuthorProfileInput inputConfig, java.util.List<String> fieldsInError, Map<String, ValidationReport> invalidFields, Context context) throws AuthorizeException, SQLException, IOException {
        boolean isPublic = Util.getBoolParameter(request, inputConfig.getId());
        AuthorizeManager.removePoliciesActionFilter(context, authorProfile, Constants.READ);
        if(isPublic){
            AuthorizeManager.addPolicy(context, authorProfile, Constants.READ, Group.find(context, Group.ANONYMOUS_ID));
        }
    }


}
