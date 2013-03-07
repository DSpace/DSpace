package org.dspace.app.xmlui.aspect.dryadfeedback;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.Label;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * Display to the user a simple form letting the user give feedback.
 * 
 * @author Scott Phillips
 */
public class MembershipApplicationForm extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    private static final String message_prefix = "xmlui.DryadFeedback.MembershipApplicationForm.";


    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        String org_name = parameters.getParameter("org_name",""); // required
        String org_legalname = parameters.getParameter("org_legalname","");
        String org_type = parameters.getParameter("org_type","");
        String org_annual_revenue = parameters.getParameter("org_annual_revenue",""); // required
        String billing_contact_name = parameters.getParameter("billing_contact_name",""); // required
        String billing_email = parameters.getParameter("billing_email",""); // required
        String billing_address = parameters.getParameter("billing_address",""); // required
        String publications = parameters.getParameter("publications","");
        String membership_year_start = parameters.getParameter("membership_year_start",""); // required
        String membership_year_end = parameters.getParameter("membership_year_end",""); // required
        String rep_name = parameters.getParameter("rep_name",""); // required
        String rep_email = parameters.getParameter("rep_email",""); // required
        String comments = parameters.getParameter("comments","");

       return HashUtil.hash(org_name + "-" + org_name + "-" + org_legalname + "-" + 
               org_type + "-" + org_annual_revenue + "-" + billing_contact_name + "-" + 
               billing_email + "-" + billing_address + "-" + publications + "-" +
               membership_year_start + "-" + membership_year_end + "-"+ rep_name + 
               "-" + rep_email + "-" + comments);
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity() 
    {
        return NOPValidity.SHARED_INSTANCE;
    }
    
    
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {       
        pageMeta.addMetadata("title").addContent(message(message_prefix + "title"));
         pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(message(message_prefix + "title"));
    }

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division membership = body.addInteractiveDivision("membership-form",
                contextPath+"/membership",Division.METHOD_POST,"primary");

        membership.setHead(message(message_prefix + "heading"));

        membership.addPara(message(message_prefix + "overview.p1"));
        membership.addPara(message(message_prefix + "overview.p2"));
        membership.addPara(message(message_prefix + "markedfields"));

        // Check for errors
        String errorFields = parameters.getParameter("error_fields","");
        final String errorFieldsArray[] = StringUtils.split(errorFields, ',');
        java.util.List<String> errorFieldList = new java.util.ArrayList<String>() {{
            for(String errorField : errorFieldsArray) { add(errorField); }
        }};

        List form = membership.addList("form", List.TYPE_FORM);
        // Org Name
        Item orgName = form.addItem("org_name","");
        Text orgNameText = orgName.addText("org_name");
        orgNameText.setLabel(message(message_prefix + "fields.org_name.label1"));
        orgNameText.setHelp(message(message_prefix + "fields.org_name.label2"));
        orgNameText.setValue(parameters.getParameter("org_name", ""));
        orgNameText.setRequired();
        if(errorFieldList.contains("org_name")) {
            orgNameText.addError(message(message_prefix + "errors.org_name"));
        }


        // Org Legal Name
        Item orgLegalName = form.addItem("org_legalname","");
        Text orgLegalNameText = orgLegalName.addText("org_legalname");
        orgLegalNameText.setLabel(message(message_prefix + "fields.org_legalname.label1"));
        orgLegalNameText.setHelp(message(message_prefix + "fields.org_legalname.label2"));
        orgLegalNameText.setValue(parameters.getParameter("org_legalname", ""));

        // Org Type
        Item orgType = form.addItem("org_type", "");
        TextArea orgTypeTextArea = orgType.addTextArea("org_type");
        orgTypeTextArea.setLabel(message(message_prefix + "fields.org_type.label1"));
        orgTypeTextArea.setHelp(message(message_prefix + "fields.org_type.label2"));
        orgTypeTextArea.setValue(parameters.getParameter("org_type", ""));
        orgTypeTextArea.setRequired();
        if(errorFieldList.contains("org_type")) {
            orgTypeTextArea.addError(message(message_prefix + "errors.org_type"));
        }

        // Annual Revenue
        Item orgAnnualRevenue = form.addItem("org_annual_revenue", "");
        Radio orgAnnualRevenueRadios = orgAnnualRevenue.addRadio("org_annual_revenue");
        orgAnnualRevenueRadios.setLabel(message(message_prefix + "fields.org_annual_revenue.label1"));
        orgAnnualRevenueRadios.setHelp(message(message_prefix + "fields.org_annual_revenue.label2"));

        orgAnnualRevenueRadios.addOption("less_than_10_million", message(message_prefix + "fields.org_annual_revenue.less_than_10_million"));
        orgAnnualRevenueRadios.addOption("greater_than_10_million", message(message_prefix + "fields.org_annual_revenue.greater_than_10_million"));
        orgAnnualRevenueRadios.setRequired();
        if(errorFieldList.contains("org_annual_revenue")) {
            orgAnnualRevenueRadios.addError(message(message_prefix + "errors.org_annual_revenue"));
        }

        // Billing Contact Name
        Item billingContactName = form.addItem("billing_contact_name","");
        Text billingContactNameText = billingContactName.addText("billing_contact_name");

        billingContactNameText.setLabel(message(message_prefix + "fields.billing_contact_name.label1"));
        billingContactNameText.setValue(parameters.getParameter("billing_contact_name", ""));
        billingContactNameText.setRequired();
        if(errorFieldList.contains("billing_contact_name")) {
            billingContactNameText.addError(message(message_prefix + "errors.billing_contact_name"));
        }

        // Billing Email
        Item billingEmail = form.addItem("billing_email","");
        Text billingEmailText = billingEmail.addText("billing_email");

        billingEmailText.setLabel(message(message_prefix + "fields.billing_email.label1"));
        billingEmailText.setValue(parameters.getParameter("billing_email", ""));
        billingEmailText.setRequired();
        if(errorFieldList.contains("billing_email")) {
            billingEmailText.addError(message(message_prefix + "errors.billing_email"));
        }

        // Billing Address
        Item billingAddress = form.addItem("billing_address","");
        Text billingAddressText = billingAddress.addText("billing_address");

        billingAddressText.setLabel(message(message_prefix + "fields.billing_address.label1"));
        billingAddressText.setValue(parameters.getParameter("billing_address", ""));
        billingAddressText.setRequired();
        if(errorFieldList.contains("billing_address")) {
            billingAddressText.addError(message(message_prefix + "errors.billing_address"));
        }

        // Publications
        Item publications = form.addItem("publications","");
        TextArea publicationsTextArea = publications.addTextArea("publications");

        publicationsTextArea.setLabel(message(message_prefix + "fields.publications.label1"));
        publicationsTextArea.setHelp(message(message_prefix + "fields.publications.label2"));
        publicationsTextArea.setValue(parameters.getParameter("publications", ""));

        // Membership Term, start and end
        Item membershipYear = form.addItem("membership_year","");
        Composite membershipYearComposite = membershipYear.addComposite("membership_year_group");
        membershipYearComposite.setLabel(message(message_prefix + "fields.membership_year.label1"));
        membershipYearComposite.setHelp(message(message_prefix + "fields.membership_year.label2"));

        Radio membershipYearStartRadio = membershipYearComposite.addRadio("membership_year_start");
        membershipYearStartRadio.setLabel(message(message_prefix + "fields.membership_year.starting_year.label"));
        membershipYearStartRadio.addOption("2013", message(message_prefix + "fields.membership_year.starting_year.2013"));
        membershipYearStartRadio.addOption("2014", message(message_prefix + "fields.membership_year.starting_year.2014"));
        membershipYearStartRadio.setOptionSelected(parameters.getParameter("membership_year_start", "2013"));
        membershipYearStartRadio.setRequired();
        if(errorFieldList.contains("membership_year_start")) {
            membershipYearStartRadio.addError(message(message_prefix + "errors.membership_year_start"));
        }

        Radio membershipYearEndRadio = membershipYearComposite.addRadio("membership_year_end");
        membershipYearEndRadio.setLabel(message(message_prefix + "fields.membership_year.ending_year.label"));
        membershipYearEndRadio.addOption("2013", message(message_prefix + "fields.membership_year.ending_year.2013"));
        membershipYearEndRadio.addOption("2014", message(message_prefix + "fields.membership_year.ending_year.2014"));
        membershipYearEndRadio.addOption("2015", message(message_prefix + "fields.membership_year.ending_year.2015"));
        membershipYearEndRadio.addOption("2016", message(message_prefix + "fields.membership_year.ending_year.2016"));
        membershipYearEndRadio.addOption("2017", message(message_prefix + "fields.membership_year.ending_year.2017"));
        membershipYearEndRadio.setOptionSelected(parameters.getParameter("membership_year_end", "2014"));
        membershipYearEndRadio.setRequired();
        if(errorFieldList.contains("membership_year_end")) {
            membershipYearEndRadio.addError(message(message_prefix + "errors.membership_year_end"));
        }

        // Representatitve Name
        Item repName = form.addItem("rep_name","");
        Text repNameText = repName.addText("rep_name");

        repNameText.setLabel(message(message_prefix + "fields.rep_name.label1"));
        repNameText.setHelp(message(message_prefix + "fields.rep_name.label2"));
        repNameText.setValue(parameters.getParameter("rep_name", ""));
        repNameText.setRequired();
        if(errorFieldList.contains("rep_name")) {
            repNameText.addError(message(message_prefix + "errors.rep_name"));
        }

        // Representative email address
        Item repEmail = form.addItem("rep_email","");
        Text repEmailText = repEmail.addText("rep_email");

        repEmailText.setLabel(message(message_prefix + "fields.rep_email.label1"));
        repEmailText.setValue(parameters.getParameter("rep_email", ""));
        repEmailText.setRequired();
        if(errorFieldList.contains("rep_email")) {
            repEmailText.addError(message(message_prefix + "errors.rep_email"));
        }

        // Comments
        Item comments = form.addItem("comments","");
        TextArea commentsTextArea = comments.addTextArea("comments");

        commentsTextArea.setLabel(message(message_prefix + "fields.comments.label1"));
        commentsTextArea.setHelp(message(message_prefix + "fields.comments.label2"));
        commentsTextArea.setValue(parameters.getParameter("comments", ""));

        // Submit button
        form.addItem().addButton("submit").setValue(message(message_prefix + "submit"));
    }
}
