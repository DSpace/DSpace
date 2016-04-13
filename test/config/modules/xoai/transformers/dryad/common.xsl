<?xml version="1.0" encoding="UTF-8"?>
<!-- 

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

	Developed by DSpace @ Lyncode <dspace@lyncode.com> 
	Following OpenAIRE Guidelines 1.1:
		- http://www.openaire.eu/component/content/article/207

 -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:doc="http://www.lyncode.com/xoai"
>

  <!-- 
    Default template to match dc.identifier, so that the override below is applied.
  -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

	<!-- 
	 Dryad override: suppress handles
	-->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']" priority="10"/>
	<xsl:template match="/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']" priority="10"/>
  
  <!-- 
    Wrap dc.identifier element for DOIs with an additional <element>, so that <field> will be at expected
    depth. 
    TODO?: debug xoai solr indexing of dois.
  -->
  <xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='none'][starts-with(string(child::doc:field),'doi:')]" priority="10">
    <xsl:copy>
      <xsl:attribute name="name">doi</xsl:attribute>
      <xsl:apply-templates select="@*" />
        <doc:element name="none">
          <xsl:apply-templates select="node()" />
        </doc:element>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
