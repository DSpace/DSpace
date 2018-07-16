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
        <%!
            public String clearInptuString(String input) {
                return input.replace("'", "`").replace("\"", "`").replaceAll("<(.*?)*>", "");
            }
        %>
        <%
            String surname = request.getParameter("surname") == null ? "" : clearInptuString(request.getParameter("surname").trim());
            String initials = request.getParameter("initials") == null ? "" : clearInptuString(request.getParameter("initials").trim());
            String action = request.getParameter("action") == null ? "" : clearInptuString(request.getParameter("action").trim());
            String submit = request.getParameter("submit") == null ? "" : clearInptuString(request.getParameter("submit").trim());
            String surname_en = request.getParameter("surname_en") == null ? "" : clearInptuString(request.getParameter("surname_en").trim());
            String initials_en = request.getParameter("initials_en") == null ? "" : clearInptuString(request.getParameter("initials_en").trim());
            String surname_uk = request.getParameter("surname_uk") == null ? "" : clearInptuString(request.getParameter("surname_uk").trim());
            String initials_uk = request.getParameter("initials_uk") == null ? "" : clearInptuString(request.getParameter("initials_uk").trim());
            String surname_ru = request.getParameter("surname_ru") == null ? "" : clearInptuString(request.getParameter("surname_ru").trim());
            String initials_ru = request.getParameter("initials_ru") == null ? "" : clearInptuString(request.getParameter("initials_ru").trim());
            String old_surname_en = request.getParameter("old_surname_en") == null ? "" : clearInptuString(request.getParameter("old_surname_en").trim());
            String old_initials_en = request.getParameter("old_initials_en") == null ? "" : clearInptuString(request.getParameter("old_initials_en").trim());
            String old_surname_uk = request.getParameter("old_surname_uk") == null ? "" : clearInptuString(request.getParameter("old_surname_uk").trim());
            String old_initials_uk = request.getParameter("old_initials_uk") == null ? "" : clearInptuString(request.getParameter("old_initials_uk").trim());
            String old_surname_ru = request.getParameter("old_surname_ru") == null ? "" : clearInptuString(request.getParameter("old_surname_ru").trim());
            String old_initials_ru = request.getParameter("old_initials_ru") == null ? "" : clearInptuString(request.getParameter("old_initials_ru").trim());
            String orcid = request.getParameter("orcid") == null ? "" : clearInptuString(request.getParameter("orcid").trim());
            String old_orcid = request.getParameter("old_orcid") == null ? "" : clearInptuString(request.getParameter("old_orcid").trim());

            orcid = orcid.replaceAll("https://", "").replaceAll("http://", "").replaceAll("orcid.org/", "");

            if (action.equals("update")) {

                try {
                    String query = "UPDATE authors SET surname_en='" + surname_en + "', initials_en='" + initials_en +
                            "', surname_ru='" + surname_ru + "', initials_ru='" + initials_ru +
                            "', surname_uk='" + surname_uk + "', initials_uk='" + initials_uk + "', " +
                            "orcid = '" + orcid + "'"+
                            " WHERE surname_en = '" + old_surname_en + "' AND initials_en='" + old_initials_en + "'; ";

                    if (submit.equals("Update and fix")) {
                        query += "UPDATE metadatavalue SET text_value = '" + surname_en + ", " + initials_en +
                                "' WHERE metadata_field_id = 3 AND text_value = '" + old_surname_en + ", " + old_initials_en + "'; " +
                                "UPDATE metadatavalue SET text_value = '" + surname_uk + ", " + initials_uk +
                                "' WHERE metadata_field_id = 3 AND text_value = '" + old_surname_uk + ", " + old_initials_uk + "'; " +
                                "UPDATE metadatavalue SET text_value = '" + surname_ru + ", " + initials_ru +
                                "' WHERE metadata_field_id = 3 AND text_value = '" + old_surname_ru + ", " + old_initials_ru + "'; ";
                    }

                    query += "COMMIT;";

                    if (DatabaseManager.updateQuery(context, query) == 1) {
        %><p>Author updated<br/><%
    } else {
    %><p>Can't update author!<br/><%
        }

    } catch (SQLException e) {
        try {
            java.io.FileWriter writer = new java.io.FileWriter("D:/projcoder.txt", true);
            writer.write("Authors edit exception at " + (new java.util.Date()) + "\n");
            writer.write("\t" + e + "\n");
            writer.close();
        } catch (Exception exc) {}
    %><p>Can't update author!<br/> <%
        }
    %>
        <br/><input type="button" value="Return back" onClick="history.go(-2)"></p>
        <%
        } else if (action.equals("new")) {
            try {
                String query = "INSERT INTO authors VALUES ('" + surname_en + "', '" + initials_en + "', '" +
                        surname_ru + "', '" + initials_ru + "', '" +
                        surname_uk + "', '" + initials_uk + "', '" + orcid + "'); COMMIT; ";

                if (DatabaseManager.updateQuery(context, query) == 1) {
        %><p>Author added<br/><%
    } else {
    %><p>Can't add author!<br/><%
        }
    } catch (SQLException e) {
        try {
            java.io.FileWriter writer = new java.io.FileWriter("D:/projcoder.txt", true);
            writer.write("Authors add exception at " + (new java.util.Date()) + "\n");
            writer.write("\t" + e + "\n");
            writer.close();
        } catch (Exception exc) {}
    %><p>Can't add author!<br/><%
        }
    %>
        <br/><input type="button" value="Return back" onClick="history.go(-2)"></p>
        <%
        } else {
            boolean newAuthor = false;

            if (surname.equals("") || initials.equals("")) newAuthor = true;



            if (newAuthor) { %>
        <p>
            <strong>Add new author</strong>

            <input type="hidden" name="action" value="new">
        </p>
        <%
        } else {
            try {
                TableRowIterator tri = null;

                try {
                    tri = DatabaseManager.query(context, "SELECT * " +
                            "  FROM authors " +
                            "  WHERE surname_en = '" + surname + "' AND initials_en = '" + initials + "' " +
                            "  LIMIT 1; ");
                    TableRow row = tri.next();

                    surname_en = row.getStringColumn("surname_en");
                    initials_en = row.getStringColumn("initials_en");
                    surname_uk = row.getStringColumn("surname_uk");
                    initials_uk = row.getStringColumn("initials_uk");
                    surname_ru = row.getStringColumn("surname_ru");
                    initials_ru = row.getStringColumn("initials_ru");
                    orcid = row.getStringColumn("orcid");
                } finally {
                    if (tri != null)
                        tri.close();
                }
            } catch (SQLException e) {
                try {
                    java.io.FileWriter writer = new java.io.FileWriter("D:/projcoder.txt", true);
                    writer.write("Authors select exception at " + (new java.util.Date()) + "\n");
                    writer.write("\t" + e + "\n");
                    writer.close();
                } catch (Exception exc) {}
            }
        %>
        <p>
            <strong>Edit author: </strong><%=surname %>, <%=initials %>

            <input type="hidden" name="old_surname_en" value="<%=surname %>"/>
            <input type="hidden" name="old_initials_en" value="<%=initials %>"/>
            <input type="hidden" name="old_surname_uk" value="<%=surname_uk %>"/>
            <input type="hidden" name="old_initials_uk" value="<%=initials_uk %>"/>
            <input type="hidden" name="old_surname_ru" value="<%=surname_ru %>"/>
            <input type="hidden" name="old_initials_ru" value="<%=initials_ru %>"/>
            <input type="hidden" name="old_orcid" value="<%=orcid %>"/>
            <input type="hidden" name="action" value="update">
        </p>
        <%
            }
        %><table align="center" width="95%">
        <tr>
            <td>Surname EN</td>
            <td><input type="text" name="surname_en" value="<%=surname_en %>"/></td>
            <td>Initials EN</td>
            <td><input type="text" name="initials_en" size="35" value="<%=initials_en %>"/></td>
        </tr>
        <tr>
            <td>Surname UK</td>
            <td><input type="text" name="surname_uk" value="<%=surname_uk %>"/></td>
            <td>Initials UK</td>
            <td><input type="text" name="initials_uk" size="35" value="<%=initials_uk %>"/></td>
        </tr>
        <tr>
            <td>Surname RU</td>
            <td><input type="text" name="surname_ru" value="<%=surname_ru %>"/></td>
            <td>Initials RU</td>
            <td><input type="text" name="initials_ru" size="35" value="<%=initials_ru %>"/></td>
        </tr>
        <tr>
            <td>ORCID</td>
            <td colspan ="3"><input type="text" name="orcid" value="<%= orcid %>" size="100"/></td>
        </tr>
    </table>

        <p>
            <input type="submit" name="submit" value="<%= newAuthor ? "Add author" : "Update author" %>"/>
            <%
                if (!newAuthor) {
            %>
            <input type="submit" name="submit" value="Update and fix"/>
            <%
                }
            %>
            <input type="button" value="Return back" onClick="history.go(-1)">
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