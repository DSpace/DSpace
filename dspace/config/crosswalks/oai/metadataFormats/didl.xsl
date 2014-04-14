<?xml version="1.0" encoding="UTF-8" ?>
<!-- 


    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/
	Developed by DSpace @ Lyncode <dspace@lyncode.com>
	
	> http://www.openarchives.org/OAI/2.0/oai_dc.xsd

 -->
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:doc="http://www.lyncode.com/xoai"
	version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />
	
	<xsl:template match="/">
		<d:DIDL xmlns:d="urn:mpeg:mpeg21:2002:02-DIDL-NS" 
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="urn:mpeg:mpeg21:2002:02-DIDL-NS http://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-21_schema_files/did/didl.xsd">
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']/doc:element/doc:field[@name='value']">
			<d:DIDLInfo>
				<dcterms:created xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/dcterms.xsd">
					<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']/doc:element/doc:field/text()" />
				</dcterms:created>
			</d:DIDLInfo>
			</xsl:if>
			<d:Item>
				<xsl:attribute name="id">
					<xsl:value-of select="concat('hdl_', translate(doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(),'/','_'))" />
				</xsl:attribute>
				<d:Descriptor>
					<d:Statement mimeType="application/xml; charset=utf-8">
						<dii:Identifier xmlns:dii="urn:mpeg:mpeg21:2002:01-DII-NS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:mpeg:mpeg21:2002:01-DII-NS http://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-21_schema_files/dii/dii.xsd">urn:hdl:<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()" /></dii:Identifier>
					</d:Statement>
				</d:Descriptor>
				<d:Descriptor>
					<d:Statement mimeType="application/xml; charset=utf-8">
						<oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
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
							<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='covarage']/doc:element/doc:field[@name='value']">
								<dc:covarage><xsl:value-of select="." /></dc:covarage>
							</xsl:for-each>
							<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='covarage']/doc:element/doc:element/doc:field[@name='value']">
								<dc:covarage><xsl:value-of select="." /></dc:covarage>
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
						</oai_dc:dc>
					</d:Statement>
				</d:Descriptor>
				<xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']">
					<xsl:if test="doc:field[@name='name']/text() = 'ORIGINAL'">
						<xsl:for-each select="doc:element[@name='bitstreams']/doc:element">
							<d:Component>
								<xsl:attribute name="id">
									<xsl:value-of select="translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(),'/','_')"></xsl:value-of>_<xsl:value-of select="doc:field[@name='sid']/text()"></xsl:value-of>
								</xsl:attribute>
								<d:Resource>
									<xsl:attribute name="ref">
										<xsl:value-of select="doc:field[@name='url']/text()"></xsl:value-of>
									</xsl:attribute>
									<xsl:attribute name="mimeType">
										<xsl:value-of select="doc:field[@name='format']/text()"></xsl:value-of>
									</xsl:attribute>
								</d:Resource>
							</d:Component>
						</xsl:for-each>
					</xsl:if>
				</xsl:for-each>
			</d:Item>			
		</d:DIDL>
	</xsl:template>
</xsl:stylesheet>
