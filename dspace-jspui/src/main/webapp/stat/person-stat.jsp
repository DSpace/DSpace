<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.storage.rdbms.DatabaseManager" %>
<%@ page import="org.dspace.storage.rdbms.TableRow" %>
<%@ page import="org.dspace.storage.rdbms.TableRowIterator" %>
<%@ page import="ua.edu.sumdu.essuir.entity.AuthorData" %>
<%@ page import="ua.edu.sumdu.essuir.utils.EssuirUtils" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.List" %>

<%
    org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
    final String LIMIT = "20";
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");
    String userEmail = "";

    if (user != null)
        userEmail = user.getEmail().toLowerCase();

    Boolean admin = (Boolean) request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());

    if (isAdmin || userEmail.equals("library_ssu@ukr.net") || userEmail.equals("libconsult@rambler.ru")) {
        int limit = Integer.parseInt(request.getParameter("limit") == null ? LIMIT : request.getParameter("limit"));
%>


<dspace:layout locbar="nolink" title="Statistics" feedData="NONE">
    <form class="form-inline" action="" method="get">
        <div class="form-group">
            <label class="sr-only" for="limit">Limit: </label>

            <div class="input-group">
                <select class="form-control" id="limit" name="limit">
                    <%
                        for (int i = 10; i <= 100; i += 10) {
                            out.println("<option " + ((limit == i) ? "selected" : "") + ">" + i + "</option>");
                        }
                    %>
                </select>
            </div>
        </div>
        <button type="submit" class="btn btn-primary"><fmt:message key="jsp.search.yearslider.button" /></button>
    </form>

    <table class="table">
        <thead>
        <tr>
            <th class="evenRowEvenCol"><fmt:message key="jsp.admin.person-stat.name" /></th>
            <th class="evenRowEvenCol"><fmt:message key="jsp.admin.person-stat.email" /></th>
            <th class="evenRowEvenCol"><fmt:message key="jsp.admin.person-stat.faculty" /></th>
            <th class="evenRowEvenCol"><fmt:message key="jsp.admin.person-stat.chair" /></th>
        </tr>
        </thead>
        <tbody>
        <%
            List<AuthorData> authorData = EssuirUtils.getLatestRegisteredAuthors(limit);
            for (AuthorData author : authorData) {
        %>
        <tr>
            <td class="evenRowOddCol"><%=author.getLastname() + " " + author.getFirstname() %></td>
            <td class="evenRowOddCol"><%=author.getEmail() %></td>
            <td class="evenRowOddCol"><%=author.getFaculty() == null ? "" : author.getFaculty() %></td>
            <td class="evenRowOddCol"><%=author.getChair() == null ? "" : author.getChair() %></td>
        </tr>
        <%
            }
        %>
        </tbody>
    </table>
</dspace:layout>

<%
    } else {
        org.dspace.app.webui.util.JSPManager.showAuthorizeError(request, response, null);
    }
%>
