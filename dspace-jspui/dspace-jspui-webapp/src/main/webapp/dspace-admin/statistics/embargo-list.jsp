<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%@ page import="org.dspace.core.Context" %>

<%@ page import="org.dspace.content.Item" %>

<%@ page import="org.dspace.storage.rdbms.DatabaseManager" %>
<%@ page import="org.dspace.storage.rdbms.TableRow" %>
<%@ page import="org.dspace.storage.rdbms.TableRowIterator" %>

<%
// this space intentionally left blank
%>

<dspace:layout title="Embargo List"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitle="Administer">

    <h1>Embargo List</h1>

    <table align="center" class="miscTable">
      <tr>
        <th class="oddRowOddCol">Handle</th>
        <th class="oddRowEvenCol">Item ID</th>
        <th class="oddRowOddCol">Bitstream ID</th>
        <th class="oddRowEvenCol">Title</th>
        <th class="oddRowOddCol">Advisor</th>
        <th class="oddRowEvenCol">Author</th>
        <th class="oddRowOddCol">Department</th>
        <th class="oddRowEvenCol">Type</th>
        <th class="oddRowOddCol">End Date</th>
      </tr>

<%@ include file="embargo-list-sql.jspf" %>

<%

Context context = UIUtil.obtainContext(request);

TableRowIterator tri = DatabaseManager.query(context, null, sql);

String r = "even";

while (tri.hasNext()) {
  TableRow row = tri.next();

  int itemid = row.getIntColumn("item_id");
  Item item = Item.find(context, itemid);
  String handle = row.getStringColumn("handle"); 
  String url = request.getContextPath() + "/handle/" + handle;
  %>

  <tr>
    <td class="<%= r %>RowOddCol">
      <a href="<%= url %>"><%= handle %></a>
    </td>
    <td class="<%= r %>RowEvenCol">
      <%= itemid %>
    </td>
    <td class="<%= r %>RowOddCol">
      <%= row.getIntColumn("bitstream_id")%>
    </td>
    <td class="<%= r %>RowEvenCol">
      <%= row.getStringColumn("title")%>
    </td>
    <td class="<%= r %>RowOddCol">
      <%= row.getStringColumn("advisor")%>
    </td>
    <td class="<%= r %>RowEvenCol">
      <%= row.getStringColumn("author")%>
    </td>
    <td class="<%= r %>RowOddCol">
      <%= row.getStringColumn("department")%>
    </td>
    <td class="<%= r %>RowEvenCol">
      <%= row.getStringColumn("type")%>
    </td>
    <td class="<%= r %>RowOddCol">
      <%= row.getDateColumn("end_date")%>
    </td>
  </tr>
  <%

  r = (r.equals("even") ? "odd" : "even" );
}
tri.close();

%>

     </table>

</dspace:layout>

