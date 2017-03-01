<?xml version="1.0" encoding="UTF-8"?>

<!--
    File: $File: geofolios.xsl $
    Version: $Revision: 1.2.1 $    
    Date: $Date: 2014/09/29 1:38:00 $    
    Author: $Author: Maslov, Alexey $
-->    

<xsl:stylesheet 
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" 
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:confman="org.dspace.core.ConfigurationManager"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    
    <xsl:import href="../shared.xsl"/>
    <xsl:output indent="yes"/>

    <xsl:template name="appendJavaScript">
            <!-- Add javascipt  -->            
            <xsl:variable name="jqueryUIVersion"><xsl:text>1.10.4</xsl:text></xsl:variable>

            <xsl:variable name="protocol">
                <xsl:choose>
                    <xsl:when test="starts-with(confman:getProperty('dspace.baseUrl'), 'https://')">
                        <xsl:text>https://</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>http://</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <script type="text/javascript" src="{concat($protocol, 'ajax.googleapis.com/ajax/libs/jqueryui/', $jqueryUIVersion ,'/jquery-ui.min.js')}">&#160;</script>
            
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript']">
                <script type="text/javascript">
                    <xsl:attribute name="src">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/themes/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                    &#160;   
                </script>
            </xsl:for-each>
            
            <!-- Javascript for geofolio map -->
            <script src="http://openlayers.org/en/v3.0.0/resources/bootstrap/js/bootstrap.min.js" type="text/javascript"></script>
            <script src="http://openlayers.org/en/v3.0.0/resources/example-behaviour.js" type="text/javascript"></script>
            <script src="http://openlayers.org/en/v3.0.0/build/ol.js" type="text/javascript">
                <xsl:text>&#160;</xsl:text>
            </script>
                    
                    <script type="text/javascript">
                        <![CDATA[
                            
                            var container = document.getElementById('popup');
                            var content = document.getElementById('popup-content');
                            var closer = document.getElementById('popup-closer');
                            if (closer) {
                                closer.onclick = function() {
                                    container.style.display = 'none';
                                    closer.blur();
                                    return false;
                                };
                            
                                var overlay = new ol.Overlay({
                                    element: container
                                });

                                var folios = [[]];
                            }                                                              
                        ]]>
                    </script>
                            
                    <xsl:choose>
                        <xsl:when test="/dri:document/dri:body//dri:referenceSet[@type='detailView' and @n='collection-view']">
                            <script type="text/javascript">    
                                <!-- FROM HERE -->                                       
                                <xsl:apply-templates select="document('feeds/formattedList.xml')/folios/*"/>
                                <![CDATA[
                                    var baseUrl = ']]><xsl:value-of select="$context-path" />/<![CDATA[';

                                    var vectorSource = new ol.source.Vector();
                                    
                                    var textStroke = new ol.style.Stroke({
                                        color: '#fff',
                                        width: 3
                                    });
                                    var textFill = new ol.style.Fill({
                                        color: '#000'
                                    });
                                    
                                    // iterate folios creating marker
                                    for(i = 1; i < folios.length; i++) {
                                        var iconFeature = new ol.Feature({
                                            geometry: new ol.geom.Point(ol.proj.transform([folios[i].lon, folios[i].lat], 'EPSG:4326', 'EPSG:3857')),
                                            name: i
                                        });
                                    
                                        var iconStyle = new ol.style.Style({
                                            image: new ol.style.Icon(({                                             
                                                anchor: [0.0, 1.0],
                                                anchorXUnits: 'fraction',
                                                anchorYUnits: 'fraction',
                                                opacity: 1.0,
                                                src: ']]><xsl:value-of select="concat($child-theme-path, 'Geofolios/images/icon_lg.png')"/><![CDATA['                                                                                               
                                            })),
                                            text: new ol.style.Text({
                                                font: '12px Calibri,sans-serif',
                                                text: i,
                                                fill: textFill,
                                                stroke: textStroke,
                                                offsetY : -18,
                                                offsetX : 13
                                            })
                                        });
                                        
                                        iconFeature.setStyle(iconStyle);
                                        
                                        vectorSource.addFeature(iconFeature);
                                    }
                                    
                                    var vectorLayer = new ol.layer.Vector({
                                        source: vectorSource
                                    });
                                    
                                    
                                    // create map and add layers and overlay
                                    var map = new ol.Map({
                                        layers: [
                                            new ol.layer.Tile({
                                                source: new ol.source.MapQuest({layer: 'sat'})
                                            }),
                                            new ol.layer.Image({
                                                extent: [-13884991, 2870341, -7455066, 6338219],
                                                source: new ol.source.ImageWMS({
                                                    url: 'http://demo.opengeo.org/geoserver/wms',
                                                    params: {'LAYERS': ' ne:ne_10m_admin_1_states_provinces_lines_shp '},
                                                    serverType: 'geoserver'
                                                })
                                            }), 
                                            vectorLayer
                                        ],
                                        overlays: [overlay],
                                        target: document.getElementById('map'),
                                        view: new ol.View({
                                            center: ol.proj.transform([-96.0, 40.0], 'EPSG:4326', 'EPSG:3857'),
                                            zoom: 4
                                        })
                                    });
                                    
                                    var element = document.getElementById('popup');
                                    
                                    // display popup on click, create content from folios
                                    map.on('click', function(evt) {
                                        var feature = map.forEachFeatureAtPixel(evt.pixel,
                                            function(feature, layer) {
                                            return feature;
                                        });
                                        if (feature) {
                                            var geometry = feature.getGeometry();
                                            var coord = geometry.getCoordinates();
                                            
                                            overlay.setPosition(coord);
                                            
                                            var id = feature.get('name');
                                            
                                            content.innerHTML = '<b>Folio ' + id + '</b><br/>' + 
                                                                folios[id].title + '<br/>' + 
                                                                folios[id].political + '<br/>' + 
                                                                'Published: ' + folios[id].date + '<br/>' + 
                                                                folios[id].lat + ', ' + 
                                                                folios[id].lon + '<br/>' +
                                                                '<a href="'+baseUrl+folios[id].url + '">View complete folio</a>';
                                            
                                            container.style.display = 'block';
                                            
                                            $(element).popover({
                                                'placement': 'top',
                                                'html': true
                                            });
                                            $(element).popover('show');
                                        } else {
                                            $(element).popover('destroy');
                                        }
                                    });
        
                                    // change mouse cursor when over marker
                                    $(map.getViewport()).on('mousemove', function(e) {
                                        var pixel = map.getEventPixel(e.originalEvent);
                                        var hit = map.forEachFeatureAtPixel(pixel, function(feature, layer) {
                                            return true;
                                        });
                                        if (hit) {
                                            map.getTarget().style.cursor = 'pointer';
                                        } else {
                                            map.getTarget().style.cursor = '';
                                        }
                                    });
                                ]]>                             
        
                            </script> 
                        </xsl:when>
                        <xsl:when test="/dri:document/dri:body//dri:div[@id='aspect.discovery.CollectionSearch.div.collection-search' or @id='aspect.artifactbrowser.AdvancedSearch.div.advanced-search']">
                                          
                            <script type="text/javascript">
                                <!-- Add the folios iteratively -->
                                
                                <xsl:for-each select="/dri:document/dri:body//dri:div[@id='aspect.discovery.CollectionSearch.div.collection-search' or @id='aspect.artifactbrowser.AdvancedSearch.div.advanced-search'] /dri:div[@n='search-results']/dri:referenceSet/dri:reference">
                                    <xsl:variable name="externalMetadataURL">
                                        <xsl:text>cocoon:/</xsl:text>
                                        <xsl:value-of select="@url"/>
                                    </xsl:variable>
                                            
                                    <xsl:variable name="data" select="document($externalMetadataURL)/mets:METS/mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"/>
                                    <xsl:variable name="number" select="substring-after($data/dim:field[@element='identifier' and @qualifier='govdoc'],':')"/>
                                    
                                    <xsl:variable name="coords_y" select="(number(substring-before(substring-after($data/dim:field[@element='coverage' and @qualifier='box'],'northlimit='),';')) + number(substring-before(substring-after($data/dim:field[@element='coverage' and @qualifier='box'],'southlimit='),';'))) div 2.0"/>
                                    <xsl:variable name="coords_x" select="(number(substring-before(substring-after($data/dim:field[@element='coverage' and @qualifier='box'],'westlimit='),';')) +  number(substring-after($data/dim:field[@element='coverage' and @qualifier='box'],'eastlimit='))) div 2.0"/>
                                    
                                    <xsl:variable name="title" select="(substring-after(title, ''))"/> 
                                    <xsl:variable name="political" select="(substring-after(coverage/political, ''))"/>
                                    <xsl:variable name="date" select="(substring-after(date, ''))"/>
                                    <xsl:variable name="url" select="(substring-after(url, ''))"/>
        
                                    <![CDATA[
                                        folios.push( {
                                            title: ']]><xsl:value-of select="$title" /><![CDATA[',
                                            political: ']]><xsl:value-of select="$political" /><![CDATA[',
                                            date: ']]><xsl:value-of select="$date" /><![CDATA[',
                                            url: ']]><xsl:value-of select="$url" /><![CDATA[',
                                            lat: parseFloat(']]><xsl:value-of select="$coords_y" /><![CDATA['),
                                            lon: -parseFloat(']]><xsl:value-of select="$coords_x" /><![CDATA[')
                                        });
                                        
                                    ]]>
                                    
                                </xsl:for-each>
                            </script>
                        </xsl:when>
                    </xsl:choose>                   
    </xsl:template>

     
    <xsl:template match="dri:p[@rend='item-view-toggle item-view-toggle-bottom']"> </xsl:template>

    <xsl:template match="dri:div[@n='collection-recent-submission']"> </xsl:template>
    
    
<!-- ============================================================================================================================================================== -->
<!-- ============================================================================================================================================================== -->
        
    <!-- Viewing an individual folio --> 
    <xsl:template name="itemSummaryView-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemSummaryView-DIM"/>
        
        <div id="bitstreams">
            <xsl:for-each select="mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@MIMETYPE='image/tiff']">
                <!-- push the back page to the back of the list -->
                <xsl:sort data-type="number" order="ascending" select="number(contains(./mets:FLocat/@xlink:title,'backcover')) * 4"/>
                <!-- sort the rest... luckily of us, "front" comes before "insidefront" comes before "pg" -->
                <xsl:sort data-type="text" order="ascending" select="./mets:FLocat/@xlink:title"/>
                <div class="thumbnail">
                    <a rel="lightbox" alt="Click for a larger preview">
                        <xsl:attribute name="href">
                            <xsl:value-of select="../../mets:fileGrp[@USE='THUMBNAIL']/mets:file[substring-before(mets:FLocat[@LOCTYPE='URL']/@xlink:title,'-') = substring-before(current()/mets:FLocat[@LOCTYPE='URL']/@xlink:title,'.') and contains(mets:FLocat[@LOCTYPE='URL']/@xlink:title,'-lg')]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                        </xsl:attribute>
                        <img alt="Click for larger version" >
                            <xsl:attribute name="src">
                                <xsl:value-of select="../../mets:fileGrp[@USE='THUMBNAIL']/mets:file[substring-before(mets:FLocat[@LOCTYPE='URL']/@xlink:title,'-') = substring-before(current()/mets:FLocat[@LOCTYPE='URL']/@xlink:title,'.') and contains(mets:FLocat[@LOCTYPE='URL']/@xlink:title,'-sm')]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                        </img>
                    </a>                    
                    <p>
                        <strong>
                            <xsl:value-of select="substring-before(./mets:FLocat[@LOCTYPE='URL']/@xlink:title,'.')"/>
                            <xsl:text>, 300 ppi</xsl:text>
                        </strong>
                        <br />
                        <xsl:for-each select="../mets:file[@MIMETYPE='image/jpeg'][mets:FLocat[substring-before(@xlink:title,'.') = substring-before(current()/mets:FLocat/@xlink:title,'.')]]">
                            <a>
                                <xsl:attribute name="href"><xsl:value-of select="./mets:FLocat[@LOCTYPE='URL']/@xlink:href"/></xsl:attribute>
                                <xsl:text>Download </xsl:text>
                                <xsl:choose>
                                    <!--
                                    <xsl:when test="./@SIZE &lt; 1000000">
                                        <xsl:value-of select="substring(string(./@SIZE div 1000),1,5)"/>
                                        <xsl:text>KB </xsl:text>
                                    </xsl:when>
                                    -->
                                    <xsl:when test="./@SIZE &lt; 1000000000">
                                        <xsl:value-of select="substring(string(./@SIZE div 1000000),1,3)"/>
                                        <xsl:text>MB </xsl:text>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="substring(string(./@SIZE div 1000000000),1,5)"/>
                                        <xsl:text>GB </xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:text>JPEG</xsl:text>
                            </a>
                        </xsl:for-each>
                        <br />
                        <a>
                            <xsl:attribute name="href"><xsl:value-of select="./mets:FLocat[@LOCTYPE='URL']/@xlink:href"/></xsl:attribute>
                            <xsl:text>Download </xsl:text>
                            <xsl:choose>
                                <xsl:when test="./@SIZE &lt; 1000">
                                    <xsl:value-of select="./@SIZE"/>
                                    <xsl:text>B </xsl:text>
                                </xsl:when>
                                <xsl:when test="./@SIZE &lt; 1000000">
                                    <xsl:value-of select="substring(string(./@SIZE div 1000),1,5)"/>
                                    <xsl:text>KB </xsl:text>
                                </xsl:when>
                                <xsl:when test="./@SIZE &lt; 1000000000">
                                    <xsl:value-of select="substring(string(floor(./@SIZE div 1000000)),1,5)"/>
                                    <xsl:text>MB </xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="substring(string(./@SIZE div 1000000000),1,5)"/>
                                    <xsl:text>GB </xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>
                            <xsl:text>TIFF</xsl:text>
                        </a>
                    </p>
                </div>
            </xsl:for-each>
        </div>
        
        <p class="linkbox">
            <span>View this folio as:</span>
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="./mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@MIMETYPE='application/pdf']/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                </xsl:attribute>
                <span class="linkboxlink" id="pdflink">
                    <xsl:text>Screen-optimized PDF</xsl:text>
                </span>
            </a>
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="./mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file/mets:FLocat[@LOCTYPE='URL' and substring(@xlink:title,1,6)='GFolio' and substring(@xlink:title,10,4)='.zip']/@xlink:href"/>
                </xsl:attribute>
                <span class="linkboxlink" id="gislink">
                    <xsl:text>GIS map data</xsl:text>
                </span>
            </a>
        </p>

        <p class="linkbox">        
            <span>View all folios as:</span>
            <a href="{$child-theme-path}Geofolios/feeds/Geologic_Atlas_of_the_United_States.kmz">
                <span class="linkboxlink" id="kmllink">
                    <xsl:text>Google Earth overlays</xsl:text>
                </span>
            </a>
            
            <a href="{$child-theme-path}Geofolios/feeds/georss.xml">
                <span class="linkboxlink" id="georsslink">
                    <xsl:text>GeoRSS feed</xsl:text>
                </span>
            </a>
            
        </p>
    </xsl:template>
    
<!-- ============================================================================================================================================================== -->
<!-- ============================================================================================================================================================== -->    
    
    <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
        <div id="metadata">
            <p class="byline">
                <xsl:text>Folio </xsl:text>
                <xsl:value-of select="substring-after(dim:field[@element='identifier' and @qualifier='govdoc'],':')"/>
                <xsl:text>, published </xsl:text>
                <xsl:value-of select="dim:field[@element='date' and @qualifier='issued']"/>
            </p>
            
            <p id="smallset">
                <strong>Latitude: </strong>
                <xsl:value-of select="substring-before(dim:field[@element='coverage' and @qualifier='point'],'N')"/>
                <xsl:text>N</xsl:text>
                <br/>
                <strong>Longitude: </strong>
                <xsl:value-of select="substring-before(substring-after(dim:field[@element='coverage' and @qualifier='point'],'N'),'W')"/>
                <xsl:text>W </xsl:text>
                <br/>
                
                <strong>Author: </strong>
                <xsl:value-of select="dim:field[@element='creator']"/>
                <br />
                <strong>Gov't Doc number: </strong>
               <xsl:value-of select="dim:field[@element='identifier' and @qualifier='govdoc']"/>
                <br />
                <strong>Published by: </strong>
                <xsl:value-of select="dim:field[@element='publisher']"/>
                <br />
                <strong>In collection: </strong> <a href="http://hdl.handle.net/1969.1/2490">Geologic Atlas of the United States</a> 
                <br />
                <strong>Permanent URI: </strong> 
                <a class="showlink">
                    <xsl:attribute name="href"><xsl:value-of select="dim:field[@element='identifier' and @qualifier='uri']"/></xsl:attribute>
                    <xsl:value-of select="dim:field[@element='identifier' and @qualifier='uri']"/>
                </a>
                <br />
            </p>
        </div>
    </xsl:template>
    
<!-- ============================================================================================================================================================== -->
<!-- ============================================================================================================================================================== -->    
    
    <!-- The templates that handle the respective cases: item, collection, and community. In the case of items
         current Manakin build does really have a special use for detailList so the logic of summaryList is 
         basically used in its place. --> 
    <xsl:template name="itemDetailView_DS-METS-1.0-MODS">
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <xsl:variable name="context" select="."/>
        
        <div id="metadata">
            <p class="byline">
                <xsl:text>Folio </xsl:text>
                <xsl:value-of select="substring-after($data/mods:identifier[@type='govdoc'],':')"/>
                <xsl:text>, published </xsl:text>
                <xsl:value-of select="$data/mods:originInfo/mods:dateIssued[@encoding='iso8601']"/>
            </p>
            <xsl:apply-templates select="$data" mode="detailView"/>
        </div>
        
        <div id="bitstreams">
            <xsl:for-each select="mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@MIMETYPE='image/tiff']">
                <!-- push the back page to the back of the list -->
                <xsl:sort data-type="number" order="ascending" select="number(contains(./mets:FLocat/@xlink:title,'backcover')) * 4"/>
                <!-- sort the rest... luckily of us, "front" comes before "insidefront" comes before "pg" -->
                <xsl:sort data-type="text" order="ascending" select="./mets:FLocat/@xlink:title"/>
                <div class="thumbnail">
                    <a rel="lightbox" alt="Click for a larger preview">
                        <xsl:attribute name="href">
                            <xsl:value-of select="../../mets:fileGrp[@USE='THUMBNAIL']/mets:file[substring-before(mets:FLocat[@LOCTYPE='URL']/@xlink:title,'-') = substring-before(current()/mets:FLocat[@LOCTYPE='URL']/@xlink:title,'.') and contains(mets:FLocat[@LOCTYPE='URL']/@xlink:title,'-lg')]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                        </xsl:attribute>
                        <img alt="Click for larger version" >
                            <xsl:attribute name="src">
                                <xsl:value-of select="../../mets:fileGrp[@USE='THUMBNAIL']/mets:file[substring-before(mets:FLocat[@LOCTYPE='URL']/@xlink:title,'-') = substring-before(current()/mets:FLocat[@LOCTYPE='URL']/@xlink:title,'.') and contains(mets:FLocat[@LOCTYPE='URL']/@xlink:title,'-sm')]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                        </img>
                    </a>                    
                    <p>
                        <strong>
                            <xsl:value-of select="substring-before(./mets:FLocat[@LOCTYPE='URL']/@xlink:title,'.')"/>
                            <xsl:text>, 300 ppi</xsl:text>
                        </strong>
                        <br />
                        <xsl:for-each select="../mets:file[@MIMETYPE='image/jpeg'][mets:FLocat[substring-before(@xlink:title,'.') = substring-before(current()/mets:FLocat/@xlink:title,'.')]]">
                            <a>
                                <xsl:attribute name="href"><xsl:value-of select="./mets:FLocat[@LOCTYPE='URL']/@xlink:href"/></xsl:attribute>
                                <xsl:text>Download </xsl:text>
                                <xsl:choose>
                                    <xsl:when test="./@SIZE &lt; 1000000">
                                        <xsl:value-of select="substring(string(./@SIZE div 1000000),2,3)"/>
                                        <xsl:text>MB </xsl:text>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="substring(string(./@SIZE div 1000000000),1,5)"/>
                                        <xsl:text>GB </xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:text>JPEG</xsl:text>
                            </a>
                        </xsl:for-each>
                        <br />
                        <a>
                            <xsl:attribute name="href"><xsl:value-of select="./mets:FLocat[@LOCTYPE='URL']/@xlink:href"/></xsl:attribute>
                            <xsl:text>Download </xsl:text>
                            <xsl:choose>
                                <xsl:when test="./@SIZE &lt; 1000">
                                    <xsl:value-of select="./@SIZE"/>
                                    <xsl:text>B </xsl:text>
                                </xsl:when>
                                <xsl:when test="./@SIZE &lt; 1000000">
                                    <xsl:value-of select="substring(string(./@SIZE div 1000),1,5)"/>
                                    <xsl:text>KB </xsl:text>
                                </xsl:when>
                                <xsl:when test="./@SIZE &lt; 1000000000">
                                    <xsl:value-of select="substring(string(./@SIZE div 1000000),1,5)"/>
                                    <xsl:text>MB </xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="substring(string(./@SIZE div 1000000000),1,5)"/>
                                    <xsl:text>GB </xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>
                            <xsl:text>TIFF</xsl:text>
                        </a>
                    </p>
                </div>
            </xsl:for-each>
        </div>
        
        <p class="linkbox">
            <span>View this folio as:</span>
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="./mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@MIMETYPE='application/pdf']/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                </xsl:attribute>
                <span class="linkboxlink" id="pdflink">
                    <xsl:text>Screen-optimized PDF</xsl:text>
                </span>
            </a>
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="./mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file/mets:FLocat[@LOCTYPE='URL' and substring(@xlink:title,1,6)='GFolio' and substring(@xlink:title,10,4)='.zip']/@xlink:href"/>
                </xsl:attribute>
                <span class="linkboxlink" id="gislink">
                    <xsl:text>GIS map data</xsl:text>
                </span>
            </a>
        </p>
        
        <p class="linkbox">
            <span>View all folios as:</span>
            <a href="{$child-theme-path}Geofolios/feeds/Geologic_Atlas_of_the_United_States.kmz">
                <span class="linkboxlink" id="kmllink">
                    <xsl:text>Google Earth overlays</xsl:text>
                </span>
            </a>
            <a href="{$child-theme-path}Geofolios/feeds/georss.xml">
                <span class="linkboxlink" id="georsslink">
                    <xsl:text>GeoRSS feed</xsl:text>
                </span>
            </a>
        </p>
    
    </xsl:template>
    
<!-- ============================================================================================================================================================== -->
<!-- ============================================================================================================================================================== -->
    
    <!-- The block of templates used to render the mods contents of a DRI object -->
    <!-- The first template creates the top level table and sets the order in which the mods elements are to be processed. -->
    <xsl:template match="mods:mods" mode="detailView" priority="2"> 
          
        <table id="fullset">
            <xsl:apply-templates select="*[not(@type='provenance') and not(name()='mods:physicalDescription')]">
                <xsl:sort data-type="number" order="ascending" select="
                    number(name()='mods:titleInfo') * 1
                    + number(name()='mods:abstract') * 2
                    + number(name()='mods:name') *3
                    + number(name()='mods:accessCondition') * 4
                    + number(name()='mods:classification') * 5
                    + number(name()='mods:genre') * 6
                    + number(name()='mods:identifier') * 7 
                    + number(name()='mods:language') * 8
                    + number(name()='mods:location') * 9
                    + number(name()='mods:note') * 10
                    + number(name()='mods:originInfo') * 11 
                    + number(name()='mods:part') * 12
                    + number(name()='mods:physicalDescription') * 13 
                    + number(name()='mods:recordInfo') * 14
                    + number(name()='mods:relatedItem') * 15
                    + number(name()='mods:subject') * 16
                    + number(name()='mods:tableOfContents') * 17 
                    + number(name()='mods:targetAudience') * 18
                    + number(name()='mods:typeOfResource') * 19
                    + number(name()='mods:extension') * 20
                    "/>
            </xsl:apply-templates>
            
            <tr>
                <td>Appears in Collections:</td>
                <td></td>
                <td>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="key('DSMets1.0', /dri:document/dri:body//dri:objectInclude[@objectSource = current()/ancestor::dri:object/@objectIdentifier]/dri:includeSet/dri:objectInclude/@objectSource)/@url"/>
                        </xsl:attribute>
                        <xsl:value-of select="key('DSMets1.0', /dri:document/dri:body//dri:objectInclude[@objectSource = current()/ancestor::dri:object/@objectIdentifier]/dri:includeSet/dri:objectInclude/@objectSource)/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods/mods:titleInfo/mods:title"/>
                    </a>
                </td>
            </tr>
        </table>
    </xsl:template>
    
<!-- ============================================================================================================================================================== -->
<!-- ============================================================================================================================================================== -->
    
    <!-- TO HERE -->
    
    <xsl:template match="folio">
        <!-- Add the flags -->
        <xsl:variable name="coords_y" select="(number(substring-before(substring-after(coverage/box,'northlimit='),';')) + number(substring-before(substring-after(coverage/box,'southlimit='),';'))) div 2.0"/>
        <xsl:variable name="coords_x" select="(number(substring-before(substring-after(coverage/box,'westlimit='),';')) + number(substring-after(coverage/box,'eastlimit='))) div 2.0"/> 
        
        <xsl:variable name="title" select="(substring-after(title, ''))"/> 
        <xsl:variable name="political" select="(substring-after(coverage/political, ''))"/>
        <xsl:variable name="date" select="(substring-after(date, ''))"/>
        <xsl:variable name="url" select="(substring-after(url, ''))"/>
        
        <![CDATA[
            folios.push({
                title: ']]><xsl:value-of select="$title" /><![CDATA[',
                political: ']]><xsl:value-of select="$political" /><![CDATA[',
                date: ']]><xsl:value-of select="$date" /><![CDATA[',
                url: ']]><xsl:value-of select="$url" /><![CDATA[',
                lat: parseFloat(']]><xsl:value-of select="$coords_y" /><![CDATA['),
                lon: -parseFloat(']]><xsl:value-of select="$coords_x" /><![CDATA[')
            });
        ]]>
        
    </xsl:template>
    
<!-- ============================================================================================================================================================== -->
<!-- ============================================================================================================================================================== --> 
    
    <xsl:template match="dri:div[@n='collection-home']/dri:head"></xsl:template>
    
    <xsl:template match="dri:div[@n='collection-home']/dri:head" mode="geo">    
        <xsl:variable name="head_count" select="count(ancestor::dri:div)"/>
        <!-- with the help of the font-sizing variable, the font-size of our header text is made continuously variable based on the character count -->
        <!-- first constant used to be 375, but I changed it to 325 - JSC -->
        <!-- in case the chosen size is less than 120%, don't let it go below. Shrinking stops at 120% -->
        <xsl:variable name="font-sizing" select="325 - $head_count * 80 - string-length(current())"></xsl:variable>
        <xsl:element name="h{$head_count}">
            <xsl:choose>
                <xsl:when test="$font-sizing &lt; 120">
                    <xsl:attribute name="style">font-size: 120%;</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="style">font-size: <xsl:value-of select="$font-sizing"/>%;</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:call-template name="standardAttributes">               
                <xsl:with-param name="class">ds-div-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates/>
        </xsl:element>  
    </xsl:template>
    
    <!-- Rendering the main collection view (map and all) --> 
    <xsl:template name="collectionDetailView-DIM">
        <div class="detail-view">&#160;
        
            <link rel="stylesheet" href="http://openlayers.org/en/v3.0.0/css/ol.css" type="text/css"></link>
            <style>
                .map {
                    height: 400px;
                    width: 100%;
                }
                .ol-popup {
                    width: 150px;
                    display: none;
                    position: absolute;
                    background-color: white;
                    -moz-box-shadow: 0 1px 4px rgba(0,0,0,0.2);
                    -webkit-filter: drop-shadow(0 1px 4px rgba(0,0,0,0.2));
                    filter: drop-shadow(0 1px 4px rgba(0,0,0,0.2));
                    padding: 15px;
                    border-radius: 10px;
                    border: 1px solid #cccccc;
                    bottom: 35px;
                    left: -38px;
                }
                .ol-popup:after, .ol-popup:before {
                    top: 100%;
                    border: solid transparent;
                    content: " ";
                    height: 0;
                    width: 0;
                    position: absolute;
                    pointer-events: none;
                }
                .ol-popup:after {
                    border-top-color: white;
                    border-width: 10px;
                    left: 48px;
                    margin-left: -10px;
                }
                .ol-popup:before {
                    border-top-color: #cccccc;
                    border-width: 11px;
                    left: 48px;
                    margin-left: -11px;
                }
                .ol-popup-closer {
                    text-decoration: none;
                    position: absolute;
                    top: 2px;
                    right: 8px;
                }
                .ol-popup-closer:after {
                    content: "X";
                }
            </style>
        
            <div id="mapContainer">
                <div id="map" class="map">
                    <div id="popup" class="ol-popup">
                        <a href="#" id="popup-closer" class="ol-popup-closer"></a>
                        <div id="popup-content"></div>
                    </div>
                </div>
            </div>
                                    
            <xsl:apply-templates select="//dri:div[@n='collection-home']/dri:head" mode="geo"/>            
            <!-- Generate the info about the collections from the metadata section -->
            <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="collectionDetailView-DIM"/>            
            <!-- JS script moved from here to the dri:document template -->
        </div>
    </xsl:template>
    
    <!-- Generate the info about the collection from the metadata section -->
    <xsl:template match="dim:dim" mode="collectionDetailView-DIM"> 
        <xsl:if test="string-length(dim:field[@element='description'][not(@qualifier)])&gt;0">
            <p class="intro-text">
                <xsl:copy-of select="dim:field[@element='description'][not(@qualifier)]/node()"/>
            </p>
        </xsl:if>
        
        <!-- The links to all the top-level stuff -->
        <p class="linkbox front">         
            <a href="{$child-theme-path}Geofolios/feeds/Geologic_Atlas_of_the_United_States.kmz">
                <span class="linkboxlink" id="kmllink">
                    <xsl:text>Google Earth overlays</xsl:text>
                </span>
            </a>
            <a href="{$child-theme-path}Geofolios/feeds/georss.xml">
                <span class="linkboxlink" id="georsslink">
                    <xsl:text>GeoRSS feed</xsl:text>
                </span>
            </a>
            <a href="{$child-theme-path}Geofolios/feeds/GIS-data.zip">
                <span class="linkboxlink" id="gislink">
                    <xsl:text>GIS map data</xsl:text>
                </span>
            </a>            
        </p>
        
        <xsl:if test="string-length(dim:field[@element='rights'][not(@qualifier)])&gt;0 or string-length(dim:field[@element='rights'][@qualifier='license'])&gt;0">
            <div class="detail-view-rights-and-license">
                <h3><i18n:text>xmlui.dri2xhtml.METS-1.0.copyright</i18n:text></h3>
                <xsl:if test="string-length(dim:field[@element='rights'][not(@qualifier)])&gt;0">
                    <div class="copyright-text">
                        <xsl:copy-of select="dim:field[@element='rights'][not(@qualifier)]/node()"/>
                    </div>
                </xsl:if>
                <xsl:if test="string-length(dim:field[@element='rights'][@qualifier='license'])&gt;0">
                    <div class="license-text">
                        <xsl:copy-of select="dim:field[@element='rights'][@qualifier='license']/node()"/>
                    </div>
                </xsl:if>
            </div>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="dri:div[@n='collection-search-browse']"></xsl:template>
  
    <xsl:template match="dri:div[@n='search-results']/dri:head">
        <h3 style="margin-bottom: 5px;"><xsl:apply-templates/></h3>
    </xsl:template>
    
    <xsl:template match="dri:referenceSet[@n='search-results-repository']/dri:head"></xsl:template>
  
    <xsl:template match="dri:div[@id='aspect.discovery.CollectionSearch.div.collection-search'] | dri:div[@id='aspect.artifactbrowser.AdvancedSearch.div.advanced-search']">         
        <div id="mapContainer"></div>            
        <!-- JS script moved from here to the dri:document template -->            
        <xsl:apply-imports/>
    </xsl:template>
      
    <!-- Included here to override the default behaviour of including the collection parent -->
    <xsl:template match="dri:reference" mode="summaryView">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryView"/>
    </xsl:template>  
    
</xsl:stylesheet>
