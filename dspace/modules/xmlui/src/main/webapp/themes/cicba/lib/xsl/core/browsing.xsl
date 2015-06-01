<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">
	
	<!--  este template machea con el div que trae los autores a listar -->
	<xsl:template match="dri:div[@id='aspect.artifactbrowser.ConfigurableBrowse.div.browse-by-author-results' and @n='browse-by-author-results'] | dri:div[@id='aspect.artifactbrowser.ConfigurableBrowse.div.browse-by-subject-results' and @n='browse-by-subject-results']">
		<!-- La variable rows contiene la cantidad de autores que se van a mostar -->
		<xsl:variable name="rows"> 
			<xsl:value-of select="dri:table/@rows" />
		</xsl:variable>
		<!--  la variable cantColumns contiene la cantidad de columnas que yo debo crear -->
		<xsl:variable name="cantColums">
				 	<xsl:value-of select="ceiling(($rows -1) div 25)"/>
		</xsl:variable>
		<div class="container">
			<div class="row item-head">
				<xsl:choose>
					<xsl:when test="$cantColums=1">
						<xsl:call-template name="create-divs" >
							<xsl:with-param name="pos-data">2</xsl:with-param>
						</xsl:call-template>
					</xsl:when>
					<xsl:when test="$cantColums=2">
						<xsl:call-template name="create-divs" >
							<xsl:with-param name="pos-data">2</xsl:with-param>
						</xsl:call-template>
						<xsl:call-template name="create-divs" >
							<xsl:with-param name="pos-data">25</xsl:with-param>
						</xsl:call-template>
					</xsl:when>
					<xsl:when test="$cantColums=3">
						<xsl:call-template name="create-divs" >
							<xsl:with-param name="pos-data">2</xsl:with-param>
						</xsl:call-template>
						<xsl:call-template name="create-divs" >
							<xsl:with-param name="pos-data">25</xsl:with-param>
						</xsl:call-template>
						<xsl:call-template name="create-divs" >
							<xsl:with-param name="pos-data">50</xsl:with-param>
						</xsl:call-template>
					</xsl:when>
					<xsl:when test="$cantColums=4">
						<xsl:call-template name="create-divs" >
							<xsl:with-param name="pos-data">2</xsl:with-param>
						</xsl:call-template>
						<xsl:call-template name="create-divs" >
							<xsl:with-param name="pos-data">25</xsl:with-param>
						</xsl:call-template>
						<xsl:call-template name="create-divs" >
							<xsl:with-param name="pos-data">50</xsl:with-param>
						</xsl:call-template>
						<xsl:call-template name="create-divs" >
							<xsl:with-param name="pos-data">75</xsl:with-param>
						</xsl:call-template>
					</xsl:when>
				</xsl:choose>
			</div>
		</div>
	</xsl:template>	
	
	
	<xsl:template name="create-divs">
		<xsl:param name="pos-data"></xsl:param>
		<div class="col-md-3">
			<xsl:call-template name="loop">
				<xsl:with-param name="count-left">25</xsl:with-param>
		 		<xsl:with-param name="pos">
		 			<xsl:value-of select="$pos-data" />
		 		</xsl:with-param>
			</xsl:call-template>				
		</div>
	</xsl:template>
	
	
	<xsl:template name="loop">
	  <xsl:param name="count-left"></xsl:param>
	  <xsl:param name="pos"></xsl:param>
	    <xsl:if test="$count-left &gt; 0 and dri:table/dri:row[position()=$pos]/dri:cell/dri:xref/@target !=''">
	     <a href="dri:table/dri:row[position()=$pos]/dri:cell/dri:xref/@target"><xsl:value-of select="dri:table/dri:row[position()=$pos]/dri:cell/dri:xref"/></a>
	     <xsl:value-of select="dri:table/dri:row[position()=$pos]/dri:cell/text()"/>
	     <br></br>
	     <xsl:call-template name="loop">
	        <xsl:with-param name="count-left">
	        	<xsl:number value="number($count-left)-1" />
	        </xsl:with-param>
	        <xsl:with-param name="pos">
	        	<xsl:number value="number($pos)+1" />
	        </xsl:with-param>
	      </xsl:call-template>
	    </xsl:if>
	</xsl:template>
	
	
		
	
</xsl:stylesheet>