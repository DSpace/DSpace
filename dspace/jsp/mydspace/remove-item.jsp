<%-- Remove Item page
  --
	-- Attributes:
	--   submitforminfo  - the information corresponding to the item the user
	--                     wishes to delete (must be personal workspace item)
  --
	-- $Id$
	--%>
<%@ page import="org.dspace.db.other.Item" %>
<%@ page import="org.dspace.servlets.MyDSpaceServlet" %>
<%@ page import="org.dspace.util.servlets.SubmitFormInfo" %>

<%@ taglib uri="/WEB-INF/dspace_tags.tld" prefix="dspace" %>

<%
	SubmitFormInfo sfi =
		(SubmitFormInfo) request.getAttribute( "submitforminfo" );
%>

<dspace:layout title="Remove Item?">
	<H1>Remove Item</H1>
	
	<P>Are you sure you want to remove the following incomplete item?</P>

	<table width=90% align=center border=0 bgcolor="#CCCCCC" cellpadding=0>
  	<tr>
    	<td>
	<%-- HACK:  Width=100% here because otherwise Netscape 4.x renders a big --%>
	<%--        dark grey rectangle on the right (the table is too small)    --%>
      	<table width=100% border=0 bgcolor="#EEEEEE" cellpadding=2>
        	<tr>
          	<td>
							<dspace:displayitem item="<%= sfi.item %>" style="full" />
          	</td>
        	</tr>
      	</table>
    	</td>
  	</tr>
	</table>


	<form action="<%= request.getContextPath() %>/mydspace" method=post>
		<%= sfi.toString() %>
		<input type=hidden name=step value=<%= MyDSpaceServlet.REMOVE_ITEM_PAGE %> />

		<table align=center border=0 width=90%>
			<tr>
				<td align=left>
					<input type=submit name=submit_delete value="Remove the Item" />
				</td>
				<td align=right>
					<input type=submit name=submit_cancel value="Cancel Removal" />
				</td>
			</tr>
		</table>
	</form>
</dspace:layout>
