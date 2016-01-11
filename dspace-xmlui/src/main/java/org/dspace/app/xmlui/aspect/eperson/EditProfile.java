/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.CollectionDropDown;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Field;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.eperson.Subscribe;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceUserAllowance;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserMetadata;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserRegistration;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;


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
 * based on class by Scott Phillips
 * modified for LINDAT/CLARIN
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

	private static final Message T_invalid_item =
		message("xmlui.EPerson.EditProfile.invalid_item");

	private static final Message T_signed_licenses =
			message("xmlui.EPerson.EditProfile.signed_licenses");


	//private static final Message T_head_identify =
    //    message("xmlui.EPerson.EditProfile.head_identify");
    
    private static final Message T_head_security =
        message("xmlui.EPerson.EditProfile.head_security");
    
    private static final Message T_auto_filled_in = message("xmlui.EPerson.EditProfile.auto_filled_in");

    private static final Message T_usermetadata_head = message("xmlui.EPerson.EditProfile.usermetadata.head");
    
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
    
	/**
	 * Determine if the user is allowed to set their own password UFAL: this is
	 * not correct, if you have local accounts enabled it will be always TRUE
	 * */
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
    
    
	private boolean isAdmin() throws SQLException {
		return AuthorizeManager.isAdmin(context);
	}

	private Text add_key_pair(String item_name, Message key, String value,
			List identity, Message error) throws WingException {
		return add_key_pair(item_name, key, value, null, identity, error, false);
       }
       
	// key, help, error can be null
	private Text add_key_pair(String item_name, Message key, String value,
			Message help, List identity, Message error, boolean auto_filled)
			throws WingException {
		Text text = identity.addItem().addText(item_name);
		text.setValue(value);
		// Message vs String hack (we would have to have more methods otherwise)
		if (null != key) {
			text.setLabel(key);
		}
		if (null != help) {
			text.setHelp(help);
		}
		if (null != error) {
			text.addError(error);
		}
		// mandatory means it will be overwritten during every login
		if (auto_filled
				|| (!registering && !ConfigurationManager.getBooleanProperty(
						"xmlui.user.editmetadata", true))) {
			text.setDisabled();
		}
		return text;
	}

	// error messages
	//

	private Message get_error(java.util.List<String> errors, String key,
			Message err_msg) {
		return errors.contains(key) ? err_msg : null;
	}

	//
	//
	private void add_subscriptions(List form) throws WingException,
			SQLException {
		List subscribe = form.addList("subscriptions", List.TYPE_FORM,
				"alert alert-admin");
		subscribe.setHead(T_subscriptions);
		subscribe.addItem(T_subscriptions_help);
       
		Collection[] currentList = Subscribe.getSubscriptions(context,
				context.getCurrentUser());
		Collection[] possibleList = Collection.findAll(context);
       
		Select subscriptions = subscribe.addItem().addSelect("subscriptions");
		subscriptions.setLabel(T_email_subscriptions);
		subscriptions.setHelp("");
		subscriptions.enableAddOperation();
		subscriptions.enableDeleteOperation();
       
		subscriptions.addOption(-1, T_select_collection);
		CollectionDropDown.CollectionPathEntry[] possibleEntries = CollectionDropDown.annotateWithPaths(possibleList);
		for(CollectionDropDown.CollectionPathEntry possible : possibleEntries)
       {
			subscriptions.addOption(possible.collection.getID(), possible.path);
       }
       
		for (Collection collection : currentList) {
			subscriptions.addInstance().setOptionSelected(collection.getID());
		}
       }
       
	private void add_set_password(List form, boolean cause_admin)
			throws WingException {
		List security = form.addList("security", List.TYPE_FORM,
				cause_admin ? "alert alert-admin" : "");
		security.setHead(T_head_security);
       
		if (registering) {
			security.addItem().addContent(T_create_password_instructions);
		} else {
			security.addItem().addContent(T_update_password_instructions);
		}
       
		Field password = security.addItem().addPassword("password");
		password.setLabel(T_password);
		if (registering) {
			password.setRequired();
       }
		if (errors.contains("password")) {
			password.addError(T_error_invalid_password);
       }
       
		Field passwordConfirm = security.addItem().addPassword(
				"password_confirm");
		passwordConfirm.setLabel(T_confirm_password);
		if (registering) {
			passwordConfirm.setRequired();
		}
		if (errors.contains("password_confirm")) {
			passwordConfirm.addError(T_error_unconfirmed_password);
       }
       }
       
	private void add_list_of_groups(Division profile) throws SQLException,
			WingException {
		// Add a list of groups that this user is apart of.
		Group[] memberships = Group.allMemberGroups(context,
				context.getCurrentUser());
		// Not a member of any groups then don't do anything.
		if (!(memberships.length > 0))
			return;

		List list = profile.addDivision("memberships_div", "alert").addList(
				"memberships", List.TYPE_SIMPLE);
		list.setHead(T_head_auth);
		for (Group group : memberships) {
			list.addItem(group.getName());
       }
       }
        
	private void add_language(List identity, String defaultLanguage)
			throws WingException {
	    if(!ConfigurationManager.getBooleanProperty("lr", "lr.xmlui.user.showlanguage", true))
        {
	        identity.addItem().addHidden("language").setValue((defaultLanguage == null || defaultLanguage
                    .equals("")) ? I18nUtil.DEFAULTLOCALE.toString()
                    : defaultLanguage);
        }
	    else {
       Select lang = identity.addItem().addSelect("language");
       lang.setLabel(T_language);
    		if (supportedLocales.length > 0) {
    			for (Locale lc : supportedLocales) {
               lang.addOption(lc.toString(), lc.getDisplayName());
           }
    		} else {
    			lang.addOption(I18nUtil.DEFAULTLOCALE.toString(),
    					I18nUtil.DEFAULTLOCALE.getDisplayName());
       }
    		lang.setOptionSelected((defaultLanguage == null || defaultLanguage
    				.equals("")) ? I18nUtil.DEFAULTLOCALE.toString()
    				: defaultLanguage);
    		if (!registering
    				&& !ConfigurationManager.getBooleanProperty(
    						"xmlui.user.editmetadata", true)) {
           lang.setDisabled();
       }
	    }
	}

	private void add_additional_metadata(List identity) throws SQLException,
			WingException {
		if (eperson == null) {
			identity.addLabel(null, "alert alert-admin").addContent(
					"Cannot fetch private metadata - eperson is null!");
			return;
		}

		// last login
		String last_login = eperson.getLoggedIn() != null ? eperson.getLoggedIn() : "Not logged in yet";
		List lastlogin = identity.addList("lastlogin", List.TYPE_FORM, "alert");
		lastlogin.addLabel("Last Login");
		lastlogin.addItem(last_login);

		List meta_form = identity.addList("usermetadata", List.TYPE_FORM,
				"alert alert-danger");
		meta_form.setHead(T_usermetadata_head);
		Map<String, String> metadata = DSpaceApi.getUserMetadata(eperson);
		if (metadata == null) {
			identity.addLabel(null, "alert alert-error").addContent(
					"Cannot fetch private metadata - metadata is null!");
			return;
           }

		for (Map.Entry<String, String> item : metadata.entrySet()) {
			// we want label from String not Message so set it manually
			add_key_pair(item.getKey(), null, item.getValue(), meta_form, null)
					.setLabel(item.getKey());
           }
       }
       
       
	private void add_signed_licenses(Division profile) throws WingException {

		IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
		functionalityManager.openSession();

		try {

			java.util.List<LicenseResourceUserAllowance> licenses = functionalityManager.getSignedLicensesByUser(eperson.getID());

			// hack for group by /////////
			/*
			java.util.List<String> keys = new ArrayList<String>();
			java.util.List<LicenseResourceUserAllowance> licenses_group_by = new ArrayList<LicenseResourceUserAllowance>();

			for (LicenseResourceUserAllowance license : licenses) {
				String createdOn = DateFormatUtils.format(license.getCreatedOn(), "ddMMyyyyhhmm");
				String epersonID = "" + license.getUserRegistration().getEpersonId();
				String licenseID = "" + license.getLicenseResourceMapping().getLicenseDefinition().getLicenseId();
				String key = createdOn + ":" + epersonID + ":" + licenseID;
				if(!keys.contains(key)) {
					keys.add(key);
					licenses_group_by.add(license);
           }
           }
			*/
			/////////////////////////////
			if(licenses!=null && licenses.size()>0) {
           
				Table wftable = profile.addTable("singed-licenses", 1, 6);
           
				Row wfhead = wftable.addRow(Row.ROLE_HEADER);

				// table items - because of GUI not all columns could be shown
	            wfhead.addCellContent("ID");
	            wfhead.addCellContent("DATE");
				wfhead.addCellContent("LICENSE");
				wfhead.addCellContent("ITEM");
				wfhead.addCellContent("BITSTREAM");
				wfhead.addCellContent("EXTRA METADATA");

				for (LicenseResourceUserAllowance license : licenses)
				{
					int bitstreamID = license.getLicenseResourceMapping().getBitstreamId();
					LicenseDefinition ld = license.getLicenseResourceMapping().getLicenseDefinition();
	                UserRegistration ur = license.getUserRegistration();
					Date signingDate = license.getCreatedOn();

	                Row r = wftable.addRow(null, null, "font_smaller bold");
	                String id = DateFormatUtils.format(signingDate, "yyyyMMddhhmmss")
	                		+ "-" + ur.getEpersonId()
	                		+ "-" + bitstreamID;
	                r.addCellContent(id);

	                r.addCellContent(DateFormatUtils.format(signingDate, "yyyy-MM-dd hh:mm:ss"));

	                r.addCell().addXref(ld.getDefinition(), ld.getName());

	                Bitstream bitstream = Bitstream.find(context, bitstreamID);
	                org.dspace.content.Item item = (org.dspace.content.Item)bitstream.getParentObject();
					String base = ConfigurationManager.getProperty("dspace.url");

					if ( null != item ) {
						StringBuffer itemLink = new StringBuffer().append(base)
							.append(base.endsWith("/") ? "" : "/")
							.append("/handle/")
							.append(item.getHandle());

						r.addCell().addXref(itemLink.toString(), "" + item.getID());

						StringBuffer bitstreamLink = new StringBuffer().append(base)
							.append(base.endsWith("/") ? "" : "/")
							.append("bitstream/handle/")
							.append(item.getHandle())
							.append("/")
							.append(URLEncoder.encode(bitstream.getName(), "UTF8"))
							.append("?sequence=").append(bitstream.getSequenceID());
						r.addCell().addXref(bitstreamLink.toString(), "" + bitstream.getID());

					}else {
						// something can go wrong
						r.addCell().addContent( T_invalid_item );
						r.addCell().addContent( T_invalid_item );
					}


	                Cell c = r.addCell();
	                java.util.List<UserMetadata> extraMetaData = functionalityManager.getUserMetadata_License(ur.getEpersonId(), license.getTransactionId());
	                for(UserMetadata metadata : extraMetaData) {
	                	c.addHighlight("label label-info font_smaller").addContent(metadata.getMetadataKey() + ": " +metadata.getMetadataValue());
           			}
           }
           
			} else {
				profile.addPara(null, "alert").addContent("Not signed any licenses yet.");
           }			

		}catch( IllegalArgumentException e1 ) {
			profile.addPara(null, "alert alert-error").addContent( "No items - " + e1.getMessage() );
		}catch( Exception e2 ) {
			profile.addPara(null, "alert alert-error").addContent( "Exception - " + e2.toString() );
		}
		finally {
			functionalityManager.closeSession();
	    }
	} 
       
	//
	//

	@Override
    public void addBody(Body body) throws WingException, SQLException {
		// Log that we are viewing a profile
		log.info(LogManager.getHeader(context, "view_profile", ""));

		Request request = ObjectModelHelper.getRequest(objectModel);

		// get parameters from submitted form or directly from eperson
		//
		String defaultFirstName = "", defaultLastName = "", defaultPhone = "";
		String defaultLanguage = null;

		if (request.getParameter("submit") != null) {
			defaultFirstName = request.getParameter("first_name");
			defaultLastName = request.getParameter("last_name");
			defaultPhone = request.getParameter("phone");
			defaultLanguage = request.getParameter("language");
		} else if (eperson != null) {
			defaultFirstName = eperson.getFirstName();
			defaultLastName = eperson.getLastName();
			defaultPhone = eperson.getMetadata("phone");
			defaultLanguage = eperson.getLanguage();
		}

		// local vs shibbie user?
		String netid = eperson.getNetid();
		boolean has_netid = netid != null && !netid.isEmpty();
		boolean has_password = eperson.getPasswordHash() != null
				&& eperson.getPasswordHash().getHashString() != null
				&& !eperson.getPasswordHash().getHashString().isEmpty();
		boolean is_probably_local_user = eperson == null ? false : has_password
				&& !has_netid;

		String action = contextPath;
		if (registering) {
			action += "/register";
		} else {
			action += "/profile";
       }

		Division profile = body.addInteractiveDivision("information", action,
				Division.METHOD_POST, "primary");

		if (registering) {
			profile.setHead(T_head_create);
		} else {
			profile.setHead(T_head_update);
       }
       
		// Add the progress list if we are registering a new user
		if (registering) {
			EPersonUtils.registrationProgressList(profile, 2);
		}
       
		List form = profile.addList("form", List.TYPE_FORM);
		List identity = form.addList("identity", List.TYPE_FORM);
		// identity.setHead(T_head_identify);

		// Email
		identity.addLabel(T_email_address);
		Item email_i = identity.addItem(null, "alert");
		email_i.addContent(email);

		// First name
		add_key_pair("first_name", T_first_name, defaultFirstName,
				T_auto_filled_in, identity,
				get_error(errors, "first_name", T_error_required),
				!is_probably_local_user).setRequired(true);

		// Last name
		add_key_pair("last_name", T_last_name, defaultLastName,
				T_auto_filled_in, identity,
				get_error(errors, "last_name", T_error_required),
				!is_probably_local_user).setRequired(true);

		// Phone
		add_key_pair("phone", T_telephone, defaultPhone, identity,
				get_error(errors, "phone", T_error_required));
       
		// Language
		//
		add_language(identity, defaultLanguage);
       
		// utilities metadata
		// - for now only for admins
		if (isAdmin()) {
			add_additional_metadata(identity);
		}
                
		// Subscriptions
		//
		if (!registering && isAdmin()) {
			add_subscriptions(form);
		}
                
		// Passwords
		//
		boolean added_pass = false;
		if (allowSetPassword) {
			if (is_probably_local_user || registering) {
				add_set_password(form, false);
				added_pass = true;
			}
		}
		// indicate that we have the pass available *only* because we are admins
		if (!added_pass && isAdmin()) {
			add_set_password(form, true);
        }
                        
		// Controls
		//
		Button submit = form.addItem().addButton("submit");
		if (registering) {
			submit.setValue(T_submit_update);
		} else {
			submit.setValue(T_submit_create);
                        }

		// Groups
		//
		if (!registering) {
			add_list_of_groups(profile);
       }

		Division signed = profile.addDivision("signed-licenses", "well well-light");
		signed.setHead(T_signed_licenses);
		add_signed_licenses(signed);

		//
		profile.addHidden("eperson-continue").setValue(knot.getId());
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
        String ll = ConfigurationManager.getProperty("webui.supported.locales");
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
