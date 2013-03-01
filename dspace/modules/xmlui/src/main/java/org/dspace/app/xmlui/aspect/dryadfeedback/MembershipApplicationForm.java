package org.dspace.app.xmlui.aspect.dryadfeedback;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
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
        String billing_address = parameters.getParameter("billing_address",""); // required
        String billing_email = parameters.getParameter("billing_email",""); // required
        String publications = parameters.getParameter("publications","");
        String membership_year = parameters.getParameter("membership_year",""); // required
        String rep_name = parameters.getParameter("rep_name",""); // required
        String rep_email = parameters.getParameter("rep_email",""); // required
        String comments = parameters.getParameter("comments","");

       return HashUtil.hash(org_name + "-" + org_name + "-" + org_legalname + "-" + 
               org_type + "-" + org_annual_revenue + "-" + billing_contact_name + "-" + 
               billing_address + "-" + billing_email + "-" + publications + "-" + 
               membership_year + "-" + rep_name + "-" + rep_email + "-" +
               comments);
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
        membership.addPara(message(message_prefix + "overview.p3"));
        membership.addPara(message(message_prefix + "markedfields"));

        List form = membership.addList("form", List.TYPE_FORM);
        // Org Name
        Item orgName = form.addItem("org_name","");
        Text orgNameText = orgName.addText("org_name");
        orgNameText.setLabel(message(message_prefix + "fields.org_name.label1"));
        orgNameText.setHelp(message(message_prefix + "fields.org_name.label2"));
        orgNameText.setValue(parameters.getParameter("org_name", ""));
        orgNameText.setRequired();

        // Org Legal Name
        Item orgLegalName = form.addItem("org_legalname","");
        Text orgLegalNameText = orgLegalName.addText("org_legalname");
        orgLegalNameText.setLabel(message(message_prefix + "fields.org_legalname.label1"));
        orgLegalNameText.setHelp(message(message_prefix + "fields.org_legalname.label2"));
        orgLegalNameText.setValue(parameters.getParameter("org_legalname", ""));

        // Org Type
        Item orgType = form.addItem("org_type", "");
        Radio orgTypeRadios = orgType.addRadio("org_type");
        orgTypeRadios.setLabel(message(message_prefix + "fields.org_type.label1"));
        orgTypeRadios.setHelp(message(message_prefix + "fields.org_type.label2"));

        orgTypeRadios.addOption("data_center_or_repository", message(message_prefix + "fields.org_type.data_center_or_repository"));
        orgTypeRadios.addOption("funding_organization", message(message_prefix + "fields.org_type.funding_organization"));
        orgTypeRadios.addOption("journal", message(message_prefix + "fields.org_type.journal"));
        orgTypeRadios.addOption("publisher", message(message_prefix + "fields.org_type.publisher"));
        orgTypeRadios.addOption("scholarly_society", message(message_prefix + "fields.org_type.scholarly_society"));
        orgTypeRadios.addOption("university_research_or_edu_institute", message(message_prefix + "fields.org_type.university_research_or_edu_institute"));
        orgTypeRadios.addOption("other", message(message_prefix + "fields.org_type.other"));
        orgTypeRadios.setRequired();

        Text org_type_other = orgType.addText("org_type_other");
        org_type_other.setValue(parameters.getParameter("org_type_other", ""));

        // Annual Revenue
        Item orgAnnualRevenue = form.addItem("org_annual_revenue", "");
        Radio orgAnnualRevenueRadios = orgAnnualRevenue.addRadio("org_annual_revenue");
        orgAnnualRevenueRadios.setLabel(message(message_prefix + "fields.org_annual_revenue.label1"));
        orgAnnualRevenueRadios.setHelp(message(message_prefix + "fields.org_annual_revenue.label2"));

        orgAnnualRevenueRadios.addOption("less_than_10_million", message(message_prefix + "fields.org_annual_revenue.less_than_10_million"));
        orgAnnualRevenueRadios.addOption("greater_than_10_million", message(message_prefix + "fields.org_annual_revenue.greater_than_10_million"));
        orgAnnualRevenueRadios.setRequired();

        // Billing Contact Name
        Item billingContactName = form.addItem("billing_contact_name","");
        Text billingContactNameText = billingContactName.addText("billing_contact_name");

        billingContactNameText.setLabel(message(message_prefix + "fields.billing_contact_name.label1"));
        billingContactNameText.setHelp(message(message_prefix + "fields.billing_contact_name.label2"));
        billingContactNameText.setValue(parameters.getParameter("billing_contact_name", ""));
        billingContactNameText.setRequired();

        // Billing Address
        Item billingAddress = form.addItem("billing_address","");
        Text billingAddressText = billingAddress.addText("billing_address");

        billingAddressText.setLabel(message(message_prefix + "fields.billing_address.label1"));
        billingAddressText.setHelp(message(message_prefix + "fields.billing_address.label2"));
        billingAddressText.setValue(parameters.getParameter("billing_address", ""));
        billingAddressText.setRequired();

        // Billing Email
        Item billingEmail = form.addItem("billing_email","");
        Text billingEmailText = billingEmail.addText("billing_email");

        billingEmailText.setLabel(message(message_prefix + "fields.billing_email.label1"));
        billingEmailText.setValue(parameters.getParameter("billing_email", ""));
        billingEmailText.setRequired();

        // Publications
        Item publications = form.addItem("publications","");
        TextArea publicationsTextArea = publications.addTextArea("publications");

        publicationsTextArea.setLabel(message(message_prefix + "fields.publications.label1"));
        publicationsTextArea.setHelp(message(message_prefix + "fields.publications.label2"));
        publicationsTextArea.setValue(parameters.getParameter("publications", ""));

        // Membership Year
        Item membershipYear = form.addItem("membership_year","");
        Text membershipYearText = membershipYear.addText("membership_year");

        membershipYearText.setLabel(message(message_prefix + "fields.membership_year.label1"));
        membershipYearText.setHelp(message(message_prefix + "fields.membership_year.label2"));
        membershipYearText.setValue(parameters.getParameter("membership_year", ""));
        membershipYearText.setRequired();

        // Representatitve Name
        Item repName = form.addItem("rep_name","");
        Text repNameText = repName.addText("rep_name");

        repNameText.setLabel(message(message_prefix + "fields.rep_name.label1"));
        repNameText.setHelp(message(message_prefix + "fields.rep_name.label2"));
        repNameText.setValue(parameters.getParameter("rep_name", ""));
        repNameText.setRequired();

        // Representative email address
        Item repEmail = form.addItem("rep_email","");
        Text repEmailText = repEmail.addText("rep_email");

        repEmailText.setLabel(message(message_prefix + "fields.rep_email.label1"));
        repEmailText.setHelp(message(message_prefix + "fields.rep_email.label2"));
        repEmailText.setValue(parameters.getParameter("rep_email", ""));
        repEmailText.setRequired();

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
