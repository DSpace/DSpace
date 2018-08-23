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
    <form method="get" action="">

        <%
            String surname = request.getParameter("surname") == null ? "" : request.getParameter("surname").trim();
            String initials = request.getParameter("initials") == null ? "" : request.getParameter("initials").trim();
            String action = request.getParameter("action") == null ? "" : request.getParameter("action").trim();

            // clear input strings
            surname = surname.replace("'", "`");
            surname = surname.replace("\"", "`");
            initials = initials.replace("'", "`");
            initials = initials.replace("\"", "`");

            if (action.equals("delete")) {
                try {
                    String query = "DELETE FROM authors WHERE surname_en='" + surname + "' AND initials_en='" + initials + "' ; COMMIT; ";

                    if (DatabaseManager.updateQuery(context, query) >= 1) {
        %><p>Author deleted<br/><%
    } else {
    %><p>Can't delete author!<br/><%
        }
    } catch (SQLException e) {
        try {
            java.io.FileWriter writer = new java.io.FileWriter("D:/projcoder.txt", true);
            writer.write("Authors remove exception at " + (new java.util.Date()) + "\n");
            writer.write("\t" + e + "\n");
            writer.close();
        } catch (Exception exc) {}
    %><p>Can't delete author!<br/> <%

        }
    %>
        <br/><a href = "authors.jsp">Return Back</a></p>
        <%
        } else {
        %>
        <p>
            <strong>Delete author: </strong><%=surname %>, <%=initials %>

            <input type="hidden" name="surname" value="<%=surname %>"/>
            <input type="hidden" name="initials" value="<%=initials %>"/>
            <input type="hidden" name="action" value="delete"><br/><br/>
        <input class="btn btn-danger" type="submit" name="submit" value="Delete author"/>
        <input class="btn btn-info" type="button" name="button" value="Back" onclick="window.location.href='authors.jsp'"/>
        </p>
        <%
            }
        %>
    </form>
</dspace:layout>

<%
    } else {
        org.dspace.app.webui.util.JSPManager.showAuthorizeError(request, response, null);
    }
%>