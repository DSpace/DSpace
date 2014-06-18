<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - set up a group with particular permissions
  -
  - attributes:
  -    collection - collection we're creating
  -    permission - one of the constants starting PERM_ at the top of
  -                 org.dspace.app.webui.servlet.admin.CollectionWizardServlet
  --%>



<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.app.webui.servlet.admin.CollectionWizardServlet" %>
<%@ page import="org.dspace.content.Collection" %>

<%
    Collection collection = (Collection) request.getAttribute("collection");
    int perm = ((Integer) request.getAttribute("permission")).intValue();
    boolean mitGroup = (request.getAttribute("mitgroup") != null);
%>

<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.dspace-admin.wizard-permissions.title"
               nocache="true">

<%
	switch (perm)
	{
	case CollectionWizardServlet.PERM_READ:
%>
	<%-- <h1>Authorization to Read</h1> --%>

    <h1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading1"/></h1>

	<%-- <p>Who has (by default) permission to read new items submitted to this collection? --%>
	<p><fmt:message key="jsp.dspace-admin.wizard-permissions.text1"/></p>
<%
	break;

	case CollectionWizardServlet.PERM_SUBMIT:
%>
	<%-- <h1>Authorization to Submit</h1> --%>
	<h1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading2"/></h1>

	<%-- <p>Who has permission to submit new items to this collection? --%>
	<p><fmt:message key="jsp.dspace-admin.wizard-permissions.text2"/></p>
<%
	break;

	case CollectionWizardServlet.PERM_WF1:
%>
	<%-- <h1>Submission Workflow Accept/Reject Step</h1> --%>
	<h1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading3"/></h1>

	<%-- <p>Who is responsible for performing the <strong>accept/reject</strong> step?
	They will be able to accept or reject incoming submissions.  They will not be
	able to edit the submission's metadata, however.  Only one of the group need perform the step
	for each submission. --%>
	<p><fmt:message key="jsp.dspace-admin.wizard-permissions.text3"/></p>
<%
	break;

	case CollectionWizardServlet.PERM_WF2:
%>
	<%-- <h1>Submission Workflow Accept/Reject/Edit Metadata Step</h1> --%>
	<h1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading4"/></h1>

	<%-- <p>Who is responsible for performing the <strong>accept/reject/edit metadata</strong> step?
		They will be able to edit the metadata of incoming submissions, and then accept
		or reject them.  Only one of the group need perform the step for each submission. --%>
	<p><fmt:message key="jsp.dspace-admin.wizard-permissions.text4"/></p>
<%
	break;

	case CollectionWizardServlet.PERM_WF3:
%>
	<%-- <h1>Submission Workflow Edit Metadata Step</h1> --%>
	<h1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading5"/></h1>

	<%-- <p>Who is responsible for performing the <strong>edit metadata</strong> step?
	They will be able to edit the metadata of incoming submissions, but will not
	be able to reject them.</p>--%>
	<p><fmt:message key="jsp.dspace-admin.wizard-permissions.text5"/>
<%
	break;

	case CollectionWizardServlet.PERM_ADMIN:
%>
	<%-- <h1>Delegated Collection Administrators</h1> --%>
	<h1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading6"/></h1>
	<%-- <p>Who are the collection administrators for this collection?  They will be able to decide who can submit items
    to the collection, withdraw items, edit item metadata (after submission), and add (map) existing items from
    other collections to this collection (subject to authorization from that collection).</p>--%>
	<p><fmt:message key="jsp.dspace-admin.wizard-permissions.text6"/></p>
<%
	break;
	}
%>
	<div><dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#wizard_permissions\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>
	<%-- <p>You can change this later using the relevant sections of the DSpace admin UI.</p> --%>
	<p><fmt:message key="jsp.dspace-admin.wizard-permissions.change"/></p>

    <form name="epersongroup" action="<%= request.getContextPath() %>/tools/collection-wizard" method="post">
		<center>
			<table>
<%
	// MIT group checkbox - only if there's an MIT group and on the READ and SUBMIT pages
	// (Sorry, everyone who isn't running DSpace at MIT, I know this isn't very elegant!)

	if (mitGroup &&
	    (perm == CollectionWizardServlet.PERM_READ || perm == CollectionWizardServlet.PERM_SUBMIT))
	{
%>
				<tr>
					<td></td>
					<%-- 	<td><input type="checkbox" name="mitgroup" value="true" />&nbsp;<span class="submitFormLabel">All MIT users</span> --%>
					<td><input type="checkbox" name="mitgroup" value="true"/>&nbsp;<span class="submitFormLabel"><fmt:message key="jsp.dspace-admin.wizard-permissions.mit"/></span>
					</td>
				</tr>
				<tr>
					<td colspan="2">&nbsp;</td>
				</tr>
				<tr>
					<%-- <td colspan="2" class="submitFormHelp"><strong>OR</strong></td> --%>
					<td colspan="2" class="submitFormHelp"><strong><fmt:message key="jsp.dspace-admin.wizard-permissions.or"/></strong></td>
				</tr>
				<tr>
					<td colspan="2">&nbsp;</td>
				</tr>
<%
	}
%>

				<tr>
					<td colspan="2">
						<table align="center" width="80%">
          				<tr>
            				<td class="submitFormHelp"><fmt:message key="jsp.dspace-admin.wizard-permissions.click"/><br/>
              					<dspace:selecteperson multiple="true" /> 
            				</td>
							<td>&nbsp;</td>
            				<td class="submitFormHelp"><fmt:message key="jsp.dspace-admin.wizard-permissions.click2"/><br/>
              					<dspace:selectgroup   multiple="true" />
            				</td>
		  				</tr>
        				</table>
					</td>
				</tr>
			</table>
		</center>

<%-- Hidden fields needed for servlet to know which collection and page to deal with --%>
        <input type="hidden" name="collection_id" value="<%= ((Collection) request.getAttribute("collection")).getID() %>" />
        <input type="hidden" name="stage" value="<%= CollectionWizardServlet.PERMISSIONS %>" />
        <input type="hidden" name="permission" value="<%= perm %>" />

        <center>
            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;

                    </td>
                    <td>
                        <%-- <input type="submit" name="submit_next" value="Next &gt;" onclick="javascript:finishEPerson();finishGroups();"> --%>
                        <input type="submit" name="submit_next" value="<fmt:message key="jsp.dspace-admin.general.next.button"/>" onclick="javascript:finishEPerson();finishGroups();"/>
                    </td>
                </tr>
            </table>
        </center>
    </form>

</dspace:layout>
