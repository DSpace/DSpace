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
            <!--<identifier identifierType="DOI">10.5061/DRYAD.2222</identifier>-->


            <xsl:if test="dspace:field[@element ='contributor']">
                <creators>
                    <xsl:for-each select="dspace:field[@element ='contributor']">
                        <creator>
                            <creatorName>
                                <xsl:value-of select="."/>
                            </creatorName>
                        </creator>
                    </xsl:for-each>
                </creators>
            </xsl:if>


            <!--titles>
                <title>National Institute for Environmental Studies and Center for Climate System Research Japan</title>
            </titles-->
            <xsl:if test="dspace:field[@element ='title']">
                <titles>
                    <xsl:for-each select="dspace:field[@element ='title']">
                        <title>
                            <xsl:value-of select="."/>
                        </title>
                    </xsl:for-each>
                </titles>
            </xsl:if>


            <publisher>Dryad Digital Repository</publisher>

            <!--publicationYear>2004</publicationYear-->
            <xsl:if test="dspace:field[@element='date' and @qualifier='available']">
                <xsl:for-each select="dspace:field[@qualifier='available']">
                    <publicationYear>
                        <xsl:variable name="date" select="."/>
                        <xsl:value-of select="substring($date, 0, 5)"/>
                    </publicationYear>
                </xsl:for-each>
            </xsl:if>


             <!--
            <dates>
                <date dateType="Valid">2005-04-05</date>
                <date dateType="Accepted">2005-01-01</date>
            </dates>

            <language>eng</language>

            <resourceType resourceTypeGeneral="Image">Animation</resourceType>

            <alternateIdentifiers>
                <alternateIdentifier alternateIdentifierType="ISBN">937-0-1234-56789-X</alternateIdentifier>
            </alternateIdentifiers>

            <relatedIdentifiers>
                <relatedIdentifier relatedIdentifierType="DOI" relationType="IsCitedBy">10.1234/testpub
                </relatedIdentifier>
                <relatedIdentifier relatedIdentifierType="URN" relationType="Cites">http://testing.ts/testpub
                </relatedIdentifier>
            </relatedIdentifiers>

            <sizes>
                <size>285 kb</size>
                <size>100 pages</size>
            </sizes>

            <formats>
                <format>text/plain</format>
            </formats>

            <version>1.0</version>

            <rights>Open Database License [ODbL]</rights>

            <descriptions>
                <description descriptionType="Other">
                    The current xml-example for a DataCite record is the official example from the documentation.
                    <br/>Please look on datacite.org to find the newest versions of sample data and schemas.
                </description>
            </descriptions>

            -->


        </resource>

    </xsl:template>

</xsl:stylesheet>



