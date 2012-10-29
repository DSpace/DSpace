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
		<oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
			xmlns:dc="http://purl.org/dc/elements/1.1/" 
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
			xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
			
			<!--snrd.identifier.handle = identifier -->
			<xsl:for-each select="doc:metadata/doc:element[@name='snrd']/doc:element[@name='identifier']/doc:element[@name='handle']/doc:element/doc:field[@name='value']">
				<dc:identifier><xsl:value-of select="." /></dc:identifier>
			</xsl:for-each>
			
			<!--dc.identifier.uri = identifier -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
				<dc:identifier><xsl:value-of select="." /></dc:identifier>
			</xsl:for-each>
			
			<!--sedici.identifier.doi = identifier -->
			<!--sedici.identifier.other = NO VA -->
			<!--sedici.identifier.isbn = identifier -->
			<!--sedici.identifier.issn = identifier -->
			<!--sedici.identifier.expediente = NO VA -->
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='identifier']/doc:element[@name!='other' and @name!='expediente']/doc:element/doc:field[@name='value']">
				<dc:identifier><xsl:value-of select="../@name" />:<xsl:value-of select="." /></dc:identifier>
			</xsl:for-each>

			<!-- dc.title=dc.title+': '+sedici.title.subtitle -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
				<dc:title>
					<xsl:value-of select="." />
					<xsl:if test="position() = 1">
						<xsl:if test="../../../../doc:element[@name='sedici']/doc:element[@name='title']/doc:element[@name='subtitle']/doc:field[@name='value']">
							<xsl:text> : </xsl:text>
							<xsl:value-of select="../../../../doc:element[@name='sedici']/doc:element[@name='title']/doc:element[@name='subtitle']/doc:field[@name='value']" />
						</xsl:if>
					</xsl:if>
				</dc:title>
			</xsl:for-each>

			<!-- dc.title.alternative -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element[@name='alternative']/doc:element/doc:field[@name='value']">
				<dc:title><xsl:value-of select="." /></dc:title>
			</xsl:for-each>
			
			<!-- sedici.creator.(person|corporate)  = creator -->
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='creator']/doc:element[@name='person' or @name='corporate']/doc:element/doc:field[@name='value']">
				<dc:creator><xsl:value-of select="." /></dc:creator>
			</xsl:for-each>

			<!--sedici.date.exposure = date -->
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='date']/doc:element[@name='exposure']/doc:element/doc:field[@name='value']">
				<dc:date><xsl:value-of select="." /></dc:date>
			</xsl:for-each>
			
			<!--dc.date.issued  = date -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']">
				<dc:date><xsl:value-of select="." /></dc:date>
			</xsl:for-each>
			
			<!--dc.date.* = date -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name!='issued' and @name!='accessioned']/doc:element/doc:field[@name='value']">
				<dc:date><xsl:value-of select="." /></dc:date>
			</xsl:for-each>
			
			<!-- sedici.embargo.period -->
			<!-- NO VA -->
			
			
			<!--sedici.contributor.compiler = creator -->
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='contributor']/doc:element[@name='compiler']/doc:element/doc:field[@name='value']">
				<dc:creator><xsl:value-of select="." /></dc:creator>
			</xsl:for-each>
			
			<!--sedici.contributor.* = contributor -->
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='contributor']/doc:element[@name='director']/doc:element/doc:field[@name='value']">
				<dc:contributor><xsl:value-of select="." /></dc:contributor>
			</xsl:for-each>
			
			<!--sedici.contributor.* = contributor -->
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='contributor']/doc:element[@name!='compiler' and @name!='editor' and @name!='director']/doc:element/doc:field[@name='value']">
				<dc:contributor><xsl:value-of select="." /></dc:contributor>
			</xsl:for-each>
			
			<!--sedici.contributor.editor = publisher -->
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='contributor']/doc:element/doc:field[@name='editor']">
				<dc:publisher><xsl:value-of select="." /></dc:publisher>
			</xsl:for-each>

			<!--dc.publisher = publisher -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']">
				<dc:publisher><xsl:value-of select="." /></dc:publisher>
			</xsl:for-each>
			
			<!--  dc.coverage.(spatial|temporal) -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element/doc:element/doc:field[@name='value']">
				<dc:covarage><xsl:value-of select="." /></dc:covarage>
			</xsl:for-each>
			
			<!--dc.language = language -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:field[@name='value']">
				<dc:language><xsl:value-of select="." /></dc:language>
			</xsl:for-each>
			
			<!--sedici.subject.materias = subject -->
			<!--sedici.subject.lcsh = subject -->
			<!--sedici.subject.decs = subject -->
			<!--sedici.subject.eurovoc = subject -->
			<!--sedici.subject.descriptores = subject -->
			<!--sedici.subject.other = subject -->
			<!--sedici.subject.keyword = subject -->
			<!--sedici.subject.acmcss98 = subject -->
			<!-- Imprimo primero en espanol y luego en el resto de los idiomas -->
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='subject']/doc:element/doc:element[not(@name) or @name='es' ]/doc:field[@name='value']">
				<dc:subject><xsl:value-of select="." /></dc:subject>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='subject']/doc:element/doc:element[@name and @name!='es' ]/doc:field[@name='value']">
				<dc:subject><xsl:value-of select="." /></dc:subject>
			</xsl:for-each>
			
			<!--dc.description.abstract = description -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']">
				<dc:description><xsl:value-of select="." /></dc:description>
			</xsl:for-each>
			<!--sedici.note = description -->
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='note']/doc:element/doc:field[@name='value']">
				<dc:description><xsl:value-of select="." /></dc:description>
			</xsl:for-each>
			
			<!--sedici.fulltext -->
			<!--No lo pongo -->
			
			<!--eprints.status -->
			<!--No lo pongo -->
			
			<!--dc.description.provenance = description -->
			<!--No lo pongo -->
			
			<!-- sedici.institucionDesarrollo -->
			<!--No lo pongo -->
			
			<!-- thesis.degree.(name|grantor) =description -->
			<xsl:for-each select="doc:metadata/doc:element[@name='tesis']/doc:element[@name='degree']/doc:element[@name='name']/doc:element/doc:field[@name='value']">
				<dc:description><xsl:value-of select="." /></dc:description>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='tesis']/doc:element[@name='degree']/doc:element[@name='name']/doc:element/doc:field[@name='value']">
				<dc:description><xsl:value-of select="." /></dc:description>
			</xsl:for-each>
			
			<!-- mods.originInfo.place=description -->
			<xsl:for-each select="doc:metadata/doc:element[@name='mods']/doc:element[@name='originInfo']/doc:element[@name='place']/doc:element/doc:field[@name='value']">
				<dc:description><xsl:value-of select="." /></dc:description>
			</xsl:for-each>
			
			<!--snrd.type.driver = type -->
			<xsl:for-each select="doc:metadata/doc:element[@name='snrd']/doc:element[@name='type']/doc:element[@name='driver']/doc:element/doc:field[@name='value']">
				<dc:type><xsl:value-of select="." /></dc:type>
			</xsl:for-each>
 			
			<!--snrd.type.snrd = type -->
 			<xsl:for-each select="doc:metadata/doc:element[@name='snrd']/doc:element[@name='type']/doc:element[@name='snrd']/doc:element/doc:field[@name='value']">
				<dc:type><xsl:value-of select="." /></dc:type>
			</xsl:for-each>

			<!--snrd.type.version = type -->
 			<xsl:for-each select="doc:metadata/doc:element[@name='snrd']/doc:element[@name='type']/doc:element[@name='version']/doc:element/doc:field[@name='value']">
				<dc:type><xsl:value-of select="." /></dc:type>
			</xsl:for-each>
			
 			<!--dc.type = type -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']">
				<dc:type><xsl:value-of select="." /></dc:type>
			</xsl:for-each>
 			
 			<!--sedici.subtype = type -->
 			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='subtype']/doc:element/doc:field[@name='value']">
				<dc:type><xsl:value-of select="." /></dc:type>
			</xsl:for-each>

			<!-- AccessType de OpenAire opcional -->
			<!-- snrd.rights.accessRights = rights -->	
			<xsl:for-each select="doc:metadata/doc:element[@name='snrd']/doc:element[@name='rights']/doc:element[@name='accessRights']/doc:element/doc:field[@name='value']">
				<dc:rights><xsl:value-of select="." /></dc:rights>
			</xsl:for-each>
			
			
			<!-- snrd.rights.embargoEndDate = rights -->	
			<xsl:for-each select="doc:metadata/doc:element[@name='snrd']/doc:element[@name='rights']/doc:element[@name='embargoEndDate']/doc:element/doc:field[@name='value']">
				<dc:rights><xsl:value-of select="." /></dc:rights>
			</xsl:for-each>
			
			<!--sedici.rights.* = rights -->
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='rights']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
				<dc:rights><xsl:value-of select="." /></dc:rights>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='rights']/doc:element[@name='license']/doc:element/doc:field[@name='value']">
				<dc:rights><xsl:value-of select="." /></dc:rights>
			</xsl:for-each>
			
			<!--  bitstream.format --> 
 			<xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle' and doc:field[@name='name' and text()='ORIGINAL']]/doc:element[@name='bitstreams']/doc:element[@name='bitstream' and position()=1]/doc:field[@name='format']">
				<dc:format><xsl:value-of select="." /></dc:format>
			</xsl:for-each>
			
			<!--  dc.format.extent -->
 			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='format']/doc:element[@name='extent']/doc:element/doc:field[@name='value']">
				<dc:format><xsl:value-of select="." /></dc:format>
			</xsl:for-each>
			
			<!-- mods.location=dc.relation -->
			<xsl:for-each select="doc:metadata/doc:element[@name='mods']/doc:element[@name='location']/doc:element/doc:field[@name='value']">
				<dc:relation>
					<xsl:value-of select="." />
				</dc:relation>
			</xsl:for-each>
			
			<!--sedici.relation.(event|journalTitle|journalVolumeAndIssue|dossier) = relation -->
			<xsl:for-each select="doc:metadata/doc:element[@name='sedici']/doc:element[@name='relation']/doc:element/doc:element/doc:field[@name='value']">
				<dc:relation><xsl:value-of select="." /></dc:relation>
			</xsl:for-each>
			
			<!--mods.recordInfo.recordContentSource = source -->
			<xsl:for-each select="doc:metadata/doc:element[@name='mods']/doc:element[@name='recordInfo']/doc:element[@name='recordContentSource']/doc:element/doc:field[@name='value']">
				<dc:source><xsl:value-of select="." /></dc:source>
			</xsl:for-each>
			
			
		</oai_dc:dc>
	</xsl:template>
</xsl:stylesheet>
