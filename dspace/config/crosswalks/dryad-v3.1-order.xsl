<?xml version="1.0" encoding="utf-8"?>

<!--
	This is a workaround for the fact that you can't have unordered lists with
	occurrence restrictions in XSD (either xs:sequence or xs:all)... this only
	needed until we do a RELAX NG schema version, which does support unordered
	
	schemaLocation added in this XSL rather than first since order is added here
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:bibo="http://purl.org/ontology/bibo/" xmlns:dcterms="http://purl.org/dc/terms/"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:dwc="http://rs.tdwg.org/dwc/terms/"
	xmlns="http://purl.org/dryad/terms/" xmlns:dryad="http://purl.org/dryad/schema/terms/v3.1" version="1.0">

	<xsl:template match="dryad:DryadDataFile">
		<xsl:copy>
			<xsl:attribute name="xsi:schemaLocation"
				>http://purl.org/dryad/terms/<xsl:text> </xsl:text>http://datadryad.org/profile/v3.1/dryad.xsd</xsl:attribute>
			<xsl:apply-templates select="dcterms:type" mode="copy"/>
			<xsl:apply-templates select="dcterms:creator" mode="copy"/>
			<xsl:apply-templates select="dcterms:title" mode="copy"/>
			<xsl:apply-templates select="dcterms:identifier" mode="copy"/>
			<xsl:apply-templates select="dcterms:rights" mode="copy"/>
			<xsl:apply-templates select="dcterms:description" mode="copy"/>
			<xsl:apply-templates select="dcterms:subject" mode="copy"/>
			<xsl:apply-templates select="dwc:scientificName" mode="copy"/>
			<xsl:apply-templates select="dcterms:spatial" mode="copy"/>
			<xsl:apply-templates select="dcterms:temporal" mode="copy"/>
			<xsl:apply-templates select="dcterms:dateSubmitted" mode="copy"/>
			<xsl:apply-templates select="dcterms:available" mode="copy"/>
			<xsl:apply-templates select="dryad:embargoedUntil" mode="copy"/>
			<xsl:apply-templates select="dcterms:format" mode="copy"/>
			<xsl:apply-templates select="dcterms:extent" mode="copy"/>
			<xsl:apply-templates select="dcterms:provenance" mode="copy"/>
			<xsl:apply-templates select="dcterms:isPartOf" mode="copy"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="dryad:DryadDataPackage">
		<xsl:copy>
			<xsl:attribute name="xsi:schemaLocation"
				>http://purl.org/dryad/terms/<xsl:text> </xsl:text>http://datadryad.org/profile/v3.1/dryad.xsd</xsl:attribute>
			<xsl:apply-templates select="dcterms:type" mode="copy"/>
			<xsl:apply-templates select="dcterms:creator" mode="copy"/>
			<xsl:apply-templates select="dcterms:dateSubmitted" mode="copy"/>
			<xsl:apply-templates select="dcterms:available" mode="copy"/>
			<xsl:apply-templates select="dcterms:title" mode="copy"/>
			<xsl:apply-templates select="dcterms:identifier" mode="copy"/>
			<xsl:apply-templates select="dcterms:description" mode="copy"/>
			<xsl:apply-templates select="dcterms:subject" mode="copy"/>
			<xsl:apply-templates select="dwc:scientificName" mode="copy"/>
			<xsl:apply-templates select="dcterms:spatial" mode="copy"/>
			<xsl:apply-templates select="dcterms:temporal" mode="copy"/>
			<xsl:apply-templates select="dryad:external" mode="copy"/>
			<xsl:apply-templates select="dcterms:relation" mode="copy"/>
			<xsl:apply-templates select="dcterms:references" mode="copy"/>
			<xsl:apply-templates select="bibo:pmid" mode="copy"/>
			<xsl:apply-templates select="bibo:Journal" mode="copy"/>
			<xsl:apply-templates select="dcterms:hasPart" mode="copy"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="*" mode="copy">
		<xsl:copy>
			<xsl:value-of select="."/>
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>
