<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
        version="1.0">


    <!-- Catch all.  This template will ensure that nothing other than explicitly what we want to xwalk will be dealt with -->
    <!--xsl:template match="text()"></xsl:template-->
    <xsl:strip-space elements="*"/>

    <xsl:output indent="no" />

    <!-- match the top level descriptionSet element and kick off the template matching process -->
    <xsl:template match="/PubmedArticleSet/PubmedArticle[1]">
        <dim:dim>

            <!-- Author -->
            <xsl:for-each select="MedlineCitation/Article/AuthorList/Author">
                <xsl:variable name="author_givenName" select="./ForeName"/>
                <xsl:variable name="author_surname" select="concat(./LastName, ', ')"/>
                <xsl:variable name="author" select="concat($author_surname, $author_givenName)"/>
                <xsl:if test="$author!=''">
                    <dim:field mdschema="dc" element="contributor" qualifier="author">
                        <xsl:value-of select="$author"/>
                    </dim:field>
                </xsl:if>
            </xsl:for-each>

            <!-- Title -->
            <xsl:if test="MedlineCitation/Article/Journal/Title">
                <dim:field mdschema="prism" element="publicationName">
                    Data from:<xsl:value-of select="MedlineCitation/Article/Journal/Title"/>
                </dim:field>
            </xsl:if>

            <!-- DATE -->
            <xsl:variable name="publication_month" select="MedlineCitation/DateCreated/Month"/>
            <xsl:variable name="publication_year"  select="concat(MedlineCitation/DateCreated/Year, '-')"/>
            <xsl:variable name="publication_date"  select="concat($publication_year, $publication_month)"/>
            <xsl:if test="$publication_date!=''">
                <dim:field mdschema="dc" element="date" qualifier="issued">
                    <xsl:value-of select="$publication_date"/>
                </dim:field>
            </xsl:if>

             <!-- ISReferencedBy -->
            <xsl:if test="MedlineCitation/PMID">
                <dim:field mdschema="dc" element="relation" qualifier="isreferencedby">
                    PMID:<xsl:value-of select="MedlineCitation/PMID"/>
                </dim:field>
            </xsl:if>

            <!-- Full Title -->
            <xsl:if test="MedlineCitation/Article/ArticleTitle">
                <dim:field mdschema="dc" element="title">
                    <xsl:value-of select="MedlineCitation/Article/ArticleTitle"/>
                </dim:field>
            </xsl:if>


            <!-- Abstract -->
            <xsl:if test="MedlineCitation/Article/Abstract/AbstractText">
                <dim:field mdschema="dc" element="description">
                    <xsl:value-of select="MedlineCitation/Article/Abstract/AbstractText"/>
                </dim:field>
            </xsl:if>


        </dim:dim>
    </xsl:template>

</xsl:stylesheet>