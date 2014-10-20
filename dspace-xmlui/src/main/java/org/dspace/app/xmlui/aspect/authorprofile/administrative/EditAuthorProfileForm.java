/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.AuthorProfileInput;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.AuthorProfileInputConfiguration;
import org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration.ValidationReport;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.AuthorProfile;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class EditAuthorProfileForm extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(EditAuthorProfileForm.class);
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_submit_create = message("xmlui.authorprofile.administrative.EditAuthorProfileForm.submit.create");
    private static final Message T_submit_edit = message("xmlui.authorprofile.administrative.EditAuthorProfileForm.submit.edit");
    private static final Message T_title = message("xmlui.authorprofile.administrative.EditAuthorProfileForm.title");
    private static final Message T_trail = message("xmlui.authorprofile.administrative.EditAuthorProfileForm.trail");
    private static final Message T_author_link = message("xmlui.authorprofile.administrative.author.link");
    private static final Message T_delete_author_profile = message("xmlui.authorprofile.administrative.author.delete");
    private AuthorProfileInputConfiguration authorProfileConfiguration;
    private AuthorProfile authorProfile;
    private List<String> fieldsInError;
    private Map<String,ValidationReport> invalidFields;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);

        Request request = ObjectModelHelper.getRequest(objectModel);
        authorProfileConfiguration = new DSpace().getSingletonService(AuthorProfileInputConfiguration.class);
        fieldsInError = new ArrayList<String>();
        //why is fieldsInError an arraylist. you don't know in advance how big it is.
        invalidFields = new HashMap<String,ValidationReport>();
        try {
            String submitButton = Util.getSubmitButton(request, null);
            if(StringUtils.equals(submitButton, "submit_create"))
            {
                //Create a new author profile (or attempt to)
                authorProfile = AuthorProfile.create(context);
                processFields(request, true);
            }else{
                int authorProfileId = Util.getIntParameter(request, "authorProfileId");
                authorProfile = AuthorProfile.find(context, authorProfileId);
                if(parameters.isParameter("delete")){
                    authorProfile.delete();

                }
                if(StringUtils.equals(submitButton, "submit_edit"))
                {
                    processFields(request, false);
                }
            }
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
            throw new ProcessingException(e);
        }
    }

    private void processFields(Request request, boolean created) throws SQLException, AuthorizeException, IOException {
        List<AuthorProfileInput> profileFields = authorProfileConfiguration.getAuthorProfileFields();
        for (AuthorProfileInput authorProfileInput : profileFields) {
            authorProfileInput.getInputType().storeField(request, authorProfile, authorProfileInput, fieldsInError,invalidFields, this.context);
        }

        if(0 < fieldsInError.size()||invalidFields.size()>0)
        {
            //Rollback our changes we just made since some fields are in error
            context.getDBConnection().rollback();
            if(created)
            {
                //In case we just created one, remove it from our cache
                context.removeCached(authorProfile, authorProfile.getID());
                authorProfile = null;
            }
        }else{
            addUpdateDate(authorProfile);
            //Abort the context & our changes !
            authorProfile.update();
            context.commit();

        }
    }

    private void addUpdateDate(AuthorProfile authorProfile)
    {
        authorProfile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "updateDate", null, Item.ANY);
        Calendar cal = Calendar.getInstance();
        DCDate date = new DCDate(cal.getTime());
        authorProfile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "updateDate", null, null, date.toString());
    }


    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        super.addPageMeta(pageMeta);
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null, T_trail);

    }


    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException, ProcessingException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Division mainDivision = body.addInteractiveDivision("author-profile-form", request.getContextPath() + "/admin/authorprofile", Division.METHOD_MULTIPART, "");
        String url = null;
        if(authorProfile != null){
            String authorSynonym = authorProfile.getMetadataFirstValue(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name", "synonym", Item.ANY);
            if(StringUtils.isBlank(authorSynonym)) {
                String firstName=authorProfile.getMetadataFirstValue(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name", "first", Item.ANY);
                String lastName=authorProfile.getMetadataFirstValue(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name", "last", Item.ANY);
                url=contextPath+"/author-page?name.first="+firstName+"&name.last="+lastName;
            }else {
                url=contextPath+"/author/" + authorSynonym;
            }
        }

        if(authorProfile!=null){
            Para p=mainDivision.addPara("author-link-para","para clearfix");
            p.addXref(url, T_author_link);
            p.addXref(url," ","author-profile");
        }
        org.dspace.app.xmlui.wing.element.List mainForm = mainDivision.addList("author-profile", org.dspace.app.xmlui.wing.element.List.TYPE_FORM);

        List<AuthorProfileInput> authorProfileFields = authorProfileConfiguration.getAuthorProfileFields();
        for (AuthorProfileInput authorProfileInput : authorProfileFields) {
            authorProfileInput.getInputType().renderField(context, mainForm, authorProfile, authorProfileInput, request, fieldsInError,invalidFields);
        }

        if(authorProfile == null)
        {
            mainDivision.addPara().addButton("submit_create").setValue(T_submit_create);
        }else{
            Para p=mainDivision.addPara("author-link-para","para clearfix");
            p.addXref(url,T_author_link);
            p.addXref(url," ","author-profile");
            mainDivision.addPara().addXref(contextPath+"/admin/authorprofile?delete=true&authorProfileId="+authorProfile.getID(), T_delete_author_profile);
            Para buttonsPara = mainDivision.addPara();
            buttonsPara.addHidden("authorProfileId").setValue(String.valueOf(authorProfile.getID()));
            buttonsPara.addButton("submit_edit").setValue(T_submit_edit);
        }
    }
}
