<%--
  - collection_select.jsp
  --%>


<%--
  - Display list of collections, with continue and cancel buttons
  -  post method invoked with collection_select or collection_select_cancel
  -     (collection_id contains ID of selected collection)
  -
  - Attributes:
  -   collections - a Collection [] containing all collections in the system
  - Returns:
  -   submit set to collection_select, user has selected a collection
  -   submit set to collection_select_cancel, return user to main page
  -   collection_id - set if user has selected one

  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Collection" %>

<%
    Collection [] collections =
        (Collection[]) request.getAttribute("collections");
%>

<dspace:layout title="Select Collection"
               navbar="admin"
               locbar="link"
               parentlink="/admin"
               parenttitle="Administer">

    <h1>System Workflows</h1>

    <form method=POST>

    <table class="miscTable" align="center">
        <tr>
            <td>
                    <select name="collection_id">
                        <%  for (int i = 0; i < collections.length; i++) { %>
                            <option value="<%= collections[i].getID()%>">
                                <%= collections[i].getMetadata("name")%>
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
                    <input type="submit" name="submit_collection_select" value="Edit Policies">
                </td>
                <td align="right">
                    <input type="submit" name="submit_collection_select_cancel" value="Cancel">
                </td>
            </tr>
        </table>
    </center>        

    </form>

</dspace:layout>
