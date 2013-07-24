<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Files listing rendering specific to the ORE bundle

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:atom="http://www.w3.org/2005/Atom"
    xmlns:ore="http://www.openarchives.org/ore/terms/"
    xmlns:oreatom="http://www.openarchives.org/ore/atom/"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:encoder="xalan://java.net.URLEncoder"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    exclude-result-prefixes="xalan encoder i18n dri mets dim  xlink xsl">


    <xsl:output indent="yes"/>

    <!-- Rendering the file list from an Atom ReM bitstream stored in the ORE bundle -->
    <xsl:template match="mets:fileGrp[@USE='ORE']">
        <xsl:variable name="AtomMapURL" select="concat('cocoon:/',substring-after(mets:file/mets:FLocat[@LOCTYPE='URL']//@*[local-name(.)='href'],$context-path))"/>
        <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
        <table class="ds-table file-list">
            <thead>
                <tr class="ds-table-header-row">
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
                </tr>
            </thead>
            <tbody>
                <xsl:apply-templates select="document($AtomMapURL)/atom:entry/atom:link[@rel='http://www.openarchives.org/ore/terms/aggregates']">
                    <xsl:sort select="@title"/>
                </xsl:apply-templates>
            </tbody>
        </table>
    </xsl:template>


    <!-- Iterate over the links in the ORE resource maps and make them into bitstream references in the file section -->
    <xsl:template match="atom:link[@rel='http://www.openarchives.org/ore/terms/aggregates']">
        <xsl:variable name="link_href" select="@href"/>
        <xsl:if test="/atom:entry/oreatom:triples/rdf:Description[@rdf:about=$link_href][dcterms:description='ORIGINAL']
                    or not(/atom:entry/oreatom:triples/rdf:Description[@rdf:about=$link_href])">
            <tr>
                <xsl:attribute name="class">
                    <xsl:text>ds-table-row </xsl:text>
                    <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
                </xsl:attribute>
                <td>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="@href"/>
                        </xsl:attribute>
                        <xsl:attribute name="title">
                            <xsl:choose>
                                <xsl:when test="@title">
                                    <xsl:value-of select="@title"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="@href"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:attribute>
                        <xsl:choose>
                            <xsl:when test="string-length(@title) > 50">
                                <xsl:variable name="title_length" select="string-length(@title)"/>
                                <xsl:value-of select="substring(@title,1,15)"/>
                                <xsl:text> ... </xsl:text>
                                <xsl:value-of select="substring(@title,$title_length - 25,$title_length)"/>
                            </xsl:when>
                            <xsl:when test="@title">
                                <xsl:value-of select="@title"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="@href"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </a>
                </td>
                <!-- File size always comes in bytes and thus needs conversion -->
                <td>
                    <xsl:choose>
                        <xsl:when test="@length &lt; 1000">
                            <xsl:value-of select="@length"/>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                        </xsl:when>
                        <xsl:when test="@length &lt; 1000000">
                            <xsl:value-of select="substring(string(@length div 1000),1,5)"/>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                        </xsl:when>
                        <xsl:when test="@length &lt; 1000000001">
                            <xsl:value-of select="substring(string(@length div 1000000),1,5)"/>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                        </xsl:when>
                        <xsl:when test="@length &gt; 1000000000">
                            <xsl:value-of select="substring(string(@length div 1000000000),1,5)"/>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                        </xsl:when>
                        <!-- When one isn't available -->
                        <xsl:otherwise><xsl:text>n/a</xsl:text></xsl:otherwise>
                    </xsl:choose>
                </td>
                <!-- Currently format carries forward the mime type. In the original DSpace, this
                    would get resolved to an application via the Bitstream Registry, but we are
                    constrained by the capabilities of METS and can't really pass that info through. -->
                <td>
                    <xsl:value-of select="substring-before(@type,'/')"/>
                    <xsl:text>/</xsl:text>
                    <xsl:value-of select="substring-after(@type,'/')"/>
                </td>
                <td>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="@href"/>
                        </xsl:attribute>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                    </a>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
