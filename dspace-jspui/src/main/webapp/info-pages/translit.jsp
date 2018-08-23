<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<% org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request); %>
<dspace:layout locbar="nolink" title="Translit page" feedData="NONE">

    <div class="row">
        <div class="col-md-5">
            <label for="inputText">Input text</label>
            <textarea class="form-control" id="inputText" rows="3" onkeyup="updateOutput()"></textarea>
        </div>
        <div class="col-md-1">
        </div>
        <div class="col-md-5">
            <label for="resultText">Result text</label>
            <textarea class="form-control" id="resultText" rows="3"></textarea>
        </div>
    </div>
    <script src="../static/js/transliteration.js"></script>
    <script>
        var translator = cyrillicToTranslit("uk");
        function updateOutput() {
            $('#resultText').val(translator.transform($('#inputText').val(), " "));
        }
    </script>
</dspace:layout>
