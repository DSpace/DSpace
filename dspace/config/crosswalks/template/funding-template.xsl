<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:ft="https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
	
	<xsl:template match="Funding">	
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
		         	<fo:block margin-bottom="5mm" padding="2mm">
						<fo:block font-size="26pt" font-weight="bold" text-align="center" >
							<xsl:value-of select="Name" />
						</fo:block>
					</fo:block>
					
			    	<fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
						<xsl:value-of select="Description" />
					</fo:block>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Basic informations'" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Acronym'" />
				    	<xsl:with-param name="value" select="Acronym" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Type'" />
				    	<xsl:with-param name="values" select="ft:Type" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Funding Code'" />
				    	<xsl:with-param name="values" select="Identifier[@type = 'https://w3id.org/cerif/vocab/IdentifierTypes#FinanceID']" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Grant Number'" />
				    	<xsl:with-param name="values" select="Identifier[@type = 'https://w3id.org/cerif/vocab/IdentifierTypes#AwardNumber']" />
			    	</xsl:call-template>
			    	
					<xsl:if test="Amount">
					  	<fo:block font-size="10pt" margin-top="2mm">
							<fo:inline font-weight="bold" text-align="right" >
								<xsl:text>Amount:</xsl:text>
							</fo:inline>
							<fo:inline>
								<xsl:value-of select="Amount"/>
								<xsl:if test="Amount/@currency">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="Amount/@currency" />
									<xsl:text>)</xsl:text>
								</xsl:if>
							</fo:inline>
						</fo:block>
				  	</xsl:if>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Funder'" />
				    	<xsl:with-param name="value" select="Funder/OrgUnit/Name" />
			    	</xsl:call-template>
			    	
					<xsl:if test="Duration/@startDate or Duration/@endDate">
					  	<fo:block font-size="10pt" margin-top="2mm">
							<fo:inline font-weight="bold" text-align="right" >
								<xsl:text>Duration: </xsl:text>
							</fo:inline >
							<fo:inline>
								<xsl:if test="Duration/@startDate">
									<xsl:text>from </xsl:text>
									 <xsl:value-of select="Duration/@startDate" />
									<xsl:text> </xsl:text>
								</xsl:if>
								<xsl:if test="Duration/@endDate">
									<xsl:text>to </xsl:text>
									 <xsl:value-of select="Duration/@endDate" />
								</xsl:if>
							</fo:inline >
						</fo:block>
				  	</xsl:if>
				  	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'OA Mandate'" />
				    	<xsl:with-param name="value" select="OAMandate/@mandated" />
			    	</xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'OA Policy URL'" />
				    	<xsl:with-param name="value" select="OAMandate/@URL" />
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
		<fo:block margin-bottom="2mm" margin-top="-4mm">
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