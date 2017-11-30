<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Show form allowing edit of collection metadata
  -
  - Attributes:
  -    community    - community to create new collection in, if creating one
  -    collection   - collection to edit, if editing an existing one.  If this
  -                  is null, we are creating one.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="org.dspace.harvest.HarvestedCollection" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="java.util.Enumeration" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    Collection collection = (Collection) request.getAttribute("collection");
    Community community = (Community) request.getAttribute("community");

    Boolean adminCollection = (Boolean)request.getAttribute("admin_collection");
    boolean bAdminCollection = (adminCollection == null ? false : adminCollection.booleanValue());
    
    Boolean adminCreateGroup = (Boolean)request.getAttribute("admin_create_button");
    boolean bAdminCreateGroup = (adminCreateGroup == null ? false : adminCreateGroup.booleanValue());

    Boolean adminRemoveGroup = (Boolean)request.getAttribute("admin_remove_button");
    boolean bAdminRemoveGroup = (adminRemoveGroup == null ? false : adminRemoveGroup.booleanValue());
    
    Boolean workflowsButton = (Boolean)request.getAttribute("workflows_button");
    boolean bWorkflowsButton = (workflowsButton == null ? false : workflowsButton.booleanValue());
    
    Boolean submittersButton = (Boolean)request.getAttribute("submitters_button");
    boolean bSubmittersButton = (submittersButton == null ? false : submittersButton.booleanValue());
    
    Boolean templateButton = (Boolean)request.getAttribute("template_button");
    boolean bTemplateButton = (templateButton == null ? false : templateButton.booleanValue());

    Boolean policyButton = (Boolean)request.getAttribute("policy_button");
    boolean bPolicyButton = (policyButton == null ? false : policyButton.booleanValue());
    
    Boolean deleteButton = (Boolean)request.getAttribute("delete_button");
    boolean bDeleteButton = (deleteButton == null ? false : deleteButton.booleanValue());
    
    // Is the logged in user a sys admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());
    
    HarvestedCollection hc = (HarvestedCollection) request.getAttribute("harvestInstance");
    
    String name = "";
    String shortDesc = "";
    String intro = "";
    String copy = "";
    String side = "";
    String license = "";
    String provenance = "";
    
    String oaiProviderValue= "";
	String oaiSetIdValue= "";
	String metadataFormatValue= "";
	String lastHarvestMsg= "";
	int harvestLevelValue=0;
	int harvestStatus= 0;
	
    Group[] wfGroups = new Group[3];
    wfGroups[0] = null;
    wfGroups[1] = null;
    wfGroups[2] = null;

    Group admins     = null;
    Group submitters = null;

    Item template = null;

    Bitstream logo = null;
    
    if (collection != null)
    {
        name = collection.getMetadata("name");
        shortDesc = collection.getMetadata("short_description");
        intro = collection.getMetadata("introductory_text");
        copy = collection.getMetadata("copyright_text");
        side = collection.getMetadata("side_bar_text");
        provenance = collection.getMetadata("provenance_description");

        if (collection.hasCustomLicense())
        {
            license = collection.getLicense();
        }
        
        wfGroups[0] = collection.getWorkflowGroup(1);
        wfGroups[1] = collection.getWorkflowGroup(2);
        wfGroups[2] = collection.getWorkflowGroup(3);

        admins     = collection.getAdministrators();
        submitters = collection.getSubmitters();

        template = collection.getTemplateItem();

        logo = collection.getLogo();
                
        /* Harvesting stuff */
        if (hc != null) {
			oaiProviderValue = hc.getOaiSource();
			oaiSetIdValue = hc.getOaiSetId();
			metadataFormatValue = hc.getHarvestMetadataConfig();
			harvestLevelValue = hc.getHarvestType();
			lastHarvestMsg= hc.getHarvestMessage();
			harvestStatus = hc.getHarvestStatus();
		}
        
    }
%>

<dspace:layout titlekey="jsp.tools.edit-collection.title"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer"
               nocache="true">

<%
    if (collection == null)
    {
%>
    <h1><fmt:message key="jsp.tools.edit-collection.heading1"/></h1>
<% } else { %>
    <h1><fmt:message key="jsp.tools.edit-collection.heading2">
        <fmt:param><%= collection.getHandle() %></fmt:param>
        </fmt:message>
    </h1>
    
      <center>
        <table width="70%">
          <tr>
            <td class="standard">
<% if(bDeleteButton) { %>
              <form method="post" action="">
                <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_DELETE_COLLECTION %>" />
                <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-collection.button.delete"/>" />
              </form>
<% } else { %>
			&nbsp;
<% } %>
            </td>
            <td align="right" class="standard">
               <dspace:popup page="/help/site-admin.html#editcollection"><fmt:message key="jsp.help"/></dspace:popup>
            </td>
          </tr>
        </table>
      </center>
    
<% } %>

    <form method="post" action="<%= request.getContextPath() %>/tools/edit-communities">
        <table>
<%-- ===========================================================
     Basic metadata
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label1"/></td>
                <td><input type="text" name="name" value="<%= Utils.addEntities(name) %>" size="50" /></td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label2"/></td>
                <td>
                    <input type="text" name="short_description" value="<%= Utils.addEntities(shortDesc) %>" size="50"/>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label3"/></td>
                <td>
                    <textarea name="introductory_text" rows="6" cols="50"><%= Utils.addEntities(intro) %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label4"/></td>
                <td>
                    <textarea name="copyright_text" rows="6" cols="50"><%= Utils.addEntities(copy) %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label5"/></td>
                <td>
                    <textarea name="side_bar_text" rows="6" cols="50"><%= Utils.addEntities(side) %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label6"/></td>
                <td>
                    <textarea name="license" rows="6" cols="50"><%= Utils.addEntities(license) %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label7"/></td>
                <td>
                    <textarea name="provenance_description" rows="6" cols="50"><%= Utils.addEntities(provenance) %></textarea>
                </td>
            </tr>
<%-- ===========================================================
     Logo
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label8"/></td>
                <td>
<%  if (logo != null) { %>
                    <table>
                        <tr>
                            <td>
                                <img src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" alt="collection logo"/>
                            </td>
                            <td>
                                <input type="submit" name="submit_set_logo" value="<fmt:message key="jsp.tools.edit-collection.form.button.add-logo"/>" /><br/><br/>
                                <input type="submit" name="submit_delete_logo" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete-logo"/>" />
                            </td>
                        </tr>
                    </table>
<%  } else { %>

                    <input type="submit" name="submit_set_logo" value="<fmt:message key="jsp.tools.edit-collection.form.button.set-logo"/>" />
<%  } %>
                </td>
            </tr>
            
            <tr><td>&nbsp;</td></tr>
<% if(bSubmittersButton || bWorkflowsButton || bAdminCreateGroup || (admins != null && bAdminRemoveGroup)) { %>
            <tr><td colspan="2"><center><h3><fmt:message key="jsp.tools.edit-collection.form.label9"/></h3></center></td></tr>

<% }
	
   if(bSubmittersButton) { %>
<%-- ===========================================================
     Collection Submitters
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label10"/></td>
                <td>
<%  if (submitters == null) {%>
                    <input type="submit" name="submit_submitters_create" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%  } else { %>
                    <input type="submit" name="submit_submitters_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    <input type="submit" name="submit_submitters_delete" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%  } %>                    
                </td>
            </tr>   
<%  } %>            

<% if(bWorkflowsButton) { %>
<%-- ===========================================================
     Workflow groups
     =========================================================== --%>
<%
    String[] roleTexts =
    {
        	LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-collection.wf-role1"),
        	LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-collection.wf-role2"),
        	LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-collection.wf-role3")
    };

    for (int i = 0; i<3; i++) { %>
            <tr>
                <td class="submitFormLabel"><em><%= roleTexts[i] %></em> <fmt:message key="jsp.tools.edit-collection.form.label11"/></td>
                <td>
<%      if (wfGroups[i] == null) { %>
                    <input type="submit" name="submit_wf_create_<%= i + 1 %>" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%      } else { %>
                    <input type="submit" name="submit_wf_edit_<%= i + 1 %>" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    <input type="submit" name="submit_wf_delete_<%= i + 1 %>" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%      } %>
                </td>
            </tr>
<%  } %>
<%  } %>        
            <tr><td>&nbsp;</td></tr>
<% if(bAdminCreateGroup || (admins != null && bAdminRemoveGroup)) { %>
<%-- ===========================================================
     Collection Administrators
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label12"/></td>
                <td>
<%  if (admins == null) {
		if (bAdminCreateGroup) {
%>
                    <input type="submit" name="submit_admins_create" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%  	} 
	} 
	else { 
		if (bAdminCreateGroup) {
	%>
                    <input type="submit" name="submit_admins_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
	<%  }
		if (bAdminRemoveGroup) { 
		%>
                    <input type="submit" name="submit_admins_delete" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%  	}
	}	%>                    
                </td>
            </tr>   
<% } %>

<% if(bTemplateButton) { %>
<%-- ===========================================================
     Item template
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label13"/></td>
                <td>
<%  if (template == null) {%>
                    <input type="submit" name="submit_create_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />

<%  } else { %>
                    <input type="submit" name="submit_edit_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    <input type="submit" name="submit_delete_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%  } %>                    
                </td>
            </tr>
<%  } %>
   
<% if(bPolicyButton) { %>
<%-- ===========================================================
     Edit collection's policies
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label14"/></td>
                <td>
                    <input type="submit" name="submit_authorization_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                </td>
            </tr>   
<%  } %>







            

<% if(bAdminCollection) { %>
<%-- ===========================================================
     Harvesting Settings
     =========================================================== --%>
     
     		<tr><td>&nbsp;</td></tr>
            <tr><td colspan="2"><center><h3><fmt:message key="jsp.tools.edit-collection.form.label15"/></h3></center></td></tr>
     
     		<%--
     		oaiProviderValue = hc.getOaiSource();
			oaiSetIdValue = hc.getOaiSetId();
			metadataFormatValue = hc.getHarvestMetadataConfig();
			harvestLevelValue = hc.getHarvestType();
			String lastHarvestMsg= hc.getHarvestMessage();
			int harvestStatus = hc.getHarvestStatus();
			
			if (lastHarvestMsg == null)
				lastHarvestMsg = "none";
			--%>
     
     		<tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label16"/></td>
                <td>
                	<input type="radio" value="source_normal" <% if (harvestLevelValue == 0) { %> checked="checked" <% } %> name="source"><fmt:message key="jsp.tools.edit-collection.form.label17"/></input><br/>
                	<input type="radio" value="source_harvested" <% if (harvestLevelValue > 0) { %> checked="checked" <% } %> name="source"><fmt:message key="jsp.tools.edit-collection.form.label18"/></input><br/>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label19"/></td>
                <td><input type="text" name="oai_provider" value="<%= oaiProviderValue %>" size="50" /></td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label20"/></td>
                <td><input type="text" name="oai_setid" value="<%= oaiSetIdValue %>" size="50" /></td>
            </tr>   
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label21"/></td>
                <td>
                	<select name="metadata_format" >
	                	<%
		                // Add an entry for each instance of ingestion crosswalks configured for harvesting 
			            String metaString = "harvester.oai.metadataformats.";
			            Enumeration pe = ConfigurationManager.propertyNames("oai");
			            while (pe.hasMoreElements())
			            {
			                String key = (String)pe.nextElement();
							
							
			                if (key.startsWith(metaString)) {
			                	String metadataString = ConfigurationManager.getProperty("oai", key);
			                	String metadataKey = key.substring(metaString.length());
								String label = "jsp.tools.edit-collection.form.label21.select." + metadataKey;
		                	
	                	%>
			                	<option value="<%= metadataKey %>" 
			                	<% if(metadataKey.equalsIgnoreCase(metadataFormatValue)) { %> 
			                	selected="selected" <% } %> >
								<fmt:message key="<%=label%>"/>
								</option>
			                	<% 
			                }
			            }
		                %>
					</select>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label22"/></td>
                <td>
                	<input type="radio" value="1" <% if (harvestLevelValue != 2 && harvestLevelValue != 3) { %> checked="checked" <% } %> name="harvest_level"><fmt:message key="jsp.tools.edit-collection.form.label23"/></input><br/>
                	<input type="radio" value="2" <% if (harvestLevelValue == 2) { %> checked="checked" <% } %> name="harvest_level"><fmt:message key="jsp.tools.edit-collection.form.label24"/></input><br/>
                	<input type="radio" value="3" <% if (harvestLevelValue == 3) { %> checked="checked" <% } %> name="harvest_level"><fmt:message key="jsp.tools.edit-collection.form.label25"/></input><br/>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label26"/></td>
                <td><%= lastHarvestMsg %></td>
            </tr> 
            <!--
            <tr>
                <td class="submitFormLabel">Current Status</td>
                <td> </td>
            </tr>
            --> 


<%  } %>


        </table>
        
        <p>&nbsp;</p>

        <center>
            <table width="70%">
                <tr>
                    <td class="standard">
<%
    if (collection == null)
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="create" value="true" />
                        <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-collection.form.button.create2"/>" />
<%
    }
    else
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                        <input type="hidden" name="create" value="false" />
                        <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-collection.form.button.update"/>" />
<% 
    }
%>
                    </td>
                    <td>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_EDIT_COLLECTION %>" /> 
                        <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.edit-collection.form.button.cancel"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>