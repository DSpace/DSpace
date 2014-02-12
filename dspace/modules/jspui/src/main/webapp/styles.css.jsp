<%--
  - styles.css.jsp
  -
  - Version: $Revision: 4603 $
  -
  - Date: $Date: 2009-12-03 08:17:54 +0000 (Thu, 03 Dec 2009) $
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
  --%>

<%--
  - Main DSpace Web UI stylesheet
  -
  - This is a JSP so it can be tailored for different browser types
  --%>
<%@ page import="org.dspace.app.webui.util.JSPManager" %>

<%
    // Make sure the browser knows we're a stylesheet
    response.setContentType("text/css");

    String imageUrl   = request.getContextPath() + "/image/";

    // Netscape 4.x?
    boolean usingNetscape4 = false;
    String userAgent = request.getHeader( "User-Agent" );
    if( userAgent != null && userAgent.startsWith( "Mozilla/4" ) )
    {
        usingNetscape4 = true;
    }
%>

A { color: #336699 }

BODY { font-family: "verdana", Arial, Helvetica, sans-serif;
       font-size: 10pt;
       font-style: normal;
       color: #000000;
       background: #bbbbbb;
       margin: 0;
       padding: 0;
       margin-left:0px;
       margin-right:0px;
       margin-top:0px;
       margin-bottom:0px }

<%-- Note: Font information must be repeated for broken Netscape 4.xx --%>
H1 { margin-left: 10px;
     margin-right: 10px;
     font-size: 16pt;
     font-weight: bold;
     font-style: normal;
     font-family: "verdana", "Arial", "Helvetica", sans-serif;
     color: #336699 }

H2 { margin-left: 10px;
     margin-right: 10px;
     font-size: 14pt;
     font-style: normal;
     font-family: "verdana", "Arial", "Helvetica", sans-serif;
     color: #336699 }

H3 { margin-left: 10px;
     margin-right: 10px;
     font-size: 12pt;
     font-weight: bold;
     font-family: "verdana", "Arial", "Helvetica", sans-serif;
     color: black }

object { display: inline; }

p {  margin-left: 10px;
     margin-right: 10px;
     font-family: "verdana", "Arial", "Helvetica", sans-serif;
     font-size: 10pt }
     
<%-- This class is here so that a "DIV" by default acts as a "P".    --%>
<%-- This is necessary since the "dspace:popup" tag must have a "DIV" --%>
<%-- (or block element) surrounding it in order to be valid XHTML 1.0 --%>
DIV { margin-left: 10px;
      margin-right: 10px;
      margin-bottom: 15px;
      font-family: "verdana", "Arial", "Helvetica", sans-serif;
      font-size: 10pt;}

UL { font-family: "verdana", "Arial", "Helvetica", sans-serif;
     font-size: 10pt }

<%-- This class is here so the standard style from "P" above can be applied --%>
<%-- to anything else. --%>
.standard { margin-left: 10px;
            margin-right: 10px;
            font-family: "verdana", "Arial", "Helvetica", sans-serif;
            font-size: 10pt }

.langChangeOff { text-decoration: none;
                 color : #bbbbbb;
                 cursor : default;
                 font-size: 10pt }

.langChangeOn { text-decoration: underline;
                color: #336699;
                cursor: pointer;
                font-size: 10pt }

.pageBanner { width: 100%;
              border: 0;
              margin: 0;
              background: #ffffff;
              color: #000000;
              padding: 0;
              vertical-align: middle }

.tagLine { vertical-align: bottom;
           padding: 10px;
           border: 0;
           margin: 0;
           background: #ffffff;
           color: #ff6600 }

.tagLineText { background: #ffffff;
               color: #ff6600;
               font-size: 10pt;
               font-weight: bold;
               border: 0;
               margin: 0 }

.stripe { background: #336699 url(<%= imageUrl %>stripe.gif) repeat-x;
          vertical-align: top;
          border: 0;
          padding: 0;
          margin: 0;
          color: #ffffff }

.locationBar { font-size: 10pt;
               font-family: "verdana", "Arial", "Helvetica", sans-serif;
               text-align: left }

.centralPane { margin: 1px;
               vertical-align: top;
               padding: 3px;
               border: 0 }

<%-- HACK: Width shouldn't really be 100%:  Really, this is asking that the --%>
<%--       table cell be the full width of the table.  The side effect of --%>
<%--       this should theoretically be that other cells in the row be made --%>
<%--       a width of 0%, but in practice browsers will only take this 100% --%>
<%--       as a hint, and just make it as wide as it can without impinging --%>
<%--       the other cells.  This, fortunately, is precisely what we want. --%>
.pageContents { FONT-FAMILY: "verdana", Arial, Helvetica, sans-serif;
                background: white;
                color: black;
                vertical-align: top;
                width: 100% }

.navigationBarTable{ width: 100%;
                     padding: 2px;
                     margin: 2px;
                     border: 0 }

.navigationBar { font-family: "verdana", "Arial", "Helvetica", sans-serif;
                 font-size: 10pt;
                 font-style: normal;
                 font-weight: bold;
                 color: #252645;
                 text-decoration: none;
                 background: white }

.navigationBarSublabel{  font-family: "verdana", "Arial", "Helvetica", sans-serif;
                         font-size: 12pt;
                         font-style: normal;
                         font-weight: bold;
                         color: black;
                         text-decoration: none;
                         background: white;
                         white-space: nowrap }

<%-- HACK: Shouldn't have to repeat font information and colour here, it --%>
<%--       should be inherited from the parent, but isn't in Netscape 4.x, --%>
<%--       IE or Opera.  (Only Mozilla functions correctly.) --%>

.navigationBarItem { font-family: "verdana", "Arial", "Helvetica", sans-serif;
                     font-size: 10pt;
                     font-style: normal;
                     font-weight: normal;
                     color: #252645;
                     background: #ffffff;
                     text-decoration: none;
                     vertical-align: middle;
                     white-space: nowrap }

.loggedIn { font-family: "verdana", "Arial", "Helvetica", sans-serif;
            font-size: 8pt;
            font-style: normal;
            font-weight: normal;
            color: #882222;
            background: #ffffff }

.pageFooterBar { width: 100%;
                 border: 0;
                 margin: 0;
                 padding: 0;
                 background: #ffffff;
                 color: #000000;
                 vertical-align: middle }

.pageFootnote { font-family: "verdana", "Arial", "Helvetica", sans-serif;
                font-size: 10pt;
                font-style: normal;
                font-weight: normal;
                background: #ffffff;
                color: #252645;
                text-decoration: none;
                text-align: left;
                vertical-align: middle;
                margin-left: 10px;
                margin-right: 10px }

.sidebar { background: #ffffff;
           color: #000000 }

.communityLink { font-family: "verdana", "Arial", "Helvetica", sans-serif;
                 font-size: 14pt;
                 font-weight: bold }

.communityStrength {
                                font-family: "verdana", "Arial", "Helvetica", sans-serif;
                 font-size: 12pt;
                 font-weight: normal }

.communityDescription { margin-left: 20px;
                        margin-right: 10px;
                        font-family: "verdana", "Arial", "Helvetica", sans-serif;
                        font-size: 10pt;
                        font-weight: normal;
                        list-style-type: none }

.collectionListItem { font-family: "verdana", "Arial", "Helvetica", sans-serif;
                      font-size: 12pt;
                      font-weight: normal }

.collectionDescription { margin-left: 20px;
                     margin-right: 10px;
                     font-family: "verdana", "Arial", "Helvetica", sans-serif;
                     font-size: 10pt;
                         font-weight: normal;
                     list-style-type: none }

.miscListItem { margin-left: 20px;
                margin-right: 10px;
                font-family: "verdana", "Arial", "Helvetica", sans-serif;
                font-size: 12pt;
                list-style-type: none }

.copyrightText { margin-left: 20px;
                 margin-right: 20px;
                 text-align: center;
                 font-style: italic;
                 font-family: "verdana", "Arial", "Helvetica", sans-serif;
                 font-size: 10pt;
                 list-style-type: none }

.browseBarLabel { font-family: "verdana", "Arial", "Helvetica", sans-serif;
                  font-size: 10pt;
                  font-style: normal;
                  font-weight: bold;
                  color: #000000;
                  background: #ffffff;
                  vertical-align: middle;
                  text-decoration: none }

.browseBar { font-family: "verdana", "Arial", "Helvetica", sans-serif;
             font-size: 12pt;
             font-style: normal;
             font-weight: bold;
             background: #ffffff;
             color: #252645;
             vertical-align: middle;
             text-decoration: none }

.itemListCellOdd { font-family: "verdana", "Arial", "Helvetica", sans-serif;
                   font-size: 12pt;
                   font-style: normal;
                   font-weight: normal;
                   color: #000000;
                   vertical-align: middle;
                   text-decoration: none;
                   background: #ffffff }


.itemListCellEven { font-family: "verdana", "Arial", "Helvetica", sans-serif;
                    font-size: 12pt;
                    font-style: normal;
                    font-weight: normal;
                    color: #000000;
                    vertical-align: middle;
                    text-decoration: none;
                    background: #eeeeee }

.itemListCellHilight { font-family: "verdana", "Arial", "Helvetica", sans-serif;
                       font-size: 12pt;
                       font-style: normal;
                       font-weight: normal;
                       color: #000000;
                       vertical-align: middle;
                       text-decoration: none;
                       background: #ddddff }

.topNavLink { margin-left: 10px;
          margin-right: 10px;
          font-family: "verdana", "Arial", "Helvetica", sans-serif;
          font-size: 10pt;
          text-align: center }

.submitFormLabel { margin-left: 10px;
           margin-right: 10px;
           font-family: "verdana", "Arial", "Helvetica", sans-serif;
                   font-weight: bold;
           font-size: 10pt;
           text-align: right;
           vertical-align: top }

.submitFormHelp {  margin-left: 10px;
           margin-right: 10px;
           font-family: "verdana", "Arial", "Helvetica", sans-serif;
           font-size: 8pt;
           text-align: center }
           

.submitFormWarn {  margin-left: 10px;
           margin-right: 10px;
           font-family: "verdana", "Arial", "Helvetica", sans-serif;
           font-weight: bold;
           font-size: 12pt;
           color: #ff6600;
           text-align: center }

.uploadHelp { margin-left: 20px;
              margin-right: 20px;
              font-family: "verdana", "Arial", "Helvetica", sans-serif;
              font-size: 10pt;
              text-align: left }

.submitFormDateLabel {  margin-left: 10px;
                        margin-right: 10px;
                        font-family: "verdana", "Arial", "Helvetica", sans-serif;
                        font-size: 10pt;
                        font-style: italic;
                        text-align: center;
                        vertical-align: top; }

.submitProgressTable{ margin: 0;
                      padding: 0;
                      border: 0;
                      vertical-align: top;
                      text-align: center;
                      white-space: nowrap }

.submitProgressButton{ border: 0 }

.submitProgressButtonDone{ border: 0;
                           background-image: url(<%= imageUrl %>/submit/done.gif);
                           background-position: center;
                           height: 30px;
                           width: 90px;
                           font-size: 12pt;
                           color: black;
                           background-repeat: no-repeat; }

.submitProgressButtonCurrent{ border: 0;
                           background-image: url(<%= imageUrl %>/submit/current.gif);
                           background-position: center;
                           height: 30px;
                           width: 90px;
                           font-size: 12pt;
                           color: white;
                           background-repeat: no-repeat; }

.submitProgressButtonNotDone{ border: 0;
                           background-image: url(<%= imageUrl %>/submit/notdone.gif);
                           background-position: center;
                           height: 30px;
                           width: 90px;
                           font-size: 12pt;
                           color: black;
                           background-repeat: no-repeat; }

.miscTable { font-family: "verdana", "Arial", "Helvetica", sans-serif;
             font-size: 12pt;
             font-style: normal;
             font-weight: normal;
             color: #000000;
             vertical-align: middle;
             text-decoration: none;
             background: #cccccc }

.miscTableNoColor { font-family: "verdana", "Arial", "Helvetica", sans-serif;
             font-size: 12pt;
             font-style: normal;
             font-weight: normal;
             color: #000000;
             vertical-align: middle;
             text-decoration: none;
             background: #ffffff }

<%-- The padding element breaks Netscape 4 - it puts a big gap at the top
  -- of the browse tables if it's present.  So, we decide here which
  -- padding elements to use. --%>
<%
    String padding = "padding: 3px";

    if( usingNetscape4 )
    {
        padding = "padding-left: 3px;  padding-right: 3px; padding-top: 1px";
    }
%>


.oddRowOddCol{ font-family: "verdana", "Arial", "Helvetica", sans-serif;
               font-size: 12pt;
               font-style: normal;
               font-weight: normal;
               color: #000000;
               vertical-align: middle;
               text-decoration: none;
               background: #ffffff;
               <%= padding %> }

.evenRowOddCol{ font-family: "verdana", "Arial", "Helvetica", sans-serif;
                font-size: 12pt;
                font-style: normal;
                font-weight: normal;
                color: #000000;
                vertical-align: middle;
                text-decoration: none;
                background: #eeeeee;
                <%= padding %>  }

.oddRowEvenCol{ font-family: "verdana", "Arial", "Helvetica", sans-serif;
                font-size: 12pt;
                font-style: normal;
                font-weight: normal;
                color: #000000;
                vertical-align: middle;
                text-decoration: none;
                background: #eeeeee;
                <%= padding %>  }

.evenRowEvenCol{ font-family: "verdana", "Arial", "Helvetica", sans-serif;
                 font-size: 12pt;
                 font-style: normal;
                 font-weight: normal;
                 color: #000000;
                 vertical-align: middle;
                 text-decoration: none;
                 background: #dddddd;
                 <%= padding %>  }

.highlightRowOddCol{ font-family: "verdana", "Arial", "Helvetica", sans-serif;
                     font-size: 12pt;
                     font-style: normal;
                     font-weight: normal;
                     color: #000000;
                     vertical-align: middle;
                     text-decoration: none;
                     background: #ccccee;
                     <%= padding %> }

.highlightRowEvenCol{ font-family: "verdana", "Arial", "Helvetica", sans-serif;
                      font-size: 12pt;
                      font-style: normal;
                      font-weight: normal;
                      color: #000000;
                      vertical-align: middle;
                      text-decoration: none;
                      background: #bbbbcc;
                      <%= padding %> }

.itemDisplayTable{ text-align: center;
                   border: 0;
                   color: #000000 }

.metadataFieldLabel{ font-family: "verdana", "Arial", "Helvetica", sans-serif;
                     font-size: 12pt;
                     font-style: normal;
                     font-weight: bold;
                     color: #000000;
                     vertical-align: top;
                     text-align: right;
                     text-decoration: none;
                     white-space: nowrap;
                     <%= padding %> }

.metadataFieldValue{ font-family: "verdana", "Arial", "Helvetica", sans-serif;
                     font-size: 12pt;
                     font-style: normal;
                     font-weight: normal;
                     color: #000000;
                     vertical-align: top;
                     text-align: left;
                     text-decoration: none;
                     <%= padding %> }  <%-- width 100% ?? --%>

.recentItem { margin-left: 10px;
              margin-right: 10px;
              font-family: "verdana", "Arial", "Helvetica", sans-serif;
              font-size: 10pt }

.searchBox { font-family: "verdana", "Arial", "Helvetica", sans-serif;
             font-size: 10pt;
             font-style: normal;
             font-weight: bold;
             color: #000000;
             vertical-align: middle;
             text-decoration: none;
             background: #fefecd;
             padding: 0;
             border: 0;
             margin: 0 }

.searchBoxLabel { font-family: "verdana", "Arial", "Helvetica", sans-serif;
                  font-size: 10pt;
                  font-style: normal;
                  font-weight: bold;
                  color: #000000;
                  background: #fefecd;
                  text-decoration: none;
                  vertical-align: middle }

.searchBoxLabelSmall { font-family: "verdana", "Arial", "Helvetica", sans-serif;
                  font-size: 8pt;
                  font-style: normal;
                  font-weight: bold;
                  color: #000000;
                  background: #fefecd;
                  text-decoration: none;
                  vertical-align: middle }

.attentionTable
{
    font-style: normal;
    font-weight: normal;
    color: #000000;
    vertical-align: middle;
    text-decoration: none;
    background: #cc9966;
}

.attentionCell
{
    background: #ffffcc;
    text-align: center;
}

.help {font-family: "verdana", "Arial", "Helvetica", sans-serif;
        background: #ffffff;
        margin-left:10px;}

.help h2{text-align:center;
                font-size:18pt;
                color:#000000;}

.help h3{font-weight:bold;
         margin-left:0px;}

.help h4{font-weight:bold;
         font-size: 10pt;
         margin-left:5px;}

.help h5{font-weight:bold;
         margin-left:10px;
         line-height:.5;}

.help p {font-size:10pt;}

.help table{margin-left:8px;
            width:90%;}

.help table.formats{font-size:10pt;}

.help ul {font-size:10pt;}

.help p.bottomLinks {font-size:10pt;
                    font-weight:bold;}

.help td.leftAlign{font-size:10pt;}
.help td.rightAlign{text-align:right;
                    font-size:10pt;}
                    

<%-- The following rules are used by the controlled-vocabulary add-on --%>

ul.controlledvocabulary  {
                list-style-type:none; }

        
.controlledvocabulary ul  li ul {
             list-style-type:none;
                display:none; }

input.controlledvocabulary  {
                border:0px; }

img.controlledvocabulary {
                margin-right:8px ! important;
                margin-left:11px ! important;
                cursor:hand; }

.submitFormHelpControlledVocabularies {
                   margin-left: 10px;
           margin-right: 10px;
           font-family: "verdana", "Arial", "Helvetica", sans-serif;
           font-size: 8pt;
           text-align: left; }

.controlledVocabularyLink {
           font-family: "verdana", "Arial", "Helvetica", sans-serif;
           font-size: 8pt; }
           
.browse_buttons
{
        float: right;
        padding: 1px;
        margin: 1px;
}

#browse_navigation
{
        margin-bottom: 10px;
}

#browse_controls
{
        margin-bottom: 10px;
}

.browse_range
{
        margin-top: 5px;
        margin-bottom: 5px;
}


<%-- styles added by authority control --%>
div.autocomplete {
    background-color:white;
    border:1px solid #888888;
    margin:0;
    padding:0;
    position:absolute;
    width:250px;
}

div.autocomplete ul {
    list-style-type:none;
    margin:0;
    padding:0;
}

div.autocomplete ul li {
    cursor:pointer;
}

div.autocomplete ul li.selected {
    text-decoration: underline;
}
div.autocomplete ul li:hover {
    text-decoration: underline;
}

div.autocomplete ul li span.value {
    display:none;
}


/* this magic gets the 16x16 icon to show up.. setting height/width didn't
   do it, but adding padding actually made it show up. */
img.ds-authority-confidence,
span.ds-authority-confidence
{ width: 16px; height: 16px; margin: 5px; background-repeat: no-repeat;
  padding: 0px 2px; vertical-align: bottom; color: transparent;}
img.ds-authority-confidence.cf-unset,
span.ds-authority-confidence.cf-unset
  { background-image: url(<%= request.getContextPath() %>/image/authority/bug.png);}
img.ds-authority-confidence.cf-novalue,
span.ds-authority-confidence.cf-novalue
  { background-image: url(<%= request.getContextPath() %>/image/confidence/0-unauthored.gif);}
img.ds-authority-confidence.cf-rejected,
img.ds-authority-confidence.cf-failed,
span.ds-authority-confidence.cf-rejected,
span.ds-authority-confidence.cf-failed
  { background-image: url(<%= request.getContextPath() %>/image/confidence/2-errortriangle.gif); }
img.ds-authority-confidence.cf-notfound,
span.ds-authority-confidence.cf-notfound
  { background-image: url(<%= request.getContextPath() %>/image/confidence/3-thumb1.gif); }
img.ds-authority-confidence.cf-ambiguous,
span.ds-authority-confidence.cf-ambiguous
  { background-image: url(<%= request.getContextPath() %>/image/confidence/4-question.gif); }
img.ds-authority-confidence.cf-uncertain,
span.ds-authority-confidence.cf-uncertain
  { background-image: url(<%= request.getContextPath() %>/image/confidence/5-pinion.gif); }
img.ds-authority-confidence.cf-accepted,
span.ds-authority-confidence.cf-accepted
  { background-image: url(<%= request.getContextPath() %>/image/confidence/6-greencheck.gif); }

/* hide authority-value inputs in forms */
input.ds-authority-value { display:none; }

/** XXX Change to this to get the authority value to show up for debugging:
 input.ds-authority-value { display:inline; }
**/

/* ..except, show authority-value inputs in on the Item EditMetadata page */
table.miscTable input.ds-authority-value { display: inline; }

table.authority-statistics {padding: 5px; margin-bottom: 15px;}
table.authority-statistics table {float: left; text-align: center;}

.statsTable {
        border: 1px gray solid;
        width: 85%;
}

.statsTable td {
        font-size: 0.8em;
}


div.authority-key-nav-link, div.authority-page-nav-link {margin-top: 20px;}

div#authority-message {
    width: 80%;
    display:block;
    text-align: center;
    margin-left: 10%;
    padding: 10px;
    border: 1px dashed #FFCC00;
    background-color: #FFF4C8;
 }

a.authority {
    background: transparent url(<%= request.getContextPath() %>/image/authority/book_key.png) no-repeat;
    background-position: top right;
    padding-right: 20px;
}

/* for edit-item-form lock button */
input.ds-authority-lock
  { vertical-align: bottom; height: 24px; width: 24px; margin-right: 8px;
    background-repeat: no-repeat; background-color: transparent; }
input.ds-authority-lock.is-locked
  { background-image: url(<%= request.getContextPath() %>/image/lock24.png); }
input.ds-authority-lock.is-unlocked
  { background-image: url(<%= request.getContextPath() %>/image/unlock24.png); }
