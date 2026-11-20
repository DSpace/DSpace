<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:cerif="https://www.openaire.eu/cerif-profile/1.1/"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
    <xsl:param name="fontFamily" />
	
	<xsl:template match="cerif:Project">	
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
				    	<xsl:with-param name="label" select="'Project Acronym'" />
				    	<xsl:with-param name="value" select="cerif:Acronym" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'OpenAIRE id(s)'" />
				    	<xsl:with-param name="values" select="cerif:Identifier[@type = 'http://namespace.openaire.eu/oaf']" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'URL(s)'" />
				    	<xsl:with-param name="values" select="cerif:Identifier[@type = 'URL']" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Start date'" />
				    	<xsl:with-param name="value" select="cerif:StartDate" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'End date'" />
				    	<xsl:with-param name="value" select="cerif:EndDate" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Status'" />
				    	<xsl:with-param name="value" select="cerif:Status" />
			    	</xsl:call-template>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Consortium'" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Consortium Coordinator(s)'" />
				    	<xsl:with-param name="values" select="cerif:Consortium/cerif:Coordinator/cerif:OrgUnit/cerif:Name" />
				    </xsl:call-template>
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Partner Organization(s)'" />
				    	<xsl:with-param name="values" select="cerif:Consortium/cerif:Partner/cerif:OrgUnit/cerif:Name" />
				    </xsl:call-template>
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Participant Organization(s)'" />
				    	<xsl:with-param name="values" select="cerif:Consortium/cerif:Member/cerif:OrgUnit/cerif:Name" />
				    </xsl:call-template>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Team'" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Project Coordinator'" />
				    	<xsl:with-param name="value" select="cerif:Team/cerif:PrincipalInvestigator/cerif:Person/@displayName" />
				    </xsl:call-template>
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Co-Investigator(s)'" />
				    	<xsl:with-param name="values" select="cerif:Team/cerif:Member/cerif:Person/@displayName" />
				    </xsl:call-template>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Other informations'" />
			    	</xsl:call-template>

					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Uses equipment(s)'" />
				    	<xsl:with-param name="values" select="cerif:Uses/cerif:Equipment/cerif:Name" />
				    </xsl:call-template>
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Keyword(s)'" />
				    	<xsl:with-param name="values" select="cerif:Keyword" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'OA Mandate'" />
				    	<xsl:with-param name="value" select="cerif:OAMandate/@mandated" />
			    	</xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'OA Policy URL'" />
				    	<xsl:with-param name="value" select="cerif:OAMandate/@URL" />
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