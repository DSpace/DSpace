<%--
  - authorize_policy_edit.jsp
  --%>


<%--
  - policy editor - for new or existing policies
  -
  - Attributes:
  -   policy - a ResourcePolicy to be edited
  -   collection_id = set if source was a collection
  -   groups - Group [] of groups to choose from
  -   epeople - EPerson [] of epeople to choose from (ignore in first version)
  - Returns:
  -   save_policy   - user wants to save a policy
  -   cancel_policy - user wants to cancel, and return to policy list
  -   collection_id - set if source was a collection
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
    Collection collection = (Collection    ) request.getAttribute("collection");
    String edit_title     = (String        ) request.getAttribute("edit_title");
%>

<dspace:layout title="Edit Policy"
               navbar="admin"
               locbar="link"
               parentlink="/admin"
               parenttitle="Administer">

    <h1>Edit Policy for Collection: <%=collection.getID()%></h1>

    <form method=POST>

    <table class="miscTable" align="center">
        <tr><td>Action:</td>
            <td><input type="hidden" name="collection_id" value="<%=
                ((collection.getID() == -1) ? -1 : collection.getID() )%>">
                <input type="hidden" name="policy_id" value="<%=policy.getID()%>" >
                <select name="action_id">
                    <%  for( int i = 0; i < Constants.actiontext.length; i++ ) { %>
                        <option value="<%= i %>"
                            <%=(policy.getAction() == i ? "selected" : "")%>>
                            <%= Constants.actiontext[i]%>
                        </option>
                    <%  } %>
                </select>
            </td>
        </tr>
        
        <tr>
            <td>Public:</td>
            <td><input type="checkbox" name="is_public" <%=
                (policy.isPublic() ? "checked" : "")%>>       </td>
        </tr>    

        <tr>     
            <td>Group:</td>
            <td>
                <select name="group_id">
                    <%  for(int i = 0; i < groups.length; i++ ) { %>
                            <option value="<%= i %>"
                             <%= (groups[i].getID() == policy.getGroupID() ? "selected" : "" ) %> >
                                <%= groups[i].getName()%>
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
                    <input type="submit" name="submit_save_policy" value="Save">
                </td>
                <td align="right">
                    <input type="submit" name="submit_cancel_policy" value="Cancel">
                </td>
            </tr>
        </table>
    </center>        

    </form>

</dspace:layout>
