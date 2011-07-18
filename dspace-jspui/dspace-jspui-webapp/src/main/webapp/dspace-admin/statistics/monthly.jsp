<%@page import="java.io.FilenameFilter"%>
<%@page import="org.dspace.core.ConfigurationManager"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

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

  String strDspace = ConfigurationManager.getProperty("dspace.dir");
  File dir = new File(strDspace + "/stats/monthly");
  
  if (strMonth == null) {
    out.write("<h1>Monthly Statistics</h1><p><table>");

    // List the available stat files
    File aFiles[] = dir.listFiles(new FilenameFilter() {
    	public boolean accept(File dir, String name) {
    		return (name.endsWith("_stats.txt") 
    				&& !name.endsWith("current_stats.txt"));
    	}    	
    });
    
   	java.util.Arrays.sort(aFiles, new Comparator() {
    	public int compare(Object o1, Object o2) { 
    		 String s1 = ((File)o1).getName();
    		 String s2 = ((File)o2).getName();
    		 return s1.compareTo(s2) * -1; 
    	}
    	 
      	public boolean equals(Object o) { 
      			return this.equals(o); 
      	}
    });
    
    for (File f : aFiles) {
      strMonth = f.getName().substring(0,6);
      out.write("<tr><td class=\"standard\">");
      out.write("<a href=\"?month=" + strMonth + "\">");
	  out.write(strMonth);
      out.write("</a></td></tr>\n");    	
    }

    out.write("</table></p>\n");

  } else {
    String strFile = "file://" + dir.toString() + "/" + strMonth + "_stats.txt";
    %>
      <h1>Monthly Statistics: <%= strMonth %></h1>

      <p><table><tr><td class="standard"><pre>
      <c:import url="<%= strFile %>"/>
      </pre></td></tr></table></p>
    <%
  }
%>


</dspace:layout>

