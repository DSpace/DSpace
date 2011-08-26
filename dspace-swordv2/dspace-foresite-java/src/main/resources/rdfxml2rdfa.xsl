<!--
 * rdfxml2rdfa.xsl
 *
 * Copyright (c) 2008, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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