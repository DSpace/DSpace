/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.fields;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.servlet.multipart.Part;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.AuthorProfileInput;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.ValidationReport;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.AuthorProfile;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorProfileImageField extends AuthorProfileField {

    private static final String T_REMOVE_FILE_PREFIX = "xmlui.authorprofile.administrative.EditAuthorProfileForm.submit.remove.";
    private static final String T_ALTER_FILE_PREFIX = "xmlui.authorprofile.administrative.EditAuthorProfileForm.submit.change.";

    @Override
    public void renderField(Context context, List form, AuthorProfile authorProfile, AuthorProfileInput inputConfig, Request request, java.util.List<String> fieldsInError, java.util.Map<String,ValidationReport> invalidFields) throws WingException, AuthorizeException, SQLException {
        form.addLabel(message(MESSAGE_PREFIX + inputConfig.getId()));

        if(authorProfile != null && authorProfile.getAuthorProfilePicture() != null)
        {
            Bitstream authorProfilePicture = authorProfile.getAuthorProfilePicture();
            String url = request.getContextPath() + "/bitstream/id/" + authorProfilePicture.getID();
            url += "/?sequence=" + authorProfilePicture.getSequenceID();



            Item wingItem = form.addItem();

            wingItem.addFile(inputConfig.getId(), "hidden").setHelp(message(HELP_PREFIX+inputConfig.getId()));
            wingItem.addFigure(url, null, null);
            wingItem.addButton("submit_remove_file_" + inputConfig.getId()).setValue(message(T_REMOVE_FILE_PREFIX + inputConfig.getId()));
            wingItem.addButton("submit_alter_file_" + inputConfig.getId()).setValue(message(T_ALTER_FILE_PREFIX + inputConfig.getId()));

            wingItem.addHidden("remove_file_" + inputConfig.getId());

        }else{
            form.addItem().addFile(inputConfig.getId()).setHelp(message(HELP_PREFIX+inputConfig.getId()));
        }
    }

    @Override
    public void storeField(Request request, AuthorProfile authorProfile, AuthorProfileInput inputConfig, java.util.List<String> fieldsInError, java.util.Map<String,ValidationReport> invalidFields, Context context) throws AuthorizeException, SQLException, IOException {
        //Check if we need to remove our file
        boolean removeFile = Util.getBoolParameter(request, "remove_file_" + inputConfig.getId());
        if(removeFile)
        {
            authorProfile.setAuthorProfilePicture(null,null,null);
        }


        //Check for an upload of a new bitstream !
        Object object = request.get(inputConfig.getId());
        Part filePart = null;
        if (object instanceof Part) {
            filePart = (Part) object;
        }

        if (filePart != null && filePart.getSize() > 0)
        {
            InputStream is = filePart.getInputStream();
            if(inputConfig.getId().equals("authorProfileSingleImage"))
            {
                authorProfile.setAuthorProfilePicture(is,filePart.getFileName(),filePart.getMimeType());
            }
        }
    }
}
