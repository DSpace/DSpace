<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                version="1.0">

    <xsl:template match="/dim:dim">
        <resource xmlns="http://datacite.org/schema/kernel-2.2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://datacite.org/schema/kernel-2.2 http://schema.datacite.org/meta/kernel-2.2/metadata.xsd">


            <xsl:if test="dspace:field[@element ='identifier'][starts-with(text(),'doi')][0]">

                <xsl:for-each select="dspace:field[@element ='identifier'][starts-with(text(),'doi')][0]">
                    <identifier identifierType="DOI"><xsl:value-of select="substring-after(text(),'doi:')"/></identifier>
                </xsl:for-each>

            </xsl:if>

            <xsl:if test="dspace:field[@element ='creator']">
                <creators>
                    <xsl:for-each select="dspace:field[@element ='creator']">
                        <creator>
                            <creatorName><xsl:value-of select="."/></creatorName>
                        </creator>
                    </xsl:for-each>
            </creators>

            </xsl:if>


            <xsl:if test="dspace:field[@element ='title']">
                <titles>
                    <xsl:for-each select="dspace:field[@element ='title']">
                        <title><xsl:value-of select="."/></title>
                    </xsl:for-each>
                </titles>
            </xsl:if>

            <!--
            <publisher>World Data Center for Climate (WDCC)</publisher>

            <publicationYear>2004</publicationYear>
            -->

             <xsl:if test="dspace:field[@element ='subject']">
                <subjects>
                    <xsl:for-each select="dspace:field[@element ='subject']">
                        <subject><xsl:value-of select="."/></subject>
                    </xsl:for-each>
                </subjects>
            </xsl:if>

            <xsl:if test="dspace:field[@element ='contributor']">
                <contributors>
                    <xsl:for-each select="dspace:field[@element ='contributor']">
                        <contributor>
                            <xsl:if test="@qualifier">
                                <xsl:attribute name="contributorType"><xsl:value-of select="@qualifier"/></xsl:attribute>
                            </xsl:if>
                            <contributorName><xsl:value-of select="."/></contributorName>
                        </contributor>
                    </xsl:for-each>
                </contributors>
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
