<%
    int totalPages = (int)pageTotal;
    int currentPage = (int)pageCurrent;


    int leftPage = Math.max(1, currentPage - 2);
    int rightPage = Math.min(totalPages, currentPage + 2);
%>


    <ul class="cd-pagination no-space move-buttons custom-icons">

        <li class="button">
            <a href="<%= prevURL %>"
                <% if(pageFirst == pageCurrent) { %>
                  class = "disabled"
                <% } %>
            ><fmt:message key="pagination.prev"/></a>
        </li>

        <% if(leftPage > 1) {%>
            <li><a href="<%= firstURL %>" <% if(1 == currentPage) { %> class="current" <% } %> >1</a></li>
            <% if(leftPage > 2) {%>
                <li><span>...</span></li>
            <%  }  %>
        <%  }  %>

        <% for(int i = leftPage; i <= rightPage; i++) {
                String link = baseURL + Integer.valueOf(rpp * (i - 1)).toString();

        %>
            <li><a href="<%= link %>" <% if(i == currentPage) { %> class="current" <% } %> > <%= i %></a></li>
        <%  }  %>

        <% if(rightPage < totalPages) {%>
            <% if(rightPage < totalPages - 1) {%>
                <li><span>...</span></li>
            <%  }  %>
             <li><a href="<%= lastURL %>" <% if(totalPages == currentPage) { %> class="current" <% } %> ><%= totalPages %></a></li>
        <%  }  %>

        <li class="button">
            <a href="<%= nextURL %>"
                <% if(pageTotal <= pageCurrent) { %>
                    class = "disabled"
                 <% } %>
             ><fmt:message key="pagination.next"/></a>
        </li>


    </ul>

<%--


Per page: <%= perPage %> <br/>
Total publications: <%= totalPublications %><br/>
From: <%= from %><br/>
To: <%= to %><br/>
Total pages: <%= totalPages %><br/>
--%>
