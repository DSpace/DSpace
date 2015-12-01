<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.storage.rdbms.DatabaseManager" %>
<%@ page import="org.dspace.storage.rdbms.TableRowIterator" %>
<%@ page import="org.dspace.storage.rdbms.TableRow" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="java.sql.SQLException" %>

<% org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
   EPerson user = (EPerson) request.getAttribute("dspace.current.user");
   String userEmail = "";

   if (user != null)
       userEmail = user.getEmail().toLowerCase();

   Boolean admin = (Boolean) request.getAttribute("is.admin");
   boolean isAdmin = (admin == null ? false : admin.booleanValue());

   if (isAdmin || userEmail.equals("library_ssu@ukr.net") || userEmail.equals("libconsult@rambler.ru")) {
%>

<dspace:layout locbar="nolink" title="Statistics" feedData="NONE">

<%
         int limit = Integer.parseInt(request.getParameter("limit") == null ? "20" : request.getParameter("limit"));

         StringBuilder sb = new StringBuilder("<table align=\"center\" width=\"95%\"><tr><td>" +
                                              "<form action=\"\" method=\"get\"><table><tr><td>");

         sb.append("Limit <select name=\"limit\">");

         sb.append("<option value=\"20\"").append(limit ==  20 ? "\" selected=\"selected\"" : "\"").append("> 20</option>");
         sb.append("<option value=\"50\"").append(limit ==  50 ? "\" selected=\"selected\"" : "\"").append("> 50</option>");
         sb.append("<option value=\"100\"").append(limit == 100 ? "\" selected=\"selected\"" : "\"").append(">100</option>");

         sb.append("</select>");

         sb.append("</td></tr></table><input type=\"submit\" value=\"Query\"/></form></td></tr></table>");
%>
  <%=sb.toString()%>
<table align="center" width="95%" border="1">
    <tr>
        <th class="evenRowEvenCol">EPerson</th>
        <th class="evenRowEvenCol">EMail</th>
        <th class="evenRowEvenCol">Faculty</th>
        <th class="evenRowEvenCol">Chair</th>
    </tr>

<%
    context = UIUtil.obtainContext(request);

    try {
        TableRowIterator tri = null;

        try {
            tri = DatabaseManager.query(context, "SELECT m1.text_value AS firstname, m2.text_value AS lastname, chair_name, faculty_name, email " +
                                                 "  FROM eperson " +
						 "  LEFT JOIN (SELECT chair_id, chair_name, faculty_name " +
                                                 "    FROM chair " +
                                                 "    LEFT JOIN faculty " +
                                                 "      ON chair.faculty_id = faculty.faculty_id) b " +
                                                 "  ON eperson.chair_id = b.chair_id " +
                                                 "LEFT JOIN metadatavalue AS m1 ON m1.resource_id = eperson.eperson_id AND m1.metadata_field_id = 129" +
                                                 "LEFT JOIN metadatavalue AS m2 ON m2.resource_id = eperson.eperson_id AND m2.metadata_field_id = 130" +
                                                 "  ORDER BY eperson_id DESC " +
                                                 "  LIMIT " + limit + " ");
            while (tri.hasNext()) {
                TableRow row = tri.next();

                %><tr>
                    <td class="evenRowOddCol"><%=row.getStringColumn("lastname") + " " + row.getStringColumn("firstname") %></td>
                    <td class="evenRowOddCol"><%=row.getStringColumn("email") %></td>
                    <td class="evenRowOddCol"><%=row.getStringColumn("faculty_name") == null ? "" : row.getStringColumn("faculty_name") %></td>
                    <td class="evenRowOddCol"><%=row.getStringColumn("chair_name") == null ? "" : row.getStringColumn("chair_name") %></td>
                </tr><%
            }
        } finally {
            if (tri != null)
                tri.close();
        }
    } catch (SQLException e) {
        %><%=e.toString()%><%
    }

%>
</table>
</dspace:layout>


<%
  } else {
    org.dspace.app.webui.util.JSPManager.showAuthorizeError(request, response, null);
  }
%>
