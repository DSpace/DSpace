<?xml version="1.0" encoding="UTF-8" ?>

<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:doc="http://www.lyncode.com/xoai"
	version="1.0"
	exclude-result-prefixes="doc"
	>
<!-- 	xmlns:doaj="http://doaj.org/" xmlns:xsi="http://doaj.org/XMLSchema-instance" xmlns:schemaLocation="http://doaj.org/static/doaj/doajArticles.xsd" -->
	
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />
	
<xsl:template match="/doc:metadata">
	<record xmlns="http://doaj.org/">
		<!-- language :: dc.language  -->
		<xsl:if test="doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:field/text()">	
			<language>
				<xsl:value-of select="doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:field[@name='value']/text()"/>
			</language>
		</xsl:if>
		<!-- publisher :: mods.originInfo.place  -->
		<xsl:if test="doc:element[@name='mods']/doc:element[@name='originInfo']/doc:element[@name='place']/doc:element/doc:field/text()">	
			<publisher>
				<xsl:value-of select="doc:element[@name='mods']/doc:element[@name='originInfo']/doc:element[@name='place']/doc:element/doc:field/text()"/>
			</publisher>
		</xsl:if>
		<!-- jounalTitle :: sedici.relation.journalTitle-->
		<journalTitle>
			<xsl:value-of select="doc:element[@name='sedici']/doc:element[@name='relation']/doc:element[@name='journalTitle']/doc:element/doc:field[@name='value']/text()"/>
		</journalTitle>
		<!-- issn :: sedici.identifier.issn -->
		<xsl:if test="doc:element[@name='sedici']/doc:element[@name='identifier']/doc:element[@name='issn']/doc:element/doc:field/text()">	
			<issn>
				<xsl:value-of select="doc:element[@name='sedici']/doc:element[@name='identifier']/doc:element[@name='issn']/doc:element/doc:field/text()"/>
			</issn>
		</xsl:if>
		<!-- publicationDate :: dc.date.issued -->
		<publicationDate>
			<xsl:value-of select="doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"/>
		</publicationDate>
		<!-- volume :: sedici.relation.journalVolumeAndIssue -->
		<xsl:if test="doc:element[@name='sedici']/doc:element[@name='relation']/doc:element[@name='journalVolumeAndIssue']/doc:element/doc:field/text()">	
			<volume>
				<xsl:value-of select="doc:element[@name='sedici']/doc:element[@name='relation']/doc:element[@name='journalVolumeAndIssue']/doc:element/doc:field/text()"/>
			</volume>
		</xsl:if>
		<!-- doi :: sedici.identifier.doi -->
		<xsl:if test="doc:element[@name='sedici']/doc:element[@name='identifier']/doc:element[@name='doi']/doc:element/doc:field/text()">
			<doi>
				<xsl:value-of select="doc:element[@name='sedici']/doc:element[@name='identifier']/doc:element[@name='doi']/doc:element/doc:field/text()"/>
			</doi>
		</xsl:if>
		<!-- publisherRecordId -->
		<publisherRecordId>
			<xsl:value-of select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field/text()"/>
		</publisherRecordId>
		<!-- documentType :: sedici.subtype -->
		<documentType>
			<xsl:value-of select="doc:element[@name='sedici']/doc:element[@name='subtype']/doc:element/doc:field[@name='value']/text()"/>
		</documentType>
		<!-- title :: dc.title[.alternative] -->
		<title>
			<xsl:if test="doc:element[@name='dc']/doc:element[@name='title']/doc:element[@name]">
				<xsl:attribute name="language">
					<xsl:value-of select="doc:element[@name='dc']/doc:element[@name='title']/doc:element/@name"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:value-of select="doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']/text()"/>
		</title>
		<xsl:for-each select="doc:element[@name='dc']/doc:element[@name='title']/doc:element[@name='alternative']/doc:element[@name]">
			<title>
				<xsl:if test="./doc:field">
					<xsl:attribute name="language">
						<xsl:value-of select="./@name"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:value-of select="./doc:field[@name='value']/text()"/>
			</title>
		</xsl:for-each>
		<!-- authors :: sedici.creator.person|corporate|interprete, sedici.contributor.compiler|coordinator|editor|translator -->
		<xsl:if test="doc:element[@name='sedici']/doc:element[@name='creator']/doc:element[@name='person' or @name='corporate' or @name='interprete']/doc:element/doc:field[@name='value'] or doc:element[@name='contributor']/doc:element[@name='compiler' or @name='coordinator' or @name='editor' or @name='translator']">	
			<authors>
				<xsl:for-each select="doc:element[@name='sedici']/doc:element[@name='creator']/doc:element[@name='person' or @name='corporate' or @name='interprete']/doc:element/doc:field[@name='value']">	
					<author>
						<name><xsl:value-of select="./text()"/></name>
					</author>
				</xsl:for-each>
				<xsl:for-each select="doc:element[@name='contributor']/doc:element[@name='compiler' or @name='coordinator' or @name='editor' or @name='translator']/doc:element/doc:field[@name='value']">	
					<author>
						<name><xsl:value-of select="./text()"/></name>
					</author>
				</xsl:for-each>
			</authors>
		</xsl:if>
		<!-- Abstract :: dc.description.abstract -->
		<xsl:for-each select="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']">
			<abstract>
				<xsl:attribute name="language">
					<xsl:value-of select="../@name"/>
				</xsl:attribute>
				<xsl:value-of select="./text()"/>
			</abstract>
		</xsl:for-each>
		<!-- fullTextUrl :: sedici.identifier.uri -->
		<!-- Si no existe bitstream en BUNDLE ORINGINAL, se crea un 'format' application/pdf, ya que es un atributo obligatorio. -->
		<fullTextUrl>
			<xsl:attribute name="format">
				<xsl:choose>
					<xsl:when test="doc:element[@name='bundles']/doc:element[@name='bundle' and doc:field[@name='name' and text()='ORIGINAL']]/doc:element[@name='bitstreams']/doc:element[@name='bitstream' and position()=1]/doc:field[@name='format']">
						<xsl:value-of select="doc:element[@name='bundles']/doc:element[@name='bundle' and doc:field[@name='name' and text()='ORIGINAL']]/doc:element[@name='bitstreams']/doc:element[@name='bitstream' and position()=1]/doc:field[@name='format']"/>
					</xsl:when>
					<xsl:otherwise>application/pdf</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
<!-- 			<xsl:if test="doc:element[@name='bundles']/doc:element[@name='bundle' and doc:field[@name='name' and text()='ORIGINAL']]/doc:element[@name='bitstreams']/doc:element[@name='bitstream' and position()=1]/doc:field[@name='format']"> -->
<!-- 				<xsl:attribute name="format"> -->
<!-- 					<xsl:value-of select="doc:element[@name='bundles']/doc:element[@name='bundle' and doc:field[@name='name' and text()='ORIGINAL']]/doc:element[@name='bitstreams']/doc:element[@name='bitstream' and position()=1]/doc:field[@name='format']"/> -->
<!-- 				</xsl:attribute> -->
<!-- 			</xsl:if> -->
			<xsl:value-of select="doc:element[@name='doaj']/doc:element[@name='identifier']/doc:element[@name='handle']/doc:element/doc:field[@name='value']/text()"/>
		</fullTextUrl>

		<!-- keywords :: dc.subject -->
		<xsl:for-each select="doc:element[@name='dc']/doc:element[@name='subject']">
			<keywords>
				<xsl:attribute name="language">
					<xsl:value-of select="./doc:element[@name]/@name"/>
				</xsl:attribute>
				<xsl:for-each select="./doc:element[@name]/doc:field[@name='value']">
					<keyword><xsl:value-of select="./text()"/></keyword>
				</xsl:for-each>
			</keywords>
		</xsl:for-each>

		<!-- keywords :: sedici.subject.(materias) -->
		<xsl:for-each select="doc:element[@name='sedici']/doc:element[@name='subject']/doc:element[@name='materias']">
			<keywords>
				<xsl:attribute name="language">
					<xsl:value-of select="./doc:element[@name]/@name"/>
				</xsl:attribute>
				<xsl:for-each select="./doc:element[@name]/doc:field[@name='value']">
					<keyword><xsl:value-of select="./text()"/></keyword>
				</xsl:for-each>
			</keywords>
		</xsl:for-each>
	</record>
</xsl:template>


</xsl:stylesheet>