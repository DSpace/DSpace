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
  -   submit_community
  -   submit_collection
  -   submit_advanced
  -
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Collection" %>

<%
// this space intentionally left blank
%>

<dspace:layout title="Administer Authorization Policies"
               navbar="admin"
               locbar="link"
               parentlink="/admin"
               parenttitle="Administer">

    <h1>Administer Authorization Policies</h1>
    <h3>Choose resource to manage policies for:</h3>
    
    <form method=POST>    

    <center>
        <table width="70%">
            <tr>
                <td align="left">
                    <input type="submit" name="submit_community" value="Manage a Community's Policies">
                </td>
            </tr>
            <tr>
                <td align="left">
                    <input type="submit" name="submit_collection" value="Manage Collection's Policies">
                </td>
            </tr>
            <tr>
                <td align="left">
                    <input type="submit" name="submit_advanced" value="Advanced Policy Admin Tool">
                </td>
            </tr>

        </table>
    </center>        

    </form>

</dspace:layout>
