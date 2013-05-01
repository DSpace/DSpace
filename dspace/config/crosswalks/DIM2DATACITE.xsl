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
                                <xsl:value-of select="."/>
                            </creatorName>
                        </creator>
                    </xsl:for-each>
                </creators>
            </xsl:if>

	    <!-- ********* Title *************** -->
            <xsl:if test="dspace:field[@element ='title']">
                <titles>
                    <xsl:for-each select="dspace:field[@element ='title']">
                        <title>
                            <xsl:value-of select="."/>
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
                          <xsl:value-of select="."/>
                        </subject>
                    </xsl:for-each>
                    <xsl:for-each select="dspace:field[@element ='coverage']">
                        <subject>
                          <xsl:value-of select="."/>
                        </subject>
                    </xsl:for-each>
                    <xsl:for-each select="dspace:field[@element ='ScientificName']">
                        <subject>
                          <xsl:value-of select="."/>
                        </subject>
                    </xsl:for-each>
                </subjects>
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
            <xsl:for-each select="dspace:field[@element ='identifier']">
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

      <!-- Embargo - only for data files -->
	    <xsl:if test="dspace:field[@element='relation' and @qualifier='ispartof']">
          <xsl:variable name="embargoedUntil"
                        select="dspace:field[@element='date' and @qualifier='embargoedUntil']"/>
          <xsl:variable name="embargoType"
                        select="dspace:field[@element='type' and @qualifier='embargo']"/>
          <xsl:variable name="dateAccepted" select="dspace:field[@element='date' and @qualifier='issued']"/>

          <xsl:if test="$embargoedUntil and not($embargoedUntil='9999-01-01')">
              <dateAvailable>
                  <xsl:value-of select="$embargoedUntil"/>
              </dateAvailable>
          </xsl:if>
          <xsl:if test="$dateAccepted">
              <dateAccepted>
                  <xsl:value-of select="$dateAccepted"/>
              </dateAccepted>
          </xsl:if>
          <xsl:variable name="embargoText">
            <xsl:choose>
                <!-- If the embargoredDate is empty, this item is no longer embargoed -->
                <xsl:when test="$embargoedUntil!=''">
                    <xsl:choose>
                        <xsl:when test="$embargoedUntil='9999-01-01' and $embargoType='oneyear'">
                            <!-- The item is under one-year embargo, but the article has not been published yet,
                                       so we don't have an end date. -->
                        <xsl:text>At the request of the author, this item is embargoed until one year after the associated article is published.</xsl:text>
                        </xsl:when>
                        <xsl:when
                                test="$embargoedUntil='9999-01-01' and ($embargoType='untilArticleAppears' or $embargoType='unknown')">
                            <!-- The item is under embargo, but the end date is not yet known -->
                        <xsl:text>At the request of the author, this item is embargoed until the associated article is published.</xsl:text>
                        </xsl:when>
                        <xsl:when test="$embargoedUntil='9999-01-01' and $embargoType='custom'">
                            <!-- The item is under embargo, but the end date is not yet known. The editor has approved a custom length. -->
                            <xsl:text>At the request of the author, this item is embargoed. The journal editor has set a custom embargo length. Once the associated article is published, the exact release date of the embargo will be shown here.</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <!-- The item is under embargo, and the end date of the embargo is known. -->
                            <xsl:text>At the request of the author, this item is embargoed until</xsl:text>
                            <xsl:value-of select="$embargoedUntil"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="dspace:field[@element='rights']"/>
                </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <!-- ************ Rights *************** -->
          <xsl:if test="$embargoText">
              <rights>
                  <xsl:value-of select="$embargoText"/>
              </rights>
          </xsl:if>
      </xsl:if>
	    <xsl:if test="dspace:field[@element='relation' and @qualifier='haspart']">
	        <!--  All data package DOIs include a CC0 statement. -->
          <!-- ************ Rights *************** -->
          <rights>
            <xsl:text>http://creativecommons.org/publicdomain/zero/1.0/</xsl:text>
          </rights>
      </xsl:if>

      <!-- *********** Description - Only for data files********* -->
	    <xsl:if test="dspace:field[@element='relation' and @qualifier='ispartof']">
	      <descriptions>
	        <description descriptionType="">
              <xsl:value-of select="dspace:field[@element='description']"/>
	        </description>
	      </descriptions>
      </xsl:if>
    </resource>
  </xsl:template>
</xsl:stylesheet>



