<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" 
		xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc ">

    
	<!--  Este template es como el "principal" llama a los otros templates y organiza la pagina-->
	
	<xsl:template match="/dri:document/dri:body/dri:div[@n='item-home' and @id='aspect.statistics.StatisticsTransformer.div.item-home']/dri:div[@n='stats' and @id='aspect.statistics.StatisticsTransformer.div.stats']">
		<xsl:choose>
			<xsl:when test="dri:table[@id='aspect.statistics.StatisticsTransformer.table.list-table']/dri:row/dri:cell[@n='01']">	
				<div class="container">
					<div class="row item-head">
				    		<div class="col-md-8 curso">
				    			<p></p>
				    			<div>
		    					<xsl:if test="dri:table[@id='aspect.statistics.StatisticsTransformer.table.list-table']/dri:row/dri:cell[@n='01']">
		    						<xsl:value-of select="dri:table[@id='aspect.statistics.StatisticsTransformer.table.list-table']/dri:row/dri:cell[@n='01']"></xsl:value-of>		
		    					</xsl:if>
				    			</div>	
				    		</div>
				    		<div class="col-md-4">
					    		<div class="contenedor-cuadrado">
					    			<div class="cuadrado">
					    				<div class="encuadrado">
					    				<xsl:choose>
					    					<xsl:when test="dri:table[@id='aspect.statistics.StatisticsTransformer.table.list-table']/dri:row/dri:cell[@n='02']">
					    						<xsl:value-of select="dri:table[@id='aspect.statistics.StatisticsTransformer.table.list-table']/dri:row/dri:cell[@n='02']"></xsl:value-of>
					       					</xsl:when>
					    				</xsl:choose>
					    					
					    				</div>
					    			</div>
					    			<h4><i18n:text select="dri:table/dri:row/dri:cell/i18n:text[text()='xmlui.statistics.visits.views']"><xsl:value-of select="dri:table/dri:row/dri:cell/i18n:text[text()='xmlui.statistics.visits.views']"></xsl:value-of></i18n:text></h4>
					    		</div>		    			
				    		</div>
				    		
				    	
				    </div>
			    </div><br/>
			    
			    	<div class="container">
				    	<div class="row ">			    			
				    			<xsl:apply-templates select="dri:div[@id='aspect.statistics.StatisticsTransformer.div.tablewrapper']"/>
				    			    		
					    		<xsl:apply-templates select="dri:table/dri:head/i18n:text[text()='xmlui.statistics.visits.countries']"/>					    		
				    	</div>
			   		</div><br/><br/><br/>
			   		<div class="container">
				    	<div class="row">		    	
					    		<div class="col-md-6 curso">
					    			<xsl:apply-templates select="dri:table/dri:head/i18n:text[text()='xmlui.statistics.visits.bitstreams']"/>
					    		</div>			    		
					    	
				    	</div>
			    	</div>
		    </xsl:when>
			   <xsl:otherwise>
			   		<div class="container">
				   		<div class="row">
			    			<div class="tittle-statistics">
					    		<p>El item no tiene accesos.</p>
					    		<xsl:variable name="path">
					    			<xsl:call-template name="print-path">
						    			<xsl:with-param name="path" select="substring-before( /dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request' and @qualifier='URI'] , '/statistics')" />
							    	</xsl:call-template>
					    		</xsl:variable>
					    		<a>
					    		<xsl:attribute name="href">
					    			<xsl:value-of select="$path"></xsl:value-of>
					    		</xsl:attribute>
					    		Volver a la vista del item
					    		</a>					    		 
					    	</div>
				    	</div>
			    	</div>
	    		</xsl:otherwise>
			    </xsl:choose>
	</xsl:template>
	
	
	<!--  Este template carga las visitas al fichero -->
	
	<xsl:template match="dri:table/dri:head/i18n:text[text()='xmlui.statistics.visits.bitstreams']">			
			<xsl:if test="../../dri:row/dri:cell[@role='data']">
				<div class="tittle-statistics">
		    		<i18n:text><xsl:value-of select="."></xsl:value-of></i18n:text>	
		    		 <div class="linea-statistics"><xsl:comment></xsl:comment></div>
		    	</div>  			    		    	
				<ul class="lista_statistics">
					<xsl:for-each select="../../dri:row">
						<xsl:if test="dri:cell[@rend='datacell']">
							<li>
								<div class ="col-md-12">
									<div class ="col-md-11"><xsl:value-of select="dri:cell[@rend='labelcell']"></xsl:value-of></div>
									<div class ="col-md-1 color"><xsl:value-of select="dri:cell[@rend='datacell']"></xsl:value-of></div>
								</div>									
							</li>
						</xsl:if>
					</xsl:for-each>
				</ul>
			</xsl:if>
					
				
	</xsl:template>
	
	<!--  Este template carga las visitas al item por mes-->
	
	<xsl:template match="dri:div[@id='aspect.statistics.StatisticsTransformer.div.tablewrapper']">
		<xsl:if test="/dri:document/dri:body/dri:div[@n='item-home']/dri:div[@n='stats']/dri:div[@id='aspect.statistics.StatisticsTransformer.div.tablewrapper']/dri:table/dri:row/dri:cell[text()!='' and @rend='datacell']">
			<div class="col-md-5">
				<xsl:comment></xsl:comment>
				<div class="tittle-statistics">
	   				 <i18n:text><xsl:value-of select="dri:table/dri:head/i18n:text[text()='xmlui.statistics.visits.month']/."></xsl:value-of></i18n:text>	
		    		 <div class="linea-statistics"><xsl:comment> &#32;</xsl:comment></div>
		    	</div> 	
		    	<div class="tamanio ct-chart ct-perfect-fourth"><xsl:comment> &#32;</xsl:comment></div>
	    	</div>
	    	<div class="col-md-1"><xsl:comment></xsl:comment></div>
	   	</xsl:if>
		   	
	    	
	</xsl:template>
	
	<!--  Este template carga las visitas al item por pais-->
		<xsl:template match="dri:table/dri:head/i18n:text[text()='xmlui.statistics.visits.countries']">		   
			<xsl:if test="/dri:document/dri:body/dri:div[@n='item-home']/dri:div[@n='stats']/dri:table[last()-1]/dri:row/dri:cell[text()!='' and @role='data' and @rend='datacell']">
				<div class="col-md-5">
					<xsl:comment></xsl:comment>
					<div class="tittle-statistics">
			    		 <i18n:text><xsl:value-of select="."></xsl:value-of></i18n:text>	
			    		 <div class="linea-statistics"><xsl:comment> &#32;</xsl:comment></div>
			    	</div> 
			    	<div class=" tamanio ct-chart" id="chart2"><xsl:comment> &#32;</xsl:comment></div>
		    	</div>
		   	</xsl:if>
		    	
		    	
		</xsl:template>
</xsl:stylesheet>