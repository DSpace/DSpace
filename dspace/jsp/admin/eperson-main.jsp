<%--
  - eperson-main.jsp
  --%>


<%--
  - main page for eperson admin
  -
  - Attributes:
  -   none
  -
  - Returns:
  -   submit_add    - admin wants to add an eperson
  -   submit_browse - admin wants to browse epeople
  -
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>


<%
%>

<dspace:layout title="EPerson Administrations"
               navbar="admin"
               locbar="link"
               parentlink="/admin"
               parenttitle="Administer">

    <h1>Administer EPeople</h1>
    <h3>Choose an action:</h3>
    
    <form method=POST>    

    <center>
        <table width="70%">
            <tr>
                <td>
                    <input type="submit" name="submit_add" value="Add EPerson">
                </td>
            </tr>
            <tr>
                <td>
                    <input type="submit" name="submit_browse" value="Browse EPeople">
                </td>
            </tr>

        </table>
    </center>        

    </form>

</dspace:layout>
