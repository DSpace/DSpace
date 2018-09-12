<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.storage.rdbms.DatabaseManager" %>
<%@ page import="org.dspace.storage.rdbms.TableRowIterator" %>
<%@ page import="org.dspace.storage.rdbms.TableRow" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="java.util.stream.Stream" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.function.BiFunction" %>
<%@ page import="java.util.function.Function" %>

<% org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");
    String userEmail = "";

    if (user != null)
        userEmail = user.getEmail().toLowerCase();

    Boolean admin = (Boolean) request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());

    if (isAdmin || userEmail.equals("library_ssu@ukr.net") || userEmail.equals("libconsult@rambler.ru")) {
%>


<dspace:layout locbar="nolink" title="Authors list" feedData="NONE">
    <form method="get" action="">
        <%!
            public String clearInputString(String input) {
                if(input == null)
                    return "";
                return input.replace("'", "`").replace("\"", "`").replaceAll("<(.*?)*>", "").trim();
            }
        %>
        <%
            List<String> parameterNames = Arrays.asList("action", "submit");
            List<String> generalValueNames = Arrays.asList("surname", "initials");
            List<String> newValueNames = Arrays.asList("surname_en", "initials_en", "surname_uk", "initials_uk", "surname_ru", "initials_ru", "orcid");
            List<String> oldValueNames = Arrays.asList("old_surname_en", "old_initials_en", "old_surname_uk", "old_initials_uk", "old_surname_ru", "old_initials_ru", "old_orcid");

            Map<String, String> requestParameters = Stream.of(parameterNames, newValueNames, oldValueNames, generalValueNames)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(item -> item, item -> clearInputString(request.getParameter(item))));

            requestParameters.get("orcid").replaceAll("https://", "").replaceAll("http://", "").replaceAll("orcid.org/", "");

            if (requestParameters.get("action").equals("update")) {

                try {
                    String updateFields = newValueNames.stream()
                            .map(item -> String.format("%s = '%s'", item, requestParameters.get(item)))
                            .collect(Collectors.joining(","));
                    String query = "UPDATE authors SET " + updateFields +
                            " WHERE surname_en = '" + requestParameters.get("old_surname_en") + "' AND initials_en='" + requestParameters.get("old_initials_en") + "'; ";


                    Function<String, String> metadataUpdateQuery = (locale) ->
                            String.format("UPDATE metadatavalue SET text_value = '%s, %s' WHERE metadata_field_id = 3 AND text_value = '%s, %s';",
                                    requestParameters.get("surname_" + locale), requestParameters.get("initials_" + locale),
                                    requestParameters.get("old_surname_" + locale), requestParameters.get("old_initials_" + locale));

                    query += Stream.of("en", "uk", "ru")
                            .map(metadataUpdateQuery)
                            .collect(Collectors.joining(""));

                    query += "COMMIT;";

                    if (DatabaseManager.updateQuery(context, query) == 1) {
        %><p>Author updated<br/><%
    } else {
    %><p>Can't update author!<br/><%
        }

    } catch (SQLException e) {
            %><p>Can't update author!<br/> <%
        }
    %>
        <br/><a href = "authors.jsp">Return Back</a></p>
        <%
        } else if (requestParameters.get("action").equals("new")) {
            try {
                String query = String.format("INSERT INTO authors VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s'); COMMIT;",
                        requestParameters.get("surname_en"),requestParameters.get("initials_en"),
                        requestParameters.get("surname_ru"),requestParameters.get("initials_ru"),
                        requestParameters.get("surname_uk"),requestParameters.get("initials_uk"),requestParameters.get("orcid"));

                if (DatabaseManager.updateQuery(context, query) == 1) {
        %><p>Author added<br/><%
    } else {
    %><p>Can't add author!<br/><%
        }
    } catch (SQLException e) {

    %><p>Can't add author!<br/><%
        }
    %>
        <br/><a href = "authors.jsp">Return Back</a></p>
        <%
        } else {
            boolean newAuthor = (requestParameters.get("surname").equals("") || requestParameters.get("initials").equals(""));

            if (newAuthor) { %>
        <p>
            <strong>Add new author</strong>

            <input type="hidden" name="action" value="new">
        </p>
        <%
        } else {
            try {
                TableRowIterator tri = null;

                try {
                    tri = DatabaseManager.query(context, "SELECT * " +
                            "  FROM authors " +
                            "  WHERE surname_en = '" + requestParameters.get("surname") + "' AND initials_en = '" + requestParameters.get("initials") + "' " +
                            "  LIMIT 1; ");
                    TableRow row = tri.next();

                    requestParameters.putAll(newValueNames
                            .stream()
                            .collect(Collectors.toMap(item -> item, row::getStringColumn)));
                } finally {
                    if (tri != null)
                        tri.close();
                }
            } catch (SQLException e) {

            }
        %>
        <p>
            <blockquote class="blockquote text-center">
                <p class="mb-0">Edit author: <%=requestParameters.get("surname") %>, <%=requestParameters.get("initials") %></p>
            </blockquote>
        <%
            BiFunction<String, String, String> generateHiddenInput = (parameterName, parameterValue) ->
                    String.format("<input type=\"hidden\" name=\"old_%s\" value=\"%s\"/>", parameterName, parameterValue);
        %>
        <%= newValueNames.stream()
                .map(item -> generateHiddenInput.apply(item, requestParameters.get(item)))
                .collect(Collectors.joining("\n")) %>

            <input type="hidden" name="action" value="update">
        </p>
        <%
            }
        %>
        <%
            BiFunction<String, String, String> generateOutputBlock = (name, fieldId) ->
                String.format("<div class=\"form-group row\">" +
                        "<label for=\"%s\" class=\"col-sm-3 col-form-label\">%s</label>" +
                        "<div class=\"col-sm-9\">" +
                        "<input type=\"text\" class=\"form-control\" name=\"%s\" id=\"%s\" placeholder=\"%s\" value=\"%s\">" +
                        "</div>" +
                        "</div>", fieldId, name, fieldId, fieldId, name, requestParameters.get(fieldId));
        %>
        <div class="row">
            <div class="col-md-5">
                <%= generateOutputBlock.apply("Surname EN", "surname_en")%>
                <%= generateOutputBlock.apply("Surname UK", "surname_uk")%>
                <%= generateOutputBlock.apply("Surname RU", "surname_ru")%>
                <%= generateOutputBlock.apply("ORCID", "orcid")%>
            </div>

            <div class="col-md-5 col-md-offset-1">
                <%= generateOutputBlock.apply("Initials EN", "initials_en")%>
                <%= generateOutputBlock.apply("Initials UK", "initials_uk")%>
                <%= generateOutputBlock.apply("Initials RU", "initials_ru")%>
            </div>
        </div>

        <p class="pull-right">
            <input class="btn btn-success" type="submit" name="submit" value="Update and fix"/>
            <input class="btn btn-info" type="button" name="button" value="Back" onclick="window.location.href='authors.jsp'"/>
            <div class="col-md-1">&nbsp;</div>
        </p>

        <%
            }
        %>
    </form>
</dspace:layout>

<%
    } else {
    org.dspace.app.webui.util.JSPManager.showAuthorizeError(request, response, null);
    }
%>