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
<%@ page import="org.dspace.content.service.CollectionService" %>
<%@ page import="org.dspace.content.factory.ContentServiceFactory" %>

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
    
   // Is the logged in user an admin or community admin or collection admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());
    
    Boolean communityAdmin = (Boolean)request.getAttribute("is.communityAdmin");
    boolean isCommunityAdmin = (communityAdmin == null ? false : communityAdmin.booleanValue());
    
    Boolean collectionAdmin = (Boolean)request.getAttribute("is.collectionAdmin");
    boolean isCollectionAdmin = (collectionAdmin == null ? false : collectionAdmin.booleanValue());
    
    String naviAdmin = "admin";
    String link = "/dspace-admin";
    
    if(!isAdmin && (isCommunityAdmin || isCollectionAdmin))
    {
        naviAdmin = "community-or-collection-admin";
        link = "/tools";
    }
    
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

    CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    if (collection != null)
    {
        name = collectionService.getMetadata(collection, "name");
        shortDesc = collectionService.getMetadata(collection, "short_description");
        intro = collectionService.getMetadata(collection, "introductory_text");
        copy = collectionService.getMetadata(collection, "copyright_text");
        side = collectionService.getMetadata(collection, "side_bar_text");
        provenance = collectionService.getMetadata(collection, "provenance_description");

        if (collectionService.hasCustomLicense(collection))
        {
            license = collectionService.getLicense(collection);
        }
        
        wfGroups[0] = collection.getWorkflowStep1();
        wfGroups[1] = collection.getWorkflowStep2();
        wfGroups[2] = collection.getWorkflowStep3();

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

<dspace:layout style="submission" titlekey="jsp.tools.edit-collection.title"
               navbar="<%= naviAdmin %>"
               locbar="link"
               parentlink="<%= link %>"
               parenttitlekey="jsp.administer"
               nocache="true">
<div class="row">
<h3 class="col-md-8">
<%
    if (collection == null)
    {
%>
    <fmt:message key="jsp.tools.edit-collection.heading1"/>
<% } else { %>
    <fmt:message key="jsp.tools.edit-collection.heading2">
        <fmt:param><%= collection.getHandle() %></fmt:param>
    </fmt:message>
<% } %>    
	<span>
		<dspace:popup page="/help/site-admin.html#editcollection"><fmt:message key="jsp.help"/></dspace:popup>
	</span>
	</h3>    
<% if(bDeleteButton) { %>
              <form class="col-md-4" method="post" action="">
                <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_DELETE_COLLECTION %>" />
                <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                <input class="btn btn-danger col-md-12" type="submit" name="submit_delete" value="<fmt:message key="jsp.tools.edit-collection.button.delete"/>" />
              </form>
<% } %>
</div>
<div class="row">
<form class="form-group" method="post" action="<%= request.getContextPath() %>/tools/edit-communities">
	<div class="col-md-8">
    
<%-- ===========================================================
     Basic metadata
     =========================================================== --%>
     <div class="panel panel-primary">
     	<div class="panel-heading"><fmt:message key="jsp.tools.edit-collection.form.basic-metadata" /></div>
     	<div class="panel-body">
        	<div class="row">        
                <label class="col-md-3" for="name"><fmt:message key="jsp.tools.edit-collection.form.label1"/></label>
                <span class="col-md-9">
                	<input class="form-control" type="text" name="name" value="<%= Utils.addEntities(name) %>" />
                </span>
            </div><br/>    
            <div class="row">        
                <label class="col-md-3" for="short_description"><fmt:message key="jsp.tools.edit-collection.form.label2"/></label>
                <span class="col-md-9">
                	<input class="form-control" type="text" name="short_description" value="<%= Utils.addEntities(shortDesc) %>" size="50"/>
                </span>	
            </div><br/>
            <div class="row">        
                <label class="col-md-3" for="introductory_text"><fmt:message key="jsp.tools.edit-collection.form.label3"/></label>
                <span class="col-md-9">
                	<textarea class="form-control" name="introductory_text" rows="6" cols="50"><%= Utils.addEntities(intro) %></textarea>
                </span>
            </div><br/>
             <div class="row">
                <label class="col-md-3" for="copyright_text"><fmt:message key="jsp.tools.edit-collection.form.label4"/></label>
                <span class="col-md-9">
                	<textarea class="form-control" name="copyright_text" rows="6" cols="50"><%= Utils.addEntities(copy) %></textarea>
                </span>
            </div><br/>
            <div class="row">
            	<label class="col-md-3" for="side_bar_text"><fmt:message key="jsp.tools.edit-collection.form.label5"/></label>
                <span class="col-md-9">
                	<textarea class="form-control" name="side_bar_text" rows="6" cols="50"><%= Utils.addEntities(side) %></textarea>
                </span>
            </div><br/>
            <div class="row">
            	<label class="col-md-3" for="license"><fmt:message key="jsp.tools.edit-collection.form.label6"/></label>
                <span class="col-md-9">
                	<textarea class="form-control" name="license" rows="6" cols="50"><%= Utils.addEntities(license) %></textarea>
                </span>
            </div><br/>
            <div class="row">
            	<label class="col-md-3" for="provenance_description"><fmt:message key="jsp.tools.edit-collection.form.label7"/></label>
                <span class="col-md-9">
                	<textarea class="form-control" name="provenance_description" rows="6" cols="50"><%= Utils.addEntities(provenance) %></textarea>
                </span>
            </div><br/>
<%-- ===========================================================
     Logo
     =========================================================== --%>
            <div class="row">
                <label class="col-md-3" for=""><fmt:message key="jsp.tools.edit-collection.form.label8"/></label>
                <div class="col-md-9">
<%  if (logo != null) { %>
                                <span class="col-md-6">
                                <img class="img-responsive" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" alt="collection logo"/>
                                </span>
                                <input class="btn btn-default col-md-3" type="submit" name="submit_set_logo" value="<fmt:message key="jsp.tools.edit-collection.form.button.add-logo"/>" />
                                <input class="btn btn-danger col-md-3" type="submit" name="submit_delete_logo" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete-logo"/>" />
<%  } else { %>

                    <input class="col-md-12 btn btn-success" type="submit" name="submit_set_logo" value="<fmt:message key="jsp.tools.edit-collection.form.button.set-logo"/>" />
<%  } %>
				</div>
			</div>	
		</div>
	</div>	
</div>
<div class="col-md-4">
<% if(bSubmittersButton || bWorkflowsButton || bAdminCreateGroup || (admins != null && bAdminRemoveGroup)) { %>
            <div class="panel panel-default"><div class="panel-heading"><fmt:message key="jsp.tools.edit-collection.form.label9"/></div>
            <div class="panel-body">

<% }
	
   if(bSubmittersButton) { %>
<%-- ===========================================================
     Collection Submitters
     =========================================================== --%>
            <div class="row">     
                <label class="col-md-6" for="submit_submitters_create"><fmt:message key="jsp.tools.edit-collection.form.label10"/></label>
                <span class="col-md-6 btn-group">
<%  if (submitters == null) {%>
                    <input class="btn btn-success col-md-12" type="submit" name="submit_submitters_create" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%  } else { %>
                    <input class="btn btn-default col-md-6"  type="submit" name="submit_submitters_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    <input class="btn btn-danger col-md-6"  type="submit" name="submit_submitters_delete" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%  } %>
				</span>
			</div><br/>                
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
    		<div class="row">  
                <label class="col-md-6" for="submit_wf_create_<%= i + 1 %>"><em><%= roleTexts[i] %></em> <fmt:message key="jsp.tools.edit-collection.form.label11"/></label>
                <span class="col-md-6 btn-group">
<%      if (wfGroups[i] == null) { %>
                    <input class="btn btn-success col-md-12" type="submit" name="submit_wf_create_<%= i + 1 %>" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%      } else { %>
                    <input class="btn btn-default col-md-6" type="submit" name="submit_wf_edit_<%= i + 1 %>" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    <input class="btn btn-danger col-md-6" type="submit" name="submit_wf_delete_<%= i + 1 %>" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%      } %>
				</span>
			</div><br/>
<%  } %>
<%  } %>        
<% if(bAdminCreateGroup || (admins != null && bAdminRemoveGroup)) { %>
<%-- ===========================================================
     Collection Administrators
     =========================================================== --%>
            <div class="row">    
                <label class="col-md-6" for="submit_admins_create"><fmt:message key="jsp.tools.edit-collection.form.label12"/></label>
                <span class="col-md-6 btn-group">
<%  if (admins == null) {
		if (bAdminCreateGroup) {
%>
                    <input class="btn btn-success col-md-12" type="submit" name="submit_admins_create" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%  	} 
	} 
	else { 
		if (bAdminCreateGroup) {
	%>
                    <input class="btn btn-default" type="submit" name="submit_admins_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
	<%  }
		if (bAdminRemoveGroup) { 
		%>
                    <input class="btn btn-danger" type="submit" name="submit_admins_delete" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%  	}
	}	%>        
				</span>
			</div>
		</div>
	</div>
<% } %>
<div class="panel panel-default">
	<div class="panel-heading"><fmt:message key="jsp.tools.edit-collection.form.collection-settings" /></div>
	<div class="panel-body">
<% if(bTemplateButton) { %>
			<div class="row">
<%-- ===========================================================
     Item template
     =========================================================== --%>
                <label class="col-md-6" for="submit_create_template"><fmt:message key="jsp.tools.edit-collection.form.label13"/></label>
                <span class="col-md-6 btn-group">
<%  if (template == null) {%>
                    <input class="btn btn-success col-md-12" type="submit" name="submit_create_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />

<%  } else { %>
                    <input class="btn btn-default col-md-6" type="submit" name="submit_edit_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    <input class="btn btn-default col-md-6" type="submit" name="submit_delete_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%  } %>
				</span>
			</div><br/>
<%  } %>
   
<% if(bPolicyButton) { %>
<%-- ===========================================================
     Edit collection's policies
     =========================================================== --%>
     		<div class="row">
                <label class="col-md-6" for="submit_authorization_edit"><fmt:message key="jsp.tools.edit-collection.form.label14"/></label>
                <span class="col-md-6 btn-group">
                    <input class="btn btn-success col-md-12" type="submit" name="submit_authorization_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                </span>
        	</div><br/>    
<%  } %>

<% if(bAdminCollection) { %>
<%-- ===========================================================
     Curate collection
     =========================================================== --%>
            <div class="row">
                <label class="col-md-6" for=""><fmt:message key="jsp.tools.edit-collection.form.label27"/></label>
                <span  class="col-md-6 btn-group">
                    <input class="btn btn-success col-md-12" type="submit" name="submit_curate_collection" value="<fmt:message key="jsp.tools.edit-collection.form.button.curate"/>" />
				</span>
			</div>
<%  } %>

		</div>
   </div>
<% if(bAdminCollection) { %>
<%-- ===========================================================
     Harvesting Settings
     =========================================================== --%>
   <div class="panel panel-default">
       	<div class="panel-heading"><fmt:message key="jsp.tools.edit-collection.form.label15"/></div>
		<div class="panel-body">
     
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
     
                <div class="input-group">	
                <label class="input-group-addon" for="source_normal"><fmt:message key="jsp.tools.edit-collection.form.label16"/></label>
                <div class="form-control">
                	<input class="col-md-1" type="radio" value="source_normal" <% if (harvestLevelValue == 0) { %> checked="checked" <% } %> name="source" />
                	<span class="col-md-11"><fmt:message key="jsp.tools.edit-collection.form.label17"/></span>
               		<input class="col-md-1" type="radio" value="source_harvested" <% if (harvestLevelValue > 0) { %> checked="checked" <% } %> name="source" />
                	<span class="col-md-11"><fmt:message key="jsp.tools.edit-collection.form.label18"/></span>
                </div>
                </div><br/>
                <div class="row">
                	<label class="col-md-3" for="oai_provider"><fmt:message key="jsp.tools.edit-collection.form.label19"/></label>
                	<span class="col-md-9">
                		<input class="form-control" type="text" name="oai_provider" value="<%= oaiProviderValue %>" size="50" />
                	</span>	
                </div><br/>
                <div class="row">
                	<label class="col-md-3" for="oai_setid"><fmt:message key="jsp.tools.edit-collection.form.label20"/></label>
                	<span class="col-md-9">
                		<input class="form-control" type="text" name="oai_setid" value="<%= oaiSetIdValue %>" size="50" />
                	</span>
                </div><br/>
                <div class="row">
                	<label class="col-md-3" for="metadata_format"><fmt:message key="jsp.tools.edit-collection.form.label21"/></label>
                	<span class="col-md-9">
                	<select class="form-control" name="metadata_format" >
	                	<%
		                // Add an entry for each instance of ingestion crosswalks configured for harvesting 
			            String metaString = "harvester.metadataformats.";
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
					</span>
				</div><br/>
				<div class="input-group">	
                <label class="input-group-addon" for="harvest_level"><fmt:message key="jsp.tools.edit-collection.form.label22"/></label>
                <div class="form-control">
                	<input class="col-md-1" type="radio" value="1" <% if (harvestLevelValue != 2 && harvestLevelValue != 3) { %> checked="checked" <% } %> name="harvest_level" />
                	<span class="col-md-11"><fmt:message key="jsp.tools.edit-collection.form.label23"/></span><br/>
                	<input class="col-md-1" type="radio" value="2" <% if (harvestLevelValue == 2) { %> checked="checked" <% } %> name="harvest_level" />
                	<span class="col-md-11"><fmt:message key="jsp.tools.edit-collection.form.label24"/></span><br/>
                	<input class="col-md-1" type="radio" value="3" <% if (harvestLevelValue == 3) { %> checked="checked" <% } %> name="harvest_level" />
                	<span class="col-md-11"><fmt:message key="jsp.tools.edit-collection.form.label25"/></span><br/>
                </div>
                </div><br/>	
                <div class="row">
                <label class="col-md-6"><fmt:message key="jsp.tools.edit-collection.form.label26"/></label>
                <span class="col-md-6"><%= lastHarvestMsg %></span>
                </div>
		</div>
	</div>	                
<%  } %>
</div>
<div class="btn-group col-md-12">
<%
    if (collection == null)
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="create" value="true" />
                        <input class="btn btn-success col-md-6" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-collection.form.button.create2"/>" />
<%
    }
    else
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                        <input type="hidden" name="create" value="false" />
                        <input class="btn btn-success col-md-6" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-collection.form.button.update"/>" />
<% 
    }
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_EDIT_COLLECTION %>" /> 
                        <input class="btn btn-warning col-md-6" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.edit-collection.form.button.cancel"/>" />
</div>                        
    </form>
    </div>
</dspace:layout>
