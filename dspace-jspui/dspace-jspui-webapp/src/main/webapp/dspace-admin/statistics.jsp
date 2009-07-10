<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
// this space intentionally left blank
%>

<dspace:layout title="Resource Statistics"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitle="Administer">

    <h1>Database Statistics</h1>

<p>
  <table>
    <tr>
      <td class="standard">
        <a href="statistics/monthly.jsp">Monthly Statistics</a>
      </td>
    </tr>
    <tr>
      <td class="standard">
        <a href="statistics/embargo-list.jsp">List of all embargoes</a>
        (<a href="statistics/embargo-list-csv.jsp">csv</a>)
      </td>
    </tr>
  </table>
</p>

</dspace:layout>

