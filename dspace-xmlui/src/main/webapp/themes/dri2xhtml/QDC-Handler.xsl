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

<xsl:stylesheet 
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns="http://www.w3.org/1999/xhtml"
    exclude-result-prefixes="i18n dri mets dc xlink dcterms xsl">
    
    <xsl:output indent="yes"/>
    
    
    
       
    <!-- 
        The summaryList display type; used to generate simple surrogates for the item involved 
    -->
    
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='QDC' or @MDTYPE='DC']]" mode="summaryList">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemSummaryList-QDC"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionSummaryList-QDC"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communitySummaryList-QDC"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    <!-- 
        The templates that handle the respective cases of summaryList: item, collection, and community 
    -->
    
    <!-- An item rendered in the summaryList pattern. Commonly encountered in various browse-by pages
        and search results. -->
    <xsl:template name="itemSummaryList-QDC">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='QDC' or @MDTYPE='DC']/mets:xmlData/*"
            mode="itemSummaryList-QDC"/>
        <!-- Generate the thunbnail, if present, from the file section -->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']"/>
    </xsl:template>
    
    
    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="dcterms:qualifieddc | dc:simpledc " mode="itemSummaryList-QDC"> 
        <div class="artifact-description">
            <div class="artifact-title">
                <!-- Put down the title -->
                <a href="{ancestor::mets:METS/@OBJID}">
                    <xsl:choose>
                        <xsl:when test="dc:title">
                            <xsl:copy-of select="dc:title[1]/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </div>
            <div class="artifact-info">
                <!-- Put down the author -->
                <span class="author">
                    <xsl:choose>
                        <xsl:when test="dc:contributor">
                            <xsl:copy-of select="dc:contributor[1]/child::node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </span>
                <!-- Put down the date -->
                <xsl:choose>
                    <xsl:when test="name(.)= 'qualifieddc' ">             
                        <span class="date">(<xsl:copy-of select="substring(dcterms:issued/child::node(),1,10)"/>)</span>
                    </xsl:when>
                    <xsl:otherwise>
                        <span class="date">(<xsl:copy-of select="substring(dc:date[1]/child::node(),1,10)"/>)</span>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
        </div>
    </xsl:template>
        
    <xsl:template name="collectionSummaryList-QDC">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.qdc-not-applicable</i18n:text>
    </xsl:template>
    
    <xsl:template name="communitySummaryList-QDC">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.qdc-not-applicable</i18n:text>
    </xsl:template>
    
    
    
    
    
    
    <!-- 
        The detailList display type; used to generate simple surrogates for the item involved, but with
        a slightly higher level of information provided. Not commonly used. 
    -->
    
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='QDC' or @MDTYPE='DC']]" mode="detailList">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemDetailList-QDC"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionDetailList-QDC"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communityDetailList-QDC"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- An item rendered in the summaryList pattern. Commonly encountered in various browse-by pages
        and search results. -->
    <xsl:template name="itemDetailList-QDC">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='QDC' or @MDTYPE='DC']/mets:xmlData/*"
            mode="itemSummaryList-QDC"/>
        <!-- Generate the thunbnail, if present, from the file section -->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']"/>
    </xsl:template>
    
    <!-- The detailList of communities and collections basically adds a paragraph of info to the mix. -->
    <xsl:template name="collectionDetailList-QDC">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.qdc-not-applicable</i18n:text>
    </xsl:template>
    
    <xsl:template name="communityDetailList-QDC">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.qdc-not-applicable</i18n:text>
    </xsl:template>
    
    
    
    
    
    
    
    
    <!-- 
        The summaryView display type; used to generate a near-complete view of the item involved. It is currently
        not applicable to communities and collections. 
    -->
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='QDC' or @MDTYPE='DC']]" mode="summaryView">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemSummaryView-QDC"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionSummaryView-QDC"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communitySummaryView-QDC"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- An item rendered in the summaryView pattern. This is the default way to view a DSpace item in Manakin. -->
    <xsl:template name="itemSummaryView-QDC">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='QDC' or @MDTYPE='DC']/mets:xmlData/*"
            mode="itemSummaryView-QDC"/>
        
        <!-- Generate the bitstream information from the file section -->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT']">
            <xsl:with-param name="context" select="."/>
            <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
        </xsl:apply-templates>
        
        <!-- Generate the license information from the file section -->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>
        
    </xsl:template>
    
    
    
    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="dcterms:qualifieddc | dc:simpledc" mode="itemSummaryView-QDC">
        <table class="ds-includeSet-table">
            <!--
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-preview</i18n:text>:</span></td>
                <td>
                    <xsl:choose>
                        <xsl:when test="mets:METS/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']">
                            <a class="image-link">
                                <xsl:attribute name="href"><xsl:value-of select="@url"/></xsl:attribute>
                                <img alt="Thumbnail">
                                    <xsl:attribute name="src">
                                        <xsl:value-of select="mets:METS/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                            mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                    </xsl:attribute>
                                </img>
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-preview</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>-->
            <tr class="ds-table-row even">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-title</i18n:text>:</span></td>
                <td>
                    <xsl:choose>
                        <xsl:when test="dc:title">
                            <xsl:copy-of select="dc:title[1]/child::node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author</i18n:text>:</span></td>
                <td><xsl:copy-of select="dc:contributor[1]/child::node()"/></td>
            </tr>
            <tr class="ds-table-row even">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text>:</span></td>
                <td><xsl:copy-of select="dcterms:abstract/child::node()"/></td>
            </tr>
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-description</i18n:text>:</span></td>
                <td><xsl:copy-of select="dc:description/child::node()"/></td>
            </tr>
            <tr class="ds-table-row even">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-uri</i18n:text>:</span></td>
                <td>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:copy-of select="dc:identifier[@type='dcterms:URI'][1]/child::node()"/>
                        </xsl:attribute>
                        <xsl:copy-of select="dc:identifier[@type='dcterms:URI'][1]/child::node()"/>
                    </a>
                </td>
            </tr>
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-date</i18n:text>:</span></td>
                <td><xsl:copy-of select="substring(dcterms:issued/child::node(),1,10)"/></td>
            </tr>
        </table>
        
    </xsl:template>
    
    
    
    <!-- The summaryView of communities and collections is generally undefined. -->
    <xsl:template name="collectionSummaryView-QDC">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.qdc-not-applicable</i18n:text>
    </xsl:template>
    
    <xsl:template name="communitySummaryView-QDC">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.qdc-not-applicable</i18n:text>
    </xsl:template>
    
    
    
    
    
    
    
    <!-- 
        The detailView display type; used to generate a complete view of the object involved. It is currently
        used with the "full item record" view of items as well as the default views of communities and collections. 
    -->
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='QDC' or @MDTYPE='DC']]" mode="detailView">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemDetailView-QDC"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionDetailView-QDC"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communityDetailView-QDC"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    
    <!-- An item rendered in the detailView pattern, the "full item record" view of a DSpace item in Manakin. -->
    <xsl:template name="itemDetailView-QDC">
        
        <!-- Output all of the metadata about the item from the metadata section -->
        <xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='QDC' or @MDTYPE='DC']/mets:xmlData/*"
            mode="itemDetailView-QDC"/>
        
        <!-- Generate the bitstream information from the file section -->
        <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CONTENT']">
            <xsl:with-param name="context" select="."/>
            <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
        </xsl:apply-templates>
        
        <!-- Generate the license information from the file section -->
        <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>
        
    </xsl:template>
    
        
    <!-- The block of templates used to render the qdc contents of a DRI object -->
    <xsl:template match="dcterms:qualifieddc | dc:simpledc " mode="itemDetailView-QDC" priority="2">
        <h3 class="ds-includeSet-table-header"><i18n:text>xmlui.dri2xhtml.METS-1.0.header-qdc-elements</i18n:text></h3>
		<table class="ds-includeSet-table">
		    <xsl:for-each select="*[namespace-uri(.)='http://purl.org/dc/elements/1.1/']">
                <tr>
                    <xsl:attribute name="class">
                        <xsl:text>ds-table-row </xsl:text>
                        <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                        <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
                    </xsl:attribute>
                    <td>
                        <xsl:text>dc.</xsl:text>
                        <xsl:value-of select="local-name(.)"/>
                        <xsl:if test="./@type">
                            <xsl:text>.</xsl:text>
                            <xsl:value-of select="./@type"/>
                        </xsl:if>
                    </td>
                    <td><xsl:copy-of select="./child::node()"/></td>
                    <td><xsl:value-of select="./@xml:lang"/></td>
                </tr>
            </xsl:for-each>
		</table>
        <h3 class="ds-includeSet-table-header"><i18n:text>xmlui.dri2xhtml.METS-1.0.header-qdc-terms</i18n:text></h3>
		<table class="ds-includeSet-table">
		    <xsl:for-each select="*[namespace-uri(.)='http://purl.org/dc/terms/']">
                <tr>
                    <xsl:attribute name="class">
                        <xsl:text>ds-table-row </xsl:text>
                        <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                        <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
                    </xsl:attribute>
                    <td>
                        <xsl:text>dcterms.</xsl:text>
                        <xsl:value-of select="local-name(.)"/>
                        <xsl:if test="./@type">
                            <xsl:text>.</xsl:text>
                            <xsl:value-of select="./@type"/>
                        </xsl:if>
                    </td>
                    <td><xsl:copy-of select="./child::node()"/></td>
                    <td><xsl:value-of select="./@xml:lang"/></td>
                </tr>
            </xsl:for-each>
		</table>
	</xsl:template>	
	
	    
    <!-- The detailView of communities and collections basically adds a paragraph of info to the mix. -->
    <xsl:template name="collectionDetailView-QDC">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.qdc-not-applicable</i18n:text>
    </xsl:template>
    
    <xsl:template name="communityDetailView-QDC">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.qdc-not-applicable</i18n:text>
    </xsl:template>
      
    
</xsl:stylesheet>
