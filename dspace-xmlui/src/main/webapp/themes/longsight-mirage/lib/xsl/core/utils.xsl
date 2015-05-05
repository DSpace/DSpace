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

    <!--added classes to differentiate between collections, communities and items-->
    <xsl:template match="dri:reference" mode="summaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL,CONTENT,ORIGINAL</xsl:text>
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

    <!-- Helper for showing mime-type-icon for item in lieu of thumbnail -->
    <xsl:template name="getFileFormatIcon">
        <xsl:param name="mimetype"/>

        <img class="mimeicon">
            <xsl:attribute name="width"><xsl:value-of select="$thumbnail.maxwidth"/></xsl:attribute>
            <xsl:attribute name="height"><xsl:value-of select="$thumbnail.maxheight"/></xsl:attribute>
            <xsl:attribute name="alt"><xsl:value-of select="$mimetype"/></xsl:attribute>
            <xsl:attribute name="src">
                <xsl:value-of select="$theme-path"/>

                <xsl:choose>
                    <xsl:when test="$mimetype='application/pdf'">
                        <xsl:text>/images/icons/pdf.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/html'">
                        <xsl:text>/images/icons/html.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='text/xml'">
                        <xsl:text>/images/icons/xml.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='text/plain'">
                        <xsl:text>/images/icons/plain.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='text/html'">
                        <xsl:text>/images/icons/html.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/msword' or $mimetype='application/vnd.openxmlformats-officedocument.wordprocessingml.document'">
                        <xsl:text>/images/icons/msword.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/vnd.ms-powerpoint' or $mimetype='application/vnd.openxmlformats-officedocument.presentationml.presentation'">
                        <xsl:text>/images/icons/vnd.ms-powerpoint.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/vnd.ms-excel' or $mimetype='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'">
                        <xsl:text>/images/icons/vnd.ms-excel.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='image/jpeg'">
                        <xsl:text>/images/icons/jpeg.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='image/gif'">
                        <xsl:text>/images/icons/gif.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='image/png'">
                        <xsl:text>/images/icons/png.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='image/tiff'">
                        <xsl:text>/images/icons/other_image.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='audio/x-aiff'">
                        <xsl:text>/images/icons/other_audio.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='audio/basic'">
                        <xsl:text>/images/icons/other_audio.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='audio/x-wav'">
                        <xsl:text>/images/icons/other_audio.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='video/mpeg'">
                        <xsl:text>/images/icons/mpeg.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='text/richtext'">
                        <xsl:text>/images/icons/richtext.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='image/x-ms-bmp'">
                        <xsl:text>/images/icons/x-ms-bmp.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/postscript'">
                        <xsl:text>/images/icons/plain.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='video/quicktime'">
                        <xsl:text>/images/icons/mov.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='audio/x-mpeg'">
                        <xsl:text>/images/icons/mpeg.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/x-dvi'">
                        <xsl:text>/images/icons/other_movie.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='audio/x-pn-realaudio'">
                        <xsl:text>/images/icons/real.png</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>/images/icons/mime.png</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
        </img>
    </xsl:template>


</xsl:stylesheet>
