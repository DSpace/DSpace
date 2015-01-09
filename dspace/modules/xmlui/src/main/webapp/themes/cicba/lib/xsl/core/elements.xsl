<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

	<!-- 
		Este template cambia el link "comenzar otro envío" en la página de Submission.
		Si el usuario es ADMIN o tiene permisos ADD sobre alguna colección o comunidad, entonces se muestra el SUBMIT general.
		Caso contrario, se le envía al submit sobre la colección de Autoarchivo
		Ver aspect "LocalConfigConfiguration"
	-->	
	<xsl:template match="dri:div[@n='unfinished-submisions']/dri:p/dri:hi/dri:xref" priority="1">
		<a>
            <xsl:if test="@target">
                <xsl:attribute name="href">
                	<xsl:choose>
						<xsl:when test="/dri:document/dri:meta/dri:userMeta/dri:metadata[@element='identifier' and @qualifier='group']">
							<xsl:value-of select="@target"/>	
						</xsl:when>
						<xsl:otherwise>
							<xsl:call-template name="print-path">
								<xsl:with-param name="path" select="'/handle/123456789/2/submit'"/>
							</xsl:call-template>
						</xsl:otherwise>
					</xsl:choose>
                </xsl:attribute>
            </xsl:if>

            <xsl:if test="@rend">
                <xsl:attribute name="class"><xsl:value-of select="@rend"/></xsl:attribute>
            </xsl:if>

            <xsl:if test="@n">
                <xsl:attribute name="name"><xsl:value-of select="@n"/></xsl:attribute>
            </xsl:if>

            <xsl:if test="@onclick">
                <xsl:attribute name="onclick"><xsl:value-of select="@onclick"/></xsl:attribute>
            </xsl:if>

            <xsl:apply-templates />
        </a>
	</xsl:template>
</xsl:stylesheet>