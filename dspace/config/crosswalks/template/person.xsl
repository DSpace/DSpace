<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	exclude-result-prefixes="fo">
	<xsl:template match="person">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
				<fo:simple-page-master master-name="simpleA4"
					page-height="29.7cm" page-width="24cm" margin-top="2cm"
					margin-bottom="2cm" margin-left="1cm" margin-right="1cm">
					<fo:region-body />
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:page-sequence master-reference="simpleA4">
				<fo:flow flow-name="xsl-region-body">
					<fo:block background-color="#8cc0db" margin-bottom="5mm" padding="2mm">
						<fo:block font-size="26pt" font-weight="bold" text-align="center" >
							<xsl:value-of select="names/preferred-name"  />
						</fo:block>
						<fo:block font-size="12pt" text-align="center">
							<xsl:value-of select="job-title"  /> at <xsl:value-of select="main-affiliation"  />
						</fo:block>
					</fo:block>
					<fo:block>
						<fo:inline font-size="10pt" space-after="5mm" font-weight="bold" text-align="right" margin-top="2mm" >
							Birth Date: 
						</fo:inline >
						<fo:inline  font-size="10pt" space-after="5mm" text-align="left" margin-top="2mm" margin-left="2mm">
							<xsl:value-of select="birth-date" /> 
						</fo:inline >
					</fo:block>
					<fo:block>
						<fo:inline  font-size="10pt" space-after="5mm" font-weight="bold" text-align="right" margin-top="2mm" >
							Email: 
						</fo:inline >
						<fo:inline  font-size="10pt" space-after="5mm" text-align="left" margin-top="2mm" margin-left="2mm">
							<xsl:value-of select="email" /> 
						</fo:inline >
					</fo:block>
					<fo:block>
						<fo:inline  font-size="10pt" space-after="5mm" font-weight="bold" text-align="right" margin-top="2mm" >
							ORCID: 
						</fo:inline >
						<fo:inline  font-size="10pt" space-after="5mm" text-align="left" margin-top="2mm" margin-left="2mm">
							<xsl:value-of select="identifiers/orcid" /> 
						</fo:inline >
					</fo:block>
					<fo:block>
						<fo:inline  font-size="10pt" space-after="5mm" font-weight="bold" text-align="right" margin-top="2mm" >
							Scopus Author IDs: 
						</fo:inline >
						<fo:inline  font-size="10pt" space-after="5mm" text-align="left" margin-top="2mm" margin-left="2mm">
							<xsl:for-each select="identifiers/scopus-author-ids/scopus-author-id">
							    <xsl:value-of select="current()" />
							    <xsl:if test="position() != last()">, </xsl:if>
							</xsl:for-each>
						</fo:inline >
					</fo:block>
					<fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
						<xsl:value-of select="biography" />
					</fo:block>
					<fo:block font-size="16pt" font-weight="bold" margin-top="10mm" margin-bottom="5mm">
						Affiliations <fo:leader leader-pattern="rule" leader-length="85%" rule-style="solid" />   
					</fo:block>          
					<xsl:for-each select="affiliations/affiliation">
						<fo:block font-size="10pt">
							<xsl:value-of select="role" /> at <xsl:value-of select="name" />
							from <xsl:value-of select="start-date" />
							<xsl:if test="end-date/text()">
							 to <xsl:value-of select="end-date" />
							</xsl:if>
						</fo:block>
					</xsl:for-each>
					<fo:block font-size="16pt" font-weight="bold" margin-top="10mm" margin-bottom="5mm">
						Education <fo:leader leader-pattern="rule" leader-length="85%" rule-style="solid" />   
					</fo:block>          
					<xsl:for-each select="educations/education">
						<fo:block font-size="10pt">
							<xsl:value-of select="role" /> at <xsl:value-of select="name" />
							from <xsl:value-of select="start-date" />
							<xsl:if test="end-date/text()">
							 to <xsl:value-of select="end-date" />
							</xsl:if>
						</fo:block>
					</xsl:for-each>
				</fo:flow>
			</fo:page-sequence>
		</fo:root>
	</xsl:template>
</xsl:stylesheet>