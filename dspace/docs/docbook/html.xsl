<?xml version='1.0'?>
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:import href="http://docbook.sourceforge.net/release/xsl/current/html/chunk.xsl"/>

<!-- <xsl:param name="html.stylesheet" select="'corpstyle.css'"/>
<xsl:param name="admon.graphics" select="1"/> -->

<!--  footer xsl stuff?
(could use          user.footer.navigational too)
-->
<xsl:template name="user.footer.content">
  <HR/>
  <xsl:apply-templates select="//copyright[1]" mode="titlepage.mode"/>
  <xsl:apply-templates select="//legalnotice[1]" mode="titlepage.mode"/>
</xsl:template>


<xsl:attribute-set name="monospace.verbatim.properties">
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
    <xsl:attribute name="hyphenation-character">\</xsl:attribute>
</xsl:attribute-set>

</xsl:stylesheet>
