<?xml version="1.0" encoding="UTF-8" ?>
<!-- http://www.loc.gov/standards/mets/mets.xsd -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:doc="http://www.lyncode.com/xoai" version="2.0">
    
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

	<xsl:template match="/">
		<mets xmlns="http://www.loc.gov/METS/" xmlns:xlink="http://www.w3.org/1999/xlink"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ID="DSpace_ITEM_10400.7-31"
			TYPE="DSpace ITEM" PROFILE="DSpace METS SIP Profile 1.0"
			xsi:schemaLocation="http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd">
			<xsl:attribute name="OBJID">
				hdl:<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()"></xsl:value-of>
			</xsl:attribute>
			<xsl:attribute name="ID">
				DSpace_ITEM_<xsl:value-of select="translate(doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(),'/','-')"></xsl:value-of>
			</xsl:attribute>
			<metsHdr>
				<xsl:attribute name="CREATEDATE">
					<xsl:value-of select="concat(format-date(current-date(), '[Y0001]-[M02]-[D02]'), 'T' , format-time(current-time(), '[H01]:[m01]:[s01]'), 'Z')"/>
				</xsl:attribute>
				<agent ROLE="CUSTODIAN" TYPE="ORGANIZATION">
					<name><xsl:value-of select="doc:metadata/doc:element[@name='repository']/doc:field[@name='name']/text()" /></name>
				</agent>
			</metsHdr>
			<dmdSec>
				<xsl:attribute name="ID">
					<xsl:value-of select="concat('DMD_', translate(doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'))" />
				</xsl:attribute>
				<mdWrap MDTYPE="MODS">
					<xmlData xmlns:mods="http://www.loc.gov/mods/v3"
						xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-1.xsd">
						<mods:mods
							xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-1.xsd">
							<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element">
								<xsl:for-each select="doc:element/doc:field[@name='value']">
								<mods:name>
									<mods:role>
										<mods:roleTerm type="text"><xsl:value-of select="../../@name" /></mods:roleTerm>
									</mods:role>
									<mods:namePart><xsl:value-of select="text()" /></mods:namePart>
								</mods:name>
								</xsl:for-each>
							</xsl:for-each>
							<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned']">
							<mods:extension>
								<mods:dateAccessioned encoding="iso8601">
									<xsl:value-of select="doc:element/doc:field[@name='value']/text()" />
								</mods:dateAccessioned>
							</mods:extension>
							</xsl:for-each>
							<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']">
							<mods:extension>
								<mods:dateAvailable encoding="iso8601">
									<xsl:value-of select="doc:element/doc:field[@name='value']/text()" />
								</mods:dateAvailable>
							</mods:extension>
							</xsl:for-each>
							<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']">
							<mods:originInfo>
								<mods:dateIssued encoding="iso8601">
									<xsl:value-of select="doc:element/doc:field[@name='value']/text()" />
								</mods:dateIssued>
							</mods:originInfo>
							</xsl:for-each>
							<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element">
							<mods:identifier>
								<xsl:attribute name="type">
									<xsl:value-of select="@name"></xsl:value-of>
								</xsl:attribute>
								<xsl:value-of select="doc:element/doc:field[@name='value']/text()" />
							</mods:identifier>
							</xsl:for-each>
							<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element">
							<mods:abstract>
								<xsl:value-of select="doc:field[@name='value']/text()" />
							</mods:abstract>
							</xsl:for-each>
							<mods:language>
								<mods:languageTerm authority="rfc3066"><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:element/doc:field[@name='value']"></xsl:value-of></mods:languageTerm>
							</mods:language>
							<mods:accessCondition type="useAndReproduction"><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value']"></xsl:value-of></mods:accessCondition>
							<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
							<mods:subject>
								<mods:topic><xsl:value-of select="text()" /></mods:topic>
							</mods:subject>
							</xsl:for-each>
							<mods:titleInfo>
								<mods:title><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']"></xsl:value-of></mods:title>
							</mods:titleInfo>
							<mods:genre><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']"></xsl:value-of></mods:genre>
						</mods:mods>
					</xmlData>
				</mdWrap>
			</dmdSec>
			<xsl:if test="doc:metadata/doc:element[@name='license']/doc:field[@name='bin']">
			<amdSec>
				<xsl:attribute name="ID">
					<xsl:value-of select="concat('TMD_', translate(doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'))" />
				</xsl:attribute>
				<rightsMD>
					<xsl:attribute name="ID">
						<xsl:value-of select="concat('RIG_', translate(doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'))" />
					</xsl:attribute>
					<mdWrap MIMETYPE="text/plain" MDTYPE="OTHER" OTHERMDTYPE="DSpaceDepositLicense">
						<binData><xsl:value-of select="doc:metadata/doc:element[@name='license']/doc:field[@name='bin']/text()" /></binData>
					</mdWrap>
				</rightsMD>
			</amdSec>
			</xsl:if>
			<xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element/doc:field[text()='ORIGINAL']">
			<xsl:for-each select="../doc:element[@name='bitstreams']/doc:element">
			<amdSec>
				<xsl:attribute name="ID">
					<xsl:value-of select="concat('FO_', translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'), '_', doc:field[@name='sid']/text())" />
				</xsl:attribute>
				<techMD>
					<xsl:attribute name="ID">
						<xsl:value-of select="concat('TECH_O_', translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'), '_', doc:field[@name='sid']/text())" />
					</xsl:attribute>
					<mdWrap MDTYPE="PREMIS">
						<xmlData xmlns:premis="http://www.loc.gov/standards/premis"
							xsi:schemaLocation="http://www.loc.gov/standards/premis http://www.loc.gov/standards/premis/PREMIS-v1-0.xsd">
							<premis:premis>
								<premis:object>
									<premis:objectIdentifier>
										<premis:objectIdentifierType>URL</premis:objectIdentifierType>
										<premis:objectIdentifierValue><xsl:value-of select="doc:field[@name='url']/text()" /></premis:objectIdentifierValue>
									</premis:objectIdentifier>
									<premis:objectCategory>File</premis:objectCategory>
									<premis:objectCharacteristics>
										<premis:fixity>
											<premis:messageDigestAlgorithm><xsl:value-of select="doc:field[@name='checksumAlgorithm']/text()" /></premis:messageDigestAlgorithm>
											<premis:messageDigest><xsl:value-of select="doc:field[@name='checksum']/text()" /></premis:messageDigest>
										</premis:fixity>
										<premis:size><xsl:value-of select="doc:field[@name='size']/text()" /></premis:size>
										<premis:format>
											<premis:formatDesignation>
												<premis:formatName><xsl:value-of select="doc:field[@name='format']/text()" /></premis:formatName>
											</premis:formatDesignation>
										</premis:format>
									</premis:objectCharacteristics>
									<premis:originalName><xsl:value-of select="doc:field[@name='name']/text()" /></premis:originalName>
								</premis:object>
							</premis:premis>
						</xmlData>
					</mdWrap>
				</techMD>
			</amdSec>
			</xsl:for-each>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element/doc:field[text()='TEXT']">
			<xsl:for-each select="../doc:element[@name='bitstreams']/doc:element">
			<amdSec>
				<xsl:attribute name="ID">
					<xsl:value-of select="concat('FT_', translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'), '_', doc:field[@name='sid']/text())" />
				</xsl:attribute>
				<techMD>
					<xsl:attribute name="ID">
						<xsl:value-of select="concat('TECH_T_', translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'), '_', doc:field[@name='sid']/text())" />
					</xsl:attribute>
					<mdWrap MDTYPE="PREMIS">
						<xmlData xmlns:premis="http://www.loc.gov/standards/premis"
							xsi:schemaLocation="http://www.loc.gov/standards/premis http://www.loc.gov/standards/premis/PREMIS-v1-0.xsd">
							<premis:premis>
								<premis:object>
									<premis:objectIdentifier>
										<premis:objectIdentifierType>URL</premis:objectIdentifierType>
										<premis:objectIdentifierValue><xsl:value-of select="doc:field[@name='url']/text()" /></premis:objectIdentifierValue>
									</premis:objectIdentifier>
									<premis:objectCategory>File</premis:objectCategory>
									<premis:objectCharacteristics>
										<premis:fixity>
											<premis:messageDigestAlgorithm><xsl:value-of select="doc:field[@name='checksumAlgorithm']/text()" /></premis:messageDigestAlgorithm>
											<premis:messageDigest><xsl:value-of select="doc:field[@name='checksum']/text()" /></premis:messageDigest>
										</premis:fixity>
										<premis:size><xsl:value-of select="doc:field[@name='size']/text()" /></premis:size>
										<premis:format>
											<premis:formatDesignation>
												<premis:formatName><xsl:value-of select="doc:field[@name='format']/text()" /></premis:formatName>
											</premis:formatDesignation>
										</premis:format>
									</premis:objectCharacteristics>
									<premis:originalName><xsl:value-of select="doc:field[@name='name']/text()" /></premis:originalName>
								</premis:object>
							</premis:premis>
						</xmlData>
					</mdWrap>
				</techMD>
			</amdSec>
			</xsl:for-each>
			</xsl:for-each>
			<xsl:if test="count(doc:metadata/doc:element[@name='bundles']/doc:element/doc:field[text()='ORIGINAL']) &gt; 0">
			<fileSec>
				<xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element/doc:field[text()='ORIGINAL']">
				<fileGrp USE="ORIGINAL">
				<xsl:for-each select="../doc:element[@name='bitstreams']/doc:element">
					<file>
						<xsl:attribute name="ID">
							<xsl:value-of select="concat('BITSTREAM_ORIGINAL_', translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'), '_', doc:field[@name='sid']/text())" />
						</xsl:attribute>
						<xsl:attribute name="MIMETYPE">
							<xsl:value-of select="doc:field[@name='format']/text()" />
						</xsl:attribute>
						<xsl:attribute name="SEQ">
							<xsl:value-of select="doc:field[@name='sid']/text()" />
						</xsl:attribute>
						<xsl:attribute name="SIZE">
							<xsl:value-of select="doc:field[@name='size']/text()" />
						</xsl:attribute>
						<xsl:attribute name="CHECKSUM">
							<xsl:value-of select="doc:field[@name='checksum']/text()" />
						</xsl:attribute>
						<xsl:attribute name="CHECKSUMTYPE">
							<xsl:value-of select="doc:field[@name='checksumAlgorithm']/text()" />
						</xsl:attribute>
						<xsl:attribute name="ADMID">
							<xsl:value-of select="concat('FO_', translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'), '_', doc:field[@name='sid']/text())" />
						</xsl:attribute>
						<xsl:attribute name="GROUPID">
							<xsl:value-of select="concat('GROUP_BITSTREAM_', translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'), '_', doc:field[@name='sid']/text())" />
						</xsl:attribute>
						<FLocat LOCTYPE="URL" xlink:type="simple">
							<xsl:attribute name="xlink:href">
								<xsl:value-of select="doc:field[@name='url']/text()" />
							</xsl:attribute>
						</FLocat>
					</file>
				</xsl:for-each>
				</fileGrp>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element/doc:field[text()='TEXT']">
				<fileGrp USE="TEXT">
				<xsl:for-each select="../doc:element[@name='bitstreams']/doc:element">
					<file>
						<xsl:attribute name="ID">
							<xsl:value-of select="concat('BITSTREAM_TEXT_', translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'), '_', doc:field[@name='sid']/text())" />
						</xsl:attribute>
						<xsl:attribute name="MIMETYPE">
							<xsl:value-of select="doc:field[@name='format']/text()" />
						</xsl:attribute>
						<xsl:attribute name="SEQ">
							<xsl:value-of select="doc:field[@name='sid']/text()" />
						</xsl:attribute>
						<xsl:attribute name="SIZE">
							<xsl:value-of select="doc:field[@name='size']/text()" />
						</xsl:attribute>
						<xsl:attribute name="CHECKSUM">
							<xsl:value-of select="doc:field[@name='checksum']/text()" />
						</xsl:attribute>
						<xsl:attribute name="CHECKSUMTYPE">
							<xsl:value-of select="doc:field[@name='checksumAlgorithm']/text()" />
						</xsl:attribute>
						<xsl:attribute name="ADMID">
							<xsl:value-of select="concat('FT_', translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'), '_', doc:field[@name='sid']/text())" />
						</xsl:attribute>
						<xsl:attribute name="GROUPID">
							<xsl:value-of select="concat('GROUP_BITSTREAM_', translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'), '_', doc:field[@name='sid']/text())" />
						</xsl:attribute>
						<FLocat LOCTYPE="URL" xlink:type="simple">
							<xsl:attribute name="xlink:href">
								<xsl:value-of select="doc:field[@name='url']/text()" />
							</xsl:attribute>
						</FLocat>
					</file>
				</xsl:for-each>
				</fileGrp>
				</xsl:for-each>
			</fileSec>
			</xsl:if>
			<structMap LABEL="DSpace Object" TYPE="LOGICAL">
				<div TYPE="DSpace Object Contents">
					<xsl:attribute name="ADMID">
						<xsl:value-of select="concat('DMD_', translate(doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'))" />
					</xsl:attribute>
					<xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element/doc:field[text()='ORIGINAL']">
					<xsl:for-each select="../doc:element[@name='bitstreams']/doc:element">
						<div TYPE="DSpace BITSTREAM">
							<fptr>
								<xsl:attribute name="FILEID">
									<xsl:value-of select="concat('BITSTREAM_ORIGINAL_', translate(/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_'), '_', doc:field[@name='sid']/text())" />
								</xsl:attribute>
							</fptr>
						</div>
					</xsl:for-each>
					</xsl:for-each>
				</div>
			</structMap>
		</mets>
	</xsl:template>
	
</xsl:stylesheet>
