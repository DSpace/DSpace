<%@ page contentType="text/html;charset=UTF-8" trimDirectiveWhitespaces="false"%><%@
    page import="org.dspace.core.Context" %><%@ 
    page import="org.dspace.app.webui.util.UIUtil" %><%@ 
    page import="org.dspace.storage.rdbms.DatabaseManager" %><%@ 
    page import="org.dspace.storage.rdbms.TableRowIterator" %><%@ 
    page import="org.dspace.storage.rdbms.TableRow" %><%@ 
    page import="org.dspace.core.ConfigurationManager" %><%@
    page import="java.sql.*" %><%

    try {
        if(request.getCharacterEncoding() == null)
            request.setCharacterEncoding("UTF-8");
    } catch (Exception e) {
    }

    String q = request.getParameter("q") == null ? "" : request.getParameter("q");
    String locale = request.getParameter("locale") == null ? "uk" : request.getParameter("locale");

    // clear q from sql-injections
    q = q.replace("'", "`");
    q = q.replace("\"", "`");

    try {
        Connection c = null;
        try {
            Class.forName(ConfigurationManager.getProperty("db.driver"));
        
            c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
                                            ConfigurationManager.getProperty("db.username"),
                                            ConfigurationManager.getProperty("db.password"));

            Statement s = c.createStatement();

            ResultSet res = s.executeQuery("SELECT * " +
                                           "    FROM authors " +
                                           "    WHERE LOWER(surname_" + locale + ") LIKE '" + q + "%'; ");

            while (res.next()) {
                %><%=res.getString("surname_" + locale) + "|" + res.getString("initials_" + locale) + "|" %><%
                %><%=res.getString("surname_en") + "|" + res.getString("initials_en") + "|" %><%
                %><%=res.getString("surname_ru") + "|" + res.getString("initials_ru") + "|" %><%
                %><%=res.getString("surname_uk") + "|" + res.getString("initials_uk") %>
<%            
            }

            s.close();
        } finally {
            if (c != null) 
                c.close();
        }
    } catch (Exception e) {
            try {
                java.io.FileWriter writer = new java.io.FileWriter("D:/projcoder.txt", true);
                writer.write("Autocomplete exception at " + (new java.util.Date()) + " - Locale: [" + locale + "] - Request: ["+ q +"]\n");
		writer.write("\t" + e + "\n");	
		writer.close();
            } catch (Exception exc) {}

    }
%>