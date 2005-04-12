<%--
  - wizard-permissions.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
  --%>

<%--
  - set up a group with particular permissions
  -
  - attributes:
  -    collection - collection we're creating
  -    permission - one of the constants starting PERM_ at the top of
  -                 org.dspace.app.webui.servlet.admin.CollectionWizardServlet
  --%>

<%@ page import="org.dspace.app.webui.servlet.admin.CollectionWizardServlet" %>
<%@ page import="org.dspace.content.Collection" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

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
	<%-- <H1>Authorization to Read</H1> --%>

<H1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading1"/></H1>

	<%-- <P>Who has (by default) permission to read new items submitted to this collection? --%>
	<P><fmt:message key="jsp.dspace-admin.wizard-permissions.text1"/>
<%
	break;

	case CollectionWizardServlet.PERM_SUBMIT:
%>
	<%-- <H1>Authorization to Submit</H1> --%>
	<H1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading2"/></H1>

	<%-- <P>Who has permission to submit new items to this collection? --%>
	<P><fmt:message key="jsp.dspace-admin.wizard-permissions.text2"/>
<%
	break;

	case CollectionWizardServlet.PERM_WF1:
%>
	<%-- <H1>Submission Workflow Accept/Reject Step</H1> --%>
	<H1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading3"/></H1>

	<%-- <P>Who is responsible for performing the <strong>accept/reject</strong> step?
	They will be able to accept or reject incoming submissions.  They will not be
	able to edit the submission's metadata, however.  Only one of the group need perform the step
	for each submission. --%>
	<P><fmt:message key="jsp.dspace-admin.wizard-permissions.text3"/>
<%
	break;

	case CollectionWizardServlet.PERM_WF2:
%>
	<%-- <H1>Submission Workflow Accept/Reject/Edit Metadata Step</H1> --%>
	<H1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading4"/></H1>

	<%-- <P>Who is responsible for performing the <strong>accept/reject/edit metadata</strong> step?
		They will be able to edit the metadata of incoming submissions, and then accept
		or reject them.  Only one of the group need perform the step for each submission. --%>
	<P><fmt:message key="jsp.dspace-admin.wizard-permissions.text4"/>
<%
	break;

	case CollectionWizardServlet.PERM_WF3:
%>
	<%-- <H1>Submission Workflow Edit Metadata Step</H1> --%>
	<H1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading5"/></H1>

	<%-- <P>Who is responsible for performing the <strong>edit metadata</strong> step?
	They will be able to edit the metadata of incoming submissions, but will not
	be able to reject them. --%>
	<P><fmt:message key="jsp.dspace-admin.wizard-permissions.text5"/>
<%
	break;

	case CollectionWizardServlet.PERM_ADMIN:
%>
	<%-- <H1>Delegated Collection Administrators</H1> --%>
	<H1><fmt:message key="jsp.dspace-admin.wizard-permissions.heading6"/></H1>

	<%-- <P>Who are the collection administrators for this collection?  They will be able to decide who can submit items
to the collection, withdraw items, edit item metadata (after submission), and add (map) existing items from
other collections to this collection (subject to authorization from that collection). --%>
	<P><fmt:message key="jsp.dspace-admin.wizard-permissions.text6"/>
<%
	break;
	}
%>
<dspace:popup page="/help/site-admin.html#wizard_permissions"><fmt:message key="jsp.morehelp"/></dspace:popup></P>

	<%-- <P>You can change this later using the relevant sections of the DSpace admin UI.</P> --%>
	<P><fmt:message key="jsp.dspace-admin.wizard-permissions.change"/></P>

    <form action="<%= request.getContextPath() %>/tools/collection-wizard" method=post>

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
					<%-- <td><input type=checkbox name="mitgroup" value="true">&nbsp;<span class="submitFormLabel">All MIT users</span> --%>
					<td><input type=checkbox name="mitgroup" value="true">&nbsp;<span class="submitFormLabel"><fmt:message key="jsp.dspace-admin.wizard-permissions.mit"/></span>
					</td>
				</tr>
				<tr>
					<td colspan=3>&nbsp;</td>
				</tr>
				<tr>
					<%-- <td colspan=3 class="submitFormHelp"><STRONG>OR</STRONG></td> --%>
					<td colspan=3 class="submitFormHelp"><STRONG><fmt:message key="jsp.dspace-admin.wizard-permissions.or"/></STRONG></td>
				</tr>
				<tr>
					<td colspan=3>&nbsp;</td>
				</tr>
<%
	}
%>

<%-- width=40% centres table nicely --%>
				<tr>
					<td width="40%"></td>
					<td class="submitFormHelp">
						<%-- Click on the 'Select E-people' button to choose e-people to add to the list.</td> --%>
						<fmt:message key="jsp.dspace-admin.wizard-permissions.click"/></td>
					<td width="40%"></td>
				</tr>
				<tr>
					<td></td>
					<td align=center>
						<dspace:selecteperson multiple="yes" />
					</td>
				</tr>
			</table>
		</center>

<%-- Hidden fields needed for servlet to know which collection and page to deal with --%>
        <input type=hidden name="collection_id" value=<%= ((Collection) request.getAttribute("collection")).getID() %>>
        <input type=hidden name="stage" value=<%= CollectionWizardServlet.PERMISSIONS %>>
        <input type=hidden name="permission" value=<%= perm %>>

        <center>
            <table border=0 width="80%">
                <tr>
                    <td width="100%">&nbsp;

                    </td>
                    <td>
                        <%-- <input type=submit name="submit_next" value="Next &gt;" onclick="javascript:finishEPerson();"> --%>
                        <input type=submit name="submit_next" value="<fmt:message key="jsp.dspace-admin.wizard-permissions.next"/>" onclick="javascript:finishEPerson();">
                    </td>
                </tr>
            </table>
        </center>
    </form>

</dspace:layout>
