<%--
  - Display list of EtdUnits, with 'edit' and 'delete' buttons next to them
  -
  - Attributes:
  -
  -   etdunits - EtdUnit [] of etdunits to work on
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
    
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.EtdUnit" %>

<%
    EtdUnit[] etdunits =
        (EtdUnit[]) request.getAttribute("etdunits");
%>

<dspace:layout titlekey="jsp.tools.etdunit-list.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

  <table width="95%">
    <tr>
      <td align="left">
    <%--  <h1>EtdUnit Editor</h1> --%>
    <h1><fmt:message key="jsp.tools.etdunit-list.title"/></h1>
      </td>
      <td align="right" class="standard">
        <%-- <dspace:popup page="/help/site-admin.html#etdunits">Help...</dspace:popup> --%>
		<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#etdunits\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

	<p><fmt:message key="jsp.tools.etdunit-list.note1"/></p>
	<p><fmt:message key="jsp.tools.etdunit-list.note2"/></p>
   
    <form method="post" action="">
        <p align="center">
	    <input type="submit" name="submit_add" value="<fmt:message key="jsp.tools.etdunit-list.create.button"/>" />
        </p>
    </form>

    <table class="miscTable" align="center" summary="EtdUnit data display table">
        <tr>
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.tools.etdunit-list.id" /></strong></th>
			<th class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.etdunit-list.name"/></strong></th>
            <th class="oddRowOddCol">&nbsp;</th>
            <th class="oddRowEvenCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    for (int i = 0; i < etdunits.length; i++)
    {
%>
            <tr>
                <td class="<%= row %>RowOddCol"><%= etdunits[i].getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <%= etdunits[i].getName() %>
                </td>
                <td class="<%= row %>RowOddCol">
<%
	// no edit button for etdunit anonymous
	if (etdunits[i].getID() > 0 )
	{
%>                  
                    <form method="post" action="">
                        <input type="hidden" name="etdunit_id" value="<%= etdunits[i].getID() %>"/>
  		        <input type="submit" name="submit_edit" value="<fmt:message key="jsp.tools.general.edit"/>" />
                   </form>
<%
	}
%>                   
                </td>
                <td class="<%= row %>RowEvenCol">
<%
	// no delete button for etdunit Anonymous 0 and Administrator 1 to avoid accidental deletion
	if (etdunits[i].getID() > 1 )
	{
%>   
                    <form method="post" action="">
                        <input type="hidden" name="etdunit_id" value="<%= etdunits[i].getID() %>"/>
	                <input type="submit" name="submit_etdunit_delete" value="<fmt:message key="jsp.tools.general.delete"/>" />
<%
	}
%>	                
                    </form>
                </td>
            </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>
</dspace:layout>
