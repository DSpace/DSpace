<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>
         
     <!-- Se crea un grupo por cada colección (celda en la columna 4) que se mustra en la dri:table correspondiente a las tareas que todavía 
     no fueron asignadas a nadie para su realización.-->
     <xsl:key name="unasignedGroups" match="dri:table[@id='aspect.xmlworkflow.Submissions.table.workflow-tasks'][position()=2]/dri:row [not(@role='header') and dri:cell[position()=4]/dri:xref]" use="substring-before(./dri:cell[position()=4]/dri:xref/@target, '/xmlworkflow')"/>

    <xsl:template match="dri:table[@id='aspect.xmlworkflow.Submissions.table.workflow-tasks'][position()=2]">

    	<xsl:variable name="nameCollectionAutoarchive" select="'Autoarchivo'"/>
    	<xsl:variable name="idCollectionAutoarchive" select="'/handle/10915/50'"/>
    	
    	<xsl:apply-templates select="dri:head"/>
	 	
    	<table class="dropdown-table">
     	
    		<xsl:apply-templates select="dri:row[@role='header']" mode="workflow"/>
	    	
	    	<!-- ¿Existe alguna colección que tenga elementos pendientes para procesar en el workflow? -->
	 		<xsl:variable name="areItemsToProcess" select="dri:row[not(@role) and not(dri:cell/@cols)]"/>
	    	<xsl:choose>	
	    		<!-- Si la tabla a procesar está vacía, entonces seguimos el curso normal de los apply-templates. -->
		  		<xsl:when test="not($areItemsToProcess)">
		  			 <xsl:apply-templates select="dri:row[not(@role='header')]"/>
				</xsl:when>
		    	<xsl:otherwise>
			    	<!-- Si hay elementos para mostrar, primero mostramos los elementos que se encuentran en "Autoarchivo"... -->
				    <tbody>	
				    	<xsl:call-template name="createFirstRowGroup">
							<xsl:with-param name="nameFirstRowGroup" select="$nameCollectionAutoarchive"/>
							<xsl:with-param name="quantityRowsGroup" select="count(key('unasignedGroups',$idCollectionAutoarchive))"/>
						</xsl:call-template>
				    	<xsl:call-template name="beginApplyTemplates">
							<xsl:with-param name="targetNodes" select="key('unasignedGroups',$idCollectionAutoarchive)"/>
				    	</xsl:call-template>
					</tbody>
			 		<!-- Luego iteramos sobre el resto de las colecciones. -->
					<!-- El siguiente XPath devuelve un nodo de cada coleccion: "dri:row[generate-id()
							= generate-id(key('unasignedGroups',substring-before(./dri:cell[position()=4]/dri:xref/@target,
							'/xmlworkflow'))[1])] /dri:cell[position()=4]/dri:xref" -->
					<xsl:for-each
						select="dri:row[generate-id() = generate-id(key('unasignedGroups',substring-before(./dri:cell[position()=4]/dri:xref/@target, '/xmlworkflow'))[1])]/dri:cell[position()=4]/dri:xref">
						<xsl:variable name="collecId" select="substring-before(@target, '/xmlworkflow')"/>
						<xsl:variable name="collecName" select="./text()"/>
						<xsl:if test="$collecName != $nameCollectionAutoarchive">
			  				<tbody>
			  					<xsl:call-template name="createFirstRowGroup">
									<xsl:with-param name="nameFirstRowGroup" select="$collecName"/>
									<xsl:with-param name="quantityRowsGroup" select="count(key('unasignedGroups',$collecId))"/>
								</xsl:call-template>
								<xsl:for-each select="key('unasignedGroups',$collecId)">
										<xsl:call-template name="beginApplyTemplates">
					    					<xsl:with-param name="targetNodes" select="."/>
					    				</xsl:call-template>
								</xsl:for-each>
							</tbody>
						</xsl:if>				    								    					        
			  		</xsl:for-each>
			  		<!-- Se procesa la fila que contiene el button de submit -->
			  		<xsl:apply-templates select="dri:row[dri:cell/dri:field/@type='button' and dri:cell/dri:field/@n='submit_take_tasks']"/>
		  		</xsl:otherwise>
		  	</xsl:choose>
	  	</table>
    </xsl:template>  
    
    <!-- Éste template se crea para generar una primer fila que va a servir para que las colecciones puedan desplegarse. -->
    <xsl:template name="createFirstRowGroup">
		<xsl:param name="nameFirstRowGroup" select="'Sin nombrar'"/>
		<xsl:param name="quantityRowsGroup" select="0"/>
		
		<xsl:variable name="quantityRowsGroupString" select="format-number($quantityRowsGroup,'0')"/>
		<tbody>
			<xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">collection-title</xsl:with-param>
            </xsl:call-template>
			<tr><td>
            	<xsl:attribute name="colspan">5</xsl:attribute>
            	<xsl:value-of select="concat($nameFirstRowGroup,' (',$quantityRowsGroupString,')')"/>
			</td>
			</tr>
		</tbody>	
	</xsl:template>
    
    <xsl:template name="beginApplyTemplates">
    	<xsl:param name="targetNodes"/>
    	<xsl:if test="$targetNodes">
    		<xsl:apply-templates select="$targetNodes" mode="workflow"/>
    	</xsl:if>	
    </xsl:template>
    
    
    <!-- Normal row, most likely filled with data cells. -->
    <!-- Se filtra la celda del "Nombre de la colección" -->
    <xsl:template match="dri:row" priority="1" mode="workflow">
        <tr>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-row
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="./dri:cell[position()!=4]"/>
        </tr>
    </xsl:template>
    
    <xsl:template match="dri:row[@role='header']" mode="workflow" priority="2">
        <tr>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-header-row</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="./dri:cell[position()!=4]"/>
        </tr>
    </xsl:template>
    
</xsl:stylesheet>  	 