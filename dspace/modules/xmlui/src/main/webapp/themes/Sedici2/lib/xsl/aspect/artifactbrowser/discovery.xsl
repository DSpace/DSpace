
<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

    <xsl:output indent="yes"/>
    
 
  <xsl:template match="dri:div[@n='search-filters']" priority="2">
  </xsl:template>
  
  <xsl:template match="dri:div[@n='search-filters']">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">top</xsl:with-param>
        </xsl:apply-templates>
        <script type="text/javascript">
             var filtros_activos=
              <xsl:choose>
            		<xsl:when test="dri:list/dri:item[@n='used-filters']">
            		   true
            		</xsl:when>
            		<xsl:otherwise>
            			false
            		</xsl:otherwise>
              </xsl:choose>;
             var ocultar_filtros_text='<i18n:text>xmlui.discovery.ocultar_filtros</i18n:text>';
             var ver_filtros_text='<i18n:text>xmlui.discovery.ver_filtros</i18n:text>(<xsl:value-of select="count(dri:list/dri:item[@n='used-filters']/dri:field)"/>)';
        </script>
		<xsl:variable name="filtro_texto">
	        	<xsl:choose>
	            		<xsl:when test="dri:list/dri:item[@n='used-filters']">	
							xmlui.discovery.ocultar_filtros
	            		</xsl:when>
	            		<xsl:otherwise>
	            		xmlui.discovery.ver_filtros(<xsl:value-of select="count(dri:list/dri:item[@n='used-filters']/dri:field)"/>)
	            		</xsl:otherwise>
	            </xsl:choose>
		</xsl:variable>
        <button id="input_mostrar_filtros" class="ds-button-field" onclick="javascript:mostrarFiltros()">
	            <xsl:choose>
	            		<xsl:when test="dri:list/dri:item[@n='used-filters']">	
							<i18n:text>xmlui.discovery.ocultar_filtros</i18n:text>
	            		</xsl:when>
	            		<xsl:otherwise>
	            		<i18n:text>xmlui.discovery.ver_filtros</i18n:text> (<xsl:value-of select="count(dri:list/dri:item[@n='used-filters']/dri:field)"/>)
	            		</xsl:otherwise>
	            </xsl:choose>
	     </button>

        <script type="text/javascript">
	        function mostrarFiltros(){
	            if (filtros_activos){
	            	$("#div-search-filters").hide('slow');
	            	filtros_activos=false;
	            		$('#input_mostrar_filtros').text(ver_filtros_text);
	            	
	            } else {            
	            	$("#div-search-filters").show('slow');
	            	filtros_activos=true;
	            	$('#input_mostrar_filtros').text(ocultar_filtros_text);
	            }
	        }
        </script>


        <div id="div-search-filters">
            <xsl:attribute name="style">
            	<xsl:choose>
            		<xsl:when test="dri:list/dri:item[@n='used-filters']">
            		   display:block
            		</xsl:when>
            		<xsl:otherwise>
            			display:none
            		</xsl:otherwise>
            	</xsl:choose>
            </xsl:attribute>
	        <form>	        
	            <xsl:call-template name="standardAttributes">
	                <xsl:with-param name="class">ds-interactive-div</xsl:with-param>
	            </xsl:call-template>
	            <xsl:attribute name="action"><xsl:value-of select="@action"/></xsl:attribute>
	            <xsl:attribute name="method"><xsl:value-of select="@method"/></xsl:attribute>
	            <xsl:if test="@method='multipart'">
	                <xsl:attribute name="method">post</xsl:attribute>
	                <xsl:attribute name="enctype">multipart/form-data</xsl:attribute>
	            </xsl:if>
	            <xsl:attribute name="onsubmit">javascript:tSubmit(this);</xsl:attribute>
	                        <!--For Item Submission process, disable ability to submit a form by pressing 'Enter'-->
	                        <xsl:if test="starts-with(@n,'submit')">
	                                <xsl:attribute name="onkeydown">javascript:return disableEnterKey(event);</xsl:attribute>
	            </xsl:if>
	                        <xsl:apply-templates select="*[not(name()='head')]"/>
	          
	        </form>
        </div>
        <!-- JS to scroll form to DIV parent of "Add" button if jump-to -->
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo']">
          <script type="text/javascript">
            <xsl:text>var button = document.getElementById('</xsl:text>
            <xsl:value-of select="translate(@id,'.','_')"/>
            <xsl:text>').elements['</xsl:text>
            <xsl:value-of select="concat('submit_',/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo'],'_add')"/>
            <xsl:text>'];</xsl:text>
            <xsl:text>
                      if (button != null) {
                        var n = button.parentNode;
                        for (; n != null; n = n.parentNode) {
                            if (n.tagName == 'DIV') {
                              n.scrollIntoView(false);
                              break;
                           }
                        }
                      }
            </xsl:text>
          </script>
        </xsl:if>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">bottom</xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>
    
    <!-- Se omite renderizar la combo para el scope-->
    <xsl:template match="dri:list[@id='aspect.discovery.SimpleSearch.list.primary-search']">
          <xsl:apply-templates select="dri:head"/>
          <xsl:apply-templates select="dri:item[dri:field[@n='query']]"/>
    </xsl:template>
    
    <xsl:template match="dri:list[@n='sort-options']">
    	<!-- preparamos el queryString base a partir de los hiddens para que no incluya los campo 'rpp', 'sort_by' y 'order' -->
    	<xsl:variable name="queryString">
    		<xsl:for-each select="/dri:document/dri:body/dri:div/dri:div[@n='main-form']/dri:p[@n='hidden-fields']/dri:field[@n!='rpp'][@n!='sort_by'][@n!='order']">
    			<xsl:value-of select="@n"/>
    			<xsl:text>=</xsl:text>
    			<xsl:value-of select="dri:value[@type='raw']"/>
    			<xsl:text>&amp;</xsl:text>
    		</xsl:for-each>
    	</xsl:variable>
    	<div>
    		<xsl:attribute name="class">
    			<xsl:text>search-controls-box </xsl:text>
    			<!-- <xsl:value-of select="@n"/> -->
    		</xsl:attribute>
	    	<!-- generamos los links de control -->
	    	<xsl:call-template name="searchResultControls">
	    		<xsl:with-param name="element" select="dri:list[@n='rpp-selections']"/>
	    		<xsl:with-param name="queryString" select="$queryString"/>
	    	</xsl:call-template>
	    	<xsl:call-template name="searchResultControls">
	    		<xsl:with-param name="element" select="dri:list[@n='sort-selections']"/>
	    		<xsl:with-param name="queryString" select="$queryString"/>
	    	</xsl:call-template>
    	</div>
    </xsl:template>
    
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.ConfigurableBrowse.div.browse-controls']">
    	<!-- preparamos el queryString base a partir de los hiddens -->
    	<xsl:variable name="queryString">
    		<xsl:for-each select="dri:p[@n='hidden-fields']/dri:field">
    			<xsl:value-of select="@n"/>
    			<xsl:text>=</xsl:text>
    			<xsl:value-of select="dri:value[@type='raw']"/>
    			<xsl:text>&amp;</xsl:text>
    		</xsl:for-each>
    	</xsl:variable>
    	
    	<div>
    		<xsl:attribute name="class">
    			<xsl:text>search-controls-box </xsl:text>
    			<xsl:value-of select="@n"/>
    		</xsl:attribute>
	    	<!-- generamos los links de control -->
	    	<xsl:call-template name="browseResultControls">
	    		<xsl:with-param name="queryString" select="$queryString"/>
	    		<xsl:with-param name="element" select="dri:p/dri:field[@n='rpp']"/>
	    	</xsl:call-template>
	    	<!-- Oculto el criterio de ordenación (asc o desc) de la lista de resultados si se ha seleccionado un término del BROWSE-->
	    	<xsl:if test="count(../dri:div[@n='browse-by-author-results' or @n='browse-by-subject-results']/dri:referenceSet/dri:reference) = 0">
		    	<xsl:call-template name="browseResultControls">
		    		<xsl:with-param name="queryString" select="$queryString"/>
		    		<xsl:with-param name="element" select="dri:p/dri:field[@n='order']"/>
		    	</xsl:call-template>
		    </xsl:if>
    	</div>
    	
    </xsl:template>

	<!-- Template para mostrar filtros de ordenamiento en Discovery -->
	<xsl:template name="searchResultControls">
		<xsl:param name="element"/>
		<xsl:param name="queryString"/>
		
		<xsl:variable name="controlName" select="$element/@n"/>
		
		<!-- incluimos los valores seleccionados de los otros controles -->
    	<xsl:variable name="otherSearchControls">
    		<xsl:for-each select="$element/../../dri:list[@n='sort-options']/dri:list[not(@n=$controlName)]/dri:item[contains(@rend,'gear-option-selected')]">
    			<xsl:value-of select="dri:xref/@target"/>
    			<xsl:text>&amp;</xsl:text>
    		</xsl:for-each>
    	</xsl:variable>
		
		<div>
			<xsl:attribute name="class">
				<xsl:text>single-search-control </xsl:text>
				<xsl:value-of select="$element/@n"/>
			</xsl:attribute>
			<h2>
				<i18n:text>
					<xsl:text>xmlui.ArtifactBrowser.AbstractSearch.</xsl:text>
					<xsl:value-of select="$element/@n"/>
				</i18n:text>
			</h2>
			<ul>
				<xsl:for-each select="$element/dri:item">
					<li>
						<xsl:if test="contains(@rend,'gear-option-selected')">
							<xsl:attribute name="class">selected</xsl:attribute>
						</xsl:if>
						<a>
							<xsl:attribute name="href">
								<xsl:text>?</xsl:text>
								<xsl:value-of select="$queryString"/>
								<xsl:value-of select="$otherSearchControls"/>
								<xsl:value-of select="dri:xref/@target"></xsl:value-of>
							</xsl:attribute>
							<xsl:copy-of select="node()"></xsl:copy-of>
						</a>
					</li>
				</xsl:for-each>
			</ul>
		</div>
	</xsl:template>
	
	<!-- Template para mostrar filtros de ordenamiento en Browse -->
	<xsl:template name="browseResultControls">
		<xsl:param name="queryString"/>
		<xsl:param name="element"/>
		
		<xsl:variable name="controlName" select="$element/@n"/>
		
		<!-- incluimos los valores de los otros controles -->
    	<xsl:variable name="otherSearchControls">
    		<xsl:for-each select="dri:list[@n='search-controls']/dri:item/dri:field[not(@n=$controlName) and not(@type='button')]/dri:value">
    			<xsl:value-of select="../@n"/>
    			<xsl:text>=</xsl:text>
    			<xsl:value-of select="@option"/>
    			<xsl:text>&amp;</xsl:text>
    		</xsl:for-each>
    	</xsl:variable>
		
		<xsl:variable name="selectedValue" select="$element/dri:value/@option"/>
		
		<div>
			<xsl:attribute name="class">
				<xsl:text>single-search-control </xsl:text>
				<xsl:value-of select="$element/@n"/>
			</xsl:attribute>
			<h2>
				<i18n:text>
					<xsl:text>xmlui.ArtifactBrowser.AbstractSearch.</xsl:text>
					<xsl:value-of select="$element/@n"/>
				</i18n:text>
			</h2>
			<ul>
				<xsl:for-each select="$element/dri:option">
					<li>
						<xsl:choose>
							<xsl:when test="@returnValue=$selectedValue">
								<xsl:attribute name="class">selected</xsl:attribute>
							</xsl:when>
							<xsl:when test="string($selectedValue)='' and position()=1">
								<xsl:attribute name="class">selected</xsl:attribute>
							</xsl:when>
						</xsl:choose>
						<a>
							<xsl:attribute name="href">
								<xsl:text>?</xsl:text>
								<xsl:value-of select="$queryString"/>
								<xsl:value-of select="$otherSearchControls"/>
								<xsl:value-of select="$element/@n"/>
								<xsl:text>=</xsl:text>
								<xsl:value-of select="@returnValue"/>
							</xsl:attribute>
							<xsl:copy-of select="node()"></xsl:copy-of>
						</a>
					</li>
				</xsl:for-each>
			</ul>
		</div>
	</xsl:template>
    
	<xsl:template match="dri:div[@id='aspect.discovery.SimpleSearch.div.search-results' and not(@itemsTotal)]">
    	<div id="no_results_found"><i18n:text>xmlui.ArtifactBrowser.AbstractSearch.no_results</i18n:text></div>
    	<div id="no_results_found_tips">
			<p><i18n:text>xmlui.ArtifactBrowser.AbstractSearch.tips_info_general</i18n:text></p>
			<p> </p>
			<ul>
				<li><i18n:text>xmlui.ArtifactBrowser.AbstractSearch.tips_info1</i18n:text></li>
				<li><i18n:text>xmlui.ArtifactBrowser.AbstractSearch.tips_info2</i18n:text></li>
				<li><i18n:text>xmlui.ArtifactBrowser.AbstractSearch.tips_info3</i18n:text></li>
			</ul>
			<p></p>
			<div id="alternative_search_services_message">
		 	<p><i18n:text>xmlui.ArtifactBrowser.AbstractSearch.other_services_no_results</i18n:text></p>
		 </div>
		 <div class="banners_no_result">	
		 	 <a href="http://www.biblioteca.mincyt.gov.ar/" target="_blank" title="Biblioteca Electrónica" >
				<img class="logo_no_result">
			    	<xsl:attribute name="src">
			        	<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>
			            <xsl:text>/themes/</xsl:text>
						<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
						<xsl:text>/images/banner-biblioteca-electronica.png</xsl:text>
			        </xsl:attribute>
			    </img>
			</a>
			<a href="http://opac-istec.prebi.unlp.edu.ar/" target="_blank" title="OPAC-ISTEC">
				<img class="logo_no_result">
			    	<xsl:attribute name="src">
			        	<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>
			            <xsl:text>/themes/</xsl:text>
						<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
						<xsl:text>/images/banner-opac.png</xsl:text>
			        </xsl:attribute>
			    </img>
			</a>
			<a href="http://prebi.unlp.edu.ar/celsius"  target="_blank" title="PREBI" >
				<img class="logo_no_result">
			    	<xsl:attribute name="src">
			        	<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>
			            <xsl:text>/themes/</xsl:text>
						<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
						<xsl:text>/images/banner-prebi.png</xsl:text>
			        </xsl:attribute>
			    </img>
			</a>
		 </div>
		 </div>
		 <br/>
   	  </xsl:template>
   	  
   	  <!-- Listado de resultados de items en DISCOVERY -->
	<xsl:template name="itemSummaryList">
        <xsl:param name="handle"/>
        <xsl:param name="externalMetadataUrl"/>
        
        <xsl:variable name="externalfileSecURL">
            <xsl:text>cocoon://metadata/handle/</xsl:text>
            <xsl:value-of select="$handle"/>
            <xsl:text>/mets.xml</xsl:text>
            <!-- Sólo obtenemos los metadatos para los bundles del item -->
            <xsl:text>?sections=fileSec</xsl:text>
        </xsl:variable>
        
        <xsl:variable name="metsBundleMdt" select="document($externalfileSecURL)"/>

		<div class="artifact-type">
			<div class="type">
				<xsl:call-template name="renderDiscoveryField">
                    <xsl:with-param name="value">
                    	<xsl:choose>
							<xsl:when test="dri:list[@n=(concat($handle, ':sedici.subtype')) and descendant::text()]"> 
									<xsl:value-of select="dri:list[@n=(concat($handle, ':sedici.subtype'))]/dri:item"/> 
							</xsl:when>
							<xsl:when test="dri:list[@n=(concat($handle, ':dc.type')) and descendant::text()]"> 
									<xsl:value-of select="dri:list[@n=(concat($handle, ':dc.type'))]/dri:item"/> 
							</xsl:when>
							<!-- No hay otherwise -->
						</xsl:choose>
                    </xsl:with-param>
                </xsl:call-template>
				<xsl:text>&#160;</xsl:text>
			</div>	

			<xsl:variable name="originInfoContent">
				
				<xsl:choose>
					
					<!-- Solo para el tipo tesis: grado alanzado e institución otorgante -->
					<xsl:when test="dri:list[@n=(concat($handle, ':dc.type'))]/dri:item = 'Tesis'">
						<xsl:value-of select="dri:list[@n=(concat($handle, ':thesis.degree.name'))]/dri:item"/>
						<xsl:text>; </xsl:text>
						<xsl:value-of select="dri:list[@n=(concat($handle, ':thesis.degree.grantor'))]/dri:item"/>
					</xsl:when>

					<!-- Solo para el tipo Objeto de coferencia: evento -->
					<xsl:when test="dri:list[@n=(concat($handle, ':dc.type'))]/dri:item = 'Objeto de conferencia'">
						<xsl:value-of select="dri:list[@n=(concat($handle, ':sedici.relation.event'))]/dri:item"/>
					</xsl:when>
					
					<!-- Si tiene journalTitle -->
					<xsl:when test="dri:list[@n=(concat($handle, ':sedici.relation.journalTitle')) and descendant::text()]">
						<xsl:value-of select="dri:list[@n=(concat($handle, ':sedici.relation.journalTitle'))]/dri:item"/>
						<xsl:if test="dri:list[@n=(concat($handle, ':sedici.relation.journalVolumeAndIssue')) and descendant::text()]">
							<xsl:text>; </xsl:text>
							<xsl:value-of select="dri:list[@n=(concat($handle, ':sedici.relation.journalVolumeAndIssue'))]/dri:item"/>
						</xsl:if>

						<!-- Si además tiene evento, lo muestro -->
						<xsl:if test="dri:list[@n=(concat($handle, ':sedici.relation.event')) and descendant::text()]">
							<xsl:text> | </xsl:text>
							<xsl:value-of select="dri:list[@n=(concat($handle, ':sedici.relation.event'))]/dri:item"/>
						</xsl:if>
					</xsl:when>
					
					<!-- En cualquier otro caso, va la Institucion de Origen -->
					<xsl:otherwise>
						<xsl:value-of select="dri:list[@n=(concat($handle, ':mods.originInfo.place'))]/dri:item"/>
					</xsl:otherwise>
					
				</xsl:choose>
			</xsl:variable>

			<div class="originInfo">
				<xsl:attribute name="title"><xsl:value-of select="$originInfoContent"/></xsl:attribute>
				<xsl:call-template name="renderDiscoveryField">
                    <xsl:with-param name="value">
						<xsl:choose>
							<xsl:when test="string-length(concat(dri:list[@n=(concat($handle, ':sedici.subtype'))]/dri:item,$originInfoContent)) > 95">
								<xsl:value-of select="substring($originInfoContent,0,(95 - string-length(dri:list[@n=(concat($handle, ':sedici.subtype'))]/dri:item)))"/><xsl:text>...</xsl:text>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="$originInfoContent"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:text>&#160;</xsl:text>
			</div>
						
			<div class="publisher-date">
				<!-- date.exposure/date.issued/date.created : extraemos el año solamente -->
				<xsl:call-template name="renderDiscoveryField">
                    <xsl:with-param name="value">
						<xsl:choose>
							<xsl:when test="(dri:list[@n=(concat($handle, ':dc.date.created')) and descendant::text()]) and (dri:list[@n=(concat($handle, ':dc.date.created')) and descendant::text()]/dri:item=$objeto_fisico)">
									<xsl:value-of select="substring(dri:list[@n=(concat($handle, ':dc.date.created'))]/dri:item,1,4)"/>
							</xsl:when>
							<xsl:when test="dri:list[@n=(concat($handle, ':sedici.date.exposure')) and descendant::text()]">
								<xsl:value-of select="substring(dri:list[@n=(concat($handle, ':sedici.date.exposure'))]/dri:item,1,4)"/>
							</xsl:when>
							<xsl:when test="dri:list[@n=(concat($handle, ':dc.date.issued')) and descendant::text()]">
								<xsl:value-of select="substring(dri:list[@n=(concat($handle, ':dc.date.issued'))]/dri:item,1,4)"/>
							</xsl:when>
						</xsl:choose>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:text>&#160;</xsl:text>
			</div>
		</div>
		
		<div class="artifact-description">
			<div  class="artifact-title">
                  <xsl:call-template name="renderDiscoveryField">
                  	<xsl:with-param name="value">
	                   <xsl:choose>
	                       <xsl:when test="dri:list[@n=(concat($handle, ':dc.title')) and descendant::text()]">
	                           <xsl:value-of select="dri:list[@n=(concat($handle, ':dc.title'))]/dri:item" disable-output-escaping="yes"/>
	                       </xsl:when>
	                       <xsl:otherwise>
	                           <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
	                       </xsl:otherwise>
	                   </xsl:choose>
	              	</xsl:with-param>
	              	<xsl:with-param name="href" select="concat('/handle/',$handle )"/>
	              	<xsl:with-param name="classname">title</xsl:with-param>
	              </xsl:call-template>
				<xsl:if test="dri:list[@n=(concat($handle, ':sedici.title.subtitle')) and descendant::text()]">
					<xsl:call-template name="renderDiscoveryField">
                    	<xsl:with-param name="value">
							<xsl:text> . </xsl:text>
							<xsl:value-of select="dri:list[@n=(concat($handle, ':sedici.title.subtitle'))]/dri:item" disable-output-escaping="yes"/>
						</xsl:with-param>
	              	<xsl:with-param name="href" select="concat('/handle/',$handle )"/>
	              	<xsl:with-param name="classname">subtitle</xsl:with-param>
					</xsl:call-template>
				</xsl:if>
			</div>

			<div class="author">
				<xsl:call-template name="renderDiscoveryField">
                    <xsl:with-param name="value">
						<xsl:choose>
							<xsl:when test="dri:list[starts-with(@n,concat($handle, ':sedici.creator.')) and descendant::text()]">
								<xsl:for-each select="dri:list[starts-with(@n,concat($handle, ':sedici.creator.'))]/dri:item">
									<xsl:value-of select="."/>
									<xsl:if test="count(following-sibling::node()) != 0">
										<xsl:text>; </xsl:text>
									</xsl:if>
								</xsl:for-each>
							</xsl:when>
							<xsl:when test="dri:list[@n=(concat($handle, ':sedici.contributor.compiler')) and descendant::text()]">
								<xsl:for-each select="dri:list[@n=(concat($handle, ':sedici.contributor.compiler'))]/dri:item">
									<xsl:value-of select="."/>
									<xsl:if test="count(following-sibling::node()) != 0">
										<xsl:text>; </xsl:text>
									</xsl:if>
								</xsl:for-each>
							</xsl:when>
							<xsl:when test="dri:list[@n=(concat($handle, ':sedici.contributor.editor')) and descendant::text()]">
								<xsl:for-each select="dri:list[@n=(concat($handle, ':sedici.contributor.editor'))]/dri:item">
									<xsl:value-of select="."/>
									<xsl:if test="count(following-sibling::node()) != 0">
										<xsl:text>; </xsl:text>
									</xsl:if>
								</xsl:for-each>
							</xsl:when>
							<xsl:when test="dri:list[@n=(concat($handle, ':sedici.contributor.translator')) and descendant::text()]">
								<xsl:for-each select="dri:list[@n=(concat($handle, ':sedici.contributor.translator'))]/dri:item">
									<xsl:value-of select="."/>
									<xsl:if test="count(following-sibling::node()) != 0">
										<xsl:text>; </xsl:text>
									</xsl:if>
								</xsl:for-each>
							</xsl:when>
							<xsl:otherwise>
								<i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:with-param>
				</xsl:call-template>
			</div>

			<!-- Tiene Localizacion Electronica -->
			<xsl:if test="dri:list[@n=(concat($handle, ':sedici.identifier.uri')) and descendant::text()]">
				<span i18n:attr="title">
					<xsl:attribute name="class">availability linked</xsl:attribute>
					<xsl:attribute name="title">xmlui.dri2xhtml.METS-1.0.item-availability-linked-help</xsl:attribute>
					<!-- ERROR: tengo que poner un espacio en blanco porque, sino, XSLT anida todos los <span> entre si. -->
					<xsl:text> </xsl:text>
				</span>
			</xsl:if>
			
			
			<!-- Tiene archivos cargados -->
			<xsl:if test="$metsBundleMdt/mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">				
				<span i18n:attr="title">
					<xsl:attribute name="class">availability local</xsl:attribute>
				    <xsl:attribute name="title">xmlui.dri2xhtml.METS-1.0.item-availability-local-help</xsl:attribute>
				    <xsl:text> </xsl:text>
				</span>
			</xsl:if>
			
			<!-- Esta a full text? -->
			<xsl:if test="dri:list[@n=(concat($handle, ':sedici.description.fulltext'))]/dri:item/text()='false'">
				<span class="fulltext-message">
					<i18n:text>xmlui.dri2xhtml.METS-1.0.item-availability-no-fulltext</i18n:text>
				</span>
			</xsl:if>
			
       	</div>
	</xsl:template>
	
	<!-- Template viejo (utilizado antes para mostrar resultados de browse y de discovery) que ahora se utiliza solamente para el BROWSE,
 		 ya que la estructura del DRI utilizada en discovery cambió en relación a como estaba antes.  -->
	<xsl:template match="dim:dim" mode="itemSummaryList-DIM-metadata">
		<xsl:param name="href"/>

		<div class="artifact-type">
			<div class="type">
				<xsl:choose>
					<xsl:when test="dim:field[@element='subtype']"> 
							<xsl:copy-of select="dim:field[@element='subtype']/node()"/> 
					</xsl:when>
					<xsl:when test="dim:field[@element='type']"> 
							<xsl:copy-of select="dim:field[@element='type']/node()"/> 
					</xsl:when>
					<!-- No hay otherwise -->
				</xsl:choose>
				<xsl:text>&#160;</xsl:text>
			</div>	

			<xsl:variable name="originInfoContent">
				
				<xsl:choose>
					
					<!-- Solo para el tipo tesis: grado alanzado e institución otorgante -->
					<xsl:when test="dim:field[@element='type'] = 'Tesis'">
						<xsl:value-of select="dim:field[@element='degree' and @qualifier='name']"/>
						<xsl:text>; </xsl:text>
						<xsl:value-of select="dim:field[@element='degree' and @qualifier='grantor']"/>
					</xsl:when>

					<!-- Solo para el tipo Objeto de coferencia: evento -->
					<xsl:when test="dim:field[@element='type'] = 'Objeto de conferencia'">
						<xsl:value-of select="dim:field[@element='relation' and @qualifier='event']"/>
					</xsl:when>
					
					<!-- Si tiene journalTitle -->
					<xsl:when test="dim:field[@element = 'relation' and @qualifier='journalTitle']">
						<xsl:value-of select="dim:field[@element='relation' and @qualifier='journalTitle']"/>
						<xsl:if test="dim:field[@element='relation' and @qualifier='journalVolumeAndIssue']">
							<xsl:text>; </xsl:text>
							<xsl:value-of select="dim:field[@element='relation' and @qualifier='journalVolumeAndIssue']"/>
						</xsl:if>

						<!-- Si además tiene evento, lo muestro -->
						<xsl:if test="dim:field[@element = 'relation' and @qualifier='event']">
							<xsl:text> | </xsl:text>
							<xsl:value-of select="dim:field[@element='relation' and @qualifier='event']"/>
						</xsl:if>
					</xsl:when>
					
					<!-- En cualquier otro caso, va la Institucion de Origen -->
					<xsl:otherwise>
						<xsl:value-of select="dim:field[@element='originInfo' and @qualifier='place']"/>
					</xsl:otherwise>
					
				</xsl:choose>
			</xsl:variable>

			<div class="originInfo">
				<xsl:attribute name="title"><xsl:value-of select="$originInfoContent"/></xsl:attribute>
				<xsl:choose>
					<xsl:when test="string-length(concat(dim:field[@element='subtype'],$originInfoContent)) > 95">
						<xsl:value-of select="substring($originInfoContent,0,(95 - string-length(dim:field[@element='subtype'])))"/><xsl:text>...</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$originInfoContent"/>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:text>&#160;</xsl:text>
			</div>
						
			<div class="publisher-date">
				<!-- date.exposure/date.issued/date.created : extraemos el año solamente -->
				<xsl:choose>
					<xsl:when test="dim:field[@element='date' and @qualifier='created'] and dim:field [@element='type']=$objeto_fisico">
							<xsl:value-of select="substring(dim:field[@element='date' and @qualifier='created'],1,4)"/>
					</xsl:when>
					<xsl:when test="dim:field[@element='date' and @qualifier='exposure']">
						<xsl:value-of select="substring(dim:field[@element='date' and @qualifier='exposure'],1,4)"/>
					</xsl:when>
					<xsl:when test="dim:field[@element='date' and @qualifier='issued']">
						<xsl:value-of select="substring(dim:field[@element='date' and @qualifier='issued'],1,4)"/>
					</xsl:when>
				</xsl:choose>
				<xsl:text>&#160;</xsl:text>
			</div>
		</div>
		
		<div class="artifact-description">
			<div  class="artifact-title">
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:value-of select="$href"/>
                    </xsl:attribute>
                    <span class="title">
	                    <xsl:choose>
	                        <xsl:when test="dim:field[@element='title']">
	                            <xsl:value-of select="dim:field[@element='title'][1]/node()" disable-output-escaping="yes"/>
	                        </xsl:when>
	                        <xsl:otherwise>
	                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
	                        </xsl:otherwise>
	                    </xsl:choose>
                    </span>
					<xsl:if test="dim:field[@element='title' and @qualifier='subtitle']">
						<span class="subtitle">
							<xsl:text> . </xsl:text>
							<xsl:value-of select="dim:field[@element='title' and @qualifier='subtitle'][1]/node()" disable-output-escaping="yes"/>
						</span>
					</xsl:if>
                </xsl:element>
			</div>

			<div class="author">
				<xsl:choose>
					<xsl:when test="dim:field[@element='creator']">
						<xsl:for-each select="dim:field[@element='creator']">
							<xsl:copy-of select="node()"/>
							<xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
								<xsl:text>; </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</xsl:when>
					<xsl:when test="dim:field[@element='contributor' and @qualifier='compiler']">
						<xsl:for-each select="dim:field[@element='contributor' and @qualifier='compiler']">
							<xsl:copy-of select="node()"/>
							<xsl:if test="count(following-sibling::dim:field[@element='contributor' and @qualifier='compiler']) != 0">
								<xsl:text>; </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</xsl:when>
					<xsl:when test="dim:field[@element='contributor' and @qualifier='editor']">
						<xsl:for-each select="dim:field[@element='contributor' and @qualifier='editor']">
							<xsl:copy-of select="node()"/>
							<xsl:if test="count(following-sibling::dim:field[@element='contributor' and @qualifier='editor']) != 0">
								<xsl:text>; </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</xsl:when>
					<xsl:when test="dim:field[@element='contributor' and @qualifier='translator']">
						<xsl:for-each select="dim:field[@element='contributor' and @qualifier='translator']">
							<xsl:copy-of select="node()"/>
							<xsl:if test="count(following-sibling::dim:field[@element='contributor' and @qualifier='translator']) != 0">
								<xsl:text>; </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</xsl:when>
					<xsl:otherwise>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
					</xsl:otherwise>
				</xsl:choose>
			</div>

			<!-- Tiene Localizacion Electronica -->
			<xsl:if test="dim:field[@element='identifier'  and @qualifier='uri' and @mdschema='sedici']">
				<span i18n:attr="title">
					<xsl:attribute name="class">availability linked</xsl:attribute>
					<xsl:attribute name="title">xmlui.dri2xhtml.METS-1.0.item-availability-linked-help</xsl:attribute>
					<!-- ERROR: tengo que poner un espacio en blanco porque, sino, XSLT anida todos los <span> entre si. -->
					<xsl:text> </xsl:text>
				</span>
			</xsl:if>
			
			
			<!-- Tiene archivos cargados -->
			<xsl:if test="../../../../mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">				
				<span i18n:attr="title">
					<xsl:attribute name="class">availability local</xsl:attribute>
				    <xsl:attribute name="title">xmlui.dri2xhtml.METS-1.0.item-availability-local-help</xsl:attribute>
				    <xsl:text> </xsl:text>
				</span>
			</xsl:if>
			
			<!-- Esta a full text? -->
			<xsl:if test="dim:field[@element='description'  and @qualifier='fulltext' and @mdschema='sedici']/text()='false'">
				<span class="fulltext-message">
					<i18n:text>xmlui.dri2xhtml.METS-1.0.item-availability-no-fulltext</i18n:text>
				</span>
			</xsl:if>
			
       	</div>
	</xsl:template>
</xsl:stylesheet>