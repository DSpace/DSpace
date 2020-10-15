<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
	<xsl:param name="imageSuffix" />
	
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
					
					<fo:block font-size="10pt" font-weight="bold" text-align="center" >	
						<fo:external-graphic src="url('file:$path')" content-height="scale-to-fit" height="2.00in"  content-width="2.00in" scaling="non-uniform"/>
					</fo:block>
					
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Birth Date'" />
				    	<xsl:with-param name="value" select="birth-date" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Gender'" />
				    	<xsl:with-param name="value" select="gender" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Country'" />
				    	<xsl:with-param name="value" select="country" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Email'" />
				    	<xsl:with-param name="value" select="email" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'ORCID'" />
				    	<xsl:with-param name="value" select="identifiers/orcid" />
				    </xsl:call-template>
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Scopus Author IDs'" />
				    	<xsl:with-param name="values" select="identifiers/scopus-author-ids/scopus-author-id" />
			    	</xsl:call-template>
			    	
					<fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
						<xsl:value-of select="biography" />
					</fo:block>
					<fo:block font-size="16pt" font-weight="bold" margin-top="8mm">
						Affiliations
					</fo:block>
					<fo:block margin-bottom="2mm" margin-top="-4mm">
						<fo:leader leader-pattern="rule" leader-length="100%" rule-style="solid" />         
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
					<fo:block font-size="16pt" font-weight="bold" margin-top="8mm">
						Education
					</fo:block>
					<fo:block margin-bottom="2mm" margin-top="-4mm">
						<fo:leader leader-pattern="rule" leader-length="100%" rule-style="solid" />         
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
					<fo:block font-size="16pt" font-weight="bold" margin-top="8mm" >
						Qualifications 
					</fo:block>
					<fo:block margin-bottom="2mm" margin-top="-4mm">
						<fo:leader leader-pattern="rule" leader-length="100%" rule-style="solid" />         
					</fo:block>
					<xsl:for-each select="qualifications/qualification">
						<fo:block font-size="10pt">
							<xsl:value-of select="name" /> from <xsl:value-of select="start-date" />
							<xsl:if test="end-date/text()">
							 to <xsl:value-of select="end-date" />
							</xsl:if>
						</fo:block>
					</xsl:for-each>
				</fo:flow>
			</fo:page-sequence>
		</fo:root>
	</xsl:template>
	
	<xsl:template name = "print-value" >
	  <xsl:param name = "label" />
	  <xsl:param name = "value" />
	  	<fo:block font-size="10pt" margin-top="2mm">
			<fo:inline font-weight="bold" text-align="right" >
				<xsl:value-of select="$label" /> 
			</fo:inline >
			<xsl:text>: </xsl:text>
			<fo:inline>
				<xsl:value-of select="$value" /> 
			</fo:inline >
		</fo:block>
	</xsl:template>
	
	<xsl:template name = "print-values" >
	  <xsl:param name = "label" />
	  <xsl:param name = "values" />
	  	<fo:block font-size="10pt" margin-top="2mm">
			<fo:inline font-weight="bold" text-align="right"  >
				<xsl:value-of select="$label" /> 
			</fo:inline >
			<xsl:text>: </xsl:text>
			<fo:inline>
				<xsl:for-each select="$values">
				    <xsl:value-of select="current()" />
				    <xsl:if test="position() != last()">, </xsl:if>
				</xsl:for-each>
			</fo:inline >
		</fo:block>
	</xsl:template>
	
</xsl:stylesheet>