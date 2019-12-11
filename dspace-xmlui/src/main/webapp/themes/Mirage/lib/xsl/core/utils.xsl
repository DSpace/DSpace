<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    This stylesheet contains helper templates for things like i18n and standard attributes.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="3.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:util="http://www.dspace.org/xmlns/dspace"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>

    <!--added classes to differentiate between collections, communities and items-->
    <xsl:template match="dri:reference" mode="summaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
            <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)->
            <xsl:if test="@type='DSpace Item'">
                <xsl:text>&amp;dmdTypes=DC</xsl:text>
            </xsl:if>-->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-artifact-item </xsl:text>
                <xsl:choose>
                    <xsl:when test="contains(@type, 'Community')">
                        <xsl:text>community </xsl:text>
                    </xsl:when>
                    <xsl:when test="contains(@type, 'Collection')">
                        <xsl:text>collection </xsl:text>
                    </xsl:when>
                </xsl:choose>
                <xsl:choose>
                    <xsl:when test="position() mod 2 = 0">even</xsl:when>
                    <xsl:otherwise>odd</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <!-- Replacing has content in original bundle facet lables (true,false) in Discover section with i18n elements -->
    <xsl:template match="//dri:list[@id='aspect.discovery.SidebarFacetsTransformer.list.has_content_in_original_bundle']/dri:item//text()">
        <xsl:choose>
            <xsl:when test="substring-before(.,' ') = 'true'">
                <i18n:text>xmlui.ArtifactBrowser.AdvancedSearch.value_has_content_in_original_bundle_true</i18n:text> 
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.ArtifactBrowser.AdvancedSearch.value_has_content_in_original_bundle_false</i18n:text> 
            </xsl:otherwise>
        </xsl:choose>
        <xsl:text> </xsl:text>
        <xsl:value-of select="substring-after(.,' ')"/>
    </xsl:template>

    <!-- Cuts off the string at the space nearest to the targetLength if there is one within
     * maxDeviation chars from the targetLength, or at the targetLength if no such space is
     * found
     -->

    <xsl:function name="util:shortenString">
        <xsl:param name="string"/>
        <xsl:param name="targetLength"/>
        <xsl:param name="maxDeviation"/>

        <xsl:choose>
            <xsl:when test="string-length($string) &lt;= $targetLength + $maxDeviation">
                <xsl:value-of select="$string"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat(util:cutString($string, $targetLength, $maxDeviation), ' ...')"/>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:function>

    <xsl:function name="util:cutString">
        <xsl:param name="string"/>
        <xsl:param name="targetLength"/>
        <xsl:param name="maxDeviation"/>

        <xsl:variable name="targetDeviation" select="substring($string,$targetLength - $maxDeviation, $maxDeviation*2)"/>

        <xsl:choose>
            <!-- There is no space so return at targetLength -->
            <xsl:when test="not(contains($targetDeviation, ' '))">
                <xsl:value-of select="substring($string, 0, $targetLength)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="firstHalf" select="substring($targetDeviation, 0, $maxDeviation)"/>
                <xsl:variable name="secondHalf" select="substring($targetDeviation, $maxDeviation)"/>

                <xsl:choose>
                    <!-- Both first half and second half have space -->
                    <xsl:when test="contains($firstHalf, ' ') and contains($secondHalf, ' ')">
                        <xsl:variable name="firstHalfIndex" select="$maxDeviation - number(util:indexOfSpace($firstHalf)[count(util:indexOfSpace($firstHalf))])"/>

                        <xsl:choose>
                            <!-- If distance to space in first half is shorter then return cut off at first half -->
                            <xsl:when test="$firstHalfIndex &lt; number(util:indexOfSpace($secondHalf)[1]) - 1">
                                <xsl:value-of select="substring($string, 0, $targetLength - $firstHalfIndex)"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="substring($string, 0, $targetLength + number(util:indexOfSpace($secondHalf)[1]) - 1)"/>
                            </xsl:otherwise>
                        </xsl:choose>

                    </xsl:when>
                    <xsl:when test="contains($firstHalf, ' ')">
                        <xsl:variable name="firstHalfIndex" select="$maxDeviation - number(util:indexOfSpace($firstHalf)[count(util:indexOfSpace($firstHalf))])"/>
                        <xsl:value-of select="substring($string, 0, $targetLength - $firstHalfIndex)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring($string, 0, $targetLength + number(util:indexOfSpace($secondHalf)[1]) - 1)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="util:indexOfSpace">
        <xsl:param name="string"/>

        <xsl:analyze-string select="$string" regex=".">
            <xsl:matching-substring>
                <xsl:choose>
                    <xsl:when test="not(compare(., ' '))">
                        <xsl:value-of select="position()"/>
                    </xsl:when>
                </xsl:choose>
            </xsl:matching-substring>
        </xsl:analyze-string>
    </xsl:function>

</xsl:stylesheet>
