<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:doc="http://www.lyncode.com/xoai"
	version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />
	
	<xsl:template match="/">
		<atom:entry xmlns:atom="http://www.w3.org/2005/Atom" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xmlns:ore="http://www.openarchives.org/ore/terms/" xmlns:oreatom="http://www.openarchives.org/ore/atom/"
			xmlns:dcterms="http://purl.org/dc/terms/" xsi:schemaLocation="http://www.w3.org/2005/Atom http://www.kbcafe.com/rss/atom.xsd.xml">
			<atom:id>
				<xsl:value-of select="concat(doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']/text(), '/ore.xml')"></xsl:value-of>
			</atom:id>
			<atom:link rel="alternate">
				<xsl:attribute name="href">
					<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
				</xsl:attribute>
			</atom:link>
			<atom:link rel="http://www.openarchives.org/ore/terms/describes">
				<xsl:attribute name="href">
					<xsl:value-of select="concat(doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']/text(), '/ore.xml')"></xsl:value-of>
				</xsl:attribute>
			</atom:link>
			<atom:link rel="self" type="application/atom+xml">
				<xsl:attribute name="href">
					<xsl:value-of select="concat(doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']/text(), '/ore.xml#atom')"></xsl:value-of>
				</xsl:attribute>
			</atom:link>
			<atom:published>
				<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
			</atom:published>
			<atom:updated>
				<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
			</atom:updated>
			<atom:source>
				<atom:generator>
					<xsl:value-of select="doc:metadata/doc:element[@name='repository']/doc:field[@name='name']/text()"></xsl:value-of>
				</atom:generator>
			</atom:source>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
			<atom:title><xsl:value-of select="." /></atom:title>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
			<atom:author><atom:name><xsl:value-of select="." /></atom:name></atom:author>
			</xsl:for-each>
			<atom:category scheme="http://www.openarchives.org/ore/terms/"
				term="http://www.openarchives.org/ore/terms/Aggregation" label="Aggregation" />
			<atom:category scheme="http://www.openarchives.org/ore/atom/modified">
				<xsl:attribute name="term">
					<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
				</xsl:attribute>
			</atom:category>
			<atom:category scheme="http://www.dspace.org/objectModel/" term="DSpaceItem" label="DSpace Item" />
			<xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']">
				<xsl:if test="doc:field[@name='name']/text() = 'ORIGINAL'">
					<xsl:for-each select="doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
						<atom:link rel="http://www.openarchives.org/ore/terms/aggregates">
							<xsl:attribute name="href">
								<xsl:value-of select="doc:field[@name='url']/text()"></xsl:value-of>
							</xsl:attribute>
							<xsl:attribute name="title">
								<xsl:value-of select="doc:field[@name='name']/text()"></xsl:value-of>
							</xsl:attribute>
							<xsl:attribute name="type">
								<xsl:value-of select="doc:field[@name='format']/text()"></xsl:value-of>
							</xsl:attribute>
							<xsl:attribute name="length">
								<xsl:value-of select="doc:field[@name='size']/text()"></xsl:value-of>
							</xsl:attribute>
						</atom:link>
					</xsl:for-each>
				</xsl:if>
			</xsl:for-each>
			<oreatom:triples>
				<rdf:Description xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
					<xsl:attribute name="rdf:about">
						<xsl:value-of select="concat(doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']/text(), '/ore.xml#atom')"></xsl:value-of>
					</xsl:attribute>
					<rdf:type rdf:resource="http://www.dspace.org/objectModel/DSpaceItem" />
					<dcterms:modified>
						<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
					</dcterms:modified>
				</rdf:Description>
				<xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']/doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
					<rdf:Description xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
						<xsl:attribute name="rdf:about">
							<xsl:value-of select="doc:field[@name='url']/text()"></xsl:value-of>
						</xsl:attribute>
						<rdf:type rdf:resource="http://www.dspace.org/objectModel/DSpaceBitstream" />
						<dcterms:description>
							<xsl:value-of select="../../doc:field[@name='name']/text()"></xsl:value-of>
						</dcterms:description>
					</rdf:Description>
				</xsl:for-each>
			</oreatom:triples>
		</atom:entry>
	</xsl:template>
</xsl:stylesheet>
