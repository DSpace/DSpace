<%--
  - authorize_policy_main.jsp
  --%>


<%--
  - main page for authorization editing
  -
  - Attributes:
  -   none
  -
  - Returns:
  -   submit set to 'collection' to modify policies for collections
  -
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Collection" %>

<%
// leftovers from a cut and paste problem?
//    ResourcePolicy policy = (ResourcePolicy) request.getAttribute("policy" );
//    Group [] groups       = (Group [])       request.getAttribute("groups" );
//    EPerson [] epeople    = (EPerson[])      request.getAttribute("epeople");
//    int collection_id     = request.getIntParameter("collection_id");
%>

<dspace:layout title="Edit Authorization Policies"
               navbar="admin"
               locbar="link"
               parentlink="/admin"
               parenttitle="Administer">

    <h1>Edit Authorization Policies</h1>
    <h3>Choose resource to manage policies for:</h3>
    
    <form method=POST>    

    <center>
        <table width="70%">
            <tr>
                <td align="left">
                    <input type="submit" name="submit_collection" value="Manage Collections' Policies">
                </td>
            </tr>
        </table>
    </center>        

    </form>

</dspace:layout>
