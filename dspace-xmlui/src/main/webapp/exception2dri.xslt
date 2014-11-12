<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!-- 

This stylesheet is used to transform Cocoon XML exceptions into
valid DRI (Digital Repository Interface) XML. That way the exceptions
can be displayed within your existing DSpace theme (e.g. Mirage).

Created by Tim Donohue

-->

<xsl:stylesheet version="1.0"
                xmlns="http://di.tamu.edu/DRI/1.0/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ex="http://apache.org/cocoon/exception/1.0"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1">
  
  <!-- let sitemap override default page title -->
  <xsl:param name="pageTitle"><xsl:value-of select="/ex:exception-report/ex:message"/></xsl:param>

  <!-- let sitemap override default context path -->
  <xsl:param name="contextPath">/</xsl:param>

  <xsl:template match="ex:exception-report">
    <document version="1.1">
      <body>
        <div id="exception" rend="exception">
          <head><xsl:value-of select="$pageTitle"/></head>
          <p>
            <xref><xsl:attribute name="target"><xsl:value-of select="$contextPath"/>/</xsl:attribute><i18n:text>xmlui.general.go_home</i18n:text></xref>
          </p>
          <p>
            <i18n:text>xmlui.error.contact_msg</i18n:text>
          </p>
          <p>
            <xref><xsl:attribute name="target"><xsl:value-of select="$contextPath"/>/contact</xsl:attribute><i18n:text>xmlui.error.contact</i18n:text></xref> ||  
            <!-- Create a link which lets users optionally display the error stacktrace (using JQuery) -->
            <xref target="#" onclick="javascript:jQuery('#errorstack').toggleClass('hidden');"><i18n:text>xmlui.error.show_stack</i18n:text></xref>
          </p>
          <!-- Include the Java stacktrace on the page, but hide it from view by default. -->
          <p id="errorstack" rend="pre hidden">
            <xsl:apply-templates select="ex:stacktrace"/>
          </p>
        </div>
      </body>
      <options/>
      <meta>
        <userMeta/>
        <!-- Add basic error page metadata -->
        <pageMeta>
          <metadata element="contextPath"><xsl:value-of select="$contextPath"/></metadata>
          <metadata element="title"><xsl:value-of select="$pageTitle"/></metadata>
          <trail>
            <xsl:attribute name="target"><xsl:value-of select="$contextPath"/></xsl:attribute>
            <i18n:text>xmlui.general.dspace_home</i18n:text>
          </trail>
        </pageMeta>
        <repositoryMeta/>
      </meta>
    </document>
  </xsl:template>

  <!-- Display Java error stack -->
  <xsl:template match="ex:stacktrace">
       <hi rend="bold">Java <xsl:value-of select="translate(local-name(), '-', ' ')"/>: </hi>
       <hi>
         <xsl:value-of select="translate(.,'&#13;','')"/>
       </hi>
  </xsl:template>
 
</xsl:stylesheet>
