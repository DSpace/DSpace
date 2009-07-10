<%--
  - All Contents.
  -
  - Attributes:
  -    groups      - CommunityGroup[] all groups
  -    communities.map - Map where a key is a group ID (Integer) and
  -                      the value is the arrary communities in that group
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.Date" %>
<%@ page import="java.util.HashMap" %>

<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.content.ItemIterator" %>

<%@ page import="org.dspace.core.Context" %>

<dspace:layout locbar="nolink" title="Home" style="default">

    <table class="standard" width="95%" align="center">
        <tr>
            <td class="standard">
              <H1>All Contents</H1>
            </td>
        </tr>
    </table>
  
</dspace:layout>


