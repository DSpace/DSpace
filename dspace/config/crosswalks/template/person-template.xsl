<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
    <xsl:param name="fontFamily" />
	
	<xsl:template match="person">	
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
				
					<fo:table>
						<xsl:choose>
							<xsl:when test="personal-picture">
							   <fo:table-column column-width="33%"/>
							   <fo:table-column column-width="67%"/>
							</xsl:when>
							<xsl:otherwise>
								<fo:table-column />
							</xsl:otherwise>
						</xsl:choose>
					   <fo:table-body>
					      <fo:table-row>
					      	 <xsl:if test="personal-picture">
						         <fo:table-cell>
									<fo:block font-size="10pt" font-weight="bold" text-align="center" >	
										<xsl:variable name="picturePath" select="concat('file:',$imageDir,'/',personal-picture)" />
										<fo:external-graphic content-height="scale-to-fit" height="2.40in"  content-width="2.00in" scaling="non-uniform">
											<xsl:attribute name="src">
												<xsl:value-of select="$picturePath" />
											</xsl:attribute>
										</fo:external-graphic>
									</fo:block>
						         </fo:table-cell>
					         </xsl:if>
					         <fo:table-cell>
					         	<fo:block background-color="#8cc0db" margin-bottom="5mm" padding="2mm">
									<fo:block font-size="26pt" font-weight="bold" text-align="center" >
										<xsl:value-of select="names/preferred-name"  />
									</fo:block>
									<fo:block font-size="12pt" text-align="center">
										<xsl:value-of select="job-title"  />
										<xsl:if test="job-title and main-affiliation">
											<xsl:text> at </xsl:text>
										</xsl:if>
										<xsl:value-of select="main-affiliation"  />
									</fo:block>
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
						    	
					         </fo:table-cell>
					      </fo:table-row>
					   </fo:table-body>
					</fo:table>
			    	
					<fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
						<xsl:value-of select="biography" />
					</fo:block>
			    	
			    	<xsl:if test="affiliations/affiliation">
			    	
						<xsl:call-template name="section-title">
					    	<xsl:with-param name="label" select="'Affiliations'" />
				    	</xsl:call-template>
			    	        
						<xsl:for-each select="affiliations/affiliation">
							<fo:block font-size="10pt">
								<xsl:value-of select="role" /> at <xsl:value-of select="name" />
								from <xsl:value-of select="start-date" />
								<xsl:if test="end-date/text()">
								 to <xsl:value-of select="end-date" />
								</xsl:if>
							</fo:block>
						</xsl:for-each>
					
			    	</xsl:if>
			    	
			    	<xsl:if test="educations/education">
					
						<xsl:call-template name="section-title">
					    	<xsl:with-param name="label" select="'Education'" />
				    	</xsl:call-template>   
				    	 
						<xsl:for-each select="educations/education">
							<fo:block font-size="10pt">
								<xsl:value-of select="role" /> at <xsl:value-of select="name" />
								from <xsl:value-of select="start-date" />
								<xsl:if test="end-date/text()">
								 to <xsl:value-of select="end-date" />
								</xsl:if>
							</fo:block>
						</xsl:for-each>
					
					</xsl:if>
					
			    	<xsl:if test="qualifications/qualification">
					
						<xsl:call-template name="section-title">
					    	<xsl:with-param name="label" select="'Qualifications'" />
				    	</xsl:call-template>
				    	
						<xsl:for-each select="qualifications/qualification">
							<fo:block font-size="10pt">
								<xsl:value-of select="name" /> from <xsl:value-of select="start-date" />
								<xsl:if test="end-date/text()">
								 to <xsl:value-of select="end-date" />
								</xsl:if>
							</fo:block>
						</xsl:for-each>
					
					</xsl:if>
					
			    	<xsl:if test="publications/publication">
			    	
						<xsl:call-template name="section-title">
					    	<xsl:with-param name="label" select="'Publications'" />
				    	</xsl:call-template>

				    	<xsl:for-each select="publications/publication">
							<fo:block font-size="10pt">
								<xsl:for-each select="authors/author">
									<xsl:value-of select="current()" />
								    <xsl:if test="position() != last()"> and </xsl:if>
								</xsl:for-each>
								<xsl:text> </xsl:text>
								<xsl:if test="date-issued">
									<xsl:text>(</xsl:text>
									<xsl:value-of select="date-issued" />
									<xsl:text>)</xsl:text>
								</xsl:if>
								<xsl:text>. </xsl:text>
								<fo:inline font-style="italic" >
									<xsl:value-of select="title" /> 
								</fo:inline >
							</fo:block>
						</xsl:for-each>
				    	
			    	</xsl:if>
			    	
			    	
	  				<xsl:if test="( count(working-groups/working-group) &gt; 0 ) or ( count(interests/interest) &gt; 0 ) or ( count(knows-languages/language) &gt; 0 )">
					
						<xsl:call-template name="section-title">
					    	<xsl:with-param name="label" select="'Other informations'" />
				    	</xsl:call-template>
						<xsl:call-template name="print-values">
					    	<xsl:with-param name="label" select="'Working groups'" />
					    	<xsl:with-param name="values" select="working-groups/working-group" />
				    	</xsl:call-template>
						<xsl:call-template name="print-values">
					    	<xsl:with-param name="label" select="'Interests'" />
					    	<xsl:with-param name="values" select="interests/interest" />
				    	</xsl:call-template>
						<xsl:call-template name="print-values">
					    	<xsl:with-param name="label" select="'Knows languages'" />
					    	<xsl:with-param name="values" select="knows-languages/language" />
				    	</xsl:call-template>
			    	
			    	</xsl:if>
			    	
	  				<xsl:if test="personal-sites/personal-site">
	  				
				    	<fo:block font-size="10pt" margin-top="2mm">
				    		<fo:inline font-weight="bold" text-align="right" >
								Personal sites:
							</fo:inline>
							<fo:inline>
								<xsl:for-each select="personal-sites/personal-site">
									<xsl:value-of select="site-url" />
									<xsl:text> </xsl:text>
									<xsl:if test="site-title">
										( <xsl:value-of select="site-title" /> )
									</xsl:if>
								    <xsl:if test="position() != last()">, </xsl:if>
								</xsl:for-each>
							</fo:inline >
						</fo:block>
						
					</xsl:if>

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