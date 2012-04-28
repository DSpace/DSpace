<xsl:stylesheet version="2.0" 
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
     xmlns="http://www.w3.org/1999/xhtml"
     xmlns:dri="http://di.tamu.edu/DRI/1.0/"
     xmlns:mets="http://www.loc.gov/METS/"
     xmlns:dim="http://www.dspace.org/xmlns/dspace/dim">
     
<xsl:template name='slideshow'>

	<div id="slides">
		<div class="slides_container">
		
			<div class="slide">
				<a target="_blank">
	    			<xsl:attribute name="href">
	    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/pages/queEsSedici
	    			</xsl:attribute>
		    		<img>
		    			<xsl:attribute name="src">
		    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/img_slideshow_1.jpg
		    			</xsl:attribute>
		    		</img>
				</a>
				<div class="caption">
					<h1>¿Qué es SeDiCI?</h1>
					<p>SeDiCI (Servicio de Difusión de la Creación Intelectual) es el repositorio institucional central de la Universidad Nacional de La Plata.</p>
				</div>
			</div>

			<div class="slide">
				<a href="http://sedici.unlp.edu.ar/blog/2012/04/25/migracion-a-dspace/‎" target="_blank">
		    		<img>
		    			<xsl:attribute name="src">
		    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/img_slideshow_2.jpg
		    			</xsl:attribute>
		    		</img>
				</a>
				<div class="caption">
					<h1>Migración a DSpace</h1>
					<p>Desde abril de 2012, SeDiCI se encuentra funcionando sobre plataforma de software completamente nueva. Bienvenidos a SeDiCI-DSpace.</p>
				</div>
			</div>
			
			
			<div class="slide">
				<a href="http://sedici.unlp.edu.ar/blog/2012/04/25/libro-electronico-supera-las-3000-descargas/" target="_blank">
		    		<img>
		    			<xsl:attribute name="src">
		    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/img_slideshow_3.jpg
		    			</xsl:attribute>
		    		</img>
				</a>
				<div class="caption">
					<h1>Libro electrónico supera las 3000 descargas</h1>
					<p>El libro electrónico Cirugía. Bases clínicas y terapéuticas editado el año pasado por SeDiCI ya superó las 3000 descargas.</p>
				</div>
			</div>
			
			
			<div class="slide">
				<a target="_blank">
	    			<xsl:attribute name="href">
	    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/pages/informacionTesistas
	    			</xsl:attribute>
		    		<img>
		    			<xsl:attribute name="src">
		    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/img_slideshow_4.jpg
		    			</xsl:attribute>
		    		</img>
				</a>
				<div class="caption">
					<h1>Depósito de tesis de posgrado</h1>
					<p>La resolución 78/11 de la UNLP dictamina que todos los alumnos de posgrado deben depositar una copia digital de sus tesis en SeDiCI.</p>
				</div>
			</div>

		</div>

		<a href="#" class="prev">
			<img>
    			<xsl:attribute name="src">
    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/arrow-prev.png
    			</xsl:attribute>
			</img>
		</a>
		<a href="#" class="next">
			<img>
    			<xsl:attribute name="src">
    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/arrow-next.png
    			</xsl:attribute>
			</img>
		</a>		
	</div>

</xsl:template>

