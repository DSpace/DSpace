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
        <div id="exception">
          <head><xsl:value-of select="$pageTitle"/></head>
          <p></p>
          <p>
            <xref><xsl:attribute name="target"><xsl:value-of select="$contextPath"/></xsl:attribute><i18n:text>xmlui.general.go_home</i18n:text></xref>
          </p>
          <p>
            <!-- TODO: This should be moved to an I18N message key -->
            Please contact your <xref><xsl:attribute name="target"><xsl:value-of select="$contextPath"/>/contact</xsl:attribute>site administrators</xref> if you have any questions about this error message. 
          </p>
          <!-- Include the full stacktrace in the page, but hide it from view. -->
          <p rend="hidden">
            <xsl:apply-templates select="ex:full-stacktrace"/>
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

  <xsl:template match="ex:full-stacktrace">
       <hi>Java <xsl:value-of select="translate(local-name(), '-', ' ')"/></hi>
       <br/>
       <xsl:value-of select="translate(.,'&#13;','')"/>
  </xsl:template>
  
</xsl:stylesheet>
