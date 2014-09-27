<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!-- 

This stylsheet to handle exception display is a modified version of the
base apache cocoon stylesheet, this is still under the Apache license.

In DSpace, this stylesheet is ONLY used when major exceptions occur. DSpace will
first attempt to use 'exception2dri.xsl' in order to display the error within your
DSpace theme. However, if that fails (or the Theme is not configured properly), then
DSpace will fallback to using this stylesheet to generate an un-themed exception display.

The original author is unknown.
Scott Phillips adapted it for Manakin's need.

-->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ex="http://apache.org/cocoon/exception/1.0"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:param name="realPath"/>

  <!-- let sitemap override default page title -->
  <xsl:param name="pageTitle">An error has occurred</xsl:param>

  <!-- let sitemap override default context path -->
  <xsl:param name="contextPath">/</xsl:param>

  <xsl:template match="ex:exception-report">
    <html>
      <head>
        <title>
          <xsl:value-of select="$pageTitle"/>
        </title>
        <style>
          h1 { font-size: 200%; color: #336699; text-align: left; margin: 0px 0px 10px 0px; padding: 0px 0px 0px 60px; border-width: 0px 0px 1px 0px; border-style: solid; border-color: #336699;}
          p.home { padding: 10px 30px 10px 15px; margin-left: 15px; font-size: 100%;}
          p.message { padding: 10px 30px 10px 15px; margin-left: 15px; font-weight: bold; font-size: 100%;  border-left: 1px #336699 dashed;}
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
        <p class="home">
          <a><xsl:attribute name="href"><xsl:value-of select="$contextPath"/></xsl:attribute><i18n:text>xmlui.general.go_home</i18n:text></a>
        </p>
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
          The <a href="https://wiki.duraspace.org/display/DSPACE/Manakin">Manakin</a> interface of the <a href="http://dspace.org/">DSpace</a> digital repository software.
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
