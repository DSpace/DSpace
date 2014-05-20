
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
    
    
    <xsl:template match="dri:div[@id='aspect.discovery.SimpleSearch.div.search-controls']">
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
	    	<xsl:call-template name="searchResultControls">
	    		<xsl:with-param name="queryString" select="$queryString"/>
	    		<xsl:with-param name="element" select="dri:list[@n='search-controls']/dri:item/dri:field[@n='rpp']"/>
	    	</xsl:call-template>
	    	<xsl:call-template name="searchResultControls">
	    		<xsl:with-param name="queryString" select="$queryString"/>
	    		<xsl:with-param name="element" select="dri:list[@n='search-controls']/dri:item/dri:field[@n='sort_by']"/>
	    	</xsl:call-template>
	    	<xsl:call-template name="searchResultControls">
	    		<xsl:with-param name="queryString" select="$queryString"/>
	    		<xsl:with-param name="element" select="dri:list[@n='search-controls']/dri:item/dri:field[@n='order']"/>
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
	    	<xsl:call-template name="searchResultControls">
	    		<xsl:with-param name="queryString" select="$queryString"/>
	    		<xsl:with-param name="element" select="dri:p/dri:field[@n='rpp']"/>
	    	</xsl:call-template>
	    	<xsl:call-template name="searchResultControls">
	    		<xsl:with-param name="queryString" select="$queryString"/>
	    		<xsl:with-param name="element" select="dri:p/dri:field[@n='order']"/>
	    	</xsl:call-template>
    	</div>
    	
    </xsl:template>

	<xsl:template name="searchResultControls">
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
</xsl:stylesheet>