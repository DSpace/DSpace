/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.eperson;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.time.DateFormatUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Highlight;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceUserAllowance;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserMetadata;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserRegistration;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/**
 * Edit an existing EPerson, display all the eperson's metadata along with two
 * special options two reset the eperson's password and delete this user.
 *
 * based on class by Alexey Maslov modified for LINDAT/CLARIN
 */
public class EditEPersonForm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_submit_save = message("xmlui.general.save");

    private static final Message T_submit_cancel = message("xmlui.general.cancel");

    private static final Message T_title = message("xmlui.administrative.eperson.EditEPersonForm.title");

    private static final Message T_eperson_trail = message("xmlui.administrative.eperson.general.epeople_trail");

    private static final Message T_trail = message("xmlui.administrative.eperson.EditEPersonForm.trail");

    private static final Message T_head1 = message("xmlui.administrative.eperson.EditEPersonForm.head1");

    private static final Message T_email_taken = message("xmlui.administrative.eperson.EditEPersonForm.email_taken");

    private static final Message T_head2 = message("xmlui.administrative.eperson.EditEPersonForm.head2");

    private static final Message T_error_email_unique = message("xmlui.administrative.eperson.EditEPersonForm.error_email_unique");

    private static final Message T_error_email = message("xmlui.administrative.eperson.EditEPersonForm.error_email");

    private static final Message T_error_fname = message("xmlui.administrative.eperson.EditEPersonForm.error_fname");

    private static final Message T_error_lname = message("xmlui.administrative.eperson.EditEPersonForm.error_lname");

    private static final Message T_req_certs = message("xmlui.administrative.eperson.EditEPersonForm.req_certs");

    private static final Message T_can_log_in = message("xmlui.administrative.eperson.EditEPersonForm.can_log_in");

    private static final Message T_submit_reset_password = message("xmlui.administrative.eperson.EditEPersonForm.submit_reset_password");

    private static final Message T_special_help = message("xmlui.administrative.eperson.EditEPersonForm.special_help");

    private static final Message T_submit_delete = message("xmlui.administrative.eperson.EditEPersonForm.submit_delete");

    private static final Message T_submit_login_as = message("xmlui.administrative.eperson.EditEPersonForm.submit_login_as");

    private static final Message T_delete_constraint = message("xmlui.administrative.eperson.EditEPersonForm.delete_constraint");

    private static final Message T_constraint_last_conjunction = message("xmlui.administrative.eperson.EditEPersonForm.delete_constraint.last_conjunction");

    private static final Message T_constraint_item = message("xmlui.administrative.eperson.EditEPersonForm.delete_constraint.item");

    private static final Message T_constraint_workflowitem = message("xmlui.administrative.eperson.EditEPersonForm.delete_constraint.workflowitem");

    private static final Message T_constraint_tasklistitem = message("xmlui.administrative.eperson.EditEPersonForm.delete_constraint.tasklistitem");

    private static final Message T_constraint_unknown = message("xmlui.administrative.eperson.EditEPersonForm.delete_constraint.unknown");

    private static final Message T_member_head = message("xmlui.administrative.eperson.EditEPersonForm.member_head");

    private static final Message T_indirect_member = message("xmlui.administrative.eperson.EditEPersonForm.indirect_member");

    private static final Message T_member_none = message("xmlui.administrative.eperson.EditEPersonForm.member_none");

    private static final Message T_org = message("xmlui.administrative.eperson.AddEPersonForm.org");

    private static final Message T_welcome_message = message("xmlui.administrative.eperson.AddEPersonForm.welcome_message");

    private static final Message T_error_org = message("xmlui.administrative.eperson.AddEPersonForm.error_org");

    /** Language string used: */

    private static final Message T_email_address = message("xmlui.EPerson.EditProfile.email_address");

    private static final Message T_first_name = message("xmlui.EPerson.EditProfile.first_name");

    private static final Message T_last_name = message("xmlui.EPerson.EditProfile.last_name");

    private static final Message T_telephone = message("xmlui.EPerson.EditProfile.telephone");

    private static final Message T_language = message("xmlui.EPerson.EditProfile.language");

    private static Locale[] supportedLocales = getSupportedLocales();
    static
    {
        Arrays.sort(supportedLocales, new Comparator<Locale>()
        {
            @Override
            public int compare(Locale a, Locale b)
            {
                return a.getDisplayName().compareTo(b.getDisplayName());
            }
        });
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/epeople", T_eperson_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

    //
    //
    //
    private void add_config_info(Request request, EPerson edited_eperson,
            List identity, java.util.List<String> errors) throws SQLException,
            WingException
    {
        String languageValue = edited_eperson.getLanguage();
        String phoneValue = edited_eperson.getMetadata("phone");
        boolean canLogInValue = edited_eperson.canLogIn();
        boolean certificatValue = edited_eperson.getRequireCertificate();

        if (request.getParameter("phone") != null)
            phoneValue = request.getParameter("phone");
        if (request.getParameter("language") != null)
            languageValue = request.getParameter("language");

        if (!ConfigurationManager.getBooleanProperty("lr",
                "lr.xmlui.user.showlanguage", true))
        {
            identity.addItem()
                    .addHidden("language")
                    .setValue(
                            (languageValue == null || languageValue.equals("")) ? I18nUtil.DEFAULTLOCALE
                                    .toString() : languageValue);
        }
        else
        {
            if (isAdmin())
            {
                Select language = identity.addItem(null, "admin-field")
                        .addSelect("language");
                language.setLabel(T_language);
                // language.setValue(languageValue);

                if (supportedLocales.length > 0)
                {
                    for (Locale lc : supportedLocales)
                    {
                        language.addOption(lc.toString(), lc.getDisplayName());
                    }

                }
                else
                {
                    language.addOption(I18nUtil.DEFAULTLOCALE.toString(),
                            I18nUtil.DEFAULTLOCALE.getDisplayName());
                }
                language.setOptionSelected((languageValue == null || languageValue
                        .equals("")) ? I18nUtil.DEFAULTLOCALE.toString()
                        : languageValue);

            }
            else
            {
                identity.addLabel(T_language);
                // identity.addItem(languageValue);
                identity.addItem((languageValue == null || languageValue
                        .equals("")) ? I18nUtil.DEFAULTLOCALE.toString()
                        : languageValue);
            }
        }

        add_key_pair(isAdmin(), false, "phone", T_telephone, phoneValue,
                identity, null);

        if (isAdmin())
        {
            // Administrative options:
            CheckBox canLogInField = identity.addItem(null, "admin-field")
                    .addCheckBox("can_log_in");
            canLogInField.setLabel(T_can_log_in);
            canLogInField.addOption(canLogInValue, "true");

            CheckBox certificateField = identity.addItem(null, "admin-field")
                    .addCheckBox("certificate");
            certificateField.setLabel(T_req_certs);
            certificateField.addOption(certificatValue, "true");

            CheckBox editsMetadata = identity.addItem(null, "admin-field")
                    .addCheckBox("can_edit_metadata");
            editsMetadata.setLabel("Can edit own item's metadata");
            editsMetadata.addOption(edited_eperson.canEditSubmissionMetadata(),
                    "true");
        }

    }

    private void add_general_info(Request request, EPerson edited_eperson,
            List identity, java.util.List<String> errors) throws SQLException,
            WingException
    {
        String emailValue = edited_eperson.getEmail();
        String firstValue = edited_eperson.getFirstName();
        String lastValue = edited_eperson.getLastName();
        String welcome_message = edited_eperson.getWelcome();

        IFunctionalities functionalitiesManager = DSpaceApi
                .getFunctionalityManager();
        functionalitiesManager.openSession();

        // in case the user is still not registered, do it
        // ?? - legacy because of updates?
        UserRegistration userReg = functionalitiesManager
                .getRegisteredUser(edited_eperson.getID());
        if (userReg == null && edited_eperson.getEmail() != null
                && !edited_eperson.getEmail().equals(""))
        {
            userReg = functionalitiesManager.registerUser(
                    edited_eperson.getID(), edited_eperson.getEmail(),
                    "anonymous", true);
        }
        // //

        /*
         * userReg can still be null if we are admins editing eperson who did
         * not have email in shibboleth md and did not confirm his email via
         * /set-email
         */
        String orgValue = "DUMMY_ORGANIZATION";
        if (userReg != null)
        {
            orgValue = userReg.getOrganization();
        }

        // if updates posted, use them
        if (request.getParameter("email_address") != null)
            emailValue = request.getParameter("email_address");
        if (request.getParameter("first_name") != null)
            firstValue = request.getParameter("first_name");
        if (request.getParameter("last_name") != null)
            lastValue = request.getParameter("last_name");
        if (request.getParameter("org") != null)
            orgValue = request.getParameter("org");
        if (request.getParameter("welcome_message") != null)
            welcome_message = request.getParameter("welcome_message");
        //

        add_key_pair(
                isAdmin(),
                true,
                "email_address",
                T_email_address,
                emailValue,
                identity,
                get_error(errors, new String[] { "eperson_email_key",
                        "email_address" }, new Message[] {
                        T_error_email_unique, T_error_email }));
        add_key_pair(isAdmin(), true, "first_name", T_first_name, firstValue,
                identity, get_error(errors, "first_name", T_error_fname));
        add_key_pair(isAdmin(), true, "last_name", T_last_name, lastValue,
                identity, get_error(errors, "last_name", T_error_lname));
        add_key_pair(true, true, "org", T_org, orgValue, identity,
                get_error(errors, "org", T_error_org));
        add_key_pair(true, false, "welcome_message", T_welcome_message,
                welcome_message, identity,
                get_error(errors, "welcome_message", T_error_org));

        functionalitiesManager.closeSession();
    }

    private void add_groups_info(Division edit, EPerson edited_eperson)
            throws WingException, SQLException
    {
        if (isAdmin())
        {
            List member = edit.addList("eperson-member-of", List.TYPE_SIMPLE,
                    "admin-field");
            member.setHead(T_member_head);

            Group[] groups = Group.allMemberGroups(context, edited_eperson);
            for (Group group : groups)
            {
                String url = contextPath
                        + "/admin/groups?administrative-continue="
                        + knot.getId() + "&submit_edit_group&groupID="
                        + group.getID();

                Item item = member.addItem();
                item.addXref(url, group.getName());

                // Check if this membership is via another group or not, if so
                // then add a note.
                Group via = findViaGroup(edited_eperson, group);
                if (via != null)
                {
                    item.addHighlight("fade").addContent(
                            T_indirect_member.parameterize(via.getName()));
                }
            }

            if (groups.length <= 0)
            {
                member.addItem().addHighlight("italic")
                        .addContent(T_member_none);
            }
        }
    }

    private void add_form_buttons(List identity, EPerson edited_eperson)
            throws WingException, SQLException
    {

        if (isAdmin())
        {
            // Buttons to reset, delete or login as
            identity.addItem().addHighlight("italic")
                    .addContent(T_special_help);
            Item special = identity.addItem();
            special.addButton("submit_reset_password").setValue(
                    T_submit_reset_password);

            Button submitDelete = special.addButton("submit_delete");
            submitDelete.setValue(T_submit_delete);

            Button submitLoginAs = special.addButton("submit_login_as");
            submitLoginAs.setValue(T_submit_login_as);
            if (!ConfigurationManager.getBooleanProperty(
                    "webui.user.assumelogin", false))
            {
                submitLoginAs.setDisabled();
            }
            add_constraints(identity, edited_eperson, submitDelete);
        }

        Item buttons = identity.addItem();
        if (isAdmin())
        {
            buttons.addButton("submit_save").setValue(T_submit_save);
        }
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);

    }

    //
    //
    //

    private boolean isAdmin() throws SQLException
    {
        return AuthorizeManager.isAdmin(context);
    }

    private void add_key_pair(boolean editable, boolean required,
            String item_name, Message key, String value, List list,
            Message error) throws WingException
    {
        if (editable)
        {
            Text text = list.addItem().addText(item_name);
            text.setRequired(required);
            text.setLabel(key);
            text.setValue(value);
            if (error != null)
            {
                text.addError(error);
            }
        }
        else
        {
            list.addLabel(T_email_address);
            list.addItem(value);
        }
    }

    private void add_constraints(List identity, EPerson edited_eperson,
            Button submit_delete) throws SQLException, WingException
    {
        java.util.List<String> deleteConstraints = edited_eperson
                .getDeleteConstraints();

        if (deleteConstraints != null && deleteConstraints.size() > 0)
        {
            submit_delete.setDisabled();

            Highlight hi = identity.addItem("eperson-delete-constraint",
                    "eperson-delete-constraint alert alert-error")
                    .addHighlight("error");
            hi.addContent(T_delete_constraint);
            hi.addContent(" ");

            for (String constraint : deleteConstraints)
            {
                int idx = deleteConstraints.indexOf(constraint);
                if (idx > 0 && idx == deleteConstraints.size() - 1)
                {
                    hi.addContent(", ");
                    hi.addContent(T_constraint_last_conjunction);
                    hi.addContent(" ");
                }
                else if (idx > 0)
                {
                    hi.addContent(", ");
                }

                if (constraint.contains("item(s)"))
                    hi.addContent(T_constraint_item);
                else if (constraint.contains("workflowitem"))
                    hi.addContent(T_constraint_workflowitem);
                else if (constraint.contains("tasklistitem"))
                    hi.addContent(T_constraint_tasklistitem);
                else
                    hi.addContent(T_constraint_unknown);

            }
            hi.addContent(".");
        }
    }

    // error messages
    //

    private Message get_error(java.util.List<String> errors, String key,
            Message err_msg)
    {
        return errors.contains(key) ? err_msg : null;
    }

    private Message get_error(java.util.List<String> errors, String[] key,
            Message[] err_msg)
    {
        for (int i = 0; i < key.length; ++i)
        {
            Message tmp = get_error(errors, key[i], err_msg[i]);
            if (tmp != null)
                return tmp;
        }
        return null;
    }

    //
    //
    //

    @Override
    public void addBody(Body body) throws WingException, SQLException,
            AuthorizeException
    {
        // Get all our parameters
        Request request = ObjectModelHelper.getRequest(objectModel);

        // Get our parameters;
        int epersonID = parameters.getParameterAsInteger("epersonID", -1);
        String errorString = parameters.getParameter("errors", null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            errors.addAll(Arrays.asList(errorString.split(",")));
        }

        // Grab the person in question
        EPerson edited_eperson = EPerson.find(context, epersonID);
        if (edited_eperson == null)
        {
            throw new UIException("Unable to find eperson for id:" + epersonID);
        }

        // DIVISION: eperson-edit
        Division edit = body.addInteractiveDivision("eperson-edit", contextPath
                + "/admin/epeople", Division.METHOD_POST,
                "primary administrative eperson");
        edit.setHead(T_head1);

        // errors
        if (errors.contains("eperson_email_key"))
        {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_email_taken);
        }

        List identity = edit.addList("form", List.TYPE_FORM);
        identity.setHead(T_head2.parameterize(edited_eperson.getFullName()));
        add_general_info(request, edited_eperson, identity, errors);
        add_config_info(request, edited_eperson, identity, errors);
        add_form_buttons(identity, edited_eperson);

        Division groups = edit.addDivision("groups", "well well-light");
        add_groups_info(groups, edited_eperson);

        Division signed = edit
                .addDivision("signed-licenses", "well well-light");
        signed.setHead("Licenses this E-Person Signed:");
        add_signed_licenses(signed, edited_eperson);

        edit.addHidden("administrative-continue").setValue(knot.getId());
    }

    private void add_signed_licenses(Division profile, EPerson edited_eperson)
            throws WingException
    {

        try
        {

            IFunctionalities functionalityManager = DSpaceApi
                    .getFunctionalityManager();
            functionalityManager.openSession();
            java.util.List<LicenseResourceUserAllowance> licenses = functionalityManager
                    .getSignedLicensesByUser(edited_eperson.getID());

            // hack for group by /////////
            /*
             * java.util.List<String> keys = new ArrayList<String>();
             * java.util.List<LicenseResourceUserAllowance> licenses_group_by =
             * new ArrayList<LicenseResourceUserAllowance>();
             * 
             * for (LicenseResourceUserAllowance license : licenses) { String
             * createdOn = DateFormatUtils.format(license.getCreatedOn(),
             * "ddMMyyyyhhmm"); String epersonID = "" +
             * license.getUserRegistration().getEpersonId(); String licenseID =
             * "" + license.getLicenseResourceMapping().getLicenseDefinition().
             * getLicenseId(); String key = createdOn + ":" + epersonID + ":" +
             * licenseID; if(!keys.contains(key)) { keys.add(key);
             * licenses_group_by.add(license); } }
             */
            // ///////////////////////////

            if (licenses != null && licenses.size() > 0)
            {

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
                    int bitstreamID = license.getLicenseResourceMapping()
                            .getBitstreamId();
                    LicenseDefinition ld = license.getLicenseResourceMapping()
                            .getLicenseDefinition();
                    UserRegistration ur = license.getUserRegistration();
                    Date signingDate = license.getCreatedOn();

                    Row r = wftable.addRow(null, null, "font_smaller bold");
                    String id = DateFormatUtils.format(signingDate,
                            "yyyyMMddhhmmss")
                            + "-"
                            + ur.getEpersonId()
                            + "-"
                            + bitstreamID;
                    r.addCellContent(id);

                    r.addCellContent(DateFormatUtils.format(signingDate,
                            "yyyy-MM-dd hh:mm:ss"));

                    r.addCell().addXref(ld.getDefinition(), ld.getName());

                    Bitstream bitstream = Bitstream.find(context, bitstreamID);
                    org.dspace.content.Item item = (org.dspace.content.Item) bitstream
                            .getParentObject();

                    String base = ConfigurationManager
                            .getProperty("dspace.url");
                    StringBuffer itemLink = new StringBuffer().append(base)
                            .append(base.endsWith("/") ? "" : "/")
                            .append("/handle/").append(item.getHandle());

                    r.addCell().addXref(itemLink.toString(), "" + item.getID());

                    StringBuffer bitstreamLink = new StringBuffer()
                            .append(base)
                            .append(base.endsWith("/") ? "" : "/")
                            .append("bitstream/handle/")
                            .append(item.getHandle())
                            .append("/")
                            .append(URLEncoder.encode(bitstream.getName(),
                                    "UTF8")).append("?sequence=")
                            .append(bitstream.getSequenceID());
                    r.addCell().addXref(bitstreamLink.toString(),
                            "" + bitstream.getID());

                    Cell c = r.addCell();
                    java.util.List<UserMetadata> extraMetaData = functionalityManager
                            .getUserMetadata_License(ur.getEpersonId(),
                                    license.getTransactionId());
                    for (UserMetadata metadata : extraMetaData)
                    {
                        c.addHighlight("label label-info font_smaller")
                                .addContent(
                                        metadata.getMetadataKey() + ": "
                                                + metadata.getMetadataValue());
                    }
                }

            }
            else
            {
                profile.addPara(null, "alert").addContent(
                        "Not signed any licenses yet.");
            }

            functionalityManager.closeSession();

        }
        catch (IllegalArgumentException e1)
        {
            profile.addPara(null, "alert alert-error").addContent(
                    "No items - " + e1.getMessage());
        }
        catch (Exception e2)
        {
            profile.addPara(null, "alert alert-error").addContent(
                    "Exception - " + e2.toString());
        }

    }

    /**
     * Determine if the given eperson is a direct member of this group if they
     * are not the return the group that membership is implied through (the via
     * group!). This will only find one possible relation path, there may be
     * multiple.
     * 
     * 
     * @param eperson
     *            The source group to search from
     * @param group
     *            The target group to search for.
     * @return The group this member is related through or null if none found.
     */
    private Group findViaGroup(EPerson eperson, Group group)
            throws SQLException
    {
        // First check if this eperson is a direct member of the group.
        for (EPerson direct : group.getMembers())
        {
            if (direct.getID() == eperson.getID())
            {
                // Direct membership
                return null;
            }
        }

        // Otherwise check what group this eperson is a member through
        Group[] targets = group.getMemberGroups();

        Group[] groups = Group.allMemberGroups(context, eperson);
        for (Group member : groups)
        {
            for (Group target : targets)
            {
                if (member.getID() == target.getID())
                {
                    return member;
                }
            }
        }

        // This should never happen, but let's just say we couldn't find the
        // relationship.
        return null;
    }

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
            result[0] = I18nUtil.DEFAULTLOCALE;
            return result;
        }
    }

}
