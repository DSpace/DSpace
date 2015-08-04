<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">
	
	<xsl:template match="dri:div[@id='aspect.submission.Submissions.div.submissions' and @n='submissions']">
		<form xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" id="aspect_submission_Submissions_div_submissions" class="ds-interactive-div primary" action="submissions" method="post" onsubmit="javascript:tSubmit(this);">
			<xsl:apply-templates select="dri:head" />
			<xsl:apply-templates select="dri:div[@id='aspect.submission.Submissions.div.unfinished-submisions' and @n='unfinished-submisions']" />
			<xsl:apply-templates select="dri:div[@id='aspect.xmlworkflow.Submissions.div.workflow-tasks' and @n='workflow-tasks']" />
			<xsl:apply-templates select="dri:div[@id='aspect.submission.Submissions.div.completed-submissions' and @n='completed-submissions']"/>
		</form>
	</xsl:template>	
	
</xsl:stylesheet>