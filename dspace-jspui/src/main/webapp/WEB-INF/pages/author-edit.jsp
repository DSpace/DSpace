<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<%@ page import="java.util.Locale" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%
    boolean loginAs = ConfigurationManager.getBooleanProperty("webui.user.assumelogin", false);
%>

<dspace:layout locbar="nolink" title="Authors list" feedData="NONE">
    <script>
        function selectionchange()
        {
            window.document.epersongroup.eperson_id.options[0] = new Option('${eperson_string}');
            window.document.epersongroup.eperson_id.options[0].value = '${author.uuid}';
        }
    </script>
    <c:if test="${hasMessage}">
        <div class="alert alert-${messageType}" role="alert">${message}</div>
    </c:if>
    <div class="panel panel-default">
        <div class="panel-heading">
            <h3 class="panel-title">
                <c:choose>
                    <c:when test="${empty author}">
                        Add new author
                    </c:when>
                    <c:otherwise>
                        Edit author
                    </c:otherwise>
                </c:choose>
            </h3>
        </div>
        <div class="panel-body">
            <form method="post" name="epersongroup"  class="form-horizontal">
                <div class="form-group">
                    <label for="surnameEn" class="col-sm-2 control-label">Surname in English</label>
                    <div class="col-sm-10">
                        <input type="text" class="form-control" name="surnameEn" id="surnameEn" placeholder="Surname in English" value="${author.getSurname(Locale.ENGLISH)}">
                    </div>
                </div>
                <div class="form-group">
                    <label for="initialsEn" class="col-sm-2 control-label">Initials in English</label>
                    <div class="col-sm-10">
                        <input type="text" class="form-control" name="initialsEn" id="initialsEn" placeholder="Initials in English" value="${author.getInitials(Locale.ENGLISH)}">
                    </div>
                </div>
                <div class="form-group">
                    <label for="surnameRu" class="col-sm-2 control-label">Фамилия на русском</label>
                    <div class="col-sm-10">
                        <input type="text" class="form-control" name="surnameRu" id="surnameRu" placeholder="Фамилия на русском" value="${author.getSurname(Locale.forLanguageTag("ru"))}">
                    </div>
                </div>
                <div class="form-group">
                    <label for="initialsRu" class="col-sm-2 control-label">Инициалы на русском</label>
                    <div class="col-sm-10">
                        <input type="text" class="form-control" name="initialsRu" id="initialsRu" placeholder="Инициалы на русском" value="${author.getInitials(Locale.forLanguageTag("ru"))}">
                    </div>
                </div>
                <div class="form-group">
                    <label for="surnameUk" class="col-sm-2 control-label">Прізвище українською</label>
                    <div class="col-sm-10">
                        <input type="text" class="form-control" name="surnameUk" id="surnameUk" placeholder="Прізвище українською" value="${author.getSurname(Locale.forLanguageTag("uk"))}">
                    </div>
                </div>
                <div class="form-group">
                    <label for="initialsUk" class="col-sm-2 control-label">Ініціали українською</label>
                    <div class="col-sm-10">
                        <input type="text" class="form-control" name="initialsUk" id="initialsUk" placeholder="Ініціали українською"value="${author.getInitials(Locale.forLanguageTag("uk"))}">
                    </div>
                </div>
                <div class="form-group">
                    <label for="orcid" class="col-sm-2 control-label">ORCID</label>
                    <div class="col-sm-4">
                        <input type="text" class="form-control" name="orcid" id="orcid" placeholder="ORCID" value="${author.orcid}">
                    </div>
                    <label class="col-sm-1 control-label" >Eperson</label>
                    <div class="col-sm-5" >
                        <dspace:selecteperson multiple="false" />
                        <c:if test="${eperson_attached}">
                            <script>
                                selectionchange();
                            </script>
                        </c:if>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-sm-offset-2 col-sm-10">
                        <input type="hidden" class="form-control" name="uuid" id="uuid" value="${author.uuid}">
                        <button type="button" class="btn btn-default" onclick="location.href = '/authors/list';javascript:finishEPerson();">Cancel</button>

                        <c:if test="${not empty author}">
                            <button type="button" class="btn btn-danger" data-toggle="modal" data-target="#delteAuthorModal">Delete author</button>
                        </c:if>
                        <button type="submit" class="btn btn-success" onclick="javascript:finishEPerson();">Save</button>
                    </div>
                </div>
            </form>
        </div>
    </div>
    <div class="modal fade" id="delteAuthorModal" tabindex="-1" role="dialog" aria-labelledby="delteAuthorModalLabel">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title" id="myModalLabel">Are you sure to delete this author?</h4>
                </div>
                <div class="modal-body">
                        <h3>${author.getSurname(Locale.ENGLISH)}, ${author.getInitials(Locale.ENGLISH)}</h3>
                </div>
                <div class="modal-footer">
                    <form action="/authors/delete" method="get">
                        <input type="hidden" class="form-control" name="uuid" id="uuid" value="${author.uuid}">
                        <input type="hidden" value="${author.getSurname(Locale.ENGLISH)}, ${author.getInitials(Locale.ENGLISH)}" name="author" id="author">
                        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                        <button type="submit" class="btn btn-danger">Delete</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</dspace:layout>
