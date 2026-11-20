<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:ft="https://www.openaire.eu/cerif-profile/vocab/COAR_Patent_Types"
	xmlns:cerif="https://www.openaire.eu/cerif-profile/1.1/"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
    <xsl:param name="fontFamily" />
	
	<xsl:template match="cerif:Patent">	
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <xsl:attribute name="font-family">
                <xsl:value-of select="$fontFamily" />
            </xsl:attribute>
			<fo:layout-master-set>
				<fo:simple-page-master master-name="simpleA4"
					page-height="29.7cm" page-width="24cm" margin-top="2cm"
					margin-bottom="2cm" margin-left="1cm" margin-right="1cm">
					<fo:region-body />
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:page-sequence master-reference="simpleA4">
				<fo:flow flow-name="xsl-region-body">
		         	<fo:block margin-bottom="5mm" padding="2mm">
						<fo:block font-size="26pt" font-weight="bold" text-align="center" >
							<xsl:value-of select="cerif:Title" />
						</fo:block>
					</fo:block>
					
			    	<fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
						<xsl:value-of select="cerif:Abstract" />
					</fo:block>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Basic informations'" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Registration date'" />
				    	<xsl:with-param name="value" select="cerif:RegistrationDate" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Approval date'" />
				    	<xsl:with-param name="value" select="cerif:ApprovalDate" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Patent number'" />
				    	<xsl:with-param name="value" select="cerif:PatentNumber" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Issuer(s)'" />
				    	<xsl:with-param name="values" select="cerif:Issuer/cerif:OrgUnit/cerif:Name" />
			    	</xsl:call-template>
			    	
					<xsl:if test="cerif:Inventors/cerif:Inventor">
				    	<fo:block font-size="10pt" margin-top="2mm">
							<fo:inline font-weight="bold" text-align="right"  >
								<xsl:text>Inventor(s): </xsl:text>
							</fo:inline >
							<fo:inline>
					    	<xsl:for-each select="cerif:Inventors/cerif:Inventor">
								<xsl:value-of select="cerif:DisplayName" />
								<xsl:if test="cerif:Affiliation/cerif:OrgUnit/cerif:Name">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="cerif:Affiliation/cerif:OrgUnit/cerif:Name"/>
									<xsl:text>)</xsl:text>
								</xsl:if>
							    <xsl:if test="position() != last()">, </xsl:if>
							</xsl:for-each>
							</fo:inline >
						</fo:block>
					</xsl:if>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Holder(s)'" />
				    	<xsl:with-param name="values" select="cerif:Holders/cerif:Holder/cerif:OrgUnit/cerif:Name" />
			    	</xsl:call-template>
			    	
			    	<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Keyword(s)'" />
				    	<xsl:with-param name="values" select="cerif:Keyword" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Predecessor(s)'" />
				    	<xsl:with-param name="values" select="cerif:Predecessor/cerif:Patent/cerif:Title" />
			    	</xsl:call-template>
				    
				</fo:flow>
			</fo:page-sequence>
		</fo:root>
	</xsl:template>
	
	<xsl:template name = "print-value" >
	  	<xsl:param name = "label" />
	  	<xsl:param name = "value" />
	  	<xsl:if test="$value">
		  	<fo:block font-size="10pt" margin-top="2mm">
				<fo:inline font-weight="bold" text-align="right" >
					<xsl:value-of select="$label" /> 
				</fo:inline >
				<xsl:text>: </xsl:text>
				<fo:inline>
					<xsl:value-of select="$value" /> 
				</fo:inline >
			</fo:block>
	  	</xsl:if>
	</xsl:template>
	
	<xsl:template name = "section-title" >
		<xsl:param name = "label" />
		<fo:block font-size="16pt" font-weight="bold" margin-top="8mm" >
			<xsl:value-of select="$label" /> 
		</fo:block>
		<fo:block>
			<fo:leader leader-pattern="rule" leader-length="100%" rule-style="solid" />         
		</fo:block>
	</xsl:template>
	
	<xsl:template name = "print-values" >
		<xsl:param name = "label" />
	  	<xsl:param name = "values" />
	  	<xsl:if test="$values">
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
		</xsl:if>
	</xsl:template>
	
</xsl:stylesheet>