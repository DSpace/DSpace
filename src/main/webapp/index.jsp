<%--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
--%>

<html>
<head>
<link rel="stylesheet" type="text/css" href="solr-admin.css">
<link rel="icon" href="favicon.ico" type="image/ico"></link>
<link rel="shortcut icon" href="favicon.ico" type="image/ico"></link>
<title>Welcome to Solr</title>
<script language="Javascript">

// derived from http://www.degraeve.com/reference/simple-ajax-example.php
function xmlhttpPost(strURL) {
    var xmlHttpReq = false;
    var self = this;
    if (window.XMLHttpRequest) { // Mozilla/Safari
        self.xmlHttpReq = new XMLHttpRequest(); 
    }
    else if (window.ActiveXObject) { // IE
        self.xmlHttpReq = new ActiveXObject("Microsoft.XMLHTTP");
    }
    self.xmlHttpReq.open('POST', strURL, true);
    self.xmlHttpReq.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    self.xmlHttpReq.onreadystatechange = function() {
        if (self.xmlHttpReq.readyState == 4) {
            updatepage(self.xmlHttpReq.responseText);
        }
    }

    var params = getstandardargs().concat(getquerystring());
    var strData = params.join('&');
    self.xmlHttpReq.send(strData);
}

function getstandardargs() {
    var params = [
        'wt=json'
        , 'indent=on'
        , 'hl=true'
        , 'hl.fl=name,features'
        ];

    return params;
}
function getquerystring() {
  var form = document.forms['f1'];
  var query = form.query.value;
  qstr = 'q=' + escape(query);
  return qstr;
}

// this function does all the work of parsing the solr response and updating the page.
function updatepage(str){
  document.getElementById("raw").innerHTML = str;
  var rsp = eval("("+str+")"); // use eval to parse Solr's JSON response
  var html= "<br>numFound=" + rsp.response.numFound;
  var first = rsp.response.docs[0];
  html += "<br>product name="+ first.name;
  var hl=rsp.highlighting[first.id];
  if (hl.name != null) { html += "<br>name highlighted: " + hl.name[0]; }
  if (hl.features != null) { html += "<br>features highligted: " + hl.features[0]; }
  document.getElementById("result").innerHTML = html;
}
</script>
</head>

<body>
<h1>Welcome to Solr!</h1>
<a href="."><img border="0" align="right" height="61" width="142" src="admin/solr-head.gif" alt="Solr"/></a>

<h1>Solr Ajax Example</h1>
<form name="f1" onsubmit='xmlhttpPost(this.core.value + "/select"); return false;'><p>
<% 
  org.apache.solr.core.CoreContainer cores = (org.apache.solr.core.CoreContainer)request.getAttribute("org.apache.solr.CoreContainer");
  if( cores != null
   && cores.getCores().size() > 0 // HACK! check that we have valid names...
   && cores.getCores().iterator().next().getName().length() != 0 ) { %>
   core: <select name="core">
   <% for( org.apache.solr.core.SolrCore core : cores.getCores() ) {%>
  		<option><%= core.getName() %></option>
   <% }%>
   </select>
<%} else { %>
  <input type="hidden" name="core" value=""/>  
<% } %>
 query: <input name="query" type="text">  <input value="Go" type="submit"></p>

<div id="result"></div>

<p/><pre>Raw JSON String: <div id="raw"></div></pre>

</form>


<% 
  //org.apache.solr.core.CoreContainer cores = (org.apache.solr.core.CoreContainer)request.getAttribute("org.apache.solr.CoreContainer");
  if( cores != null
   && cores.getCores().size() > 0 // HACK! check that we have valid names...
   && cores.getCores().iterator().next().getName().length() != 0 ) { 
    for( org.apache.solr.core.SolrCore core : cores.getCores() ) {%>
	<a href="<%= core.getName() %>/admin/">Admin <%= core.getName() %></a><br/>
<% }} else { %>
<a href="admin/">Solr Admin</a>
<% } %>

</body>
</html>
