<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    TODO: Describe this XSL file
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

    <xsl:import href="../dri2xhtml-alt/dri2xhtml.xsl"/>
    <xsl:import href="lib/xsl/core/global-variables.xsl"/>
    <xsl:import href="lib/xsl/core/page-structure.xsl"/>
    <xsl:import href="lib/xsl/core/navigation.xsl"/>
    <xsl:import href="lib/xsl/core/elements.xsl"/>
    <xsl:import href="lib/xsl/core/forms.xsl"/>
    <xsl:import href="lib/xsl/core/attribute-handlers.xsl"/>
    <xsl:import href="lib/xsl/core/utils.xsl"/>
    <xsl:import href="lib/xsl/aspect/general/choice-authority-control.xsl"/>
    <xsl:import href="lib/xsl/aspect/administrative/administrative.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/item-list.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/item-view.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/community-list.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/collection-list.xsl"/>
    <xsl:output indent="yes"/>
    

  <xsl:template match="mets:file">
        <xsl:param name="context" select="."/>
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
            </xsl:attribute>
            <td>
              <!-- Thumbnail or Mime-Icon -->
              <xsl:choose>
                <xsl:when test="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=current()/@GROUPID]">
                    <a class="image-link">
                        <xsl:attribute name="href">
                            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                        </xsl:attribute>
                        <img alt="Thumbnail">
                            <xsl:attribute name="src">
                                <xsl:value-of select="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                    mets:file[@GROUPID=current()/@GROUPID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                        </img>
                    </a>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="getFileFormatIcon">
                        <xsl:with-param name="mimetype">
                            <xsl:value-of select="@MIMETYPE"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:otherwise>
              </xsl:choose>
            </td>
            <td>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="string-length(mets:FLocat[@LOCTYPE='URL']/@xlink:title) > 50">
                            <xsl:variable name="title_length" select="string-length(mets:FLocat[@LOCTYPE='URL']/@xlink:title)"/>
                            <xsl:value-of select="substring(mets:FLocat[@LOCTYPE='URL']/@xlink:title,1,15)"/>
                            <xsl:text> ... </xsl:text>
                            <xsl:value-of select="substring(mets:FLocat[@LOCTYPE='URL']/@xlink:title,$title_length - 25,$title_length)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </td>
            <!-- File size always comes in bytes and thus needs conversion -->
            <td>
                <xsl:choose>
                    <xsl:when test="@SIZE &lt; 1024">
                        <xsl:value-of select="@SIZE"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@SIZE &lt; 1024 * 1024">
                        <xsl:value-of select="substring(string(@SIZE div 1024),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@SIZE &lt; 1024 * 1024 * 1024">
                        <xsl:value-of select="substring(string(@SIZE div (1024 * 1024)),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring(string(@SIZE div (1024 * 1024 * 1024)),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <!-- Lookup File Type description in local messages.xml based on MIME Type.
                In the original DSpace, this would get resolved to an application via
                the Bitstream Registry, but we are constrained by the capabilities of METS
                and can't really pass that info through. -->
            <td>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                    </xsl:attribute>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                </a>
            </td>
      <!-- Display the contents of 'Description' as long as at least one bitstream contains a description -->
      <xsl:if test="$context/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file/mets:FLocat/@xlink:label != ''">
          <td>
              <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label"/>
          </td>
      </xsl:if>

        </tr>
    </xsl:template>

<xsl:template name="getFileFormatIcon">
  <xsl:param name="mimetype"/>

    <img class="mimeicon">
      <xsl:attribute name="width">128</xsl:attribute>
      <xsl:attribute name="height">128</xsl:attribute>
      <xsl:attribute name="src">
      <xsl:choose>
        <xsl:when test="$mimetype='application/pdf'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/pdf.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='application/html'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/html.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='text/xml'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/xml.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='text/plain'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/plain.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='text/html'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/html.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='application/msword' or $mimetype='application/vnd.openxmlformats-officedocument.wordprocessingml.document'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/msword.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='application/vnd.ms-powerpoint' or $mimetype='application/vnd.openxmlformats-officedocument.presentationml.presentation'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/vnd.ms-powerpoint.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='application/vnd.ms-excel' or $mimetype='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/vnd.ms-excel.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='image/jpeg'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/jpeg.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='image/gif'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/gif.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='image/png'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/png.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='image/tiff'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/other_image.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='audio/x-aiff'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/other_audio.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='audio/basic'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/other_audio.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='audio/x-wav'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/other_audio.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='video/mpeg'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/mpeg.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='text/richtext'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/richtext.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='image/x-ms-bmp'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/x-ms-bmp.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='application/postscript'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/plain.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='video/quicktime'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/mov.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='audio/x-mpeg'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/mpeg.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='application/x-dvi'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/other_movie.png</xsl:text>
        </xsl:when>
        <xsl:when test="$mimetype='audio/x-pn-realaudio'">
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/real.png</xsl:text>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$theme-path"/>
            <xsl:text>/images/icons/mime.png</xsl:text>
        </xsl:otherwise>
    </xsl:choose>
    </xsl:attribute>
    </img>
</xsl:template>

  <xsl:template name="communitySummaryList-DIM">
    <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
    <span class="bold">
      <a href="{@OBJID}">
        <xsl:choose>
          <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
              <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
          </xsl:when>
          <xsl:otherwise>
              <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
          </xsl:otherwise>
        </xsl:choose>
      </a>
      <!--Display community strengths (item counts) if they exist-->
      <xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
        <xsl:text> [</xsl:text>
        <xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]"/>
        <xsl:text>]</xsl:text>
      </xsl:if>
    </span>
  </xsl:template>
</xsl:stylesheet>
