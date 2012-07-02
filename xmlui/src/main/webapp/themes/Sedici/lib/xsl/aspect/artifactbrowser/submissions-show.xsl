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

  <!-- Template - se redefine para cambiar el orden de como muestra en el listado de las tareas  -->
    <xsl:output indent="yes"/>
    <xsl:template match="dri:div[@id='aspect.submission.Submissions.div.submissions']">
    	<h1 class="ds-div-head"><xsl:copy-of select="dri:head" /></h1>
		<form id="aspect_submission_Submissions_div_submissions" 
		      class="ds-interactive-div primary" 
		      onsubmit="javascript:tSubmit(this);" 
		      method="post" 
		      xmlns="http://di.tamu.edu/DRI/1.0/" 
		      xmlns:i18n="http://apache.org/cocoon/i18n/2.1">
		    <xsl:attribute name="action">
						<xsl:value-of select="@action"/>
			</xsl:attribute>
			<xsl:apply-templates select="dri:div[@id='aspect.submission.Submissions.div.start-submision']" />
	    	<xsl:apply-templates select="dri:div[@id='aspect.submission.Submissions.div.unfinished-submisions']" />
	        <xsl:apply-templates select="dri:div[@id='aspect.xmlworkflow.Submissions.div.workflow-tasks']" />
	        <xsl:apply-templates select="dri:div[@id='aspect.xmlworkflow.Submissions.div.submissions-inprogress']" />
	        <xsl:apply-templates select="dri:div[@id='aspect.submission.Submissions.div.completed-submissions']" />
     	</form>
     </xsl:template>
	  
</xsl:stylesheet>