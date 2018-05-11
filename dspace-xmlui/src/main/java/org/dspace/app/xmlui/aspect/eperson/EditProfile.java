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
import org.dspace.app.util.CollectionDropDown;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Field;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.SubscribeService;
import org.xml.sax.SAXException;


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

    protected SubscribeService subscribeService = EPersonServiceFactory.getInstance().getSubscribeService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

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
       if (request.getParameter("submit") != null)
       {
           defaultFirstName = request.getParameter("first_name");
           defaultLastName = request.getParameter("last_name");
           defaultPhone = request.getParameter("phone");
           defaultLanguage = request.getParameter("language");
       }
       else if (eperson != null)
       {
            defaultFirstName = eperson.getFirstName();
            defaultLastName = eperson.getLastName();
            defaultPhone = ePersonService.getMetadata(eperson, "phone");
            defaultLanguage = eperson.getLanguage();
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
       identity.setHead(T_head_identify);
       
       // Email
       identity.addLabel(T_email_address);
       identity.addItem(email);
       
       // First name
       Text firstName = identity.addItem().addText("first_name");
       firstName.setAutofocus("autofocus");
       firstName.setRequired();
       firstName.setLabel(T_first_name);
       firstName.setValue(defaultFirstName);
       if (errors.contains("first_name"))
       {
           firstName.addError(T_error_required);
       }
       if (!registering && !DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.user.editmetadata", true))
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
       if (!registering &&!DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.user.editmetadata", true))
       {
           lastName.setDisabled();
       }
       
       // Phone
       Text phone = identity.addItem().addText("phone");
       phone.setLabel(T_telephone);
       phone.setValue(defaultPhone);
       if (errors.contains("phone"))
       {
           phone.addError(T_error_required);
       }
       if (!registering && !DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.user.editmetadata", true))
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
       if (!registering && !DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.user.editmetadata", true))
       {
           lang.setDisabled();
       }

       // Subscriptions
       if (!registering)
       {
           List subscribe = form.addList("subscriptions",List.TYPE_FORM);
           subscribe.setHead(T_subscriptions);
           
           subscribe.addItem(T_subscriptions_help);
           
           java.util.List<Subscription> currentList = subscribeService.getSubscriptions(context, context.getCurrentUser());
           java.util.List<Collection> possibleList = collectionService.findAll(context);
           
           Select subscriptions = subscribe.addItem().addSelect("subscriptions");
           subscriptions.setLabel(T_email_subscriptions);
           subscriptions.setHelp("");
           subscriptions.enableAddOperation();
           subscriptions.enableDeleteOperation();
           
           subscriptions.addOption(-1,T_select_collection);
	       CollectionDropDown.CollectionPathEntry[] possibleEntries = CollectionDropDown.annotateWithPaths(context, possibleList);
           for (CollectionDropDown.CollectionPathEntry possible : possibleEntries)
           {
               subscriptions.addOption(possible.collection.getID().toString(), possible.path);
           }

           for (Subscription subscription : currentList) {
               subscriptions.addInstance().setOptionSelected(subscription.getCollection().getID().toString());
           }
       }
       
       
       if (allowSetPassword)
       {
           List security = form.addList("security",List.TYPE_FORM);
           security.setHead(T_head_security);
           
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
       
       
       
       if (!registering)
       {
                // Add a list of groups that this user is apart of.
                        java.util.Set<Group> memberships = groupService.allMemberGroupsSet(context, context.getCurrentUser());
                
                
                        // Not a member of any groups then don't do anything.
                        if (!(memberships.size() > 0))
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
     * property webui.supported.locales
     * returns an array of Locales or null
     *
     * @return an array of supported Locales or null
     */
    private static Locale[] getSupportedLocales()
    {
        String[] ll = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("webui.supported.locales");
        if(ll != null && ll.length>0)
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
