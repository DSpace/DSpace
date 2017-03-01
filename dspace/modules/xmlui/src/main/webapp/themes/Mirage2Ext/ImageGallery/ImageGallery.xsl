<?xml version="1.0" encoding="UTF-8"?>

<!--
    NewImageGallery.xsl
    
    Version: $Revision: 1.0 $
    
    Date: $Date: 2006/07/27 22:54:52 $    
-->

<!--
    Author: Adam Mikeal <adam@mikeal.org>
            Alexey Maslov <alexey@library.tamu.edu>
-->    

<xsl:stylesheet 
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" 
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:url="http://www.jclark.com/xt/java/java.net.URLEncoder"
    exclude-result-prefixes="url">
    
    <xsl:import href="../shared.xsl"/>
    <xsl:output indent="yes"/>

    <!-- inject child theme content into Mirage2 addJavascript template (after jquery is loaded) -->
   <xsl:template name="appendJavaScript">
        <script src="{concat($child-theme-path,'lib/jquery-ui.1.8.6.min.js')}"></script>
        <script src="{concat($child-theme-path,'ImageGallery/ImageGallery.js')}"></script>
    </xsl:template>   
    
    <!-- Global variable to get the repository URL -->
    <xsl:variable name="repository-url">
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request' and @qualifier='scheme'][1]/node()" />
        <xsl:text>://</xsl:text>
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request' and @qualifier='serverName'][1]/node()" />
        <xsl:text>:</xsl:text>
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request' and @qualifier='serverPort'][1]/node()" />
    </xsl:variable>
    
    <!-- Global variable to get the URL to the Djatoka image server from the metadata -->
    <xsl:variable name="image-server-url" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme' and @qualifier='image-server'][1]/node()"/>
    
    <!-- Override the template that decides what to do with the list of items
          (we want to change the <ul> that the TAMU theme normally generates
           into a collection of <div>s) -->
    <xsl:template match="dri:referenceSet[@type = 'summaryList']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <!-- Here we decide whether we have a hierarchical list or a flat one -->
        <xsl:choose>
            <!--  This case is only for the hierarchical lists of collections on the item detail pages -->
            <xsl:when test="descendant-or-self::dri:referenceSet/@rend='hierarchy' or ancestor::dri:referenceSet/@rend='hierarchy'">
                <ul>
                    <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                </ul>
            </xsl:when>
            
            <!--  Our main case: lists of items that appear in browse pages -->
            <xsl:otherwise>
                <div class="image-gallery-tile-set">
                    <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                </div>
                <div class="clear">&#160;</div>
            </xsl:otherwise>
        </xsl:choose>        
    </xsl:template>
    
    <!-- Override the template that generates elements for each item reference in the list --> 
    <xsl:template match="dri:reference" mode="summaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <div class="image-gallery-tile">
            <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList"/>
            <xsl:apply-templates />
        </div>
    </xsl:template>
    
    <!-- We resolve the reference tag to an external mets object --> 
    <xsl:template name="itemSummaryList-DIM">
        <xsl:param name="position"/>
    
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
        </xsl:variable>
        
        <xsl:variable name="item_list_position">
            <xsl:value-of select="$position"></xsl:value-of>
        </xsl:variable>        
        
        <div class="image-gallery-tile-content">
            <!-- Generate the thumbnail and direct file links -->
            <xsl:apply-templates select="." mode="metadataPopup"/>
       </div>
    </xsl:template>
        
   
    <!-- Generate the metadata popup text about the item from the metadata section -->
    <xsl:template match="dim:dim" mode="itemMetadataPopup-DIM">
   
        <!-- display bitstreams -->     
        <xsl:variable name="context" select="ancestor::mets:METS"/>
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <xsl:variable name="image-width" select="dim:field[@element='resolution' and @qualifier='width'][1]/node()"/>
        <xsl:variable name="image-date" select="dim:field[@element='date' and @qualifier='created'][1]/node()"/>    
        <xsl:variable name="image-title">
            <xsl:choose>
                <xsl:when test="dim:field[@element='title']">
                    <xsl:value-of select="dim:field[@element='title'][1]/node()"/>
                </xsl:when>
                <xsl:otherwise><i18n:text>Untitled</i18n:text></xsl:otherwise>
             </xsl:choose>
        </xsl:variable>       
        
        <xsl:apply-templates select="$data" mode="detailView"/>
            <!-- First, figure out if there is a primary bitstream -->
            <xsl:variable name="primary" select="$context/mets:structMap[@TYPE = 'LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:div[@TYPE='DSpace Content Bitstream']/mets:fptr/@FILEID" />
            <xsl:variable name="jp2-url" select="$context/mets:fileSec/mets:fileGrp[@USE = 'CONTENT']/mets:file[@MIMETYPE = 'image/jp2'][1]/mets:FLocat/@xlink:href" />
            <xsl:variable name="jp2-size" select="$context/mets:fileSec/mets:fileGrp[@USE = 'CONTENT']/mets:file[@MIMETYPE = 'image/jp2'][1]/@SIZE" />
            <xsl:variable name="thumb-url" select="$context/mets:fileSec/mets:fileGrp[@USE = 'THUMBNAIL']/mets:file[@MIMETYPE = 'image/jpeg'][1]/mets:FLocat/@xlink:href" />
            <xsl:variable name="bitstream-count" select="count($context/mets:structMap[@TYPE = 'LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:div[@TYPE='DSpace Content Bitstream'])" />
            
            <a class="image-gallery-anchor" alt="Click to view item">
                <xsl:attribute name="title">
                    <xsl:value-of select="$image-title" /> 
                    <xsl:if test="$image-date">
                        <xsl:text> (</xsl:text>
                        <xsl:value-of select="$image-date" />
                        <xsl:text>) </xsl:text>
                    </xsl:if>
                </xsl:attribute>    
                <xsl:attribute name="href">
                    <xsl:value-of select="ancestor::mets:METS/@OBJID"></xsl:value-of>
                </xsl:attribute>
                <!-- 
                <xsl:attribute name="href">
                    <xsl:value-of select="$image-server-url"/>
                    <xsl:text>resolver?url_ver=Z39.88-2004&amp;rft_id=</xsl:text>
                    <xsl:value-of select="url:encode($repository-url)"/>
                    <xsl:value-of select="url:encode($jp2-url)"/>
                    <xsl:text>&amp;svc_id=info:lanl-repo/svc/getRegion&amp;svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000&amp;svc.format=image/jpeg&amp;svc.scale=600</xsl:text>
                </xsl:attribute>
                -->             
                <img class="image-gallery-thumbnail">
                    <xsl:attribute name="src">
                        <!-- <xsl:value-of select="$thumb-url" /> -->
                        <xsl:choose>
                            <xsl:when test="$thumb-url">
                                <xsl:value-of select="$thumb-url" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$image-server-url"/>
                                <xsl:text>resolver?url_ver=Z39.88-2004&amp;rft_id=</xsl:text>
                                <xsl:value-of select="url:encode($repository-url)"/>
                                <xsl:value-of select="url:encode($jp2-url)"/>
                                <xsl:text>&amp;svc_id=info:lanl-repo/svc/getRegion&amp;svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000&amp;svc.format=image/jpeg&amp;svc.scale=192</xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:attribute name="alt">
                        <xsl:value-of select="$image-title"/>
                        <xsl:text> &#160; (click for a larger preview)</xsl:text>
                    </xsl:attribute>
                </img>
            </a>
            
            <div class="image-gallery-title">
                <xsl:choose>
                    <xsl:when test="string-length($image-title) &gt; 28">
                        <xsl:value-of select="substring($image-title, 1, 26)" />&#8230;
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$image-title" />
                    </xsl:otherwise>
                </xsl:choose>
            </div>
            
            <div class="image-gallery-date">
                <xsl:value-of select="$image-date" />
            </div>
    </xsl:template>
    
    <xsl:template name="itemSummaryView-DIM-thumbnail">
        <div class="thumbnail">
            <xsl:choose>
                <xsl:when test="//mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']">
                    <xsl:variable name="src">
                        <xsl:choose>
                                <xsl:when test="/mets:METS/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=../../mets:fileGrp[@USE='CONTENT']/mets:file[@GROUPID=../../mets:fileGrp[@USE='THUMBNAIL']/mets:file/@GROUPID][1]/@GROUPID]">
                                <xsl:value-of
                                        select="/mets:METS/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=../../mets:fileGrp[@USE='CONTENT']/mets:file[@GROUPID=../../mets:fileGrp[@USE='THUMBNAIL']/mets:file/@GROUPID][1]/@GROUPID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                        select="//mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:choose>
                        <xsl:when test="//mets:fileSec/mets:fileGrp/mets:file[@MIMETYPE='image/jp2']">
                            <a href="{$image-server-url}viewer.html?rft_id={url:encode($repository-url)}{url:encode(//mets:fileSec/mets:fileGrp/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href)}">
                                <img alt="Thumbnail">
                                    <xsl:attribute name="src">
                                        <xsl:value-of select="$src"/>
                                    </xsl:attribute>
                                </img>
                            </a> 
                        </xsl:when>
                        <xsl:otherwise>
                            <img alt="Thumbnail">
                                <xsl:attribute name="src">
                                    <xsl:value-of select="$src"/>
                                </xsl:attribute>
                            </img>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <img alt="Thumbnail">
                        <xsl:attribute name="data-src">
                            <xsl:text>holder.js/100%x</xsl:text>
                            <xsl:value-of select="$thumbnail.maxheight"/>
                            <xsl:text>/text:No Thumbnail</xsl:text>
                        </xsl:attribute>
                    </img>
                </xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>
     
 
</xsl:stylesheet>