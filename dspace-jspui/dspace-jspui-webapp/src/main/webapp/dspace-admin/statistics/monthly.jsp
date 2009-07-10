<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.io.File" %>

<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.TreeSet" %>
<%@ page import="java.util.Iterator" %>

<%
// this space intentionally left blank
%>

<dspace:layout title="Monthly Statistics"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitle="Administer">

<%
  String strMonth = request.getParameter("month");

  if (strMonth == null) {
    out.write("<h1>Monthly Statistics</h1><p><table>");

    // List the avaiable stat files
    TreeSet sFiles = new TreeSet(new Comparator() {
    	 public int compare(Object o1, Object o2) { return ((String)o1).compareTo((String)o2) * -1; }
      public boolean equals(Object o) { return this.equals(o); }
    });
    
    sFiles.addAll(application.getResourcePaths("/stats"));
    
    // Filter out non monthly stats
    for (Iterator iFiles = sFiles.iterator(); iFiles.hasNext(); ) {
      String strFile = (String)iFiles.next();
    
    	if (strFile.endsWith("_stats.txt") && !strFile.endsWith("current_stats.txt")) {
        strMonth = strFile.substring(7,13);
        out.write("<tr><td class=\"standard\">");
        out.write("<a href=\"?month=" + strMonth + "\">");
	     out.write(strMonth);
        out.write("</a></td></tr>\n");
      }
    }

    out.write("</table></p>\n");

  } else {
    String strFile = "/stats/" + strMonth + "_stats.txt";
    %>
      <h1>Monthly Statistics: <%= strMonth %></h1>
      
      <p><table><tr><td class="standard"><pre>
<dspace:include page="<%= strFile%>" />
      </pre></td></tr></table></p>
    <%
  }
%>


</dspace:layout>

