<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.Enumeration" %>
<%@ page import="org.apache.commons.pool.impl.GenericObjectPool" %>
<%@ page import="org.apache.commons.dbcp.PoolingDriver" %>

<%
// this space intentionally left blank
%>

<dspace:layout title="Resource Statistics"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitle="Administer">

    <h1>Resource Statistics</h1>

<% 
  PoolingDriver driver = new PoolingDriver();
  GenericObjectPool pool = (GenericObjectPool)driver.getPool("dspacepool");
%>

<table>
  <tr>
    <th width="10%"></th>
    <th></th>
    <th></th>
  </tr>

  <tr>
    <td colspan="3">JVM Memory</td>
  </tr>

  <% Runtime rt = Runtime.getRuntime(); %>

  <tr>
    <td></td>
    <td>freeMemory</td>
    <td><%= (rt.freeMemory() / 1024 / 1024)%> Mb</td>
  </td>
  <tr>
    <td></td>
    <td>totalMemory</td>
    <td><%= (rt.totalMemory() / 1024 / 1024)%> Mb</td>
  </td>
  <tr>
    <td></td>
    <td>maxMemory</td>
    <td><%= (rt.maxMemory() / 1024 / 1024)%> Mb</td>
  </td>
  <tr>
    <td colspan="3">JDBC Pooled Connections</td>
  </tr>
  <tr>
    <td></td>
    <td>num active</td>
    <td><%= pool.getNumActive() %></td>
  </td>
  <tr>
    <td></td>
    <td>num idle</td>
    <td><%= pool.getNumIdle() %></td>
  </tr>
  <tr>
    <td></td>
    <td>max active</td>
    <td><%= pool.getMaxActive() %></td>
  </tr>
  <tr>
    <td></td>
    <td>max idle</td>
    <td><%= pool.getMaxIdle() %></td>
  </tr>
  <tr>
    <td></td>
    <td>max wait</td>
    <td><%= pool.getMaxWait() %> ms</td>
  </tr>

  <tr>
    <td colspan="3">HttpSession Attributes</td>
  </tr>
  <% 
    for (Enumeration e = request.getSession().getAttributeNames(); e.hasMoreElements(); ) {
      String strName = (String)e.nextElement();
  %>
  <tr>
    <td></td>
    <td><%= strName %></td>
    <td><%= request.getSession().getAttribute(strName)%></td>
  </tr>
  <%
    }
  %>

  <tr>
    <td colspan="3">HttpServletRequest Attributes</td>
  </tr>
  <% 
    for (Enumeration e = request.getAttributeNames(); e.hasMoreElements(); ) {
      String strName = (String)e.nextElement();
  %>
  <tr>
    <td></td>
    <td><%= strName %></td>
    <td><%= request.getAttribute(strName)%></td>
  </tr>
  <%
    }
  %>

  <tr>
    <td colspan="3">HttpServletRequest Headers</td>
  </tr>
  <% 
    for (Enumeration e = request.getHeaderNames(); e.hasMoreElements(); ) {
      String strName = (String)e.nextElement();

      for (Enumeration v = request.getHeaders(strName); v.hasMoreElements(); ) {
  %>
  <tr>
    <td></td>
    <td><%= strName %></td>
    <td><%= v.nextElement() %></td>
  </tr>
  <%
     }
    }
  %>

</table>
</dspace:layout>

