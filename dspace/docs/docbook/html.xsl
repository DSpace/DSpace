<?xml version='1.0'?>
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:import href="http://docbook.sourceforge.net/release/xsl/current/html/chunk.xsl"/>

<!-- <xsl:param name="html.stylesheet" select="'corpstyle.css'"/>
<xsl:param name="admon.graphics" select="1"/> -->

<xsl:template name="user.footer.content">
  <HR/>
  <xsl:apply-templates select="//copyright[1]" mode="titlepage.mode"/>
  <xsl:apply-templates select="//legalnotice[1]" mode="titlepage.mode"/>
</xsl:template>

<xsl:template name="body.attributes">
   <xsl:attribute name="bgcolor">white</xsl:attribute>
   <xsl:attribute name="text">black</xsl:attribute>
   <xsl:attribute name="link">#0000FF</xsl:attribute>
   <xsl:attribute name="vlink">#840084</xsl:attribute>
   <xsl:attribute name="alink">#0000FF</xsl:attribute>
   <xsl:attribute name="marginwidth">5m</xsl:attribute>
</xsl:template>

<xsl:param name="toc.max.depth">4</xsl:param>
<xsl:param name="toc.section.depth">4</xsl:param>
<xsl:param name="section.autolabel" select="1"></xsl:param>
<xsl:param name="section.autolabel.max.depth">3</xsl:param>
<xsl:param name="admon.graphics" select="1"></xsl:param>
<xsl:param name="admon.graphics.extension">.png</xsl:param>
<xsl:param name="admon.graphics.path">/jspui/doc/image/</xsl:param>
<xsl:param name="admon.textlabel" select="0"></xsl:param>

<xsl:attribute-set name="monospace.verbatim.properties">
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
    <xsl:attribute name="hyphenation-character">\</xsl:attribute>
</xsl:attribute-set>

</xsl:stylesheet>
