<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display list of DC schemas
  -
  - Attributes:
  -
  -   formats - the DC formats in the system (MetadataValue[])
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.MetadataSchema" %>


<%
    MetadataSchema[] schemas =
        (MetadataSchema[]) request.getAttribute("schemas");
%>

<dspace:layout titlekey="jsp.dspace-admin.list-metadata-schemas.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

  <table width="95%">
    <tr>
      <td align="left">
        <h1><fmt:message key="jsp.dspace-admin.list-metadata-schemas.title"/></h1>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#dublincore\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>
  
<%
String error = (String)request.getAttribute("error");
if (error!=null) { 
%>
    <p align="center">
    <font color="red"><%=error%></font>
    </p>
<% } %>
  

    <table class="miscTable" align="center" width="500">
        <tr>
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.general.id" /></strong></th>
            <th class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.list-metadata-schemas.namespace"/></strong></th> 
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.dspace-admin.list-metadata-schemas.name"/></strong></th> 
            <th class="oddRowOddCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    for (int i = 0; i < schemas.length; i++)
    {
%>
        <tr>
            <td class="<%= row %>RowOddCol"><%= schemas[i].getSchemaID() %></td>
            <td class="<%= row %>RowEvenCol">
                <a href="<%=request.getContextPath()%>/dspace-admin/metadata-field-registry?dc_schema_id=<%= schemas[i].getSchemaID() %>"><%= schemas[i].getNamespace() %></a>
            </td>
            <td class="<%= row %>RowOddCol">
                <%= schemas[i].getName() %>
            </td>
            <td class="<%= row %>RowOddCol">
		<% if ( schemas[i].getSchemaID() != 1 ) { %>
                <form method="post" action="">
                    <input type="hidden" name="dc_schema_id" value="<%= schemas[i].getSchemaID() %>"/>
                    <input type="button" name="submit_update" value="<fmt:message key="jsp.dspace-admin.general.update"/>" onclick="javascript:document.schema.namespace.value='<%= schemas[i].getNamespace() %>';document.schema.short_name.value='<%= schemas[i].getName() %>';document.schema.dc_schema_id.value='<%= schemas[i].getSchemaID() %>';return null;"/>
                    <input type="submit" name="submit_delete" value="<fmt:message key="jsp.dspace-admin.general.delete-w-confirm"/>"/>
                </form>
		    <% } %>
                </td>
            </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>
        
  <form method="post" name="schema" action="">
  <input type="hidden" name="dc_schema_id" value=""/>
  <table align="center" width="600">
    <tr>
      <td align="left" colspan="2">
         <p>
             <br/><fmt:message key="jsp.dspace-admin.list-metadata-schemas.instruction"/>
             <br/><br/>
         </p>
       </td>
       </tr>
       <tr>
          <td><p><fmt:message key="jsp.dspace-admin.list-metadata-schemas.namespace"/>:</p></td>
          <td><input type="text" name="namespace" value=""/></td>
       </tr>
       <tr>
          <td><p><fmt:message key="jsp.dspace-admin.list-metadata-schemas.name"/>:</p></td>
          <td><input type="text" name="short_name" value=""/></td>
       </tr>
       <tr>
         <td><p><input type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.general.save"/>"/></p></td>
	</tr>
  </table>
  </form>
</dspace:layout>
