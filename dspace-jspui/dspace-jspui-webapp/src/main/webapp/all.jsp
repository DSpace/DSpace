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

<%
  Context context = null;
  try {
    String strKey = "edu.umd.lib.dspace.cache-all";
    StringBuffer sbComm = null;
    StringBuffer sbColl = null;
    StringBuffer sbItem = null;
    String s = "?";
    
    // Get the cache
    boolean bRebuild = false;
    HashMap cache = (HashMap)application.getAttribute(strKey);
    
    // Determine if it needs rebuilding
    if (cache == null) {
      bRebuild = true;
      cache = new HashMap();
    } else {
      Date oldd = (Date)cache.get("date");
    
      if (oldd == null) {
        bRebuild = true;
      } else {
        Date newd = new Date();
    
        // 24 hours
        if (newd.getTime() - oldd.getTime() > (long)(1000l * 24l * 60l * 60l)) {
          bRebuild = true;
        }
      }
    }
    
    if (bRebuild) {  
      s = "a";
      context = UIUtil.obtainContext(request);
      application.setAttribute(strKey, cache);

      // community      
      sbComm = new StringBuffer();
      Community comms[] = Community.findAll(context);
      for (int i=0; i < comms.length; i++) {
        Community comm = comms[i];
        sbComm.append("<tr><td class=\"standard\">");
        sbComm.append("<a href=\"");
        sbComm.append(request.getContextPath());
        sbComm.append("/handle/");
        sbComm.append(comm.getHandle());
        sbComm.append("\">");
        sbComm.append(comm.getMetadata("name"));
        sbComm.append("</a></td></tr>");
        sbComm.append("\n");
      }
      
      // collection
      sbColl = new StringBuffer();
      Collection colls[] = Collection.findAll(context);
      for (int i=0; i < colls.length; i++) {
        Collection coll = colls[i];
        sbColl.append("<tr><td class=\"standard\">");
        sbColl.append("<a href=\"");
        sbColl.append(request.getContextPath());
        sbColl.append("/handle/");
        sbColl.append(coll.getHandle());
        sbColl.append("\">");
        sbColl.append(coll.getMetadata("name"));
        sbColl.append("</a></td></tr>");
        sbColl.append("\n");
      }
      
      // item
      sbItem = new StringBuffer();
      for (ItemIterator i = Item.findAll(context); i.hasNext(); ) {
        Item item = i.next();
        sbItem.append("<tr><td class=\"standard\">");
        sbItem.append("<a href=\"");
        sbItem.append(request.getContextPath());
        sbItem.append("/handle/");
        sbItem.append(item.getHandle());
        sbItem.append("\">");
        sbItem.append((item.getDC("title", null, Item.ANY))[0].value);
        sbItem.append("</a></td></tr>");
        sbItem.append("\n");
      }
      
      cache.put("community", sbComm);
      cache.put("collection", sbColl);
      cache.put("item", sbItem);
      cache.put("date", new Date());
      
    } else {
      s="b";
      sbComm = (StringBuffer)cache.get("community");
      sbColl = (StringBuffer)cache.get("collection");
      sbItem = (StringBuffer)cache.get("item");
    }    
%>

<dspace:layout locbar="nolink" title="Home" style="default">

    <table class="standard" width="95%" align="center">
        <tr>
            <td class="standard">
              <H1>All Contents</H1>
            </td>
        </tr>
    </table>
  
    <br>

    <table class="standard" width="95%" align="center" border="0">
        <tr>
            <td class="standard">
              <h2>Communities</h2>
       
              <table class="standard" width="95%" align="center" border="0">
                <%= sbComm %>
              </table>
            </td>
        </tr>
    </table>

    <br>

    <table class="standard" width="95%" align="center" border="0">
        <tr>
            <td class="standard">
              <h2>Collections</h2>
       
              <table class="standard" width="95%" align="center" border="0">
                <%= sbColl %>
              </table>
            </td>
        </tr>
    </table>

    <br>

    <table class="standard" width="95%" align="center" border="0">
        <tr>
            <td class="standard">
              <h2>Items</h2>
       
              <table class="standard" width="95%" align="center" border="0">
                <%= sbItem %>
              </table>
            </td>
        </tr>
    </table>

</dspace:layout>

<%
  }
  finally {
    if (context != null) {
      context.abort();
    }
  }
%>


