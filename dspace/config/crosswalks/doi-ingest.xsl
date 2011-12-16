<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
        version="1.0">

    <xsl:strip-space elements="*"/>

    <xsl:output indent="no" />

    <!-- Catch all.  This template will ensure that nothing other than explicitly what we want to xwalk will be dealt with
    <xsl:template match="text()"></xsl:template>                                                       -->


    <!-- match the top level descriptionSet element and kick off the template matching process -->
    <xsl:template match="/doi_records/doi_record">
        <dim:dim>

            <!-- Author | dc:contributor.author | doi_record/crossref/journal/journal_article/contributors/person_name/given_name and doi_record/crossref/journal/journal_article/contributors/person_name/surname | need to combine the values from crossref into a single field for Dryad (surname, given name)  -->
            <xsl:for-each select="crossref/journal/journal_article/contributors/person_name">
                <xsl:variable name="author_givenName" select="./given_name"/>
                <xsl:variable name="author_surname" select="concat(./surname, ', ')"/>
                <xsl:variable name="author" select="concat($author_surname, $author_givenName)"/>

                <xsl:if test="$author!=''">
                    <dim:field mdschema="dc" element="contributor" qualifier="author">
                        <xsl:value-of select="$author"/>
                    </dim:field>
                </xsl:if>
            </xsl:for-each>


            <!-- Data Package Title | dc:title | doi_record/crossref/journal/journal_article/titles/title | will need to add "Data from: " to Crossref value to store correctly in Dryad  -->
            <xsl:if test="crossref/journal/journal_article/titles/title">
                <dim:field mdschema="dc" element="title">
                    Data from:<xsl:value-of select="crossref/journal/journal_article/titles/title"/>
                </dim:field>
            </xsl:if>

            <!--Article publication date | dc:date.issued | doi_record/crossref/journal/journal_article/publication_date/month and doi_record/crossref/journal/journal_article/publication_date/year | Crossref values will need to be combined into a single Dryad field as YYYY-MM-->
            <xsl:variable name="publication_month" select="crossref/journal/journal_article/publication_date/month"/>
            <xsl:variable name="publication_year"  select="concat(crossref/journal/journal_article/publication_date/year, '-')"/>
            <xsl:variable name="publication_date"  select="concat($publication_year, $publication_month)"/>

            <xsl:if test="$publication_date!=''">
                <dim:field mdschema="dc" element="date" qualifier="issued">
                    <xsl:value-of select="$publication_date"/>
                </dim:field>
            </xsl:if>


            <!-- Article DOI | dc:relation.isreferencedby | doi_record/crossref/journal/journal_article/doi_data/doi | will need to add "doi:" to Crossref value to store correctly in Dryad  -->
            <xsl:if test="crossref/journal/journal_article/doi_data/doi">
                <dim:field mdschema="dc" element="relation" qualifier="isreferencedby">
                    doi:<xsl:value-of select="crossref/journal/journal_article/doi_data/doi"/>
                </dim:field>
            </xsl:if>

            <!--Journal Name | prism:publicationName | doi_record/crossref/journal/journal_metadata/full_title | none-->
            <xsl:if test="crossref/journal/journal_metadata/full_title">
                <dim:field mdschema="prism" element="publicationName">
                    <xsl:value-of select="crossref/journal/journal_metadata/full_title"/>
                </dim:field>
            </xsl:if>


        </dim:dim>
    </xsl:template>

</xsl:stylesheet>