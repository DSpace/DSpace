/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 * Tag to display embargo settings
 *
 * @author Keiji Suzuki
 * @version $Revision$
 */
public class AccessSettingTag extends TagSupport
{
	/** log4j category */
    private static final Logger log = Logger.getLogger(AccessSettingTag.class);

    /** is advanced form enabled? */
    private static final boolean advanced = ConfigurationManager.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

    /** Name of the restricted group */
    private static final String restrictedGroup = ConfigurationManager.getProperty("webui.submission.restrictstep.groups");

    /** the SubmissionInfo */
    private transient SubmissionInfo subInfo = null;

    /** the target DSpaceObject */
    private transient DSpaceObject dso = null;

    /** the target ResourcePolicy */
    private transient ResourcePolicy rp = null;

    /** disable the radio button for open/embargo access */
    private boolean embargo = false;

    /** hide the embargo date and reason fields */
    private boolean hidden = false;

    /** add the policy button */
    private boolean addpolicy = false;
    
    private final transient AuthorizeService authorizeService
            = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    private final transient GroupService groupService
            = EPersonServiceFactory.getInstance().getGroupService();

    public AccessSettingTag()
    {
        super();
    }

    @Override
    public int doStartTag() throws JspException
    {
//        String legend = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.legend");
        String label_name = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.label_name");
        String label_group = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.label_group");
        String label_embargo = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.label_embargo");
        String label_date = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.label_date");
        String radio0 = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.radio0");
        String radio1 = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.radio1");
        String radio_help = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.radio_help");
        String label_reason = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.label_reason");
        String button_confirm = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.button_confirm");

        String help_name = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.name_help");
        String help_reason = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.reason_help");

        JspWriter out = pageContext.getOut();
        StringBuffer sb = new StringBuffer();

        try
        {
            HttpServletRequest hrq = (HttpServletRequest) pageContext.getRequest();
            Context context = UIUtil.obtainContext(hrq);
    
            // get startDate and reason of the resource policy of the target DSpaceObject
            List<ResourcePolicy> policies = null;
            if (!advanced && dso != null)
            {
                policies = authorizeService.findPoliciesByDSOAndType(context, dso, ResourcePolicy.TYPE_CUSTOM);
            }
            else if (rp != null)
            {
                policies = new ArrayList<>();
                policies.add(rp);
            }

            String name = "";
            UUID group_id = null; 
            String startDate = "";
            String reason = "";
            String radio0Checked = " checked=\"checked\"";
            String radio1Checked = "";
            String disabled      = " disabled=\"disabled\"";
            if (policies != null && policies.size() > 0)
            {
                ResourcePolicy rp = policies.get(0);
                name = (rp.getRpName() == null ? "" : rp.getRpName());
                group_id = rp.getGroup().getID();
                startDate = (rp.getStartDate() != null ? DateFormatUtils.format(rp.getStartDate(), "yyyy-MM-dd") : "");
                reason = (rp.getRpDescription() == null ? "" : rp.getRpDescription());
                if (!startDate.equals(""))
                {
                    radio0Checked = "";
                    radio1Checked = " checked=\"checked\"";
                    disabled      = "";
                }
            }

            // if advanced embargo is disabled, embargo date and reason fields are always enabled
            if (!advanced) {
                disabled = "";
            }
                        
            if (embargo)
            {
                // Name
            	sb.append("<div class=\"form-group\">");
                sb.append(label_name).append("\n");                
                sb.append("<p class=\"help-block\">").append(help_name).append("</p>").append("\n");             
                sb.append("<input class=\"form-control\" name=\"name\" id=\"policy_name\" type=\"text\" maxlength=\"30\" value=\"").append(name).append("\" />\n");
                sb.append("</div>"); 
                		
                // Group
                sb.append("<div class=\"form-group\">");
                sb.append(label_group).append("\n");
                sb.append("<select class=\"form-control\" name=\"group_id\" id=\"select_group\">\n");

                List<Group> groups = getGroups(context, hrq, subInfo);
                if (groups != null)
                {
                    for (Group group : groups)
                    {
                        sb.append("<option value=\"").append(group.getID()).append("\"");
                        if (group_id == group.getID()) {
                            sb.append(" selected=\"selected\"");
                        }
                        sb.append(">").append(group.getName()).append("</option>\n");
                    }
                }
                else
                {
                    sb.append("<option value=\"0\" selected=\"selected\">Anonymous</option>\n");
                }
                sb.append("</select>\n");
                sb.append("</div>"); 
                // Select open or embargo
                sb.append(label_embargo).append("\n");
                sb.append("<div class=\"radio\">");                
                sb.append("<label><input name=\"open_access_radios\" type=\"radio\" value=\"0\"").append(radio0Checked).append(" />").append(radio0).append("</label>\n");
                sb.append("</div>");
                sb.append("<div class=\"radio\">");  
                sb.append("<label><input name=\"open_access_radios\" type=\"radio\" value=\"1\"").append(radio1Checked).append(" />").append(radio1).append("</label>\n");
                sb.append("</div>");
                 
            }

            // Embargo Date
            if (hidden)
            {
                sb.append("<input name=\"embargo_until_date\" id=\"embargo_until_date_hidden\" type=\"hidden\" value=\"").append(startDate).append("\" />\n");
                sb.append("<input name=\"reason\" id=\"reason_hidden\" type=\"hidden\" value=\"").append(reason).append("\" />\n");
            }
            else
            {
            	sb.append("<div class=\"form-group col-md-12\">");
            	sb.append("<div class=\"col-md-2\">");
            	sb.append(label_date);
            	sb.append("</div>");
            	sb.append("<div class=\"col-md-2\">");
                sb.append("<input class=\"form-control\" name=\"embargo_until_date\" id=\"embargo_until_date\" maxlength=\"10\" size=\"10\" type=\"text\" value=\"").append(startDate).append("\"").append(disabled).append(" />\n");
                sb.append("</div>");
                sb.append("<div class=\"col-md-8\">");
                sb.append("<span class=\"help-block\">"+radio_help+"</span>");
                sb.append("</div>");
                sb.append("</div>");
                // Reason                
                sb.append("<div class=\"form-group col-md-12\">");
                sb.append("<div class=\"col-md-12\">");
                sb.append(label_reason).append("\n"); 
                sb.append("</div>");
                sb.append("<div class=\"col-md-12\">");
                sb.append("<p class=\"help-block\">").append(help_reason).append("</p>").append("\n");
                sb.append("</div>");
                sb.append("<div class=\"col-md-12\">");
                sb.append("<textarea class=\"form-control\" name=\"reason\" id=\"reason\" cols=\"30\" rows=\"5\"").append(disabled).append(">").append(reason).append("</textarea>\n");
                sb.append("</div>");
                sb.append("</div>");
            }

            // Add policy button
            if (addpolicy)
            {
                
                sb.append("<input class=\"btn btn-success col-md-offset-5\" name=\"submit_add_policy\" type=\"submit\" value=\"").append(button_confirm).append("\" />\n");
                
            }
            

            out.println(sb.toString());
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }
        catch (SQLException e)
        {
        	throw new JspException(e);
        }

        return SKIP_BODY;
    }

    /**
     * Get the browseInfo
     *
     * @return the browseInfo
     */
    public SubmissionInfo getSubInfo()
    {
        return subInfo;
    }

    /**
     * Set the subInfo (SubmissionInfo)
     *
     * @param subInfo
     *            the subInfo
     */
    public void setSubInfo(SubmissionInfo subInfo)
    {
        this.subInfo = subInfo;
    }

    /**
     * Get the dso
     *
     * @return the dso
     */
    public DSpaceObject getDso()
    {
        return dso;
    }

    /**
     * Set the the dso
     *
     * @param dso
     *            the dso
     */
    public void setDso(DSpaceObject dso)
    {
        this.dso = dso;
    }

    /**
     * Get the rp
     *
     * @return the rp
     */
    public ResourcePolicy getRp()
    {
        return rp;
    }

    /**
     * Set the the rp
     *
     * @param rp
     *            the rp
     */
    public void setRp(ResourcePolicy rp)
    {
        this.rp = rp;
    }

    /**
     * Get the display open/embargo setting radio flag
     *
     * @return radio
     */
    public boolean getEmbargo()
    {
        return embargo;
    }

    /**
     * Set the display open/embargo setting radio flag
     *
     * @param embargo
     *            boolean
     */
    public void setEmbargo(boolean embargo)
    {
        this.embargo = embargo;
    }

    /**
     * Get the hidden flag
     *
     * @return hidden
     */
    public boolean getHidden()
    {
        return hidden;
    }

    /**
     * Set the hidden flag
     *
     * @param hidden
     *            boolean
     */
    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    /**
     * Set the add_policy button flag
     *
     * @param addpolicy
     *            boolean
     */
    public void setAddpolicy(boolean addpolicy)
    {
        this.addpolicy = addpolicy;
    }

    /**
     * Get the add_policy button flag
     *
     * @return addpolicy
     */
    public boolean getAddpolicy()
    {
        return addpolicy;
    }

    @Override
    public void release()
    {
        dso = null;
        subInfo = null;
        rp = null;
        embargo = false;
        hidden = false;
        addpolicy = false;
    }

    private List<Group> getGroups(Context context, HttpServletRequest request, SubmissionInfo subInfo)
        throws SQLException
    {
        List<Group> groups = null;
        // retrieve groups
        if (restrictedGroup != null)
        {
            Group uiGroup = groupService.findByName(context, restrictedGroup);
            if (uiGroup != null)
            {
                groups = uiGroup.getMemberGroups();
            }
        }

        if (groups == null || groups.size() == 0){
            groups = groupService.findAll(context, null);
        }

        return groups;
    }

}
