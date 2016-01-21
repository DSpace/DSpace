<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display list of E-people, with pagination
  -
  - Attributes:
  -
  -   epeople    - EPerson[] - all epeople to browse
  -   page_size  - size of pages (number of epeople per page)
  -   page_index - current page to display
  -   page_count - number of pages
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="java.util.List" %>
<%@ page import="org.dspace.eperson.service.EPersonService" %>
<%@ page import="org.dspace.eperson.factory.EPersonServiceFactory" %>

<%
    List<EPerson> epeople =
        (List<EPerson>) request.getAttribute("epeople");
    int pageSize  = ((Integer)request.getAttribute("page_size" )).intValue();
    int pageIndex = ((Integer)request.getAttribute("page_index")).intValue();
    int pageCount = ((Integer)request.getAttribute("page_count")).intValue();

    int firstEPerson = pageSize*pageIndex;
    int lastEPerson  = firstEPerson + (pageSize - 1);  // index of last person
                                                       // most common case is full page

    if (lastEPerson >= epeople.size())
    {
        // oops, less than a full page left, trim
        lastEPerson = -1 + firstEPerson + ((epeople.size()-firstEPerson) % pageSize);
    }


    String previousButton = "";
    String nextButton     = "";

    if (pageIndex > 0)
    {
        // not at start, so create 'previous' button
        previousButton = "<input type=\"hidden\" name=\"page_index\" value=\"" + pageIndex + "\"/>" +
                         "<input type=\"hidden\" name=\"page_request\" value=\"previous\"/> "       +
                         "<input type=\"submit\" name=\"submit_browse\" value=\"Previous\"/>";
    }

    if (pageIndex < (pageCount-1) )
    {
        // not showing last either, so create 'next' button
        nextButton = "<input type=\"hidden\" name=\"page_index\" value=\"" + pageIndex + "\"/>" +
                         "<input type=\"hidden\" name=\"page_request\" value=\"next\"/> "       +
                         "<input type=\"submit\" name=\"submit_browse\" value=\"Next\"/>";
    }

%>

<dspace:layout titlekey="jsp.dspace-admin.eperson-browse.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Browse EPeople <%=firstEPerson%>-<%=lastEPerson%> of <%=epeople.length%></h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.eperson-browse.heading">
        <fmt:param><%=firstEPerson%></fmt:param>
        <fmt:param><%=lastEPerson%></fmt:param>
        <fmt:param><%=epeople.size()%></fmt:param>
    </fmt:message></h1>

    <table class="miscTable" align="center" summary="Browse E-people">
        <tr>
            <th id="t1" class="oddRowOddCol"> <strong><a href="<%= request.getContextPath() %>/dspace-admin/edit-epeople?submit_browse=1&sortby=id">ID</a></strong></th>
            <%-- <th class="oddRowEvenCol"><strong><a href="<%= request.getContextPath() %>/dspace-admin/edit-epeople?submit_browse=1&sortby=email">E-mail Address</a></strong></th> --%>
            <th id="t2" class="oddRowEvenCol"><strong><a href="<%= request.getContextPath() %>/dspace-admin/edit-epeople?submit_browse=1&sortby=email"><fmt:message key="jsp.dspace-admin.eperson-browse.email"/></a></strong></th>
            <%-- <th class="oddRowOddCol"> <strong><a href="<%= request.getContextPath() %>/dspace-admin/edit-epeople?submit_browse=1&sortby=lastname">Last Name</a></strong></th> --%>
            <th id="t3" class="oddRowOddCol"> <strong><a href="<%= request.getContextPath() %>/dspace-admin/edit-epeople?submit_browse=1&sortby=lastname"><fmt:message key="jsp.dspace-admin.eperson.general.lastname"/></a></strong></th>
            <%-- <th class="oddRowEvenCol"><strong>First Name</strong></th> --%>
            <th id="t4" class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.eperson.general.lastname"/></strong></th>
            <%-- <th class="oddRowOddCol"> <strong>Can Log In?</strong></th> --%>
            <th id="t5" class="oddRowOddCol"> <strong><fmt:message key="jsp.dspace-admin.eperson-browse.canlogin"/></strong></th>
            <%-- <th class="oddRowEvenCol"><strong>Must Use Cert?</strong></th> --%>
            <th id="t6" class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.eperson-browse.mustusecert"/></strong></th>
            <%-- <th class="oddRowOddCol"> <strong>Self Registered</strong></th> --%>
            <th id="t7" class="oddRowOddCol"> <strong><fmt:message key="jsp.dspace-admin.eperson-browse.self"/></strong></th>
            <%-- <th class="oddRowEvenCol"><strong>Telephone</strong></th> --%>
            <th id="t8" class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.eperson-browse.phone"/></strong></th>
            <th id="t9" class="oddRowOddCol">&nbsp;</th>
            <th id="t10" class="oddRowEvenCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    for (int i = firstEPerson; i <= lastEPerson; i++)
    {
        EPerson e = epeople.get(i);
        String commandString = request.getContextPath() + "/dspace-admin/edit-epeople?submit_edit&amp;eperson_id=" + e.getID();
%>
        <form method="post" action="<%= request.getContextPath() %>/dspace-admin/edit-epeople">
            <tr>
                <td headers="t1" class="<%= row %>RowOddCol"><%= e.getID() %></td>
                <td headers="t2" class="<%= row %>RowEvenCol">
                    <%= (e.getEmail() == null ? "" : e.getEmail()) %>
                </td>
                <td headers="t3" class="<%= row %>RowOddCol">
                    <%= (e.getLastName() == null ? "" : Utils.addEntities(e.getLastName())) %>
                </td>
                <td headers="t4" class="<%= row %>RowEvenCol">
                    <%= (e.getFirstName() == null ? "" : Utils.addEntities(e.getFirstName())) %>
                </td>
                <td headers="t5" class="<%= row %>RowOddCol" align="center">
                    <%= e.canLogIn() ? "yes" : "no" %>
                </td>
                <td headers="t6" class="<%= row %>RowEvenCol" align="center">
                    <%= e.getRequireCertificate() ? "yes" : "no" %>
                </td>
                <td headers="t7" class="<%= row %>RowOddCol">
                    <%= e.getSelfRegistered() ? "yes" : "no" %>
                </td>
                <td headers="t8" class="<%= row %>RowEvenCol">
                    <%= (ePersonService.getMetadata(e, "phone") == null ? "" : Utils.addEntities(ePersonService.getMetadata(e, "phone"))) %>
                </td>
                <td headers="t9" class="<%= row %>RowOddCol">
                    <input type="hidden" name="eperson_id" value="<%= e.getID() %>"/>
<%      if (request.getParameter("sortby") != null) { %>
                    <input type="hidden" name="sortby" value="<%= request.getParameter("sortby") %>"/>
<%      } %>
                    <%-- <input type="submit" name="submit_edit" value="Edit..."> --%>
                    <input type="submit" name="submit_edit" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
                </td>
                <td headers="t10" class="<%= row %>RowEvenCol">
                    <%-- <input type="submit" name="submit_delete" value="Delete..."> --%>
                    <input type="submit" name="submit_delete" value="<fmt:message key="jsp.dspace-admin.general.delete-w-confirm"/>" />
                </td>
            </tr>
        </form>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>

    <form method="post" action="">
    <%=previousButton%>
    </form>

    <form method="post" action="">
    <%=nextButton%>
    </form>


</dspace:layout>
