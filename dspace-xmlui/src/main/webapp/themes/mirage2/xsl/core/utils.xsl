
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
    xmlns:confman="org.dspace.core.ConfigurationManager"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

    <xsl:output indent="yes"/>

    <!--added classes to differentiate between collections, communities and items-->
    <xsl:template match="dri:reference" mode="summaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL,ORIGINAL</xsl:text>
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
                <xsl:variable name="emphasis" select="confman:getProperty('xmlui.theme.mirage.item-list.emphasis')"/>
                <xsl:choose>
                    <xsl:when test="'file' = $emphasis">
                        <xsl:text>emphasis-file </xsl:text>
                    </xsl:when>
                    <xsl:when test="'gallery' = $emphasis">
                        <xsl:text>emphasis-gallery </xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>emphasis-other </xsl:text>
                    </xsl:otherwise>
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

    <!-- Helper for showing mime-type-icon for item in lieu of thumbnail -->
    <xsl:template name="getFileFormatIcon">
        <xsl:param name="mimetype"/>

        <img class="mimeicon">
            <xsl:attribute name="width"><xsl:value-of select="$thumbnail.maxwidth"/></xsl:attribute>
            <xsl:attribute name="height"><xsl:value-of select="$thumbnail.maxheight"/></xsl:attribute>
            <xsl:attribute name="alt"><xsl:value-of select="$mimetype"/></xsl:attribute>
            <xsl:attribute name="src">
                <xsl:value-of select="$theme-path"/>
                <xsl:text>../mirage2</xsl:text>

                <xsl:choose>
                    <xsl:when test="$mimetype='application/pdf'">
                        <xsl:text>/images/mimeicons/pdf.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/html'">
                        <xsl:text>/images/mimeicons/html.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='text/xml'">
                        <xsl:text>/images/mimeicons/xml.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='text/plain'">
                        <xsl:text>/images/mimeicons/plain.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='text/html'">
                        <xsl:text>/images/mimeicons/html.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/msword' or $mimetype='application/vnd.openxmlformats-officedocument.wordprocessingml.document'">
                        <xsl:text>/images/mimeicons/msword.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/vnd.ms-powerpoint' or $mimetype='application/vnd.openxmlformats-officedocument.presentationml.presentation'">
                        <xsl:text>/images/mimeicons/vnd.ms-powerpoint.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/vnd.ms-excel' or $mimetype='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'">
                        <xsl:text>/images/mimeicons/vnd.ms-excel.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='image/jpeg'">
                        <xsl:text>/images/mimeicons/jpeg.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='image/gif'">
                        <xsl:text>/images/mimeicons/gif.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='image/png'">
                        <xsl:text>/images/mimeicons/png.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='image/tiff'">
                        <xsl:text>/images/mimeicons/other_image.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='audio/x-aiff'">
                        <xsl:text>/images/mimeicons/other_audio.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='audio/basic'">
                        <xsl:text>/images/mimeicons/other_audio.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='audio/x-wav'">
                        <xsl:text>/images/mimeicons/other_audio.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='video/mpeg'">
                        <xsl:text>/images/mimeicons/mpeg.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='text/richtext'">
                        <xsl:text>/images/mimeicons/richtext.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='image/x-ms-bmp'">
                        <xsl:text>/images/mimeicons/x-ms-bmp.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/postscript'">
                        <xsl:text>/images/mimeicons/plain.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='video/quicktime'">
                        <xsl:text>/images/mimeicons/mov.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='audio/x-mpeg'">
                        <xsl:text>/images/mimeicons/mpeg.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='application/x-dvi'">
                        <xsl:text>/images/mimeicons/other_movie.png</xsl:text>
                    </xsl:when>
                    <xsl:when test="$mimetype='audio/x-pn-realaudio'">
                        <xsl:text>/images/mimeicons/real.png</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>/images/mimeicons/mime.png</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
        </img>
    </xsl:template>


</xsl:stylesheet>
