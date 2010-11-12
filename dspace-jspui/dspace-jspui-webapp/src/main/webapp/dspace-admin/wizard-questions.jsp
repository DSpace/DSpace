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


    
<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.dspace-admin.wizard-questions.title"
               nocache="true">
    <%-- <h1>Describe the Collection</h1> --%>
<h1><fmt:message key="jsp.dspace-admin.wizard-questions.title"/></h1>

    <form action="<%= request.getContextPath() %>/tools/collection-wizard" method="post">
        <%--<p>Please check the boxes next to the statements that apply to the collection. --%>
        <div><fmt:message key="jsp.dspace-admin.wizard-questions.text"/>
          	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#createcollection\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>
        <center>
            <table class="miscTable">
                <tr class="oddRowOddCol">
                    <td class="oddRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!sysadmin_button ) { %> <input type="hidden" name="public_read" value="true"/>
                                <input type="checkbox" name="public_read" value="true" disabled="disabled" checked="checked"/>
                                <% } else { %>
                                <input type="checkbox" name="public_read" value="true" checked="checked"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>New items should be publicly readable</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap">
                                	<fmt:message key="jsp.dspace-admin.wizard-questions.check1"/>
									<% if(!sysadmin_button ) { %> 
										<fmt:message key="jsp.dspace-admin.wizard-questions.check1-disabled"/>
									<% } %>
								</td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="evenRowOddCol">
                    <td class="evenRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!bSubmittersButton) { %> <input type="hidden" name="submitters" value="false" />
                                <input type="checkbox" name="submitters" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="submitters" value="true" checked="checked"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>Some users will be able to submit to this collection</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check2"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="oddRowOddCol">
                    <td class="oddRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!bWorkflowsButton) { %> <input type="hidden" name="workflow1" value="false" />
                                <input type="checkbox" name="workflow1" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="workflow1" value="true"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>The submission workflow will include an <em>accept/reject</em> step</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check3"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="evenRowOddCol">
                    <td class="evenRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!bWorkflowsButton) { %> <input type="hidden" name="workflow2" value="false" />
                                <input type="checkbox" name="workflow2" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="workflow2" value="true"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>The submission workflow will include an <em>accept/reject/edit metadata</em> step</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check4"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="oddRowOddCol">
                    <td class="oddRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!bWorkflowsButton) { %> <input type="hidden" name="workflow3" value="false" />
                                <input type="checkbox" name="workflow3" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="workflow3" value="true"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>The submission workflow will include an <em>edit metadata</em> step</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check5"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="evenRowOddCol">
                    <td class="evenRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!bAdminCreateGroup) { %> <input type="hidden" name="admins" value="false" />
                                <input type="checkbox" name="admins" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="admins" value="true"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>This collection will have delegated collection administrators</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check6"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>                
                <tr class="oddRowOddCol">
                    <td class="oddRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
								<% if(!bTemplateButton) { %> <input type="hidden" name="default.item" value="false" />
                                <input type="checkbox" name="default.item" value="true" disabled="disabled"/>
                                <% } else { %>
								<input type="checkbox" name="default.item" value="true"/></td>
								<% } %>
                                <%-- <td class="submitFormLabel" nowrap>New submissions will have some metadata already filled out with defaults</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check7"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>
			</table>
		</center>
	
       <p>&nbsp;</p>

<%-- Hidden fields needed for servlet to know which collection and page to deal with --%>
        <input type="hidden" name="collection_id" value="<%= ((Collection) request.getAttribute("collection")).getID() %>" />
        <input type="hidden" name="stage" value="<%= CollectionWizardServlet.INITIAL_QUESTIONS %>" />

        <center>
            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;
                        
                    </td>
                    <td>
                        <input type="submit" name="submit_next" value="<fmt:message key="jsp.dspace-admin.general.next.button"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>

</dspace:layout>
