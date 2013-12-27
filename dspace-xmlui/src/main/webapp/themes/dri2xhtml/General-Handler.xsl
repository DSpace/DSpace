<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Author: Alexey Maslov
    Description: This stylesheet contains templates to perform tasks associated with metadata visualization
        that are common to all handlers. This includes things like bitstream display and thumbnails.
    Version: Manakin-1.1 and up (basically, those version making use of the Virtual Object Store)
-->    

<xsl:stylesheet 
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:jstring="java.lang.String"
    xmlns:rights="http://cosimo.stanford.edu/sdr/metsrights/"
    exclude-result-prefixes="i18n dri mets xlink xsl jstring rights">
    
    <xsl:output indent="yes"/>
    

    
    
    <!-- Generate the thunbnail, if present, from the file section -->
    <xsl:template match="mets:fileSec" mode="artifact-preview">
        <xsl:if test="mets:fileGrp[@USE='THUMBNAIL']">
            <div class="artifact-preview">
                <a href="{ancestor::mets:METS/@OBJID}">
                    <img alt="Thumbnail">
                        <xsl:attribute name="src">
                            <xsl:value-of select="mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href" />
                        </xsl:attribute>
                    </img>
                </a>
            </div>
        </xsl:if>
    </xsl:template>
    
    
    
    <!-- Generate the bitstream information from the file section -->
    <xsl:template match="mets:fileGrp[@USE='CONTENT']">
        <xsl:param name="context"/>
        <xsl:param name="primaryBitstream" select="-1"/>
        
        <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
        <table class="ds-table file-list">
            <tr class="ds-table-header-row">
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
                <!-- Display header for 'Description' only if at least one bitstream contains a description -->
                <xsl:if test="mets:file/mets:FLocat/@xlink:label != ''">
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-description</i18n:text></th>
                </xsl:if>
		    </tr>
            <xsl:choose>
                <!-- If one exists and it's of text/html MIME type, only display the primary bitstream -->
                <xsl:when test="mets:file[@ID=$primaryBitstream]/@MIMETYPE='text/html'">
                    <xsl:apply-templates select="mets:file[@ID=$primaryBitstream]">
                        <xsl:with-param name="context" select="$context"/>
                    </xsl:apply-templates>
                </xsl:when>
                <!-- Otherwise, iterate over and display all of them -->
                <xsl:otherwise>
                    <xsl:apply-templates select="mets:file">
                        <xsl:with-param name="context" select="$context"/>
                    </xsl:apply-templates>
                </xsl:otherwise>
            </xsl:choose>
        </table>
    </xsl:template>   
    
    
    <!-- Build a single row in the bitstreams table of the item view page -->
    <xsl:template match="mets:file">
        <xsl:param name="context" select="."/>
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
            </xsl:attribute>
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
                    <xsl:if test="contains(mets:FLocat[@LOCTYPE='URL']/@xlink:href,'isAllowed=n')">
                        <img>
                            <xsl:attribute name="src">
                                <xsl:value-of select="$context-path"/>
                                <xsl:text>/static/icons/lock24.png</xsl:text>
                            </xsl:attribute>
                            <xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.blocked</xsl:attribute>
                            <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
                        </img>
                     </xsl:if>
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
              <xsl:call-template name="getFileTypeDesc">
                <xsl:with-param name="mimetype">
                  <xsl:value-of select="substring-before(@MIMETYPE,'/')"/>
                  <xsl:text>/</xsl:text>
                  <xsl:value-of select="substring-after(@MIMETYPE,'/')"/>
                </xsl:with-param>
              </xsl:call-template>
            </td>
            <td>
                <xsl:choose>
                    <xsl:when test="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                        mets:file[@GROUPID=current()/@GROUPID]">
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
                        <xsl:choose>
                            <xsl:when test="@ADMID">
                                <xsl:call-template name="display-rights"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:call-template name="view-open"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>                        
            </td>
	    <!-- Display the contents of 'Description' as long as at least one bitstream contains a description -->
	    <xsl:if test="$context/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file/mets:FLocat/@xlink:label != ''">
	        <td>
	            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label"/>
	        </td>
	    </xsl:if>

        </tr>
    </xsl:template>
    
    <xsl:template name="view-open">
        <a>
            <xsl:attribute name="href">
                <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
            </xsl:attribute>
            <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
        </a>
    </xsl:template>

    <xsl:template name="display-rights">
        <xsl:variable name="file_id" select="jstring:replaceAll(jstring:replaceAll(string(@ADMID), '_METSRIGHTS', ''), 'rightsMD_', '')"/>
        <xsl:variable name="rights_declaration" select="../../../mets:amdSec/mets:rightsMD[@ID = concat('rightsMD_', $file_id, '_METSRIGHTS')]/mets:mdWrap/mets:xmlData/rights:RightsDeclarationMD"/>
        <xsl:variable name="rights_context" select="$rights_declaration/rights:Context"/>
        <xsl:variable name="users">
            <xsl:for-each select="$rights_declaration/*">
                <xsl:value-of select="rights:UserName"/>
                <xsl:choose>
                    <xsl:when test="rights:UserName/@USERTYPE = 'GROUP'">
                       <xsl:text> (group)</xsl:text>
                    </xsl:when>
                    <xsl:when test="rights:UserName/@USERTYPE = 'INDIVIDUAL'">
                       <xsl:text> (individual)</xsl:text>
                    </xsl:when>
                </xsl:choose>
                <xsl:if test="position() != last()">, </xsl:if>
            </xsl:for-each>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="not ($rights_context/@CONTEXTCLASS = 'GENERAL PUBLIC') and ($rights_context/rights:Permissions/@DISPLAY = 'true')">
                <a href="{mets:FLocat[@LOCTYPE='URL']/@xlink:href}">
                    <img width="64" height="64" src="{concat($theme-path,'/images/Crystal_Clear_action_lock3_64px.png')}" title="Read access available for {$users}"/>
                    <!-- icon source: http://commons.wikimedia.org/wiki/File:Crystal_Clear_action_lock3.png -->
                </a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="view-open"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!--
    File Type Mapping template

    This maps format MIME Types to human friendly File Type descriptions.
    Essentially, it looks for a corresponding 'key' in your messages.xml of this
    format: xmlui.dri2xhtml.mimetype.{MIME Type}
    
    (e.g.) <message key="xmlui.dri2xhtml.mimetype.application/pdf">PDF</message>

    If a key is found, the translated value is displayed as the File Type (e.g. PDF)
    If a key is NOT found, the MIME Type is displayed by default (e.g. application/pdf)
    -->
    <xsl:template name="getFileTypeDesc">
      <xsl:param name="mimetype"/>

      <!--Build full key name for MIME type (format: xmlui.dri2xhtml.mimetype.{MIME type})-->
      <xsl:variable name="mimetype-key">xmlui.dri2xhtml.mimetype.<xsl:value-of select='$mimetype'/></xsl:variable>

      <!--Lookup the MIME Type's key in messages.xml language file.  If not found, just display MIME Type-->
      <i18n:text i18n:key="{$mimetype-key}"><xsl:value-of select="$mimetype"/></i18n:text>
    </xsl:template>


    <!-- Generate the license information from the file section -->
    <xsl:template match="mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']">
        <div class="license-info">
            <p><i18n:text>xmlui.dri2xhtml.METS-1.0.license-text</i18n:text></p>
            <ul>
                <xsl:if test="@USE='CC-LICENSE'">
                    <li><a href="{mets:file/mets:FLocat[@xlink:title='license_text']/@xlink:href}"><i18n:text>xmlui.dri2xhtml.structural.link_cc</i18n:text></a></li>
                </xsl:if>
                <xsl:if test="@USE='LICENSE'">
                    <li><a href="{mets:file/mets:FLocat[@xlink:title='license.txt']/@xlink:href}"><i18n:text>xmlui.dri2xhtml.structural.link_original_license</i18n:text></a></li>
                </xsl:if>
            </ul>
        </div>
    </xsl:template>
    
    
    
    <!-- Generate the logo, if present, from the file section -->
    <xsl:template match="mets:fileGrp[@USE='LOGO']">
        <div class="ds-logo-wrapper">
            <img src="{mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href}" class="logo">
                <xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.collection-logo-alt</xsl:attribute>
                <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
            </img>
        </div>
    </xsl:template>
    
    
</xsl:stylesheet>
