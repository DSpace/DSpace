<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        xmlns:ore="http://www.openarchives.org/ore/terms"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:dcterms="http://purl.org/dc/terms/"
        xmlns:foaf="http://xmlns.com/foaf/0.1/"
        xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
        version="1.0">

    <!-- match the top level RDF tag, and convert to the appropriate DIV tag -->
    <xsl:template match="/rdf:RDF">
        <div id="ore:ResourceMap">
    		<xsl:apply-templates/>
    	</div>
    </xsl:template>

    <!-- match each of the rdf:Description tags and convert -->
    <xsl:template match="/rdf:RDF/rdf:Description">
        <xsl:element name="div">

            <xsl:choose>
                <xsl:when test="@rdf:about">
                    <xsl:attribute name="about"><xsl:value-of select="@rdf:about"/></xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="about"><xsl:value-of select="@rdf:nodeID"/></xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>

            <xsl:for-each select="./*">
                <xsl:choose>
                    <xsl:when test="@rdf:resource">
                        <xsl:element name="a">
                            <xsl:attribute name="rel"><xsl:value-of select="name()"/></xsl:attribute>
                            <xsl:attribute name="href"><xsl:value-of select="@rdf:resource"/></xsl:attribute>
                            &#160; <!-- the space is a hack to force a closing tag -->
                        </xsl:element>
                    </xsl:when>
                    <xsl:when test="@rdf:nodeID">
                        <xsl:element name="a">
                            <xsl:attribute name="rel"><xsl:value-of select="name()"/></xsl:attribute>
                            <xsl:attribute name="href"><xsl:value-of select="@rdf:nodeID"/></xsl:attribute>
                            &#160; <!-- the space is a hack to force a closing tag -->
                        </xsl:element>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:element name="span">
                            <xsl:attribute name="property"><xsl:value-of select="name()"/></xsl:attribute>
                            <xsl:attribute name="content"><xsl:value-of select="."/></xsl:attribute>
                        </xsl:element>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>

        </xsl:element>
    </xsl:template>

</xsl:stylesheet>