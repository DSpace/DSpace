<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - initial questions page for collection creation wizard
  -
  - attributes:
  -    collection - collection we're creating
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.app.webui.servlet.admin.CollectionWizardServlet" %>
<%@ page import="org.dspace.content.Collection" %>

<%  Collection collection = (Collection) request.getAttribute("collection"); %>

<%  Boolean sysadmin_b = (Boolean)request.getAttribute("sysadmin_button");
	boolean sysadmin_button = (sysadmin_b == null ? false : sysadmin_b.booleanValue());
    
    Boolean adminCreateGroup = (Boolean)request.getAttribute("admin_create_button");
    boolean bAdminCreateGroup = (adminCreateGroup == null ? false : adminCreateGroup.booleanValue());
    
    Boolean workflowsButton = (Boolean)request.getAttribute("workflows_button");
    boolean bWorkflowsButton = (workflowsButton == null ? false : workflowsButton.booleanValue());
    
    Boolean submittersButton = (Boolean)request.getAttribute("submitters_button");
    boolean bSubmittersButton = (submittersButton == null ? false : submittersButton.booleanValue());
    
    Boolean templateButton = (Boolean)request.getAttribute("template_button");
    boolean bTemplateButton = (templateButton == null ? false : templateButton.booleanValue());
%>


    
<dspace:layout style="submission" locbar="off"
               navbar="off"
               titlekey="jsp.dspace-admin.wizard-questions.title"
               nocache="true">
    <%-- <h1>Describe the Collection</h1> --%>
<h1><fmt:message key="jsp.dspace-admin.wizard-questions.title"/>
<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#createcollection\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
</h1>

    <form action="<%= request.getContextPath() %>/tools/collection-wizard" method="post">
        <%--<p>Please check the boxes next to the statements that apply to the collection. --%>
        <div class="help-block"><fmt:message key="jsp.dspace-admin.wizard-questions.text"/></div>

					<div class="input-group">
							<span class="input-group-addon">
                                <% if(!sysadmin_button ) { %> 
                                	<input type="hidden" name="public_read" value="true"/>
                                	<input type="checkbox" name="public_read" value="true" disabled="disabled" checked="checked"/>
                                <% } else { %>
                                	<input type="checkbox" name="public_read" value="true" checked="checked"/>
                                <% } %>
                            </span>    
                                <%-- <td class="submitFormLabel" nowrap>New items should be publicly readable</td> --%>
                            <label class="form-control" for="public_read">    
                                <fmt:message key="jsp.dspace-admin.wizard-questions.check1"/>
                                
									<% if(!sysadmin_button ) { %> 
										<fmt:message key="jsp.dspace-admin.wizard-questions.check1-disabled"/>
									<% } %>
							</label>
					</div>
					<div class="input-group">
							<span class="input-group-addon">
                                <% if(!bSubmittersButton) { %> <input type="hidden" name="submitters" value="false" />
                                <input type="checkbox" name="submitters" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="submitters" value="true" checked="checked"/>
                                <% } %>
                         	</span>
                         	<label class="form-control" for="submitters">
                                <%-- <td class="submitFormLabel" nowrap>Some users will be able to submit to this collection</td> --%>
                                <fmt:message key="jsp.dspace-admin.wizard-questions.check2"/>
                            </label>
                   </div>
                   <div class="input-group">         
                            <span class="input-group-addon">
                                <% if(!bWorkflowsButton) { %> <input type="hidden" name="workflow1" value="false" />
                                <input type="checkbox" name="workflow1" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="workflow1" value="true"/>
                                <% } %>
                            </span>
                            <label class="form-control" for="workflow1">
                                <%-- <td class="submitFormLabel" nowrap>The submission workflow will include an <em>accept/reject</em> step</td> --%>
                            	<fmt:message key="jsp.dspace-admin.wizard-questions.check3"/>
                            </label>
                        </div>    
                       <div class="input-group">
                            <span class="input-group-addon">
                                <% if(!bWorkflowsButton) { %> <input type="hidden" name="workflow2" value="false" />
                                <input type="checkbox" name="workflow2" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="workflow2" value="true"/>
                                <% } %>
                            </span>
                            <label class="form-control" for="workflow2">
                                <%-- <td class="submitFormLabel" nowrap>The submission workflow will include an <em>accept/reject/edit metadata</em> step</td> --%>
                                <fmt:message key="jsp.dspace-admin.wizard-questions.check4"/>
                            </label>
                        </div>    
                       <div class="input-group">
                            <span class="input-group-addon">
                                <% if(!bWorkflowsButton) { %> <input type="hidden" name="workflow3" value="false" />
                                <input type="checkbox" name="workflow3" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="workflow3" value="true"/>
                                <% } %>
                            </span>
                           	<label class="form-control" for="workflow3">
                                <%-- <td class="submitFormLabel" nowrap>The submission workflow will include an <em>edit metadata</em> step</td> --%>
                           		<fmt:message key="jsp.dspace-admin.wizard-questions.check5"/>
                            </label>
                      </div>      
                       <div class="input-group">     
                            <span class="input-group-addon">
                                <% if(!bAdminCreateGroup) { %> <input type="hidden" name="admins" value="false" />
                                <input type="checkbox" name="admins" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="admins" value="true"/>
                                <% } %>
                            </span>    
                            <label class="form-control" for="admins">
                                <%-- <td class="submitFormLabel" nowrap>This collection will have delegated collection administrators</td> --%>
                                <fmt:message key="jsp.dspace-admin.wizard-questions.check6"/>
                            </label>
                      </div>
                      <div class="input-group">      
                            <span class="input-group-addon">
								<% if(!bTemplateButton) { %> <input type="hidden" name="default.item" value="false" />
                                <input type="checkbox" name="default.item" value="true" disabled="disabled"/>
                                <% } else { %>
								<input type="checkbox" name="default.item" value="true"/>
								<% } %>
							</span>	
							<label class="form-control" for="default.item">
                                <%-- <td class="submitFormLabel" nowrap>New submissions will have some metadata already filled out with defaults</td> --%>
                                <fmt:message key="jsp.dspace-admin.wizard-questions.check7"/>
                            </label>
                       </div> 	
					       

	<div class="row">
		<div class="col-md-6 pull-right btn-group">
			<%-- Hidden fields needed for servlet to know which collection and page to deal with --%>
        	<input type="hidden" name="collection_id" value="<%= ((Collection) request.getAttribute("collection")).getID() %>" />
        	<input type="hidden" name="stage" value="<%= CollectionWizardServlet.INITIAL_QUESTIONS %>" />
    	    <input class="btn btn-primary pull-right col-md-6" type="submit" name="submit_next" value="<fmt:message key="jsp.dspace-admin.general.next.button"/>" />
    	</div>
    </div>   
</form>

</dspace:layout>
