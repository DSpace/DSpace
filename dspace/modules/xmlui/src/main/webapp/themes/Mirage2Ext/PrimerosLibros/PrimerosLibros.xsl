<?xml version="1.0" encoding="UTF-8"?>

<!--
    PrimerosLibros.xsl
    
    Version: $Revision: 2.0 $
    
    Date: $Date: 2011/06/01 15:04:52 $    
-->

<!--
    Author: Adam Mikeal <adam@mikeal.org>
    Author: James Creel <jcreel@library.tamu.edu>
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

    <!-- inject child theme content into Mirage2 addJavascript template -->
   <xsl:template name="appendJavaScript">
        <script src="{concat($child-theme-path,'PrimerosLibros/lib/js/lightbox.js')}"></script>
        <script src="{concat($child-theme-path,'PrimerosLibros/PrimerosLibros.js')}"></script>
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
    <xsl:variable name="image-server-resolver">
        <xsl:value-of select="$image-server-url"></xsl:value-of>
        <xsl:text>adore-djatoka/resolver</xsl:text>
    </xsl:variable>
    <!--<xsl:variable name="django-app-url"><xsl:text>http://h012.library.tamu.edu/book/oai:labs.library.tamu.edu:</xsl:text></xsl:variable>-->
    <xsl:variable name="django-app-url" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme' and @qualifier='django-app'][1]/node()"/>
    
    
    <!-- Override the template that generates elements for each item reference in the list --> 
    <xsl:template match="dri:referenceSet[@rend='recent-submissions']/dri:reference" mode="summaryList" priority="5">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
        </xsl:variable>
        <xsl:comment> No: <xsl:value-of select="position()"/></xsl:comment>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <!-- Override the recent submissions listing to only display the last four -->
        <xsl:if test="position() &lt; 5">
            <div class="image-gallery-tile">
                <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList"/>
                <xsl:apply-templates />
            </div>
        </xsl:if>
    </xsl:template>


    <!-- Override the template that generates elements for each item reference in the list --> 
    <xsl:template match="dri:reference" mode="summaryList" priority="4">
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
            
            <!-- Generate the title from the metadata section -->
            <!-- <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemSummaryList-DIM"/> -->
                
            <!-- Generate the thunbnail, if present, from the file section -->
            <!--  <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>  -->
                                             
       </div>

    </xsl:template>
    
    
    <!-- 
    <xsl:template match="dri:reference" mode="summaryView">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!- No options selected, render the full METS document ->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <div class="image-gallery-tile-set">
            <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryView"/>
            <xsl:apply-templates />
        </div>
    </xsl:template>
     -->

     <xsl:template name="itemSummaryView-DIM-file-section" />
    
    <!-- Viewing an individual folio --> 
    <xsl:template name="itemSummaryView-DIM">
        
        <xsl:variable name="bitstreamMetadataBitstreamURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="./mets:fileSec/mets:fileGrp[@USE='METADATA']/mets:file/mets:FLocat[@xlink:title='bitstream_metadata.xml']/@xlink:href"/>
        </xsl:variable>
        
        <!-- /metadata/handle/123456789/88281/mets.xml -->
        
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
            mode="itemSummaryView-DIM"/>
        
        <a>
            <xsl:attribute name="href">
                <xsl:value-of select="$bitstreamMetadataBitstreamURL"/>
            </xsl:attribute>
            View Bitstream Metadata XML
        </a>
        
        <p class="linkbox">
            <!--<span>View book as: </span>-->
            <a>
                <xsl:attribute name="href">
                     <xsl:value-of select="./mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@MIMETYPE='application/pdf']/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                </xsl:attribute>
                <span class="linkboxlink" id="pdflink">
                     <xsl:text>Download PDF</xsl:text>
                </span>
            </a>
        </p>

        <!-- display the tiled pages of the libro -->
        <div class="image-gallery-tile-set">
            <xsl:choose>
                <!-- if bitstream metadata bitstream is present, display pages in accordance with the <pages> listing -->
                <xsl:when test="string-length($bitstreamMetadataBitstreamURL) &gt; string-length('cocoon:/')">
                    <xsl:apply-templates select="document($bitstreamMetadataBitstreamURL)" mode="libro-page-tiles"/>                    
                </xsl:when>
                <!-- if bitstream metadata bitstream is not present, display pages in titular order -->
                <xsl:otherwise>
                    <xsl:for-each select="mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@MIMETYPE='application/octet-stream' or @MIMETYPE = 'image/jp2']">
                        
                        <xsl:sort data-type="text" order="ascending" select="./mets:FLocat/@xlink:title"/>
                        <div class="image-gallery-tile">
                            <div class="image-gallery-tile-content">
                                
                                <!--  Link to the image -->
                                <xsl:variable name="image-title" select="./mets:FLocat/@xlink:title"/>
                                <xsl:variable name="jp2-url" select="./mets:FLocat/@xlink:href" />
                                <xsl:variable name="jp2-size" select="@SIZE" />
                                <!-- Weird ways to avoid catching the system-generated thumbnail -->
                                <xsl:variable name="thumb-url" select="../../mets:fileGrp[@USE = 'THUMBNAIL']/mets:file[@MIMETYPE = 'image/jpeg']/mets:FLocat[substring(@xlink:title, string-length(@xlink:title) - string-length(concat(substring-before($image-title,'.'),'.jpg')) + 1) = concat(substring-before($image-title,'.'),'.jpg')]/@xlink:href" />
                                
                                <!-- 
                                    <span><xsl:value-of select="count(../../mets:fileGrp[@USE = 'THUMBNAIL'])"/></span>
                                    <span><xsl:value-of select="count(../../mets:fileGrp[@USE = 'THUMBNAIL']/mets:file[@MIMETYPE = 'image/jpeg'])"/></span>
                                    <span><xsl:value-of select="substring-before($image-title,'.')"/></span>
                                    <span><xsl:value-of select="concat(substring-before($image-title,'.'),'.jpg')"/></span>
                                    
                                    <span><xsl:value-of select="contains('thumbs/pl_tamu_012_00001.jpg',concat(substring-before($image-title,'.'),'.jpg'))"/></span>
                                    <span><xsl:value-of select="substring('thumbs/pl_tamu_012_00001.jpg', string-length('thumbs/pl_tamu_012_00001.jpg') - string-length(concat(substring-before($image-title,'.'),'.jpg')) + 1) = concat(substring-before($image-title,'.'),'.jpg')"/></span>
                                    <span><xsl:value-of select="'thumbs/pl_tamu_012_00001.jpg' = concat('thumbs/',concat(substring-before($image-title,'.'),'.jpg'))"/></span>
                                    
                                    <span><xsl:value-of select="count(../../mets:fileGrp[@USE = 'THUMBNAIL']/mets:file[@MIMETYPE = 'image/jpeg']/mets:FLocat[contains(@xlink:title,concat(substring-before($image-title,'.'),'.jpg'))]/@xlink:href)"/></span>
                                    Ghetto implementation of an "ends-with()" function that seems to be missing in XSL
                                    <span><xsl:value-of select="count(../../mets:fileGrp[@USE = 'THUMBNAIL']/mets:file[@MIMETYPE = 'image/jpeg']/mets:FLocat[substring(@xlink:title, string-length(@xlink:title) - string-length(concat(substring-before($image-title,'.'),'.jpg')) + 1) = concat(substring-before($image-title,'.'),'.jpg')]/@xlink:href)"/></span>
                                    
                                    <span><xsl:value-of select="../../mets:fileGrp[@USE = 'THUMBNAIL']/mets:file[@MIMETYPE = 'image/jpeg']/mets:FLocat[contains(@xlink:title,concat(substring-before($image-title,'.'),'.jpg'))]/@xlink:href"/></span>
                                -->
                                
                                <a rel="lightbox" alt="Click for a larger preview" title="View preview" class="lightboxlink">
                                    <!--<xsl:attribute name="title">
                                        <xsl:value-of select="substring($image-title,9)" /> 
                                        </xsl:attribute>--> 
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="$image-server-resolver"/>
                                        <xsl:text>?url_ver=Z39.88-2004&amp;rft_id=</xsl:text>
                                        <xsl:value-of select="url:encode($repository-url)"/>
                                        <xsl:value-of select="url:encode($jp2-url)"/>
                                        <xsl:text>&amp;svc_id=info:lanl-repo/svc/getRegion&amp;svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000&amp;svc.format=image/jpeg&amp;svc.scale=600</xsl:text>
                                    </xsl:attribute>
                                    
                                    <img class="image-gallery-thumbnail">
                                        <xsl:attribute name="src">
                                            <xsl:choose>
                                                <xsl:when test="$thumb-url">
                                                    <xsl:value-of select="$thumb-url" />
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:value-of select="$image-server-resolver"/>
                                                    <xsl:text>?url_ver=Z39.88-2004&amp;rft_id=</xsl:text>
                                                    <xsl:value-of select="url:encode($repository-url)"/>
                                                    <xsl:value-of select="url:encode($jp2-url)"/>
                                                    <xsl:text>&amp;svc_id=info:lanl-repo/svc/getRegion&amp;svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000&amp;svc.format=image/jpeg&amp;svc.scale=172</xsl:text>
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
                                    <xsl:value-of select="substring($image-title,9)" />
                                    <!-- 
                                        <xsl:choose>
                                        
                                        <xsl:when test="string-length($image-title) &gt; 18">
                                        <xsl:value-of select="substring($image-title, 1, 16)" />&#8230;
                                        </xsl:when>
                                        <xsl:otherwise>
                                        <xsl:value-of select="$image-title" />
                                        </xsl:otherwise>
                                        </xsl:choose>
                                    -->
                                </div>
                                
                                <div class="image-gallery-date">
                                    <xsl:text>Page </xsl:text>
                                    <xsl:value-of select="substring-before(substring($image-title,15),'.')" />
                                </div>
                                <div class="image-gallery-links">
                                    <!--
                                        <a target="_new">
                                        <xsl:attribute name="href">
                                        <xsl:value-of select="$image-server-url"/>
                                        <xsl:text>viewer.html?rft_id=</xsl:text>
                                        <xsl:value-of select="url:encode($repository-url)"/>
                                        <xsl:value-of select="url:encode($jp2-url)"/>
                                        </xsl:attribute>
                                        
                                        <img src="{$child-theme-path}/PrimerosLibros/imgs/image.jpg" alt="View detailed image" title="View detailed image" />
                                        </a>
                                    -->

                                     <span class="image-gallery-save-options" style="display: none;">
                                        <a href="{$jp2-url}">
                                            <xsl:text>Download original JPEG 2000 (</xsl:text>
                                            <xsl:choose>
                                                <xsl:when test="$jp2-size &lt; 1000000">
                                                    <xsl:value-of select="substring(string($jp2-size div 1000),1,5)"/>
                                                    <xsl:text>Kb) </xsl:text>
                                                </xsl:when>
                                                <xsl:when test="$jp2-size &lt; 1000000000">
                                                    <xsl:value-of select="substring(string($jp2-size div 1000000),1,5)"/>
                                                    <xsl:text>Mb) </xsl:text>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:value-of select="substring(string($jp2-size div 1000000000),1,5)"/>
                                                    <xsl:text>Gb) </xsl:text>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </a>
                                        <a>
                                            <xsl:attribute name="href">
                                                <xsl:value-of select="$image-server-resolver"/>
                                                <xsl:text>?url_ver=Z39.88-2004&amp;rft_id=</xsl:text>
                                                <xsl:value-of select="url:encode($repository-url)"/>
                                                <xsl:value-of select="url:encode($jp2-url)"/>
                                                <xsl:text>&amp;svc_id=info:lanl-repo/svc/getRegion&amp;svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000&amp;svc.format=image/jpeg&amp;svc.level=6</xsl:text>
                                            </xsl:attribute>
                                            <xsl:text>Download JPEG derivative</xsl:text>
                                        </a>
                                    </span>
                                    <span class="image-gallery-save-link"> 
                                        <!--    <xsl:attribute name="onClick">
                                            <xsl:text>if (jQuery(this).next().css('display')=='block') {jQuery(this).next().css('display', 'none'); } else { jQuery(this).next().css('display', 'block'); } return false;</xsl:text>
                                            </xsl:attribute>
                                        -->
                                        <img src="{$child-theme-path}/PrimerosLibros/imgs/save_icon.jpg" alt="Download image" title="Download image" />
                                    </span>
                                    <a>
                                        <xsl:attribute name="href">
                                            <xsl:value-of select="$django-app-url"/>
                                            <xsl:value-of select="substring-after(ancestor::mets:METS/@ID,':')"/>
                                            <xsl:text>/pages/?sequence=</xsl:text>
                                            <xsl:value-of select="substring-before(substring($image-title,15),'.')"/>
                                        </xsl:attribute>
                                        <img src="{$child-theme-path}/PrimerosLibros/imgs/filmstrip_icon.png" alt="Detailed view of this page" title="Detailed view of this page" />
                                    </a>
                                </div><!-- end of image gallery links -->
                            </div><!-- end of image gallery tile content -->
                        </div><!-- end of image gallery tile -->
                    </xsl:for-each>
                </xsl:otherwise>
            </xsl:choose>
            
            

            <!-- Add a scaling factor to the lightbox images based on size of the browser screen -->
            <script type="text/javascript">
            <![CDATA[
              var links = $$('a.lightboxlink');
              var ref = 0;

              for (var i=0; i<links.length; i++)
              {
                ref = links[i].readAttribute('href');
                ref = ref.substring(0, ref.length-3).concat(document.viewport.getHeight()-100);
                links[i].setAttribute('href',ref);
              }
            ]]>
            </script>
        </div><!-- end image-gallery-tile-set div -->
        
        <p class="linkbox">
            <!--<span>View book as: </span>-->
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="./mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@MIMETYPE='application/pdf']/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                </xsl:attribute>
                <span class="linkboxlink" id="pdflink">
                    <xsl:text>Download PDF</xsl:text>
                </span>
            </a>
        </p>
    </xsl:template>
        
   
    <!-- A book in the tiled view -->
    <xsl:template match="dim:dim" mode="itemMetadataPopup-DIM">
        <!--
        <xsl:variable name="bitstreamMetadataBitstreamURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="ancestor::mets:METS/mets:fileSec/mets:fileGrp[@USE='METADATA']/mets:file/mets:FLocat[@xlink:title='bitstream_metadata.xml']/@xlink:href"/>
        </xsl:variable>
        -->
        
        <!-- display bitstreams -->     
        <xsl:variable name="context" select="ancestor::mets:METS"/>
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <xsl:variable name="image-width" select="dim:field[@element='resolution' and @qualifier='width'][1]/node()"/>
        <xsl:variable name="image-date" select="dim:field[@element='date' and @qualifier='created'][1]/node()"/>
        <xsl:variable name="book-type" select="dim:field[@element='type'][1]/node()"/>
        <xsl:variable name="image-title">
            <xsl:choose>
                <xsl:when test="dim:field[@element='title']">
                    <xsl:value-of select="dim:field[@element='title'][1]/node()"/>
                </xsl:when>
                <xsl:otherwise>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                </xsl:otherwise>
             </xsl:choose>
        </xsl:variable>       
        
        <xsl:apply-templates select="$data" mode="detailView"/>
            <!-- First, figure out if there is a primary bitstream -->
            <xsl:variable name="primary" select="$context/mets:structMap[@TYPE = 'LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID" />
            <xsl:variable name="jp2-url" select="$context/mets:fileSec/mets:fileGrp[@USE = 'CONTENT']/mets:file[@MIMETYPE = 'image/jp2'][1]/mets:FLocat/@xlink:href" />
            <xsl:variable name="jp2-size" select="$context/mets:fileSec/mets:fileGrp[@USE = 'CONTENT']/mets:file[@MIMETYPE = 'image/jp2'][1]/@SIZE" />
            <xsl:variable name="thumb-url" select="$context/mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@element='thumbnail']" />
            <xsl:variable name="thumb-url-alt" select="$context/mets:fileSec/mets:fileGrp[@USE = 'THUMBNAIL']/mets:file[@MIMETYPE = 'image/jpeg'][1]/mets:FLocat/@xlink:href" />
            <xsl:variable name="bitstream-count" select="count($context/mets:structMap[@TYPE = 'LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:div[@TYPE='DSpace Content Bitstream'])" />
            
           
            
            <a alt="Click to enter book" href="{ancestor::mets:METS/@OBJID}">
                <xsl:attribute name="title">
                    <xsl:value-of select="$image-title" /> 
                    <xsl:if test="$image-date">
                        <xsl:text> (</xsl:text>
                        <xsl:value-of select="$image-date" />
                        <xsl:text>) </xsl:text>
                    </xsl:if>
                </xsl:attribute>
                <img class="image-gallery-thumbnail">
                    <xsl:attribute name="src">
                  <xsl:choose>
                    <xsl:when test="$bitstream-count = '0'">
                      <xsl:value-of select="$child-theme-path"/><xsl:text>/PrimerosLibros/imgs/blank_book.png</xsl:text>
                    </xsl:when>
                    <!-- use url alt as temp hack -->
                    <xsl:otherwise>
                      <xsl:value-of select="$thumb-url-alt"/>
                    </xsl:otherwise>  
                    <!-- temp hack commented out thumbnail handling 
                    <xsl:when test="$thumb-url">
                      <xsl:value-of select="$thumb-url"/>
                    </xsl:when>
                    <xsl:when test="$thumb-url-alt">
                      <xsl:value-of select="$thumb-url-alt" />
                            </xsl:when>
                            <xsl:otherwise>
                      <xsl:value-of select="$image-server-resolver"/>
                                <xsl:text>?url_ver=Z39.88-2004&amp;rft_id=</xsl:text>
                                <xsl:value-of select="url:encode($repository-url)"/>
                                <xsl:value-of select="url:encode($jp2-url)"/>
                                <xsl:text>&amp;svc_id=info:lanl-repo/svc/getRegion&amp;svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000&amp;svc.format=image/jpeg&amp;svc.scale=172</xsl:text>
                            </xsl:otherwise> -->
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:attribute name="alt">
                        <xsl:value-of select="$image-title"/>
                        <xsl:text> &#160; (Click to enter book)</xsl:text>
                    </xsl:attribute>
                </img>
            </a>
            
            <div class="image-gallery-title">
                <xsl:choose>
                    <xsl:when test="string-length($image-title) &gt; 18">
                        <xsl:value-of select="substring($image-title, 1, 16)" />&#8230;
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$image-title" />
                    </xsl:otherwise>
                </xsl:choose>
            </div>
           
            <div class="image-gallery-type">
                <!-- <xsl:apply-templates select="document($bitstreamMetadataBitstreamURL)" mode="renderBookType"/> -->    
                <!--
                <xsl:if test="$bitstream-count = '0'">
                    <xsl:text>(Abstract record)</xsl:text>
                </xsl:if>
                -->
                <xsl:choose>
                    <xsl:when test="$book-type = 'work'">
                        <xsl:text>(Abstract record)</xsl:text>
                    </xsl:when>
                    <xsl:when test="$book-type = 'canonical'">
                        <xsl:text>(Canonical record)</xsl:text>
                    </xsl:when>
                    <xsl:when test="$book-type = 'frankenbook'">
                        <xsl:text>(Frankenbook)</xsl:text>
                    </xsl:when>
                </xsl:choose>
            </div>
            
            <div class="image-gallery-date">
                <xsl:value-of select="$image-date" />
            </div>
            
            <div class="image-gallery-links">
              <!--
                <a href="{ancestor::mets:METS/@OBJID}">
                    <img src="{$child-theme-path}/PrimerosLibros/imgs/detail_icon.jpg" alt="View item details and metadata" title="View item details and metadata" />
                </a>-->
                <xsl:if test="not($bitstream-count = '0')">
                <span class="image-gallery-save-options" style="display: none;">
                    <a href="{$jp2-url}">
                     <xsl:text>Download original JPEG 2000 (</xsl:text>
                     <xsl:choose>
                         <xsl:when test="$jp2-size &lt; 1000000">
                             <xsl:value-of select="substring(string($jp2-size div 1000),1,5)"/>
                             <xsl:text>Kb) </xsl:text>
                         </xsl:when>
                         <xsl:when test="$jp2-size &lt; 1000000000">
                             <xsl:value-of select="substring(string($jp2-size div 1000000),1,5)"/>
                             <xsl:text>Mb) </xsl:text>
                         </xsl:when>
                         <xsl:otherwise>
                             <xsl:value-of select="substring(string($jp2-size div 1000000000),1,5)"/>
                             <xsl:text>Gb) </xsl:text>
                          </xsl:otherwise>
                      </xsl:choose>
                       </a>
                   <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="$image-server-resolver"/>
                        <xsl:text>?url_ver=Z39.88-2004&amp;rft_id=</xsl:text>
                        <xsl:value-of select="url:encode($repository-url)"/>
                        <xsl:value-of select="url:encode($jp2-url)"/>
                        <xsl:text>&amp;svc_id=info:lanl-repo/svc/getRegion&amp;svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000&amp;svc.format=image/jpeg&amp;svc.level=6</xsl:text>
                    </xsl:attribute>
                    <xsl:text>Download JPEG derivative</xsl:text>
                  </a>
                </span> 
                <span class="image-gallery-save-link"> 
                  <!--  <xsl:attribute name="onClick">
                        <xsl:text>if (jQuery(this).next().css('display')=='block') {jQuery(this).next().css('display', 'none'); } else { jQuery(this).next().css('display', 'block'); } return false;</xsl:text>
                        </xsl:attribute>
                    -->     <img src="{$child-theme-path}/PrimerosLibros/imgs/save_icon.jpg" alt="Download other sizes" title="Download other sizes" />
                
                </span>
                <a target="_new">
                    <xsl:attribute name="href">
                        <xsl:value-of select="$django-app-url"/>
                        <xsl:value-of select="substring-after(ancestor::mets:METS/@ID,':')"/>
                    </xsl:attribute>
                    <img src="{$child-theme-path}/PrimerosLibros/imgs/image.jpg" alt="View book in book reader" title="View book in book reader" />
                </a>
               </xsl:if> 
            </div>
      
    </xsl:template>
     
    <xsl:template match="book" mode="renderBookType">
        <xsl:variable name="bookType" select = "@type"/>
        <xsl:choose>
            <xsl:when test="$bookType = 'work'">
                <div>Abstract</div>                
            </xsl:when>
            <xsl:when test="$bookType = 'canonical'">
                <div>Canonical</div>                
            </xsl:when>
            <xsl:when test="$bookType = 'frankenbook'">
                <div>Frankenbook</div>                
            </xsl:when>
        </xsl:choose>
        
    </xsl:template>
    
    <xsl:template match="book" mode="libro-page-tiles">
        <xsl:apply-templates select="./exterior/*[not(@missing='true')]" mode="page-tile"/>
        <xsl:for-each select="./interior[not(@missing='true')]">
            <xsl:apply-templates mode="page-tile"/>
        </xsl:for-each> 
    </xsl:template>



    <xsl:template match="*" mode="page-tile">
        <div class="image-gallery-tile">
            <div class="image-gallery-tile-content">
                <!--  Link to the image -->
                <xsl:variable name="image-title">
                    <xsl:choose>
                        <xsl:when test="@title">
                            <xsl:value-of select="@title"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="name(.)"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="jp2-handle" select="./handle[@mimetype='image/jp2']" />
                <!-- <xsl:variable name="jp2-size" select="@SIZE" /> -->
                <!-- Weird ways to avoid catching the system-generated thumbnail -->
                <xsl:variable name="thumb-handle" select="./handle[@type='thumbnail']" />
                <a rel="lightbox" alt="Click for a larger preview" title="View preview" class="lightboxlink">
                    <xsl:attribute name="href">
                        <xsl:value-of select="$image-server-resolver"/>
                        <xsl:text>?url_ver=Z39.88-2004&amp;rft_id=</xsl:text>
                        <xsl:value-of select="url:encode($repository-url)"/>
                        <xsl:value-of select="url:encode($jp2-handle)"/>
                        <xsl:text>&amp;svc_id=info:lanl-repo/svc/getRegion&amp;svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000&amp;svc.format=image/jpeg&amp;svc.scale=600</xsl:text>
                    </xsl:attribute>
                    <img class="image-gallery-thumbnail">
                        <xsl:attribute name="src">
                            <xsl:choose>
                                <xsl:when test="$thumb-handle">
                                    <xsl:value-of select="$thumb-handle" />
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="$image-server-resolver"/>
                                    <xsl:text>?url_ver=Z39.88-2004&amp;rft_id=</xsl:text>
                                    <xsl:value-of select="url:encode($repository-url)"/>
                                    <xsl:value-of select="url:encode($jp2-handle)"/>
                                    <xsl:text>&amp;svc_id=info:lanl-repo/svc/getRegion&amp;svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000&amp;svc.format=image/jpeg&amp;svc.scale=172</xsl:text>
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
                    <xsl:value-of select="$image-title" />
                </div>
                <!-- <div class="image-gallery-date">
                    <xsl:value-of select="$image-title" />
                </div> -->
                <div class="image-gallery-links">
                    <span class="image-gallery-save-link"> 
                        <img src="{$child-theme-path}/PrimerosLibros/imgs/save_icon.jpg" alt="Download image" title="Download image" />
                    </span>
                    <span class="image-gallery-save-options" style="display: none;">
                        <a href="{$jp2-handle}">
                            <xsl:text>Download original JPEG 2000 </xsl:text>
                        </a>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="$image-server-resolver"/>
                                <xsl:text>?url_ver=Z39.88-2004&amp;rft_id=</xsl:text>
                                <xsl:value-of select="url:encode($repository-url)"/>
                                <xsl:value-of select="url:encode($jp2-handle)"/>
                                <xsl:text>&amp;svc_id=info:lanl-repo/svc/getRegion&amp;svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000&amp;svc.format=image/jpeg&amp;svc.level=6</xsl:text>
                            </xsl:attribute>
                            <xsl:text>Download JPEG derivative</xsl:text>
                        </a>
                    </span>
                     <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="$django-app-url"/>
                            <xsl:value-of select="substring-after(ancestor::mets:METS/@ID,':')"/>
                            <xsl:text>/pages/?sequence=</xsl:text>
                            <xsl:value-of select="substring-before(substring($image-title,15),'.')"/>
                        </xsl:attribute>
                        <img src="{$child-theme-path}/PrimerosLibros/imgs/filmstrip_icon.png" alt="Detailed view of this page" title="Detailed view of this page" />
                    </a>
                </div><!-- end of image gallery links -->
            </div><!-- end of image gallery tile content -->
        </div><!-- end of image gallery tile -->
    </xsl:template>
</xsl:stylesheet>