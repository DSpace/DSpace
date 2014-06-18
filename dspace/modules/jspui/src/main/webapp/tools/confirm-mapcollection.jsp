<%--
  - Confirm mapping of a Collection into a Community.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Community community = (Community) request.getAttribute("community");
    Collection collection = (Collection) request.getAttribute("collection");

    int count;
    try {
      count = collection.countItems();
    }
    catch (Exception e) {
      count = -1;
    }
%>

<dspace:layout title="Add Collection" navbar="admin" locbar="link" parentlink="/tools" parenttitle="Administer">

    <H1>Confirm Add Collection</H1>
    
    <P>Are you sure want to add the Collection 
       <strong><%= collection.getMetadata("name") %></strong> into the Community 
       <strong><%= community.getMetadata("name")%></strong>? 
    </P>
    
    <p> During this process <%= (count > -1 ? ""+count : "??") %> Items
        in <strong><%= collection.getMetadata("name") %></strong> will
        be reindexed.
    </p>

    <form method="POST">
        <input type="hidden" name="community_id" value="<%= community.getID() %>">
        <input type="hidden" name="collection_id" value="<%= collection.getID() %>">
        <input type="hidden" name="action" value="confirm_map">

        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                        <input type="submit" name="submit" value="Add">
                    </td>
                    <td align="right">
                        <input type="submit" name="submit_cancel" value="Cancel">
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
