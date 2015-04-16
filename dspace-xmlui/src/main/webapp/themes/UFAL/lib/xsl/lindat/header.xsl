<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : header.xsl
    Created on : April 14, 2012, 4:48 PM
    Author     : sedlak
    Description:
        Purpose of transformation follows.
-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
        xmlns:dri="http://di.tamu.edu/DRI/1.0/"
        xmlns:mets="http://www.loc.gov/METS/"
        xmlns:xlink="http://www.w3.org/TR/xlink/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
        xmlns:xhtml="http://www.w3.org/1999/xhtml"
        xmlns:mods="http://www.loc.gov/mods/v3"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:confman="org.dspace.core.ConfigurationManager"
        xmlns="http://www.w3.org/1999/xhtml"
        exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

    <xsl:output indent="yes" />

    <xsl:template name="buildHeader">
      <xsl:variable name="active-locale" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='currentLocale']"/>
      <xsl:choose>
          <xsl:when test="$active-locale='cs'">
              <xsl:copy-of select="document('../../lindat/cs/header.htm')" />
          </xsl:when>
          <xsl:otherwise>
              <xsl:copy-of select="document('../../lindat/header.htm')" />
          </xsl:otherwise>
      </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
