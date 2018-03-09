<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:cc="http://creativecommons.org/ns#"
	xmlns:old-cc="http://web.resource.org/cc/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	exclude-result-prefixes="old-cc">

	<xsl:output method="xml" indent="yes" />


	<xsl:template match="/">
		<xsl:apply-templates select="result/rdf/rdf:RDF" />
	</xsl:template>

	<!-- process incoming RDF, copy everything add our own statements for cc:Work -->
	<xsl:template match="result/rdf/rdf:RDF">
		<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:cc="http://creativecommons.org/ns#">
			<xsl:copy-of select="@*" />
			<xsl:apply-templates select="cc:License" />
		</rdf:RDF>
	</xsl:template>

	<!-- handle License element -->
	<xsl:template match="cc:License">
		<cc:Work rdf:about="">
			<cc:license rdf:resource="{@rdf:about}" />
		</cc:Work>
		<cc:License>
			<xsl:copy-of select="@*" />
			<xsl:apply-templates select="node()" />
		</cc:License>
	</xsl:template>

	<!-- Identity transform -->
	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>