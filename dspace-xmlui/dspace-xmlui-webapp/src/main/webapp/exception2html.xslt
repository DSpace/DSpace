<?xml version="1.0"?>
<!--
  Copyright 1999-2004 The Apache Software Foundation

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

<!-- 

This stylsheet to handle exception display is a modified version of the
base apache cocoon stylesheet, this is still under the Apache license.

The original author is unknown.
Scott Phillips adapted it for Manakin's need.

-->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ex="http://apache.org/cocoon/exception/1.0">

  <xsl:param name="realPath"/>

  <!-- let sitemap override default page title -->
  <xsl:param name="pageTitle">An error has occured</xsl:param>

  <xsl:template match="ex:exception-report">
    <html>
      <head>
        <title>
          <xsl:value-of select="$pageTitle"/>
        </title>
        <style>
          h1 { font-size: 200%; color: #336699; text-align: left; margin: 0px 0px 10px 0px; padding: 0px 0px 0px 60px; border-width: 0px 0px 1px 0px; border-style: solid; border-color: #336699;}
          p.message { padding: 10px 30px 10px 15px; margin-left: 15px; font-weight: bold; font-size: 110%;  border-left: 1px #336699 dashed;}
          p.description { padding: 10px 30px 20px 30px; border-width: 0px 0px 1px 0px; border-style: solid; border-color: #336699;}
          p.topped { padding-top: 10px; border-width: 1px 0px 0px 0px; border-style: solid; border-color: #336699; }
          span.switch { cursor: pointer; margin-left: 5px; text-decoration: underline; }
          span.description { color: #336699; font-weight: bold; }
          
          .row-1 { background-color: #F0F0F0;}
          table { border-collapse: collapse; margin-top: 0.3em; }
          td { padding: 0.1em; }
        </style>
        <script type="text/javascript">
          function toggle(id) {
            var element = document.getElementById(id);
            with (element.style) {
              if ( display == "none" ) {
                display = ""
              } else {
                display = "none"
              }
            }
          
            var text = document.getElementById(id + "-switch").firstChild;
            if (text.nodeValue == "[show]") {
              text.nodeValue = "[hide]";
            } else {
              text.nodeValue = "[show]";
            }
          }
        </script>
      </head>
      <body>
        <xsl:attribute name="onload">
          <xsl:if test="ex:cocoon-stacktrace">toggle('locations');</xsl:if>
          <xsl:if test="ex:stacktrace">toggle('stacktrace');</xsl:if>
          <xsl:if test="ex:full-stacktrace">toggle('full-stacktrace');</xsl:if>
        </xsl:attribute>

        <h1><xsl:value-of select="$pageTitle"/></h1>
        <p class="message">
          <xsl:value-of select="@class"/>:
          <xsl:apply-templates select="ex:message" mode="breakLines"/>
          <xsl:if test="ex:location">
             <br/><span style="font-weight: normal"><xsl:apply-templates select="ex:location"/></span>
          </xsl:if>
        </p>

        <p><span class="description">Cocoon stacktrace</span>
           <span class="switch" id="locations-switch" onclick="toggle('locations')">[hide]</span>
        </p>
        <div id="locations">
          <xsl:for-each select="ex:cocoon-stacktrace/ex:exception">
            <xsl:sort select="position()" order="descending"/>
            <strong>
               <xsl:apply-templates select="ex:message" mode="breakLines"/>
            </strong>
            <table>
               <xsl:for-each select="ex:locations/*[string(.) != '[cause location]']">
                 <!-- [cause location] indicates location of a cause, which 
                      the exception generator outputs separately -->
                <tr class="row-{position() mod 2}">
                   <td><xsl:call-template name="print-location"/></td>
                   <td><em><xsl:value-of select="."/></em></td>
                </tr>
              </xsl:for-each>
            </table>
            <br/>
           </xsl:for-each>
        </div>

        <xsl:apply-templates select="ex:stacktrace"/>
        <xsl:apply-templates select="ex:full-stacktrace"/>

        <p class="topped">
          The <a href="http://di.tamu.edu/projects/xmlui/">Manakin</a> / <a href="http://dspace.org/">DSpace</a> digital repository software.
        </p>
      </body>
    </html>
  </xsl:template>
  
  <xsl:template match="ex:stacktrace|ex:full-stacktrace">
      <p class="stacktrace">
       <span class="description">Java <xsl:value-of select="translate(local-name(), '-', ' ')"/></span>
       <span class="switch" id="{local-name()}-switch" onclick="toggle('{local-name()}')">[hide]</span>
       <pre id="{local-name()}">
         <xsl:value-of select="translate(.,'&#13;','')"/>
       </pre>
      </p>
  </xsl:template>
  
  <xsl:template match="ex:location">
   <xsl:if test="string-length(.) > 0">
     <em><xsl:value-of select="."/></em>
     <xsl:text> - </xsl:text>
   </xsl:if>
   <xsl:call-template name="print-location"/>
  </xsl:template>
  
  <xsl:template name="print-location">
     <xsl:choose>
       <xsl:when test="contains(@uri, $realPath)">
         <xsl:text>context:/</xsl:text>
         <xsl:value-of select="substring-after(@uri, $realPath)"/>
       </xsl:when>
       <xsl:otherwise>
         <xsl:value-of select="@uri"/>
       </xsl:otherwise>
      </xsl:choose>
      <xsl:text> - </xsl:text>
      <xsl:value-of select="@line"/>:<xsl:value-of select="@column"/>
  </xsl:template>
  
  <!-- output a text by splitting it with <br>s on newlines
       can be uses either by an explicit call or with <apply-templates mode="breakLines"/> -->
  <xsl:template match="node()"  mode="breakLines" name="breakLines">
     <xsl:param name="text" select="string(.)"/>
     <xsl:choose>
        <xsl:when test="contains($text, '&#10;')">
           <xsl:value-of select="substring-before($text, '&#10;')"/>
           <br/>
           <xsl:call-template name="breakLines">
              <xsl:with-param name="text" select="substring-after($text, '&#10;')"/>
           </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
           <xsl:value-of select="$text"/>
        </xsl:otherwise>
     </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
