<%--
  - group_edit.jsp
  -
  - $Id: group-edit.jsp,v 1.3 2004/06/08 19:01:29 rtansley Exp $
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
  --%>


<%--
  - Show contents of a group (name, epeople)
  -
  - Attributes:
  -   group - group to be edited
  -
  - Returns:
  -   cancel - if user wants to cancel
  -   add_eperson - go to group_eperson_select.jsp to choose eperson
  -   change_name - alter name & redisplay
  -   eperson_remove - remove eperson & redisplay
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group"   %>

<%
    Group group = (Group) request.getAttribute("group");
    EPerson [] epeople = (EPerson []) request.getAttribute("members");
    
%>

<dspace:layout title="Edit Group"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitle="Administer"
               nocache="true">

  <table width=95%>
    <tr>
      <td align=left>
    <h1>Edit Group : <%=group.getName()%> (<%=group.getID()%>)</h1>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="/help/collection-admin.html#groupeditor">Help...</dspace:popup>
      </td>
    </tr>
  </table>

  <center>
    <form method=post>

        <P>Name: <input name="group_name" value="<%=group.getName()%>"></P>

        <h3>Current Group Members</h3>

        <input type="hidden" name="group_id" value="<%=group.getID()%>">
        <dspace:selecteperson multiple="true" selected="<%= epeople %>"/> 

        <br>

        <P><input type="submit" name="submit_group_update" value="Update Group" onclick="javascript:finishEPerson();"></P>
    </form>
  </center>
</dspace:layout>
