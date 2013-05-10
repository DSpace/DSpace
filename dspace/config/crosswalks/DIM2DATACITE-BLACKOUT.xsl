<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:dryad="http://purl.org/dryad/terms/"
                version="1.0">

    <xsl:strip-space elements="*"/>
    <xsl:output method="xml" version="1.0"
                encoding="utf-8" indent="yes"/>

    <xsl:template match="/dim:dim">
      <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
      <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

        <resource xmlns="http://datacite.org/schema/kernel-2.2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://datacite.org/schema/kernel-2.2 http://schema.datacite.org/meta/kernel-2.2/metadata.xsd"
                  lastMetadataUpdate="2006-05-04" metadataVersionNumber="1">

	    <!-- ********** Identifiers ********** -->
      <xsl:if test="dspace:field[@element ='identifier']">
          <xsl:for-each select="dspace:field[@element ='identifier']">
              <xsl:variable name="id" select="."/>
              <xsl:choose>
                  <xsl:when test="starts-with($id,'doi')">
                      <identifier identifierType="DOI">
                          <xsl:value-of select="translate(substring-after($id,'doi:'), $smallcase, $uppercase)"/>
                      </identifier>
                  </xsl:when>
              </xsl:choose>
          </xsl:for-each>
      </xsl:if>

	    <!-- ********** Creators ************* -->
            <xsl:if test="dspace:field[@element ='contributor' and @qualifier='author']">
                <creators>
                    <xsl:for-each select="dspace:field[@element ='contributor' and @qualifier='author']">
                        <creator>
                            <creatorName>
                                <xsl:text>(:tba)</xsl:text>
                            </creatorName>
                        </creator>
                    </xsl:for-each>
                </creators>
            </xsl:if>

	    <!-- ********* Title *************** -->
            <xsl:if test="dspace:field[@element ='title']">
                <xsl:variable name="title-doi" select="dspace:field[@element='identifier' and not(@qualifier)]" />
                <titles>
                    <xsl:for-each select="dspace:field[@element ='title']">
                        <title>
                            <xsl:text>Dryad Item </xsl:text>
                            <xsl:value-of select="translate(substring-after($title-doi,'doi:'), $smallcase, $uppercase)"/>
                        </title>
                    </xsl:for-each>
                </titles>
            </xsl:if>

	    <!-- *********** Publisher ************ -->
            <publisher>Dryad Digital Repository</publisher>

	    <!-- ************ Publication Year ************** -->
            <xsl:if test="dspace:field[@element='date' and @qualifier='accessioned']">
                <xsl:for-each select="dspace:field[@qualifier='accessioned']">
                    <publicationYear>
                        <xsl:variable name="date" select="."/>
                        <xsl:value-of select="substring($date, 0, 5)"/>
                    </publicationYear>
                </xsl:for-each>
            </xsl:if>

	    <!-- ************ Subjects ************** -->
            <xsl:if test="dspace:field[@element ='subject' or @element='coverage']">
                <subjects>
                    <xsl:for-each select="dspace:field[@element ='subject']">
                        <subject>
                          <xsl:text>(:tba)</xsl:text>
                        </subject>
                    </xsl:for-each>
                    <xsl:for-each select="dspace:field[@element ='coverage']">
                        <subject>
                          <xsl:text>(:tba)</xsl:text>
                        </subject>
                    </xsl:for-each>
                    <xsl:for-each select="dspace:field[@element ='ScientificName']">
                        <subject>
                          <xsl:text>(:tba)</xsl:text>
                        </subject>
                    </xsl:for-each>
                </subjects>
            </xsl:if>
            
      <!-- ************ Dates - Only for Data Files ************** -->
	    <xsl:if test="dspace:field[@element='relation' and @qualifier='ispartof']">
          <xsl:variable name="embargoedUntil"
                        select="dspace:field[@element='date' and @qualifier='embargoedUntil']"/>
          <xsl:variable name="dateAccepted" select="dspace:field[@element='date' and @qualifier='issued']"/>
          <xsl:if test="($embargoedUntil and not($embargoedUntil='9999-01-01')) or $dateAccepted">
            <dates>
              <xsl:if test="$embargoedUntil and not($embargoedUntil='9999-01-01')">
                  <date dateType="Available">
                      <xsl:text>(:tba)</xsl:text>
                  </date>
              </xsl:if>
              <xsl:if test="$dateAccepted">
                  <date dateType="Accepted">
                      <xsl:text>(:tba)</xsl:text>
                  </date>
              </xsl:if>
            </dates>
          </xsl:if>
      </xsl:if>      

	    <!-- ************ Resource Type ************** -->
	    <xsl:if test="dspace:field[@element='relation' and @qualifier='ispartof']">
  	    <resourceType resourceTypeGeneral="Dataset">DataFile</resourceType>
  	  </xsl:if>
	    <xsl:if test="dspace:field[@element='relation' and @qualifier='haspart']">
  	    <resourceType resourceTypeGeneral="Dataset">DataPackage</resourceType>
  	  </xsl:if>
  	  
  	  <!-- ************ Alternate Identifiers ************** -->
  	  <xsl:variable name="alternateIdentifiers">
        <xsl:if test="dspace:field[@element ='identifier']">
            <xsl:for-each select="dspace:field[@element ='identifier' and not(@qualifier='manuscriptNumber')]">
                <xsl:variable name="id" select="."/>
                <xsl:choose>
                    <xsl:when test="not(starts-with($id,'doi'))">
                        <xsl:element name="alternateIdentifier">
                            <xsl:attribute name="alternateIdentifierType">
                                <xsl:value-of select="@qualifier"/>
                            </xsl:attribute>
                            <xsl:value-of select="."/>
                        </xsl:element>
                    </xsl:when>
                </xsl:choose>
            </xsl:for-each>
        </xsl:if>
      </xsl:variable>
      
      <xsl:if test="$alternateIdentifiers">
        <alternateIdentifiers>
          <xsl:copy-of select="$alternateIdentifiers"/>
        </alternateIdentifiers>
      </xsl:if>

	    <!-- *********** Related Identifiers ********* -->
      <xsl:if test="dspace:field[@element='relation']">
	      <relatedIdentifiers>
                <xsl:for-each select="dspace:field[@element='relation' and @qualifier='haspart']">
                    <relatedIdentifier relatedIdentifierType="DOI" relationType="HasPart">
                      <xsl:variable name="id" select="."/>
                      <xsl:if test="starts-with($id,'doi')">
                        <xsl:value-of select="translate(substring-after($id,'doi:'), $smallcase, $uppercase)"/>
                      </xsl:if>
                    </relatedIdentifier>
                </xsl:for-each>
                <xsl:for-each select="dspace:field[@element='relation' and @qualifier='ispartof']">
                    <relatedIdentifier relatedIdentifierType="DOI" relationType="IsPartOf">
                      <xsl:variable name="id" select="."/>
                      <xsl:if test="starts-with($id,'doi')">
                        <xsl:value-of select="translate(substring-after($id,'doi:'), $smallcase, $uppercase)"/>
	              </xsl:if>
                    </relatedIdentifier>
                </xsl:for-each>
                <xsl:for-each select="dspace:field[@element='relation' and @qualifier='isreferencedby']">
                    <relatedIdentifier relatedIdentifierType="DOI" relationType="IsReferencedBy">
                      <xsl:variable name="id" select="."/>
                      <xsl:if test="starts-with($id,'doi')">
                        <xsl:value-of select="translate(substring-after($id,'doi:'), $smallcase, $uppercase)"/>
                      </xsl:if>
                    </relatedIdentifier>
                </xsl:for-each>
	      </relatedIdentifiers>
            </xsl:if>

      <!-- ************ Rights *************** -->
      <!-- Rights for data files: (:tba) -->
      <xsl:choose>
        <xsl:when test="dspace:field[@element='relation' and @qualifier='ispartof']">
            <rights>
              <xsl:text>(:tba)</xsl:text>
            </rights>	    
        </xsl:when>
        <xsl:otherwise>
            <!--  Rights for packages always CC0 -->
            <rights>
              <xsl:text>http://creativecommons.org/publicdomain/zero/1.0/</xsl:text>
            </rights>
        </xsl:otherwise>
      </xsl:choose>

      <!-- *********** Description - Only for data files********* -->
	    <xsl:if test="dspace:field[@element='relation' and @qualifier='ispartof']">
	      <descriptions>
	        <description descriptionType="Other">
              <xsl:text>(:tba)</xsl:text>
	        </description>
	      </descriptions>
      </xsl:if>
    </resource>
  </xsl:template>
</xsl:stylesheet>
