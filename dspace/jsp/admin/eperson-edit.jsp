<%--
  - eperson-edit.jsp
  --%>


<%--
  - eperson editor - for new or existing epeople
  -
  - Attributes:
  -   eperson - eperson to be edited
  - Returns:
  -   submit_save   - admin wants to save edits
  -   submit_delete - admin wants to delete edits
  -   submit_cancel - admin wants to cancel
  -
  -   eperson_id
  -   email
  -   firstname
  -   lastname
  -   phone
  -   can_log_in          - (boolean)
  -   require_certificate - (boolean)
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("eperson");

    String email     = eperson.getEmail();
    String firstName = eperson.getFirstName();
    String lastName  = eperson.getLastName();
    String phone     = eperson.getMetadata("phone");
%>

<dspace:layout title="Edit EPerson"
               navbar="admin"
               locbar="link"
               parentlink="/admin"
               parenttitle="Administer">

    <h1>Edit EPerson <%= eperson.getEmail() %>:</h1>

    <form method=POST>

    <table class="miscTable" align="center">
        <tr>     
            <td>Email:</td>
            <td>
                <input type="hidden" name="eperson_id" value="<%=eperson.getID()%>">
                <input name="email" size="24" value="<%=email == null ? "" : email%>">
            </td>
        </tr>

        <tr>
            <td>Last Name:</td>
            <td>
                <input name="lastname" size="24" value="<%=lastName == null ? "" : lastName%>">
            </td>
        </tr>

        <tr>
            <td>First Name:</td>
            <td>
                <input name="firstname" size="24" value="<%=firstName == null ? "" : firstName%>">
            </td>
        </tr>

        <tr>
            <td>Phone:</td>
            <td>
                <input name="phone" size="24" value="<%=phone == null ? "" : phone%>">
            </td>
        </tr>

        <tr>
            <td>Can Log In:</td>
            <td>
                <input type="checkbox" name="can_log_in" value="true"<%= eperson.canLogIn() ? " CHECKED" : "" %>>
            </td>
        </tr>

        <tr>
            <td>Require Certificate:</td>
            <td>
                <input type="checkbox" name="require_certificate" value="true"<%= eperson.getRequireCertificate() ? " CHECKED" : "" %>>
            </td>
        </tr>


    </table>
    
    <center>
        <table width="70%">
            <tr>
                <td align="left">
                    <input type="submit" name="submit_save" value="Save Edits">
                </td>
                <td align="right">
                    <input type="submit" name="submit_delete" value="Delete EPerson...">
                </td>
            </tr>
        </table>
    </center>        

    </form>

</dspace:layout>
