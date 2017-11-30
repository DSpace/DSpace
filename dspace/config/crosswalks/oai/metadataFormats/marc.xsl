<?xml version="1.0" encoding="UTF-8" ?>
<!-- 


    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/
	Developed by DSpace @ Lyncode <dspace@lyncode.com>
	
	> http://www.loc.gov/marc/bibliographic/ecbdlist.html

 -->
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:doc="http://www.lyncode.com/xoai"
	version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />
	
	<xsl:template match="/">
		<record xmlns="http://www.loc.gov/MARC21/slim" 
			xmlns:dcterms="http://purl.org/dc/terms/"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd">
			<leader>00925njm 22002777a 4500</leader>
			<datafield ind2=" " ind1=" " tag="042">
				<subfield code="a">dc</subfield>
			</datafield>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
			<datafield ind2=" " ind1=" " tag="720">
				<subfield code="a"><xsl:value-of select="." /></subfield>
				<subfield code="e">author</subfield>
			</datafield>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']">
			<datafield ind2=" " ind1=" " tag="260">
				<subfield code="c"><xsl:value-of select="." /></subfield>
			</datafield>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']">
			<datafield ind2=" " ind1=" " tag="520">
				<subfield code="a"><xsl:value-of select="." /></subfield>
			</datafield>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element/doc:element/doc:field[@name='value']">
			<datafield ind1="8" ind2=" " tag="024">
				<subfield code="a"><xsl:value-of select="." /></subfield>
			</datafield>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
			<datafield tag="653" ind2=" " ind1=" " >
				<subfield code="a"><xsl:value-of select="." /></subfield>
			</datafield>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
			<datafield ind2="0" ind1="0" tag="245">
				<subfield code="a"><xsl:value-of select="." /></subfield>
			</datafield>
			</xsl:for-each>
		</record>
	</xsl:template>
</xsl:stylesheet>
