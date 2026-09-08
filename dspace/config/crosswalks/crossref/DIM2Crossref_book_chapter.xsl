<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : DIM2Crossref_book_chapter.xsl
    Created on : October 4, 2020, 1:26 PM
    Author     : jdamerow
    Description: Converts metadata from DSpace Intermediat Format (DIM) into
                 metadata following the Crossref Schema for Book Chapters, version 4.4.2
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
            <book>
              <xsl:attribute name="book_type">
                  <xsl:choose>
                      <xsl:when test="$bookType='Book chapter'">monograph</xsl:when>
                      <xsl:otherwise>other</xsl:otherwise>
                  </xsl:choose>
              </xsl:attribute>
              <book_metadata>

                <!--
                  CrossRef
                  Add title information
                -->
                <titles>
                    <xsl:choose>
                      <xsl:when test="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']">
                          <xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']"/>
                      </xsl:when>
                      <xsl:otherwise>
                        <title>
                          (:unas) unassigned
                        </title>
                      </xsl:otherwise>
                    </xsl:choose>
                </titles>

                <!--
                  CrossRef
                  Add publication year information
                  This is required by crossref
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
                  Add ISBN information
                  Crossref requires this field
                -->
                <xsl:choose>
                  <xsl:when test="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='isbn']">
                    <isbn>
                      <xsl:value-of select="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='isbn']/text()" />
                    </isbn>
                  </xsl:when>
                  <xsl:otherwise>
                    <noisbn reason="monograph" />
                  </xsl:otherwise>
                </xsl:choose>

                <!--
                  CrossRef
                  Add publisher information
                  This is required by crossref
                -->
                <publisher>
                  <publisher_name>
                    <xsl:value-of select="//dspace:field[@mdschema='dc' and @element='publisher']/text()" />
                  </publisher_name>
                </publisher>
              </book_metadata>

              <!-- Add chapter info -->
              <content_item component_type="chapter">
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
              </content_item>
            </book>
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
    <xsl:template match="dspace:field[@mdschema='dc' and @element='title']">
        <xsl:choose>
            <xsl:when test="@qualifier='alternative'">
                <subtitle>
                  <xsl:value-of select="." />
                </subtitle>
            </xsl:when>
            <xsl:otherwise>
              <title>
                <xsl:value-of select="." />
              </title>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- template to create book title -->
    <xsl:template match="dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']">
        <title>
          <xsl:value-of select="." />
        </title>
    </xsl:template>

</xsl:stylesheet>
