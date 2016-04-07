<%



    int totalPublications = bi.getTotal();
    int from = bi.getStart();
    int to = bi.getFinish();
    int totalPages = (int) Math.ceil(Double.valueOf(totalPublications) / rpp);
    int currentPage = bi.getOffset() / rpp + 1;


    int leftPage = Math.max(1, currentPage - 2);
    int rightPage = Math.min(totalPages, currentPage + 2);
    if(currentPage == 1 && bi.hasPrevPage()) {
        currentPage = 2;
    }
    if(totalPages == 1 && bi.hasPrevPage()) {
        totalPages = 2;
    }
%>


    <ul class="cd-pagination no-space move-buttons custom-icons">
        <% if(!isSinglePage) { %>
            <li class="button">
                <a href="<%= prev %>"
                    <% if(!bi.hasPrevPage()) { %>
                      class = "disabled"
                    <% } %>
                ><fmt:message key="pagination.prev"/></a>
            </li>
        <% } %>

        <% if(leftPage > 1) {%>
            <li><a href="<%= linkBase %>" <% if(1 == currentPage) { %> class="current" <% } %> >1</a></li>
            <% if(leftPage > 2) {%>
                <li><span>...</span></li>
            <%  }  %>
        <%  }  %>

        <% for(int i = leftPage; i <= rightPage; i++) {
                String link = linkBase + "offset=" + Integer.valueOf(rpp * (i - 1)).toString();

        %>
            <li><a href="<%= link %>" <% if(i == currentPage) { %> class="current" <% } %> > <%= i %></a></li>
        <%  }  %>

        <% if(rightPage < totalPages) {%>
            <% if(rightPage < totalPages - 1) {%>
                <li><span>...</span></li>
            <%  }  %>
             <li><a href="<%= linkBase + "offset=" + Integer.valueOf(rpp * (totalPages - 1)).toString() %>" <% if(totalPages == currentPage) { %> class="current" <% } %> ><%= totalPages %></a></li>
        <%  }  %>

        <% if(!isSinglePage) { %>
            <li class="button">
                <a href="<%= next %>"
                    <% if(!bi.hasNextPage()) { %>
                        class = "disabled"
                     <% } %>
                 ><fmt:message key="pagination.next"/></a>
            </li>
        <% } %>


    </ul>
