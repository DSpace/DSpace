<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:pt="https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
	
	<xsl:template match="Publication">	
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
							<xsl:value-of select="Title" />
						</fo:block>
					</fo:block>
			    	
					<fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
						<xsl:value-of select="Abstract" />
					</fo:block>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Publication basic information'" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Other titles'" />
				    	<xsl:with-param name="values" select="Subtitle" />
			    	</xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Publication date'" />
				    	<xsl:with-param name="value" select="PublicationDate" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'DOI'" />
				    	<xsl:with-param name="value" select="DOI" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'ISBN'" />
				    	<xsl:with-param name="value" select="ISBN" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'ISI number'" />
				    	<xsl:with-param name="value" select="ISI-Number" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'SCP number'" />
				    	<xsl:with-param name="value" select="SCP-Number" />
			    	</xsl:call-template>
					<fo:block font-size="10pt" margin-top="2mm">
						<fo:inline font-weight="bold" text-align="right"  >
							<xsl:text>Authors: </xsl:text>
						</fo:inline >
						<fo:inline>
						<xsl:for-each select="Authors/Author">
							<xsl:value-of select="DisplayName" />
							<xsl:if test="Affiliation/OrgUnit/Name">
								( <xsl:value-of select="Affiliation/OrgUnit/Name"/> )
							</xsl:if>
						    <xsl:if test="position() != last()"> and </xsl:if>
						</xsl:for-each>
						</fo:inline >
					</fo:block>
					<fo:block font-size="10pt" margin-top="2mm">
						<fo:inline font-weight="bold" text-align="right"  >
							<xsl:text>Editors: </xsl:text>
						</fo:inline >
						<fo:inline>
						<xsl:for-each select="Editors/Editor">
							<xsl:value-of select="DisplayName" />
							<xsl:if test="Affiliation/OrgUnit/Name">
								( <xsl:value-of select="Affiliation/OrgUnit/Name"/> )
							</xsl:if>
						    <xsl:if test="position() != last()"> and </xsl:if>
						</xsl:for-each>
						</fo:inline >
					</fo:block>
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Keywords'" />
				    	<xsl:with-param name="values" select="Keyword" />
			    	</xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Type'" />
				    	<xsl:with-param name="value" select="pt:Type" />
				    </xsl:call-template>
			    	
			    	
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Publication bibliographic details'" />
			    	</xsl:call-template>
			    	
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Published in'" />
				    	<xsl:with-param name="value" select="PublishedIn/Publication/Title" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'ISSN'" />
				    	<xsl:with-param name="value" select="ISSN" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Volume'" />
				    	<xsl:with-param name="value" select="Volume" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Issue'" />
				    	<xsl:with-param name="value" select="Issue" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Start page'" />
				    	<xsl:with-param name="value" select="StartPage" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'End page'" />
				    	<xsl:with-param name="value" select="EndPage" />
				    </xsl:call-template>
			    	
			    	
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Projects'" />
			    	</xsl:call-template>
					<xsl:for-each select="OriginatesFrom/Project">
			    		<fo:block font-size="10pt" margin-top="2mm">
							<xsl:value-of select="Title" />
							<xsl:if test="Acronym">
								( <xsl:value-of select="Acronym"/> )
							</xsl:if>
						    <xsl:text> - </xsl:text>
						    <xsl:if test="StartDate">
						    	from <xsl:value-of select="StartDate"/>
						    </xsl:if>
						    <xsl:if test="EndDate">
						    	to <xsl:value-of select="EndDate"/>
						    </xsl:if>
						</fo:block>
					</xsl:for-each>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Fundings'" />
			    	</xsl:call-template>
			    	<xsl:for-each select="OriginatesFrom/Funding">
			    		<fo:block font-size="10pt" margin-top="2mm">
							<xsl:value-of select="Name" />
							<xsl:if test="Acronym">
								( <xsl:value-of select="Acronym"/> )
							</xsl:if>
							<xsl:if test="Type">
						    	<xsl:text> - Type: </xsl:text>
						    	<xsl:value-of select="Type"/>
					    	</xsl:if>
						    <xsl:if test="Funder/OrgUnit/Name">
						    	<xsl:text> - Funder: </xsl:text>
						    	<xsl:value-of select="Funder/OrgUnit/Name"/>
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