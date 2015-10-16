<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" 

    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:java="http://xml.apache.org/xalan/java"
    xmlns:encoder="xalan://java.net.URLEncoder"
	xmlns:helper="ar.edu.unlp.sedici.dspace.util.HelperFunctions" 
	xmlns:confman="org.dspace.core.ConfigurationManager"
    xmlns:math="http://exslt.org/math"
    xmlns:sets="http://exslt.org/sets"
    xmlns:common="http://exslt.org/common"
    xmlns:dyn="http://exslt.org/dynamic"
    xmlns:str="http://exslt.org/strings"
    xmlns:regexp="http://exslt.org/regular-expressions"
    xmlns:xmlui="xalan://ar.edu.unlp.sedici.dspace.xmlui.util.XSLTHelper"
    extension-element-prefixes="str regexp xmlui math sets common dyn"
	xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc regexp str xmlui math sets common  dyn helper confman java xalan"
	>

<!-- 	<xsl:template match="//dri:div[@n='item-view']"> -->
<!-- 		 Shhhh -->
<!-- 	</xsl:template> -->
	
	
	<!-- Esta template se usa para parsear la fecha que entra como parametro -->
	<xsl:template name="cambiarFecha" match="dim:field/text()">
		<xsl:param name="isDate"></xsl:param>
		<xsl:if test="$isDate">
		<!--  El choose se usa para saber en que idioma mostrar la fecha -->
			<xsl:choose>
				<xsl:when test="contains($query-string,'locale-attribute=en')">
				<!--  ingles -->
					<xsl:value-of select="xmlui:formatearFecha(.,'en')" />			
				</xsl:when>
				<xsl:otherwise>
				<!--  español -->
					<xsl:value-of select="xmlui:formatearFecha(.,'es')" />
				</xsl:otherwise>				
			</xsl:choose>
		</xsl:if>
		
	</xsl:template>	

	
	<xsl:template name="render-metadata-values">
		<xsl:param name="separator">;</xsl:param>
		<xsl:param name="nodes"></xsl:param>
		<xsl:param name="anchor"></xsl:param>
		<xsl:param name="isDate"></xsl:param>
		<xsl:param name="editor">False</xsl:param>
		<xsl:param name="reduced">False</xsl:param>
		<xsl:param name="local_browse_type"></xsl:param>

		<xsl:for-each select="$nodes">
			<span>
				<xsl:if test="@language">
					<xsl:attribute name="xml:lang" ><xsl:value-of select="@language"/></xsl:attribute>
				</xsl:if>
				<xsl:choose>
					<xsl:when test="$anchor">
						<xsl:choose>
							<xsl:when test="$local_browse_type">
								<xsl:choose>
									<xsl:when test="@authority!=''">
										<xsl:call-template name="build-anchor">
											<xsl:with-param name="a.href" select="concat('http://digital.cic.gba.gob.ar/browse?authority=', encoder:encode(@authority), '&amp;', 'type=', $local_browse_type)"/>
											<xsl:with-param name="a.value" select="text()"/>
										</xsl:call-template>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="text()" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:when>
							<xsl:when test="@authority!=''">
								<xsl:call-template name="build-anchor">
									<xsl:with-param name="a.href" select="@authority"/>
									<xsl:with-param name="a.value" select="text()"/>
								</xsl:call-template>
							</xsl:when>
							<xsl:otherwise>
								<xsl:call-template name="build-anchor">
									<xsl:with-param name="a.href" select="text()"/>
									<xsl:with-param name="a.value" select="text()"/>
								</xsl:call-template>
							</xsl:otherwise>								
							</xsl:choose>
					</xsl:when>
					<xsl:when test="$isDate">
							<xsl:call-template name="cambiarFecha" >
									<xsl:with-param name="isDate" select="$isDate"></xsl:with-param>									
							</xsl:call-template>
					</xsl:when>
					<xsl:when test="($editor='True') and ($reduced='True')">
						<xsl:value-of select="substring(text(),1,200)" disable-output-escaping="yes"/>
						<xsl:value-of select="concat(substring-before(substring(text(),200,300),'.'), '.')" disable-output-escaping="yes"/>
					</xsl:when>
					<xsl:when test="$editor='True'">
						<xsl:value-of select="text()" disable-output-escaping="yes"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:copy-of select="text()"/>
					</xsl:otherwise>				
				</xsl:choose>
			</span>
			<xsl:if test="not(position()=last())">
	        	<xsl:value-of select="$separator" /> 
	        </xsl:if>
		</xsl:for-each>
	</xsl:template>
	
<!-- 	<xsl:template name="render-metadata-group"> -->
<!-- 		<xsl:param name="fields">dc.title</xsl:param>Comma separated list of metadata -->
<!-- 		<xsl:param name="separator">;</xsl:param> -->
<!-- 		<xsl:param name="filter"></xsl:param>Si tiene filtername es un link, sino no -->
<!-- 		<xsl:param name="label"></xsl:param> -->
<!-- 		<xsl:param name="context" select="." /> -->
<!-- 		<xsl:if test="$label"> -->
<!-- 			<div class="metadata-group-label"> -->
<!-- 				<xsl:copy-of select="$label" /> -->
<!-- 			</div> -->
<!-- 		</xsl:if> -->
		
<!-- 		<xsl:for-each select="str:split($fields,',')"> -->
<!-- 			<xsl:call-template name="render-metadata"> -->
<!-- 				<xsl:with-param name="field" select="."/> -->
<!-- 				<xsl:with-param name="context" select="$context" /> -->
<!-- 				<xsl:with-param name="separator" select="$separator"/> -->
<!-- 				<xsl:with-param name="filter" select="$filter"/>Si tiene filtername es un link, sino no -->
<!-- 				<xsl:with-param name="show_label"> -->
<!-- 					<xsl:if test="(last() = 1) and not($label)"> -->
<!-- 						<xsl:text>true</xsl:text> -->
<!-- 					</xsl:if> -->
<!-- 				</xsl:with-param> -->
<!-- 			</xsl:call-template> -->
<!-- 		</xsl:for-each> -->
<!-- 	</xsl:template> -->
	
	<!-- Imprime un metadato y sus valores -->
	<xsl:template name="render-metadata">
		<xsl:param name="field"></xsl:param>
		<xsl:param name="context" select="." />
		<xsl:param name="separator">; </xsl:param>
		<xsl:param name="is_linked_authority"></xsl:param><!-- Si viene en true, es un link, sino no -->
		<xsl:param name="show_label">true</xsl:param>
		<xsl:param name="container">div</xsl:param>
		<xsl:param name="null_message"></xsl:param>
		<xsl:param name="isDate"></xsl:param>
		<xsl:param name="editor">False</xsl:param>
		<xsl:param name="reduced"></xsl:param>
		<xsl:param name="local_browse_type"></xsl:param>
				
		<xsl:variable name="mp" select="str:split($field,'.')" />
		<xsl:variable name="schema" select="$mp[1]"/>
		<xsl:variable name="element" select="$mp[position()=2]"/>
		<xsl:variable name="qualifier" >
			<xsl:if test="$mp[last()=3]">
					<xsl:value-of select="$mp[position()=3]/text()"/>
			</xsl:if>
		</xsl:variable>
		<xsl:variable name="fqmn" select="xmlui:replaceAll(string($field), '[\.\*]', '_')"/>
		
		<xsl:variable name="nodes" select="$context/dim:field[@mdschema=$schema and @element=$element and (($qualifier='' and not(@qualifier)) or ($qualifier!='' and (@qualifier=$qualifier or $qualifier='*')) ) ]"/>
		
		<xsl:if test="$nodes or $null_message">
			<xsl:element name="{$container}" >
				<xsl:attribute name="class"><xsl:value-of select="concat('metadata-', $fqmn)"/></xsl:attribute>
				<xsl:if test="$show_label ='true'">
					<span class="metadata-label">
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="$fqmn" /></i18n:text>: 
					</span>
				</xsl:if>
			
				<span class="metadata-values" >
					<xsl:choose>
						<xsl:when test="$nodes">
							<xsl:call-template name="render-metadata-values">
								<xsl:with-param name="separator" select="$separator"/>
								<xsl:with-param name="nodes" select="$nodes"/>
								<xsl:with-param name="isDate" select="$isDate"/>
								<xsl:with-param name="anchor" select="$is_linked_authority"/>
								<xsl:with-param name="reduced" select="$reduced"/>
								<xsl:with-param name="editor" select="$editor"/>
								<xsl:with-param name="local_browse_type" select="$local_browse_type"/>
							</xsl:call-template>
						</xsl:when>
						<xsl:when test="$null_message">
							<xsl:copy-of select="$null_message"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>Sin datos (no debería mostrarse)</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</span>
			</xsl:element>
		</xsl:if>
	</xsl:template>
	
    <xsl:template name="itemSummaryView-DIM">
    	<xsl:for-each select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim">
	    	<div class="row item-head, col-md-12">
		    	<div class="col-md-8 col-md-push-3">
			    	<xsl:call-template name="render-metadata">
			    		<xsl:with-param name="field" select="'dc.type'" />
			    		<xsl:with-param name="show_label" select="'false'" />
			    	</xsl:call-template>
		    		<xsl:call-template name="render-metadata">
		    			<xsl:with-param name="field" select="'dc.title'" />
		    			<xsl:with-param name="show_label" select="'false'" />
						<xsl:with-param name="container" select="'h1'" />
		    			<xsl:with-param name="null_message"><i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text></xsl:with-param>
		    		</xsl:call-template>
		    		<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.title.subtitle'"/>
						<xsl:with-param name="show_label" select="false"/>
						<xsl:with-param name="container" select="'h4'"/>
		    		</xsl:call-template>
			    	<xsl:call-template name="render-metadata">
		    			<xsl:with-param name="field" select="'dcterms.creator.*'" />
		    			<xsl:with-param name="separator" select="' | '" />
		    			<xsl:with-param name="show_label" select="'false'" />
		    			<xsl:with-param name="null_message"><i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text></xsl:with-param>
		    			<xsl:with-param name="is_linked_authority" select="'true'" />
		    			<xsl:with-param name="local_browse_type" select="'author'" />
		    		</xsl:call-template>
		    		
				</div>
				<div class="col-md-1 col-md-push-3 hidden-xs hidden-sm year-box">
	    			<label id="año"><i18n:text>xmlui.ArtifactBrowser.ItemViewer.year</i18n:text><h3><xsl:value-of select="substring(//mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@mdschema='dcterms'][@element='issued']/text(), 1, 4)"></xsl:value-of></h3></label>
	    		</div>
	    		<div class="col-md-1 col-md-push-3 visible-xs visible-sm year-box" id="year-box-small">
	    			<label id="año"><i18n:text>xmlui.ArtifactBrowser.ItemViewer.year</i18n:text>: <xsl:value-of select="substring(//mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@mdschema='dcterms'][@element='issued']/text(), 1, 4)"></xsl:value-of></label>
	    		</div>
	    		
	    	</div>
	    </xsl:for-each>
    	
    	<div class="row">
	    	<div class="col-md-9 col-md-push-3">
	    	
	    <!-- Generate the info about the item from the metadata section -->
		        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
		        mode="itemSummaryView-DIM" />
		    	
		    	
	    	</div>
	    	
	    	<div class="col-md-3 col-md-pull-9" id="thumbnail-container">
	    	
	    		<!-- Advertencia de embargo (por ahora esta deshabilitado) -->
				<xsl:if test="has-embargo">
					<div class="alert alert-warning" role="alert">
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-dcterms_rights_embargoPeriod.msg</i18n:text>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dcterms.rights.embargo_period'" />
							<xsl:with-param name="show_label" select="'false'" />
						</xsl:call-template>
					</div>
				</xsl:if>
				
				<div class="item-preview  hidden-xs">
					 <xsl:variable name="thumbnail_file" select="./mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[1]"/>
					 <xsl:call-template name="build-anchor">
						<xsl:with-param name="a.href"><xsl:value-of select="./mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[position()=1]/mets:FLocat/@xlink:href"/></xsl:with-param>
						<xsl:with-param name="a.class">thumbnail</xsl:with-param>
						<xsl:with-param name="img.src">
							<xsl:choose>
                          		<xsl:when test="$thumbnail_file/mets:FLocat[@LOCTYPE='URL']/@xlink:href != ''">
                          			<xsl:value-of select="$thumbnail_file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                          		</xsl:when>
                          		<xsl:otherwise>
                          			<xsl:call-template name="print-theme-path">
                          				<xsl:with-param name="path" select="'images/preview_no_disponible.png'"/>
                          			</xsl:call-template>
                          		</xsl:otherwise>
	                         </xsl:choose>
						</xsl:with-param>
						<xsl:with-param name="img.alt">Preview</xsl:with-param>
					</xsl:call-template>
				</div>

		        <!-- Generate the bitstream information from the file section -->
		        <xsl:choose>
		            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file">
		            	<ul class="media-list item-file-list">
		                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file" />
		                </ul>
		            </xsl:when>
		            <xsl:otherwise>
		                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-no-files</i18n:text>
		            </xsl:otherwise>
		        </xsl:choose>
		        <!-- Campo dcterms.identifier.url -->
		        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim" mode="dctermsIdentifierUrl"/>		        
		        <!-- optional: Altmeric.com badge and PlumX widget -->
		        <xsl:if test='confman:getProperty("altmetrics", "altmetric.enabled") and ($identifier_doi or $identifier_handle)'>
		            <xsl:call-template name='impact-altmetric'/>
		        </xsl:if>
		        <xsl:if test='confman:getProperty("altmetrics", "plumx.enabled") and $identifier_doi'>
		            <xsl:call-template name='impact-plumx'/>
		        </xsl:if>
	    	</div>
    	</div>

	    <!-- Creative Commons Logo -->
        <div class="row" id="item-view-CC">
        	<xsl:variable name="cc-uri">
				<xsl:value-of select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='dcterms' and @element='license']/@authority"/>
			</xsl:variable>
        	<div class="col-md-1">
		        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default)-->
				<!-- <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/> -->
				<xsl:call-template name="generate-CC-Anchor-Logo">
					<xsl:with-param name="cc-uri" select="$cc-uri"/>
				</xsl:call-template>
			</div>
			<div class="col-md-6">
				<i18n:text>xmlui.dri2xhtml.structural.cc-item-view-text</i18n:text>
				<i18n:text><xsl:value-of select="concat('xmlui.dri2xhtml.structural.cc-',xmlui:stripDash(xmlui:replaceAll(substring-after($cc-uri, 'http://creativecommons.org/licenses/'), '/', '-')))"/></i18n:text>
			</div>
	     </div>
	     
	     <!-- Show full link -->
	        <div class="row ds-paragraph item-view-toggle item-view-toggle-bottom">
	            <div class="col-md-12">
	            	<a>
	                	<xsl:attribute name="href"><xsl:value-of select="$ds_item_view_toggle_url"/></xsl:attribute>
	                	<i18n:text>xmlui.ArtifactBrowser.ItemViewer.show_full</i18n:text>
	            	</a>
	            </div>
	        </div>
    	
        <span class="Z3988">
            <xsl:attribute name="title">
                <xsl:call-template name="renderCOinS"/>
            </xsl:attribute>
            &#xFEFF; <!-- non-breaking space to force separating the end tag -->
        </span>
	     
    </xsl:template>
	
	<xsl:template match="mets:file" priority="10">
		<li class="media">
			<xsl:variable name="documentTitle">
				<xsl:value-of select="xmlui:replaceAll(substring-after(/mets:METS/@ID,':'), '\/', '_')"/>
			</xsl:variable>
			
			<xsl:variable name="extension" select="substring-after(mets:FLocat[@LOCTYPE='URL']/@xlink:title, '.')"/>
			<xsl:variable name="sequence" select="substring-after(mets:FLocat[@LOCTYPE='URL']/@xlink:href, '?')"/>
			
			<xsl:variable name="file_url">
				<xsl:value-of select="substring-before(mets:FLocat[@LOCTYPE='URL']/@xlink:href, substring-after(/mets:METS/@ID, ':'))"/><xsl:value-of select="substring-after(/mets:METS/@ID, ':')"/>/<xsl:value-of select="$documentTitle"/>.<xsl:value-of select="$extension"/>?<xsl:value-of select="$sequence"/>
			</xsl:variable>
			<a class="media-left thumbnail_file" href="{$file_url}">
				<xsl:variable name="file_type" select="substring-before(@MIMETYPE, '/')" />
				<xsl:variable name="file_subtype" select="substring-after(@MIMETYPE, '/')" />
				<xsl:variable name="img_path">
					<xsl:choose>
						<xsl:when test="$file_type = 'image'">mime_img.png</xsl:when>
						<xsl:when test="$file_subtype = 'pdf'">mime_pdf.png</xsl:when>
						<xsl:when test="$file_subtype = 'msword'">mime_msword.png</xsl:when>
						<xsl:otherwise>mime.png</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<img alt="Icon" src="{concat($theme-path, '/images/', $img_path)}" />
				<xsl:if
					test="contains(mets:FLocat[@LOCTYPE='URL']/@xlink:href,'isAllowed=n')">
					<img>
						<xsl:attribute name="src">
		                                <xsl:value-of select="$context-path" />
		                                <xsl:text>/static/icons/lock24.png</xsl:text>
		                            </xsl:attribute>
						<xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.blocked</xsl:attribute>
						<xsl:attribute name="attr"
							namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
					</img>
				</xsl:if>
			</a>
			<div class="media-body">
				<p>
					<a href="{$file_url}">
						<xsl:value-of select="mets:FLocat/@xlink:label" />&#160;
					</a>
				</p>
				<p>
					<xsl:variable name="extension" select="xmlui:getFileExtension(mets:FLocat[@LOCTYPE='URL']/@xlink:title)" />
					<i18n:translate>
						<i18n:text>xmlui.ArtifactBrowser.ItemViewer.file</i18n:text>
						<i18n:param><xsl:value-of select="$extension" /></i18n:param>
					</i18n:translate>
					<span>
						(<xsl:choose>
							<xsl:when test="@SIZE &lt; 1024">
								<xsl:value-of select="@SIZE" />
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
							</xsl:when>
							<xsl:when test="@SIZE &lt; 1024 * 1024">
								<xsl:value-of select="substring(string(@SIZE div 1024),1,5)" />
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
							</xsl:when>
							<xsl:when test="@SIZE &lt; 1024 * 1024 * 1024">
								<xsl:value-of select="substring(string(@SIZE div (1024 * 1024)),1,5)" />
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of
									select="substring(string(@SIZE div (1024 * 1024 * 1024)),1,5)" />
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
							</xsl:otherwise>
						</xsl:choose>)
					</span>
				</p>
			</div>
		</li>
	</xsl:template>
	
	<xsl:template match="dim:dim" mode="dctermsIdentifierUrl">
				<xsl:call-template name="render-metadata">
					<xsl:with-param name="field" select="'dcterms.identifier.url'" />
					<xsl:with-param name="container" select="'h5'" />
					<xsl:with-param name="is_linked_authority" select="'true'"/>
				</xsl:call-template>
	</xsl:template>
	
	 <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
		<xsl:variable name="abstract" select="./dim:field[@mdschema='dcterms' and @element='abstract']"/>
       	<xsl:if test="$abstract != ''">
	       	<div class="row">
	       		<script>
	       			function swap(show, hide){
	       				document.getElementById(show).style.display = 'block';
	       				document.getElementById(hide).style.display = 'none';
	       			}
	     			
	       			function show_more(){
	       				if (document.getElementById("abstract-xs-short")) {
	       					swap("abstract-xs-long", "abstract-xs-short");
	       					swap("show-less-btn", "show-more-btn");
	       				}
	       			}
	       			function show_less(){
	       				if (document.getElementById("abstract-xs-long")) {
	       					swap("abstract-xs-short", "abstract-xs-long")
	       					swap("show-more-btn", "show-less-btn");
	       				}
	       			}
	       		</script>
	       		
	       		<div class="col-md-12" id="abstract-xs-short">	       		
		       		<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.abstract'" />
						<xsl:with-param name="separator" select="''" />
						<xsl:with-param name="reduced">True</xsl:with-param>
						<xsl:with-param name="editor">True</xsl:with-param>
					</xsl:call-template>
	      		</div>
	       		<div class="col-md-12" id="abstract-xs-long">	       		
		       		<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.abstract'" />
						<xsl:with-param name="separator" select="''" />
						<xsl:with-param name="editor">True</xsl:with-param>
					</xsl:call-template>
	      		</div>
	      		<div class="col-xs-4 col-xs-push-7">
					<div class="view-btn" id="show-more-btn">
		      			<a id="toggle-view-btn" onClick="show_more()"><i18n:text>xmlui.ArtifactBrowser.ItemViewer.show_more</i18n:text></a>
		      		</div> 
		      		<div class="view-btn" id="show-less-btn">
		      			<a id="toggle-view-btn" onClick="show_less()"><i18n:text>xmlui.ArtifactBrowser.ItemViewer.show_less</i18n:text></a>
		      		</div> 
	      		</div>
	      	</div>
      	</xsl:if>
	    <div class="row">
	    
	
		   	<div class="col-md-6">
		   		<h3><i18n:text>xmlui.ArtifactBrowser.ItemViewer.general_info</i18n:text></h3>
				<ul class="list-unstyled">
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.alternative'" />
						<xsl:with-param name="container" select="'li'" />
				    </xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'cic.lugarDesarrollo'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.publisher'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.subject.area'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.subject.materia'" />
						<xsl:with-param name="container" select="'li'" />
						<xsl:with-param name="is_linked_authority" select="'true'" />
						<xsl:with-param name="local_browse_type" select="'subject'"/>
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.subject'" />
						<xsl:with-param name="separator" select="' | '" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.spatial'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.language'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.extent'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
				</ul>

		   	</div>
		   	
		   	<div class="col-md-6">
		   		<h3><i18n:text>xmlui.ArtifactBrowser.ItemViewer.specific_info</i18n:text></h3>
				<ul class="list-unstyled">
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.title.investigacion'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.contributor.director'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'cic.thesis.degree'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'cic.thesis.grantor'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
				</ul>
				
				<ul class="list-unstyled">
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.isPartOf.item'" />
						<xsl:with-param name="container" select="'li'" />
						<xsl:with-param name="is_linked_authority" select="'true'"/>
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.isPartOf.series'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.isPartOf.issue'" />
						<xsl:with-param name="container" select="'li'" />
						<xsl:with-param name="is_linked_authority" select="'true'"/>
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.identifier.isbn'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
	
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.relation'" />
						<xsl:with-param name="container" select="'li'" />
						<xsl:with-param name="is_linked_authority" select="'true'"/>
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.hasPart'" />
						<xsl:with-param name="container" select="'li'" />
						<xsl:with-param name="is_linked_authority" select="'true'"/>
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.isVersionOf'" />
						<xsl:with-param name="container" select="'li'" />
						<xsl:with-param name="is_linked_authority" select="'true'"/>
					</xsl:call-template>
				</ul>
		   	</div>
		   	
		   	
    	</div>
			<div class="row">
				<div class="col-md-12">
					<h3><i18n:text>xmlui.ArtifactBrowser.ItemViewer.other_info</i18n:text></h3>
					<ul class="list-unstyled">
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dcterms.description'" />
							<xsl:with-param name="container" select="'li'" />
						</xsl:call-template>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dc.date.available'"/>							 
							<xsl:with-param name="container" select="'li'" />
							<!-- Esta parametro sirve para que el template declarado arriba sepa que esta campo es una fecha -->
							<xsl:with-param name="isDate">True</xsl:with-param>
						</xsl:call-template>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dc.date.accessioned'" />
							<xsl:with-param name="container" select="'li'" />
							<xsl:with-param name="isDate">True</xsl:with-param>
						</xsl:call-template>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dcterms.issued'" />
							<xsl:with-param name="container" select="'li'" />
							<xsl:with-param name="isDate">True</xsl:with-param>
						</xsl:call-template>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dcterms.identifier.other'" />
							<xsl:with-param name="container" select="'li'" />
						</xsl:call-template>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dc.identifier.uri'" />
							<xsl:with-param name="container" select="'li'" />
							<xsl:with-param name="is_linked_authority" select="'true'" />
						</xsl:call-template>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dcterms.license'" />
							<xsl:with-param name="container" select="'li'" />
						</xsl:call-template>
					
					</ul>
				</div>
			</div>
			
			
        
    </xsl:template>
    
</xsl:stylesheet>