<%--
  - authorize_policy_edit.jsp
  --%>


<%--
  - policy editor - for new or existing policies
  -
  - Attributes:
  -   policy - a ResourcePolicy to be edited
  -   groups - Group [] of groups to choose from
  -   epeople - EPerson [] of epeople to choose from (unused in first version)
  -   edit_title - title of the page ("Collection 13", etc.
  -   id_name - name of string to put in hidden arg (collection_id, etc.) 
  -   id - ID of the object policy relates to (collection.getID(), etc.)
  - Returns:
  -   save_policy   - user wants to save a policy
  -   cancel_policy - user wants to cancel, and return to policy list
  -   "id_name"     - name/value passed in from id_name/id above
  -   group_id      - set if user selected a group
  -   eperson_id    - set if user selected an eperson
  -   start_date    - not set, unused
  -   end_date      - not set, unused
  -   action_id     - set to whatever user chose
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.content.Collection"       %>
<%@ page import="org.dspace.core.Constants"           %>
<%@ page import="org.dspace.eperson.EPerson"          %>
<%@ page import="org.dspace.eperson.Group"            %>

<%
    ResourcePolicy policy = (ResourcePolicy) request.getAttribute("policy"    );
    Group   [] groups     = (Group []      ) request.getAttribute("groups"    );
    EPerson [] epeople    = (EPerson[]     ) request.getAttribute("epeople"   );
    String edit_title     = (String        ) request.getAttribute("edit_title");
    String id_name        = (String        ) request.getAttribute("id_name"   );
    String id             = (String        ) request.getAttribute("id"        );
%>

<dspace:layout title="Edit Policy"
               navbar="admin"
               locbar="link"
               parentlink="/admin"
               parenttitle="Administer">

    <h1>Edit Policy for <%= edit_title %>:</h1>

    <form method=POST>

    <table class="miscTable" align="center">
        <tr>     
            <td>Group:</td>
            <td>
                <select size="5" name="group_id">
                    <%  for(int i = 0; i < groups.length; i++ ) { %>
                            <option value="<%= groups[i].getID() %>" <%= (groups[i].getID() == policy.getGroupID() ? "selected" : "" ) %> >
                            <%= groups[i].getName()%>
                            </option>
                        <%  } %>
                </select>
            </td>
        </tr>

        <tr><td>Action:</td>
            <td><input type="hidden" name="<%=id_name%>" value="<%=id%>">
                <input type="hidden" name="policy_id" value="<%=policy.getID()%>" >
                <select name="action_id">
                    <%  for( int i = 0; i < Constants.actionText.length; i++ ) { %>
                        <option value="<%= i %>"
                            <%=(policy.getAction() == i ? "selected" : "")%>>
                            <%= Constants.actionText[i]%>
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
                    <input type="submit" name="submit_save_policy" value="Save Policy">
                </td>
                <td align="right">
                    <input type="submit" name="submit_cancel_policy" value="Cancel">
                </td>
            </tr>
        </table>
    </center>        

    </form>

</dspace:layout>
