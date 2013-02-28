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
        form.addLabel("org_name_label1","ds-form-label").addContent(message(message_prefix + "fields.org_name.label1"));
        form.addItem("org_name_label2","ds-form-content").addContent(message(message_prefix + "fields.org_name.label2"));
        Text org_name = form.addItem().addText("org_name");
        org_name.setValue(parameters.getParameter("org_name", ""));

        form.addLabel("org_legalname_label1","ds-form-label").addContent(message(message_prefix + "fields.org_legalname.label1"));
        form.addItem("org_legalname_label2","ds-form-content").addContent(message(message_prefix + "fields.org_legalname.label2"));
        Text org_legalname = form.addItem().addText("org_legalname");
        org_legalname.setValue(parameters.getParameter("org_legalname", ""));

        form.addLabel("org_type_label11","ds-form-label").addContent(message(message_prefix + "fields.org_type.label1"));
        form.addItem("org_type_label2","ds-form-content").addContent(message(message_prefix + "fields.org_type.label2"));
        List options = form.addList("options", List.TYPE_GLOSS);
        options.addItem().addRadio("org_type").addOption("data_center_or_repository", message(message_prefix + "fields.org_type.data_center_or_repository"));
        options.addItem().addRadio("org_type").addOption("funding_organization", message(message_prefix + "fields.org_type.funding_organization"));
        options.addItem().addRadio("org_type").addOption("journal", message(message_prefix + "fields.org_type.journal"));
        options.addItem().addRadio("org_type").addOption("publisher", message(message_prefix + "fields.org_type.publisher"));
        options.addItem().addRadio("org_type").addOption("scholarly_society", message(message_prefix + "fields.org_type.scholarly_society"));
        options.addItem().addRadio("org_type").addOption("university_research_or_edu_institute", message(message_prefix + "fields.org_type.university_research_or_edu_institute"));
        options.addItem().addRadio("org_type").addOption("other", message(message_prefix + "fields.org_type.other"));
        Text org_type_other = options.addItem().addText("org_type_other");
        org_type_other.setValue(parameters.getParameter("org_type_other", ""));

        form.addLabel("org_annual_revenue_label1","ds-form-label").addContent(message(message_prefix + "fields.org_annual_revenue.label1"));
        form.addItem("org_annual_revenue_label2","ds-form-content").addContent(message(message_prefix + "fields.org_annual_revenue.label2"));
        options = form.addList("fields", List.TYPE_GLOSS);
        options.addItem().addRadio("org_annual_revenue").addOption("less_than_10_million", message(message_prefix + "fields.org_annual_revenue.less_than_10_million"));
        options.addItem().addRadio("org_annual_revenue").addOption("greater_than_10_million", message(message_prefix + "fields.org_annual_revenue.greater_than_10_million"));

        form.addLabel("billing_contact_name_label1","ds-form-label").addContent(message(message_prefix + "fields.billing_contact_name.label1"));
        form.addItem("billing_contact_name_label2","ds-form-content").addContent(message(message_prefix + "fields.billing_contact_name.label2"));
        Text billing_contact_name = form.addItem().addText("billing_contact_name");
        billing_contact_name.setValue(parameters.getParameter("billing_contact_name", ""));

        form.addLabel("billing_address_label1","ds-form-label").addContent(message(message_prefix + "fields.billing_address.label1"));
        form.addItem("billing_address_label2","ds-form-content").addContent(message(message_prefix + "fields.billing_address.label2"));
        TextArea billing_address = form.addItem().addTextArea("billing_address");
        billing_address.setValue(parameters.getParameter("billing_address", ""));

        form.addLabel("billing_email_label1","ds-form-label").addContent(message(message_prefix + "fields.billing_email.label1"));
        Text billing_email = form.addItem().addText("billing_email");
        billing_email.setValue(parameters.getParameter("billing_email", ""));

        form.addLabel("publications_label1","ds-form-label").addContent(message(message_prefix + "fields.publications.label1"));
        form.addItem("publications_label2","ds-form-content").addContent(message(message_prefix + "fields.publications.label2"));
        TextArea publications = form.addItem().addTextArea("publications");
        publications.setValue(parameters.getParameter("publications", ""));

        form.addLabel("membership_year_label1","ds-form-label").addContent(message(message_prefix + "fields.membership_year.label1"));
        form.addItem("membership_year_label2","ds-form-content").addContent(message(message_prefix + "fields.membership_year.label2"));
        Text membership_year = form.addItem().addText("membership_year");
        membership_year.setValue(parameters.getParameter("membership_year", ""));

        form.addLabel("rep_name_label1","ds-form-label").addContent(message(message_prefix + "fields.rep_name.label1"));
        form.addItem("rep_name_label2","ds-form-content").addContent(message(message_prefix + "fields.rep_name.label2"));
        Text rep_name = form.addItem().addText("rep_name");
        rep_name.setValue(parameters.getParameter("rep_name", ""));

        form.addLabel("rep_email_label1","ds-form-label").addContent(message(message_prefix + "fields.rep_email.label1"));
        form.addItem("rep_email_label2","ds-form-content").addContent(message(message_prefix + "fields.rep_email.label2"));
        Text rep_email = form.addItem().addText("rep_email");
        rep_email.setValue(parameters.getParameter("rep_email", ""));

        form.addLabel("comments_label1","ds-form-label").addContent(message(message_prefix + "fields.comments.label1"));
        form.addItem("comments_label2","ds-form-content").addContent(message(message_prefix + "fields.comments.label2"));
        TextArea comments = form.addItem().addTextArea("comments");
        comments.setValue(parameters.getParameter("comments", ""));
        
        form.addItem().addButton("submit").setValue(message(message_prefix + "submit"));
    }
}
