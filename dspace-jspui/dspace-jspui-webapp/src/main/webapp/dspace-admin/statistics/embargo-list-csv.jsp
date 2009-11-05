<%@ page contentType="text/csv;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%@ page import="org.dspace.core.Context" %>

<%@ page import="org.dspace.content.Item" %>

<%@ page import="org.dspace.storage.rdbms.DatabaseManager" %>
<%@ page import="org.dspace.storage.rdbms.TableRow" %>
<%@ page import="org.dspace.storage.rdbms.TableRowIterator" %>

<%!
public static String escape(String s) {
  if (s != null) {
    return s.replaceAll("\"","\"\"");
  } else {
    return "";
  }
}
%>


<%@ include file="embargo-list-sql.jspf" %>

<%

Context context = UIUtil.obtainContext(request);

TableRowIterator tri = DatabaseManager.query(context, sql);

String r = "even";

out.clear();

while (tri.hasNext()) {
  TableRow row = tri.next();

  int itemid = row.getIntColumn("item_id");
  Item item = Item.find(context, itemid);
  String handle = row.getStringColumn("handle"); 
  String url = request.getContextPath() + "/handle/" + handle;


  out.write("\"" + handle + "\"");
  out.write(",");
  out.write("\"" + itemid + "\"");
  out.write(",");
  out.write("\"" + row.getIntColumn("bitstream_id") + "\"");
  out.write(",");
  out.write("\"" + escape(row.getStringColumn("title")) + "\"");
  out.write(",");
  out.write("\"" + escape(row.getStringColumn("advisor")) + "\"");
  out.write(",");
  out.write("\"" + escape(row.getStringColumn("author")) + "\"");
  out.write(",");
  out.write("\"" + escape(row.getStringColumn("department")) + "\"");
  out.write(",");
  out.write("\"" + escape(row.getStringColumn("type")) + "\"");
  out.write(",");
  out.write("\"" + row.getDateColumn("end_date") + "\"");
  out.write("\n");
}
tri.close();

%>
