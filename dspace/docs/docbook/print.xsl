<?xml version='1.0'?> 
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    version="1.0"> 

<xsl:import href="http://docbook.sourceforge.net/release/xsl/current/fo/docbook.xsl"/> 

<xsl:param name="body.start.indent">0pt</xsl:param>
<xsl:param name="header.column.widths">1 3 1</xsl:param>
<xsl:param name="toc.max.depth">4</xsl:param>
<xsl:param name="toc.section.depth">4</xsl:param>
<xsl:param name="section.autolabel" select="1"></xsl:param>
<xsl:param name="section.autolabel.max.depth">3</xsl:param>
<xsl:param name="section.label.includes.component.label" select="1"></xsl:param>
<xsl:param name="page.margin.inner">.75in</xsl:param>
<xsl:param name="page.margin.outer">.75in</xsl:param>
	
<xsl:param name="admon.graphics" select="1"></xsl:param>
<xsl:param name="admon.graphics.extension">.png</xsl:param>
<xsl:param name="admon.graphics.path">/dcbk/image/</xsl:param>
<xsl:param name="admon.textlabel" select="0"></xsl:param>

<xsl:template match="lineannotation">
  <fo:inline font-style="italic">
    <xsl:call-template name="inline.charseq"/>
  </fo:inline>
</xsl:template>

<xsl:attribute-set name="monospace.verbatim.properties">
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
    <xsl:attribute name="hyphenation-character">\</xsl:attribute>
</xsl:attribute-set>

</xsl:stylesheet>
