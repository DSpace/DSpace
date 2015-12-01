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


<dspace:layout locbar="nolink" title="Authors list" feedData="NONE">
<table align="center" width="95%">
	<tr>
		<td>
			<p align="center">
<% 
    String link = "<a href=\"authors.jsp?startWith=";
    for (char i = 'A'; i <= 'Z'; i++) {
        %><%=(link + i) + "\">" + i + "</a> " %><%
    }
%>  
			</p>
		</td>
		<td align="right">
			<a href="authors_edit.jsp">New author</a>
		</td>
	</tr>
</table>

<table align="center" width="95%" border="1">
    <tr>
        <th bgcolor="lightsteelblue">Surname EN</th>
        <th bgcolor="lightsteelblue">Initials EN</th>
        <th bgcolor="lightsteelblue">Surname UK</th>
        <th bgcolor="lightsteelblue">Initials UK</th>
        <th bgcolor="lightsteelblue">Surname RU</th>
        <th bgcolor="lightsteelblue">Initials RU</th>
        <th bgcolor="lightsteelblue">Action</th>
    </tr>
  
<%       
    String startWith = request.getParameter("startWith");
    if (startWith == null) startWith = "A";

    // clear sql-injections
    startWith = startWith.replace("'", "`");
    startWith = startWith.replace("\"", "`");

    try {
        TableRowIterator tri = null;

        try {
            tri = DatabaseManager.query(context, "SELECT * " +
                                                 "  FROM authors " +
                                                 "  WHERE surname_en LIKE '" + startWith + "%'; ");
            while (tri.hasNext()) {
                TableRow row = tri.next();

                link = "?surname=" + row.getStringColumn("surname_en") + "&initials=" + row.getStringColumn("initials_en");

                %><tr><td bgcolor="lightblue"><%=row.getStringColumn("surname_en") + "</td><td bgcolor=\"lightblue\">" + row.getStringColumn("initials_en") + "</td>" %><%
                %><td><%=row.getStringColumn("surname_uk") + "</td><td>" + row.getStringColumn("initials_uk") + "</td>" %><%
                %><td><%=row.getStringColumn("surname_ru") + "</td><td>" + row.getStringColumn("initials_ru") %>
                </td><td bgcolor="white" align="center"><a href="authors_edit.jsp<%=link%>">Edit</a><br/><a href="authors_remove.jsp<%=link%>">Delete</a></td></tr>
<%
            }
        } finally {
            if (tri != null)
                tri.close();
        }
    } catch (SQLException e) {
            try {
                java.io.FileWriter writer = new java.io.FileWriter("D:/projcoder.txt", true);
                writer.write("Authors exception at " + (new java.util.Date()) + "\n");
		writer.write("\t" + e + "\n");	
		writer.close();
            } catch (Exception exc) {}
    }

%>
</table>
</dspace:layout>

<%
  } else {
    org.dspace.app.webui.util.JSPManager.showAuthorizeError(request, response, null);
  }
%>
