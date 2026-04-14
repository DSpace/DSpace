<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : DIM2CRossref_article.xsl
    Created on : October 4, 2020, 1:26 PM
    Author     : jdamerow
    Description: Converts metadata from DSpace Intermediat Format (DIM) into
                 metadata following the Crossref Schema for Article, version 4.4.2
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns="http://www.crossref.org/schema/4.4.2"
                version="1.0">
    <xsl:variable name="bookType" select="//dspace:field[@mdschema='dc' and @element='type']/text()" ></xsl:variable>

    <xsl:output method="xml" indent="yes" encoding="utf-8" />

    <!-- Don't copy everything by default! -->
    <xsl:template match="@* | text()" />

    <xsl:template match="/dspace:dim[@dspaceType='ITEM']">
      <doi_batch version="4.4.2" xmlns="http://www.crossref.org/schema/4.4.2"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://www.crossref.org/schema/4.4.2 http://www.crossref.org/schema/deposit/crossref4.4.2.xsd">

          <head>
              <!-- This section will be filled programmatically. Do not remove! -->
          </head>

          <body>
            <journal>
              <journal_metadata>

                <!--
                  CrossRef
                  Add title information
                -->
                <full_title>
                    <xsl:choose>
                      <xsl:when test="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']">
                        <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']"/>
                      </xsl:when>
                      <xsl:otherwise>
                        (:unas) unassigned
                      </xsl:otherwise>
                    </xsl:choose>
                </full_title>
                <abbrev_title>
                    <xsl:choose>
                      <xsl:when test="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']">
                        <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']"/>
                      </xsl:when>
                      <xsl:otherwise>
                        (:unas) unassigned
                      </xsl:otherwise>
                    </xsl:choose>
                </abbrev_title>

                <xsl:if test="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='issn']">
                  <xsl:apply-templates select="dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='issn']" />
                </xsl:if>
              </journal_metadata>

              <!-- Add journal article info -->
              <journal_article>

                <!--
                  CrossRef
                  Add title information
                -->
                <titles>
                    <xsl:variable name="title-field" select="(//dspace:field[@mdschema='dc' and @element='title' and not(@qualifier)])[1]" />
                    <xsl:choose>
                      <xsl:when test="$title-field">
                        <xsl:apply-templates select="$title-field"/>
                        <original_language_title>
                          (:unas) unassigned
                        </original_language_title>
                      </xsl:when>
                      <xsl:otherwise>
                        <title>
                          (:unas) unassigned
                        </title>
                        <original_language_title>
                          (:unas) unassigned
                        </original_language_title>
                      </xsl:otherwise>
                    </xsl:choose>
                </titles>

                <!--
                  CrossRef
                  Add author information
                -->
                <contributors>
                  <xsl:choose>
                      <xsl:when test="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']">
                        <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']" />
                      </xsl:when>
                      <xsl:otherwise>
                        <anonymous contributor_role="author" sequence="first" />
                      </xsl:otherwise>
                  </xsl:choose>
                </contributors>

                <!--
                  CrossRef
                  Add publication year information
                -->
                <publication_date>
                  <year>
                    <xsl:choose>
                        <xsl:when test="//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued']">
                            <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued'], 1, 4)" />
                        </xsl:when>
                        <xsl:when test="//dspace:field[@mdschema='dc' and @element='date' and @qualifier='available']">
                            <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued'], 1, 4)" />
                        </xsl:when>
                        <xsl:when test="//dspace:field[@mdschema='dc' and @element='date']">
                            <xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date'], 1, 4)" />
                        </xsl:when>
                        <xsl:otherwise>0000</xsl:otherwise>
                    </xsl:choose>
                  </year>
                </publication_date>

                <!--
                  CrossRef
                  Add DOI information
                -->
                <doi_data>
                    <!-- This section will be filled programmitcally. Do not remove! -->
                </doi_data>
              </journal_article>
            </journal>
          </body>
      </doi_batch>
    </xsl:template>

    <!-- template to create first author -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author'][1]">
      <person_name sequence="first" contributor_role="author" >
      <given_name>
        <xsl:value-of select="substring-before(./text(), ',')"/>
      </given_name>
      <surname>
        <xsl:value-of select="substring-after(./text(), ',')"/>
      </surname>
    </person_name>
    </xsl:template>

    <!-- template to create additional authors -->
    <xsl:template match="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author'][position() > 1]">
      <person_name sequence="additional" contributor_role="author" >
      <given_name>
        <xsl:value-of select="substring-before(./text(), ',')"/>
      </given_name>
      <surname>
        <xsl:value-of select="substring-after(./text(), ',')"/>
      </surname>
    </person_name>
    </xsl:template>

    <!-- template to create titles -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='title' and not(@qualifier)]">
      <title>
        <xsl:value-of select="." />
      </title>
    </xsl:template>

    <!-- template to create journal title -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']">
      <xsl:value-of select="." />
    </xsl:template>

    <!-- template to create journal issn -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='issn']">
      <issn>
        <xsl:value-of select="." />
      </issn>
    </xsl:template>
</xsl:stylesheet>
