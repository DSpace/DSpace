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

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Collection collection = (Collection) request.getAttribute("collection");
	int perm = ((Integer) request.getAttribute("permission")).intValue();
	boolean mitGroup = (request.getAttribute("mitgroup") != null);
%>

<dspace:layout locbar="off" navbar="off" title="Collection Authorization" nocache="true">

<%
	switch (perm)
	{
	case CollectionWizardServlet.PERM_READ:
%>
	<H1>Authorization to Read</H1>
	
	<P>Who has (by default) permission to read new items submitted to this collection?</P>
<%
	break;

	case CollectionWizardServlet.PERM_SUBMIT:
%>
	<H1>Authorization to Submit</H1>
	
	<P>Who has permission to submit new items to this collection?</P>
<%
	break;
	
	case CollectionWizardServlet.PERM_WF1:
%>
	<H1>Workflow Reviewers</H1>
	
	<P>Who are the workflow reviews for this collection?  They be able to accept or reject
incoming submissions.  They will not be able to edit item metadata, however.</P>
<%
	break;

	case CollectionWizardServlet.PERM_WF2:
%>
	<H1>Workflow Approvers</H1>
	
	<P>Who are the workflow approvers for this collection?  They be able to accept or reject
incoming submissions, and edit item metadata.</P>
<%
	break;

	case CollectionWizardServlet.PERM_WF3:
%>
	<H1>Workflow Metadata Editors</H1>
	
	<P>Who are the workflow metadata editors for this collection?  They be able to edit item metadata of
incoming submissions, but will not be able to reject them.</P>
<%
	break;
	}
%>

	<P>You can change this later using the relevant sections of the DSpace admin UI.</P>

    <form action="<%= request.getContextPath() %>/dspace-admin/collection-wizard" method=post>

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
					<td><input type=checkbox name="mitgroup" value="true">&nbsp;<span class="submitFormLabel">All MIT users</span>
					</td>
				</tr>
				<tr>
					<td colspan=3>&nbsp;</td>
				</tr>
				<tr>
					<td colspan=3 class="submitFormHelp"><STRONG>OR</STRONG></td>
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
						Click on the 'Add E-people' button to start adding to the list.</td>
					<td width="40%"></td>
				</tr>
				<tr>
					<td></td>
					<td align=center class="submitFormLabel">
						<select size=10 name="epersonList" multiple>
						</select>
					</td>
				</tr>
				<tr>
					<td></td>
					<td align=center>
						<input type=button value="Add E-people" onclick="javascript:eperson_window();">
						<input type=button value="Remove Selected" onclick="javascript:removeSelected(epersonList);">
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
                    <td width="100%">
                        &nbsp;
                    </td>
                    <td>
                        <input type=submit name="submit_next" value="Next &gt;" onclick="javascript:selectList(epersonList);">
                    </td>
                </tr>
            </table>
        </center>
    </form>
    
</dspace:layout>
