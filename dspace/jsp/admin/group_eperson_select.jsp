<%--
  - group_eperson_select.jsp
  --%>


<%--
  - Display list of epeople, with continue and cancel buttons
  -
  - Attributes:
  -   collections - a Collection [] containing all collections in the system
  - Returns:
  -   submit set to add_eperson_add, user has selected an eperson
  -   submit set to add_eperson_cancel, user has cancelled operation
  -   group_id - set if user has selected one
  -   eperson_id - set if user has selected one

  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group"   %>

<%
    Group group = (Group) request.getAttribute("group");
    EPerson [] epeople = 
        (EPerson []) request.getAttribute("epeople");
%>

<dspace:layout title="Select EPerson"
               navbar="admin"
               locbar="link"
               parentlink="/admin"
               parenttitle="Administer">

    <h1>Select EPerson to Add to Group <%= group.getID() %></h1>

    <form method=POST>

    <table class="miscTable" align="center">
        <tr>
            <td>
                <input type="hidden" name="group_id" value="<%=group.getID()%>">
                
                <select size="5" name="eperson_id">
                        <%  for (int i = 0; i < epeople.length; i++) { %>
                            <option value="<%= epeople[i].getID()%>">
                                <%= epeople[i].getEmail()%>
                            </option>
                        <%  } %>
                </select>
            </td>
        </tr>
    </table>

    <center>
        <table width="70%">
            <tr>
                <td align="left">
                    <input type="submit" name="submit_add_eperson_add" value="Add EPerson">
                </td>
                <td align="right">
                    <input type="submit" name="submit_add_eperson_cancel" value="Cancel">
                </td>
            </tr>
        </table>
    </center>        

    </form>

</dspace:layout>
