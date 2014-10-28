/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authority.orcid.Orcid;
import org.dspace.authority.orcid.model.Bio;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.Subscribe;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Display a form that allows the user to edit their profile.
 * There are two cases in which this can be used: 1) when an
 * existing user is attempting to edit their own profile, and
 * 2) when a new user is registering for the first time.
 *
 * There are several parameters this transformer accepts:
 *
 * email - The email address of the user registering for the first time.
 *
 * registering - A boolean value to indicate whether the user is registering for the first time.
 *
 * retryInformation - A boolean value to indicate whether there was an error with the user's profile.
 *
 * retryPassword - A boolean value to indicate whether there was an error with the user's password.
 *
 * allowSetPassword - A boolean value to indicate whether the user is allowed to set their own password.
 *
 * @author Scott Phillips
 */
public class EditProfile extends AbstractDSpaceTransformer
{
    private static Logger log = Logger.getLogger(EditProfile.class);

    /** Language string used: */
    private static final Message T_title_create =
        message("xmlui.EPerson.EditProfile.title_create");
    
    private static final Message T_title_update =
        message("xmlui.EPerson.EditProfile.title_update");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail_new_registration =
        message("xmlui.EPerson.trail_new_registration");
    
    private static final Message T_trail_update =
        message("xmlui.EPerson.EditProfile.trail_update");
    
    private static final Message T_head_create =
        message("xmlui.EPerson.EditProfile.head_create");
    
    private static final Message T_head_update =
        message("xmlui.EPerson.EditProfile.head_update");
    
    private static final Message T_email_address =
        message("xmlui.EPerson.EditProfile.email_address");
    
    private static final Message T_first_name =
        message("xmlui.EPerson.EditProfile.first_name");
    
    private static final Message T_error_required =
        message("xmlui.EPerson.EditProfile.error_required");
    
    private static final Message T_last_name =
        message("xmlui.EPerson.EditProfile.last_name");
    
    private static final Message T_telephone =
        message("xmlui.EPerson.EditProfile.telephone");
    
    private static final Message T_language =
        message("xmlui.EPerson.EditProfile.Language");
    
    private static final Message T_create_password_instructions =
        message("xmlui.EPerson.EditProfile.create_password_instructions");
    
    private static final Message T_update_password_instructions =
        message("xmlui.EPerson.EditProfile.update_password_instructions");
    
    private static final Message T_password =
        message("xmlui.EPerson.EditProfile.password");
    
    private static final Message T_error_invalid_password =
        message("xmlui.EPerson.EditProfile.error_invalid_password");
    
    private static final Message T_confirm_password =
        message("xmlui.EPerson.EditProfile.confirm_password");
    
    private static final Message T_error_unconfirmed_password =
        message("xmlui.EPerson.EditProfile.error_unconfirmed_password");
    
    private static final Message T_submit_update =
        message("xmlui.EPerson.EditProfile.submit_update");
    
    private static final Message T_submit_create =
        message("xmlui.EPerson.EditProfile.submit_create");
    
    private static final Message T_subscriptions =
        message("xmlui.EPerson.EditProfile.subscriptions");

    private static final Message T_subscriptions_help =
        message("xmlui.EPerson.EditProfile.subscriptions_help");

    private static final Message T_email_subscriptions =
        message("xmlui.EPerson.EditProfile.email_subscriptions");

    private static final Message T_select_collection =
        message("xmlui.EPerson.EditProfile.select_collection");
 
    private static final Message T_head_auth =
        message("xmlui.EPerson.EditProfile.head_auth");
    
    private static final Message T_head_identify =
        message("xmlui.EPerson.EditProfile.head_identify");
    
    private static final Message T_head_security =
        message("xmlui.EPerson.EditProfile.head_security");

    private static final Message T_terms =
	message("xmlui.EPerson.EditProfile.terms");

    private static final Message T_terms_help =
            message("xmlui.EPerson.EditProfile.terms_help");

    private static final Message T_terms_checkbox =
            message("xmlui.EPerson.EditProfile.terms.checkbox");

    
    private static Locale[] supportedLocales = getSupportedLocales();
    static
    {
        Arrays.sort(supportedLocales, new Comparator<Locale>() {
            public int compare(Locale a, Locale b)
            {
                return a.getDisplayName().compareTo(b.getDisplayName());
            }
        });
    }
    
    /** The email address of the user registering for the first time.*/
    private String email;

    /** Determine if the user is registering for the first time */
    private boolean registering;
    
    /** Determine if the user is allowed to set their own password */
    private boolean allowSetPassword;
    
    /** A list of fields in error */
    private java.util.List<String> errors;
    
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver,objectModel,src,parameters);
        
        this.email = parameters.getParameter("email","unknown");
        this.registering = parameters.getParameterAsBoolean("registering",false);
        this.allowSetPassword = parameters.getParameterAsBoolean("allowSetPassword",false);
        
        String errors = parameters.getParameter("errors","");
        if (errors.length() > 0)
        {
            this.errors = Arrays.asList(errors.split(","));
        }
        else
        {
            this.errors = new ArrayList<String>();
        }
        
        // Ensure that the email variable is set.
        if (eperson != null)
        {
            this.email = eperson.getEmail();
        }
    }
       
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        // Set the page title
        if (registering)
        {
            pageMeta.addMetadata("title").addContent(T_title_create);
        }
        else
        {
            pageMeta.addMetadata("title").addContent(T_title_update);
        }
        
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        if (registering)
        {
            pageMeta.addTrail().addContent(T_trail_new_registration);
        }
        else
        {
            pageMeta.addTrail().addContent(T_trail_update);
        }
    }
    
    
   public void addBody(Body body) throws WingException, SQLException
   {
       // Log that we are viewing a profile
       log.info(LogManager.getHeader(context, "view_profile", ""));

       Request request = ObjectModelHelper.getRequest(objectModel);
       
       String defaultFirstName="",defaultLastName="",defaultPhone="";
       String defaultLanguage=null;
       String defaultOrcidId=null;
       boolean defaultTerms=false;
       if (request.getParameter("submit") != null)
       {
           defaultFirstName = request.getParameter("first_name");
           defaultLastName = request.getParameter("last_name");
           defaultPhone = request.getParameter("phone");
           defaultLanguage = request.getParameter("language");
       }
       if (request.getParameter("remove-link") != null)
       {
           try{
               //remove orcid information from eperson
               eperson.setMetadata("orcid","");
               eperson.setMetadata("access_token","");
               defaultOrcidId=null;
               eperson.update();
               context.commit();
           }catch (Exception e)
           {
               log.error("error when remove orcid id on eperson",e);
           }

       }
       if (request.getParameter("link") != null)
       {
           try{
           //redirect to orcid page to get authentication from orcid
           HttpServletResponse response1 =  (HttpServletResponse)objectModel.get("httpresponse");

           response1.sendRedirect("/oauth-login");
           }catch (Exception e)
           {
               log.error("error when try to link to orcid",e);
           }

       }
       else if (eperson != null)
       {
            defaultFirstName = eperson.getFirstName();
            defaultLastName = eperson.getLastName();
            defaultPhone = eperson.getPhone();
            defaultLanguage = eperson.getLanguage();
		    defaultTerms = eperson.getTerms();
            try{
                defaultOrcidId = eperson.getMetadata("orcid");
            }catch (Exception e)

            {
                log.error("error when getting the orcid id from eperson metadata",e);
            }
       }
       
       String action = contextPath;
       if (registering)
       {
           action += "/register";
       }
       else
       {
           action += "/profile";
       }
       
       
       
       
       Division profile = body.addInteractiveDivision("information",
               action,Division.METHOD_POST,"primary");
       
       if (registering)
       {
           profile.setHead(T_head_create);
       }
       else
       {
           profile.setHead(T_head_update);
       }
       
       // Add the progress list if we are registering a new user
       if (registering)
       {
           EPersonUtils.registrationProgressList(profile, 2);
       }
       
       
       
       
       
       List form = profile.addList("form",List.TYPE_FORM);
       
       List identity = form.addList("identity",List.TYPE_FORM);
       identity.addItem(T_head_identify);
       
       // Email
       identity.addLabel(T_email_address);
       identity.addItem(email);

       // First name
       Text firstName = identity.addItem().addText("first_name");
       firstName.setRequired();
       firstName.setLabel(T_first_name);
       firstName.setValue(defaultFirstName);
       if (errors.contains("first_name"))
       {
           firstName.addError(T_error_required);
       }
       if (!registering && !ConfigurationManager.getBooleanProperty("xmlui.user.editmetadata", true))
       {
           firstName.setDisabled();
       }
       
       // Last name
       Text lastName = identity.addItem().addText("last_name");
       lastName.setRequired();
       lastName.setLabel(T_last_name);
       lastName.setValue(defaultLastName);
       if (errors.contains("last_name"))
       {
           lastName.addError(T_error_required);
       }
       if (!registering &&!ConfigurationManager.getBooleanProperty("xmlui.user.editmetadata", true))
       {
           lastName.setDisabled();
       }
       
       // Phone
       Text phone = identity.addItem().addText("phone");
       phone.setRequired();
       phone.setLabel(T_telephone);
       phone.setValue(defaultPhone);
       if (errors.contains("phone"))
       {
           phone.addError(T_error_required);
       }
       if (!registering && !ConfigurationManager.getBooleanProperty("xmlui.user.editmetadata", true))
       {
           phone.setDisabled();
       }
        
       // Language
       Select lang = identity.addItem().addSelect("language");
       lang.setLabel(T_language);
       if (supportedLocales.length > 0)
       {
           for (Locale lc : supportedLocales)
           {
               lang.addOption(lc.toString(), lc.getDisplayName());
           }
       }
       else
       {
           lang.addOption(I18nUtil.DEFAULTLOCALE.toString(), I18nUtil.DEFAULTLOCALE.getDisplayName());
       }
       lang.setOptionSelected((defaultLanguage == null || defaultLanguage.equals("")) ?
                              I18nUtil.DEFAULTLOCALE.toString() : defaultLanguage);
       if (!registering && !ConfigurationManager.getBooleanProperty("xmlui.user.editmetadata", true))
       {
           lang.setDisabled();
       }

       /* Subscriptions aren't currently used in Dryad
       // Subscriptions
       if (!registering)
       {
           List subscribe = form.addList("subscriptions",List.TYPE_FORM);
           subscribe.setHead(T_subscriptions);
           
           subscribe.addItem(T_subscriptions_help);
           
           Collection[] currentList = Subscribe.getSubscriptions(context, context.getCurrentUser());
           Collection[] possibleList = Collection.findAll(context);
           
           Select subscriptions = subscribe.addItem().addSelect("subscriptions");
           subscriptions.setLabel(T_email_subscriptions);
           subscriptions.setHelp("");
           subscriptions.enableAddOperation();
           subscriptions.enableDeleteOperation();
           
           subscriptions.addOption(-1,T_select_collection);
           for (Collection possible : possibleList)
           {
               String name = possible.getMetadata("name");
               if (name.length() > 50)
               {
                   name = name.substring(0, 47) + "...";
               }
               subscriptions.addOption(possible.getID(), name);
           }
                   
           for (Collection collection: currentList)
           {
               subscriptions.addInstance().setOptionSelected(collection.getID());
           }
       }
       */

       if (allowSetPassword)
       {
           List security = form.addList("security",List.TYPE_FORM);
           security.addItem(T_head_security);
           
           if (registering)
           {
                   security.addItem().addContent(T_create_password_instructions);
           }
           else
           {
                   security.addItem().addContent(T_update_password_instructions);
           }
           
           
           Field password = security.addItem().addPassword("password");
           password.setLabel(T_password);
           if (registering)
           {
               password.setRequired();
           }
           if (errors.contains("password"))
           {
               password.addError(T_error_invalid_password);
           }
           
           Field passwordConfirm = security.addItem().addPassword("password_confirm");
           passwordConfirm.setLabel(T_confirm_password);
           if (registering)
           {
               passwordConfirm.setRequired();
           }
           if (errors.contains("password_confirm"))
           {
               passwordConfirm.addError(T_error_unconfirmed_password);
           }
       }
       /* temporarily disable ORCID linking
          
       List orcid = form.addList("orcid",List.TYPE_FORM);
       orcid.setHead("Associate Account with ORCID");
       if(defaultOrcidId!=null&&defaultOrcidId.length()>0){
           orcid.addItem().addContent("Your account is now associated with the following ORCID ID. You may now authenticate with this DSpace exclusively with your ORCID login. If this ORCID account is incorrect, it may be disconnected by selecting the \"Disconnect from ORCID\" button.\n");
           orcid.addLabel("Orcid Id");
           orcid.addItem().addContent(defaultOrcidId);
           orcid.addItem().addButton("remove-link").setValue("Disconnect from ORCID");

       }
       else
       {
           orcid.addItem().addContent("Select the following button to connect to ORCID and associate this profile with your ORCID account.");
           orcid.addItem().addButton("link").setValue("Link to Orcid");
       }
       */
       List tl = form.addList("terms",List.TYPE_FORM);
       tl.setHead(T_terms);
       tl.addItem(T_terms_help);

       //Terms and condition
       CheckBox termsCheckBox = tl.addItem().addCheckBox("terms");
       termsCheckBox.addOption(defaultTerms,"true",T_terms_checkbox);

       if (errors.contains("terms"))
       {
           termsCheckBox.addError(T_error_required);
       }


       Button submit = form.addItem().addButton("submit");
       if (registering)
       {
           submit.setValue(T_submit_update);
       }
       else
       {
           submit.setValue(T_submit_create);
       }
       
       profile.addHidden("eperson-continue").setValue(knot.getId());
       
       
       /* The list of groups is not displayed for Dryad users.
       if (!registering)
       {
                // Add a list of groups that this user is apart of.
                        Group[] memberships = Group.allMemberGroups(context, context.getCurrentUser());
                
                
                        // Not a member of any groups then don't do anything.
                        if (!(memberships.length > 0))
                        {
                            return;
                        }
                        
                        List list = profile.addList("memberships");
                        list.setHead(T_head_auth);
                        for (Group group: memberships)
                        {
                                list.addItem(group.getName());
                        }
       }
       */
   }
   
   /**
    * Recycle
    */
    public void recycle()
    {
        this.email = null;
        this.errors = null;
        super.recycle();
    }

    /**
     * get the available Locales for the User Interface as defined in dspace.cfg
     * property xmlui.supported.locales
     * returns an array of Locales or null
     *
     * @return an array of supported Locales or null
     */
    private static Locale[] getSupportedLocales()
    {
        String ll = ConfigurationManager.getProperty("xmlui.supported.locales");
        if (ll != null)
        {
            return I18nUtil.parseLocales(ll);
        }
        else
        {
            Locale result[] = new Locale[1];
            result[0] =  I18nUtil.DEFAULTLOCALE;
            return result;
        }
    }
}
