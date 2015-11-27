<%
    int perPage = PAGESIZE;
    int totalUsers = epeople.length;
    int totalPages = (int) Math.ceil(Double.valueOf(totalUsers) / perPage);
    int currentPage = first / perPage + 1;

    int leftPage = Math.max(1, currentPage - 2);
    int rightPage = Math.min(totalPages, currentPage + 2);
%>


    <ul class="cd-pagination no-space move-buttons custom-icons">
        <li class="button">
            <a href="<%= jumpLink %>0"
                <% if(currentPage == 1) { %>
                  class = "disabled"
                <% } %>
            ><fmt:message key="pagination.prev"/></a>
        </li>


        <% if(leftPage > 1) {%>
            <li><a href="<%= jumpLink %>" <% if(1 == currentPage) { %> class="current" <% } %> >1</a></li>
            <% if(leftPage > 2) {%>
                <li><span>...</span></li>
            <%  }  %>
        <%  }  %>

        <% for(int i = leftPage; i <= rightPage; i++) {
                String link = jumpLink + Integer.valueOf(perPage * (i - 1)).toString();

        %>
            <li><a href="<%= link %>" <% if(i == currentPage) { %> class="current" <% } %> > <%= i %></a></li>
        <%  }  %>

        <% if(rightPage < totalPages) {%>
            <% if(rightPage < totalPages - 1) {%>
                <li><span>...</span></li>
            <%  }  %>
             <li><a href="<%= jumpLink + Integer.valueOf(perPage * (totalPages - 1)).toString() %>" <% if(totalPages == currentPage) { %> class="current" <% } %> ><%= totalPages %></a></li>
        <%  }  %>

        <li class="button">
            <a href="<%= jumpLink %><%= jumpEnd %>"
                <% if(first + perPage > totalUsers) { %>
                    class = "disabled"
                 <% } %>
             ><fmt:message key="pagination.next"/></a>
        </li>
    </ul>
