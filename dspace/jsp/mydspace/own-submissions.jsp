<%--
  - own-submissions.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
  - Show user's previous (accepted) submissions
  -
  - Attributes to pass in:
  -    user     - the e-person who's submissions these are (EPerson)
  -    items    - the submissions themselves (Item[])
  -    handles  - Corresponding Handles (String[])
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.eperson.EPerson" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("user");
    Item[] items = (Item[]) request.getAttribute("items");
%>

<dspace:layout locbar="link" parenttitle="My DSpace" parentlink="/mydspace" title="Your Submissions">

    <H2>Your Submissions</H2>
    
<%
    if (items.length == 0)
    {
%>
    <P>There are no items in the main archive that have been submitted by you.</P>
<%
    }
    else
    {
%>
    <P>Below are listed your previous submissions that have been accepted into
    the archive.</P>
<%
        if (items.length == 1)
        {
%>
    <P>There is <strong>1</strong> item in the main archive that was submitted by you.</P>
<%
        }
        else
        {
%>
    <P>There are <strong><%= items.length %></strong> items in the main archive that were submitted by you.</P>
<%
        }
%>
    <dspace:itemlist items="<%= items %>" />
<%
    }
%>

    <P align=center><A HREF="<%= request.getContextPath() %>/mydspace">Back to My DSpace</A></P>

</dspace:layout>
