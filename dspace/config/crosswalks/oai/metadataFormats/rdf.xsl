<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:doc="http://www.lyncode.com/xoai"
	version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />
	
	<xsl:template match="/">
		<rdf:RDF xmlns:rdf="http://www.openarchives.org/OAI/2.0/rdf/"
			xmlns:ow="http://www.ontoweb.org/ontology/1#" xmlns:dc="http://purl.org/dc/elements/1.1/"
			xmlns:ds="http://dspace.org/ds/elements/1.1/"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/rdf/ http://www.openarchives.org/OAI/2.0/rdf.xsd">
			<ow:Publication>
				<xsl:if test="doc:metadata/doc:element[@name='others']/doc:field[@name='identifier']">
					<xsl:attribute name="rdf:about">
						<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='identifier']/text()"></xsl:value-of>
					</xsl:attribute>
				</xsl:if>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
				<dc:title><xsl:value-of select="." /></dc:title>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
					<dc:creator><xsl:value-of select="." /></dc:creator>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name!='author']/doc:element/doc:field[@name='value']">
					<dc:contributor><xsl:value-of select="." /></dc:contributor>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
					<dc:subject><xsl:value-of select="." /></dc:subject>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value']">
					<dc:description><xsl:value-of select="." /></dc:description>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']">
					<dc:description><xsl:value-of select="." /></dc:description>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element/doc:element/doc:field[@name='value']">
					<dc:date><xsl:value-of select="." /></dc:date>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']">
					<dc:type><xsl:value-of select="." /></dc:type>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element/doc:element/doc:field[@name='value']">
					<dc:identifier><xsl:value-of select="." /></dc:identifier>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:element/doc:field[@name='value']">
					<dc:language><xsl:value-of select="." /></dc:language>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:element/doc:field[@name='value']">
					<dc:relation><xsl:value-of select="." /></dc:relation>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field[@name='value']">
					<dc:relation><xsl:value-of select="." /></dc:relation>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:element/doc:field[@name='value']">
					<dc:rights><xsl:value-of select="." /></dc:rights>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value']">
					<dc:rights><xsl:value-of select="." /></dc:rights>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='bitstreams']/doc:element[@name='bitstream']/doc:field[@name='format']">
					<dc:format><xsl:value-of select="." /></dc:format>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element/doc:field[@name='value']">
					<dc:coverage><xsl:value-of select="." /></dc:coverage>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element/doc:element/doc:field[@name='value']">
					<dc:coverage><xsl:value-of select="." /></dc:coverage>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']">
					<dc:publisher><xsl:value-of select="." /></dc:publisher>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:element/doc:field[@name='value']">
					<dc:publisher><xsl:value-of select="." /></dc:publisher>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element/doc:field[@name='value']">
					<dc:source><xsl:value-of select="." /></dc:source>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element/doc:element/doc:field[@name='value']">
					<dc:source><xsl:value-of select="." /></dc:source>
				</xsl:for-each>
				<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='peerreviewed']">
					<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='peerreviewed']/doc:element/doc:element/doc:field[@name='value']">
						<dc:peerreviewed><xsl:value-of select="." /></dc:peerreviewed>
					</xsl:for-each>
					<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='peerreviewed']/doc:element/doc:field[@name='value']">
						<dc:peerreviewed><xsl:value-of select="." /></dc:peerreviewed>
					</xsl:for-each>
				</xsl:if>
			</ow:Publication>
		</rdf:RDF>
	</xsl:template>
</xsl:stylesheet>
