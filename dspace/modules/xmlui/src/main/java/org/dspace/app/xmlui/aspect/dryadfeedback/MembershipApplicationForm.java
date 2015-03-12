package org.dspace.app.xmlui.aspect.dryadfeedback;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

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
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Hidden;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Select;
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
        String org_annual_revenue_currency = parameters.getParameter("org_annual_revenue_currency",""); // required
        String billing_contact_name = parameters.getParameter("billing_contact_name",""); // required
        String billing_email = parameters.getParameter("billing_email",""); // required
        String billing_address = parameters.getParameter("billing_address",""); // required
        String publications = parameters.getParameter("publications","");
        String membership_year_start = parameters.getParameter("membership_year_start",""); // required
        String membership_length = parameters.getParameter("membership_length",""); // required
        String rep_name = parameters.getParameter("rep_name",""); // required
        String rep_email = parameters.getParameter("rep_email",""); // required
        String comments = parameters.getParameter("comments","");

       return HashUtil.hash(org_name + "-" + org_name + "-" + org_legalname + "-" + 
               org_type + "-" + org_annual_revenue + "-" + org_annual_revenue_currency + "-" +
               billing_contact_name + "-" + billing_email + "-" + billing_address + "-" +
               publications + "-" + membership_year_start + "-" + membership_length + "-"+
               rep_name + "-" + rep_email + "-" + comments);
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
        pageMeta.addMetadata("stylesheet", "screen").addContent("lib/css/reset.css");
	pageMeta.addMetadata("stylesheet", "screen").addContent("lib/css/base.css");
	pageMeta.addMetadata("stylesheet", "screen").addContent("lib/css/helper.css");
	pageMeta.addMetadata("stylesheet", "screen").addContent("lib/css/jquery-ui-1.9.1.custom.css");
	pageMeta.addMetadata("stylesheet", "screen").addContent("lib/css/style.css");
	pageMeta.addMetadata("stylesheet", "screen").addContent("lib/css/authority-control.css");
	pageMeta.addMetadata("stylesheet", "screen").addContent("lib/css/jquery-ui-1.css");
	pageMeta.addMetadata("stylesheet", "screen").addContent("lib/css/jquery.bxslider.css");
	pageMeta.addMetadata("stylesheet", "screen").addContent("lib/css/superfish.css");
	pageMeta.addMetadata("stylesheet", "screen").addContent("lib/css/membership-form.css");
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(message(message_prefix + "title"));
    }

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division membership = body.addInteractiveDivision("membership-form",
                contextPath+"/membership",Division.METHOD_POST,"primary membership-form");

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

        Select orgAnnualRevenueCurrencySelect = orgAnnualRevenue.addSelect("org_annual_revenue_currency");
        orgAnnualRevenueCurrencySelect.setLabel(message(message_prefix + "fields.org_annual_revenue.label1"));
        orgAnnualRevenueCurrencySelect.setHelp(message(message_prefix + "fields.org_annual_revenue.label2"));
        orgAnnualRevenueCurrencySelect.addOption("USD", "$ USD");
        orgAnnualRevenueCurrencySelect.addOption("GBP", "£ GBP");
        orgAnnualRevenueCurrencySelect.addOption("CAD", "C$ CAD");
        orgAnnualRevenueCurrencySelect.addOption("EUR", "€ EUR");
        orgAnnualRevenueCurrencySelect.addOption("AUD", "$ AUD");
    	orgAnnualRevenueCurrencySelect.addOption("JPY", "¥ JPY");
        orgAnnualRevenueCurrencySelect.setOptionSelected(parameters.getParameter("org_annual_revenue_currency", "USD"));
        orgAnnualRevenueCurrencySelect.setRequired();
        if(errorFieldList.contains("org_annual_revenue_currency")) {
            orgAnnualRevenueCurrencySelect.addError(message(message_prefix + "errors.org_annual_revenue_currency"));
        }

        Radio orgAnnualRevenueRadios = orgAnnualRevenue.addRadio("org_annual_revenue");
        orgAnnualRevenueRadios.addOption("greater_than_10_million", message(message_prefix + "fields.org_annual_revenue.greater_than_10_million"));
        orgAnnualRevenueRadios.addOption("less_than_10_million", message(message_prefix + "fields.org_annual_revenue.less_than_10_million"));
        orgAnnualRevenueRadios.addOption("advocate_all_organizations", message(message_prefix + "fields.org_annual_revenue.advocate_all_organizations"));        
        orgAnnualRevenueRadios.setRequired();
        orgAnnualRevenueRadios.setOptionSelected(parameters.getParameter("org_annual_revenue", ""));
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
        TextArea billingAddressTextArea = billingAddress.addTextArea("billing_address");

        billingAddressTextArea.setLabel(message(message_prefix + "fields.billing_address.label1"));
        billingAddressTextArea.setValue(parameters.getParameter("billing_address", ""));
        billingAddressTextArea.setRequired();
        if(errorFieldList.contains("billing_address")) {
            billingAddressTextArea.addError(message(message_prefix + "errors.billing_address"));
        }

        // Publications
        Item publications = form.addItem("publications","");
        TextArea publicationsTextArea = publications.addTextArea("publications");

        publicationsTextArea.setLabel(message(message_prefix + "fields.publications.label1"));
        publicationsTextArea.setHelp(message(message_prefix + "fields.publications.label2"));
        publicationsTextArea.setValue(parameters.getParameter("publications", ""));

        // Membership Term, start and end
        Item membershipTerm = form.addItem("membership_term","");

        Composite membershipTermComposite = membershipTerm.addComposite("membership_term_group");
        membershipTermComposite.setLabel(message(message_prefix + "fields.membership_term.label1"));
        membershipTermComposite.setHelp(message(message_prefix + "fields.membership_term.label2"));

        Select membershipYearStartSelect = membershipTermComposite.addSelect("membership_year_start","label-at-left");
        membershipYearStartSelect.setLabel(message(message_prefix + "fields.membership_year.starting_year.label"));
        membershipYearStartSelect.addOption("current", message(message_prefix + "fields.membership_year.starting_year.current"));
        membershipYearStartSelect.addOption("following", message(message_prefix + "fields.membership_year.starting_year.following"));
        membershipYearStartSelect.setOptionSelected(parameters.getParameter("membership_year_start", ""));
        membershipYearStartSelect.setRequired();
        if(errorFieldList.contains("membership_year_start")) {
            membershipYearStartSelect.addError(message(message_prefix + "errors.membership_year_start"));
        }

        Select membershipLengthSelect = membershipTermComposite.addSelect("membership_length", "label-at-left");
        membershipLengthSelect.setLabel(message(message_prefix + "fields.membership_length.label"));
        membershipLengthSelect.addOption("1yr", message(message_prefix + "fields.membership_length.1"));
        membershipLengthSelect.addOption("2yr", message(message_prefix + "fields.membership_length.2"));
        membershipLengthSelect.addOption("3yr", message(message_prefix + "fields.membership_length.3"));
        membershipLengthSelect.addOption("4yr", message(message_prefix + "fields.membership_length.4"));
        membershipLengthSelect.addOption("5yr", message(message_prefix + "fields.membership_length.5"));
        membershipLengthSelect.setOptionSelected(parameters.getParameter("membership_length", ""));
        membershipLengthSelect.setRequired();
        if(errorFieldList.contains("membership_length")) {
            membershipLengthSelect.addError(message(message_prefix + "errors.membership_length"));
    	}

        // Organizational representative
        Item repOrg = form.addItem("rep_org","");
        Hidden repOrgHidden = repOrg.addHidden("rep_org");
        repOrgHidden.setLabel(message(message_prefix + "fields.rep_org.label1"));
        repOrgHidden.setHelp(message(message_prefix + "fields.rep_org.label2"));
        if(errorFieldList.contains("rep_org")) {
            repOrgHidden.addError(message(message_prefix + "errors.rep_org"));
        }
        
        // Representatitve Name
        Item repName = form.addItem("rep_name","");
        Text repNameText = repName.addText("rep_name");
        repNameText.setLabel(message(message_prefix + "fields.rep_name"));
        repNameText.setValue(parameters.getParameter("rep_name", ""));
        repNameText.setRequired();
        if(errorFieldList.contains("rep_name")) {
            repNameText.addError(message(message_prefix + "errors.rep_name"));
        }
        // Representatitve Ttile
        Item repTitle = form.addItem("rep_title","");
        Text repTitleText = repTitle.addText("rep_title");
        repTitleText.setLabel(message(message_prefix + "fields.rep_title"));
        repTitleText.setValue(parameters.getParameter("rep_title", ""));
        repTitleText.setRequired();
        if(errorFieldList.contains("rep_title")) {
            repTitleText.addError(message(message_prefix + "errors.rep_title"));
        }
        // Representatitve Email
        Item repEmail = form.addItem("rep_email","");
        Text repEmailText = repEmail.addText("rep_email");
        repEmailText.setLabel(message(message_prefix + "fields.rep_email"));
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

        // Hidden field for submitted once, lets the action know to check radios
        Item submittedOnce = form.addItem("submitted_once", "");
        Hidden submittedOnceHidden = submittedOnce.addHidden("submitted_once");
        submittedOnceHidden.setValue("1");

        // Submit button
        form.addItem().addButton("submit").setValue(message(message_prefix + "submit"));
    }
}
