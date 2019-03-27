<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Location bar component
  -
  - This component displays the "breadcrumb" style navigation aid at the top
  - of most screens.
  -
  - Uses request attributes set in org.dspace.app.webui.jsptag.Layout, and
  - hence must only be used as part of the execution of that tag.  Plus,
  - dspace.layout.locbar should be verified to be true before this is included.
  -
  -  dspace.layout.parenttitles - List of titles of parent pages
  -  dspace.layout.parentlinks  - List of URLs of parent pages, empty string
  -                               for non-links
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>
  
<%@ page import="java.util.List" %>
<ol class="breadcrumb" > 
<%
    List parentTitles = (List) request.getAttribute("dspace.layout.parenttitles"); //obtiene el nombre de todas las pag anteriores
    List parentLinks = (List) request.getAttribute("dspace.layout.parentlinks"); //obtiene la referencia a todas las pag anteriores 

    for (int i = 0; i < parentTitles.size(); i++) //ciclo que se repite dependiendo del numero de paginas anteriores obtenidas
    {
        String s = (String) parentTitles.get(i); //obtiene una de las pag anteriores
        String u = (String) parentLinks.get(i); //obtiene la referencia a la misma pag anterior que s

        if (u.equals(""))
        {
            if (i == parentTitles.size()) //si llega al ultimo elemento de la cadena que guarda todas las paginas anteriores
            {
%>
<li class="active"><%= s %></li>
<%           
            }
            else
            {
%>
<li><%= s %></li>
<%			}
        }
        else
        {
%>
  <li><a href="<%= request.getContextPath() %><%= u %>"><%= s %></a></li>
<%
        }
    }
%>
<script>
  var title1 = document.title;
  var aux = title1.split(":");
  document.write("<li style='color: #ccc; padding: 0 5px;'>/</li>");  
  document.write("<li2 style='font-style: normal;content: none; color: #410401;' >"+ aux[1] +"</li2>");
</script>
</ol>