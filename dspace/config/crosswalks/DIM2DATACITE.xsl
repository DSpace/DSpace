<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
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

	  <!-- ********** Identifier ********** -->
            <xsl:if test="dspace:field[@element ='identifier']">
                <xsl:for-each select="dspace:field[@element ='identifier']">
                    <xsl:variable name="id" select="."/>
                    <xsl:if test="starts-with($id,'doi')">
                        <identifier identifierType="DOI">
                            <xsl:value-of select="translate(substring-after($id,'doi:'), $smallcase, $uppercase)"/>
	                  </identifier>
                    </xsl:if>
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
            <xsl:if test="dspace:field[@element='rights']">
              <xsl:for-each select="dspace:field[@element='rights']">
                <rights>
                  <xsl:value-of select="."/>
                </rights>
              </xsl:for-each>
            </xsl:if>

        </resource>

    </xsl:template>

</xsl:stylesheet>



