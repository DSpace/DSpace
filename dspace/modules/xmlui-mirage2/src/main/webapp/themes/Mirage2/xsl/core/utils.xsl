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
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>


    <!-- templates for required textarea attributes used if not found in DRI document -->
    <xsl:template name="textAreaCols">
        <xsl:attribute name="cols">20</xsl:attribute>
    </xsl:template>

    <xsl:template name="textAreaRows">
        <xsl:attribute name="rows">5</xsl:attribute>
    </xsl:template>



    <!-- This does it for all the DRI elements. The only thing left to do is to handle Cocoon's i18n
        transformer tags that are used for text translation. The templates below simply push through
        the i18n elements so that they can translated after the XSL step. -->
    <xsl:template match="i18n:text">
        <xsl:param name="text" select="."/>
        <xsl:choose>
            <xsl:when test="contains($text, '&#xa;')">
                <xsl:value-of select="substring-before($text, '&#xa;')"/>
                <ul>
                    <xsl:attribute name="style">float:left; list-style-type:none; text-align:left;</xsl:attribute>
                    <xsl:call-template name="linebreak">
                        <xsl:with-param name="text" select="substring-after($text,'&#xa;')"/>
                    </xsl:call-template>
                </ul>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$text"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Function to replace \n -->
    <xsl:template name="linebreak">
        <xsl:param name="text" select="."/>
        <xsl:choose>
            <xsl:when test="contains($text, '&#xa;')">
                <li>
                    <xsl:value-of select="substring-before($text, '&#xa;')"/>
                </li>
                <xsl:call-template name="linebreak">
                    <xsl:with-param name="text" select="substring-after($text,'&#xa;')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="i18n:translate">
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template match="i18n:param">
        <xsl:copy-of select="."/>
    </xsl:template>

    <!--added classes to differentiate between collections, communities and items-->
    <xsl:template match="dri:reference" mode="summaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
            <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)-->
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

    <xsl:template name="standardAttributes">
        <xsl:param name="class"/>
        <xsl:param name="placeholder"/>
        <xsl:if test="@id">
            <xsl:attribute name="id"><xsl:value-of select="translate(@id,'.','_')"/></xsl:attribute>
        </xsl:if>
        <xsl:attribute name="class">
            <xsl:value-of select="normalize-space($class)"/>
            <xsl:if test="@rend">
                <xsl:text> </xsl:text>
                <xsl:value-of select="@rend"/>
            </xsl:if>
        </xsl:attribute>
        <xsl:if test="string-length($placeholder)>0">
            <xsl:attribute name="placeholder"><xsl:value-of select="$placeholder"/></xsl:attribute>
            <xsl:attribute name="i18n:attr">placeholder</xsl:attribute>
        </xsl:if>
    </xsl:template>

    <!-- Replacing has content in original bundle lables (true,false) in Discover section with i18n elements -->
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

</xsl:stylesheet>
