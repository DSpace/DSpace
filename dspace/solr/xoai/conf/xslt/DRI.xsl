<?xml version='1.0' encoding='UTF-8'?>

<!--
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 -->

<!--
  Simple transform of Solr query results to HTML
 -->
<xsl:stylesheet version='1.0'
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
>

  <xsl:output media-type="text/xml; charset=utf-8" encoding="UTF-8"/>

  <xsl:template match='/'>
    <dri:div rend="solr-results">
      <dri:div rend="solr-facets">
          <xsl:apply-templates select="response/lst[@name='facet_counts']/lst[@name='facet_fields']/lst"/>
      </dri:div>
      <dri:div rend="solr-info">
          <dri:div rend="solr-numFound">
              <xsl:value-of select="response/result/@numFound"/>
          </dri:div>
          <dri:div rend="solr-start">
              <xsl:value-of select="response/result/@start"/>
          </dri:div>
          <dri:div rend="solr-dspace">
              <xsl:value-of select="response/lst[@name='responseHeader']/lst[@name='params']/str[@name='dspace']"/>
          </dri:div>
          <dri:div rend="contextPath">
              <xsl:value-of select="response/lst[@name='responseHeader']/lst[@name='params']/str[@name='contextPath']"/>
          </dri:div>
          <dri:div rend="queryString">
              <xsl:value-of select="response/lst[@name='responseHeader']/lst[@name='params']/str[@name='queryString']"/>
          </dri:div>
          <dri:div rend="URI">
              <xsl:value-of select="response/lst[@name='responseHeader']/lst[@name='params']/str[@name='URI']"/>
          </dri:div>
      </dri:div>
      <dri:div rend="solr-facet-queries">
          <dri:list n="facet">
            <xsl:apply-templates select="response/lst[@name='responseHeader']/lst[@name='params']/str[@name='fq']" mode="facet-field"/>
            <xsl:apply-templates select="response/lst[@name='responseHeader']/lst[@name='params']/arr[@name='fq']/str" mode="facet-field"/>
          </dri:list>
      </dri:div>
      <dri:div rend="solr-objects">
          <dri:referenceSet type="summaryList">
            <xsl:apply-templates select="response/result/doc"/>
          </dri:referenceSet>
      </dri:div>
    </dri:div>
  </xsl:template>

  <xsl:template match="doc">
      <dri:reference repositoryID="123456789" type="DSpace Item">
          <xsl:attribute name="url">
              <xsl:text>/metadata/handle/</xsl:text>
              <xsl:value-of select="str[@name = 'handle']"/>
              <xsl:text>/mets.xml</xsl:text>
          </xsl:attribute>
      </dri:reference>
  </xsl:template>

  <xsl:template match="lst">
      <dri:list n="facet">
          <dri:head>
              <xsl:value-of select="@name"/>
          </dri:head>
          <xsl:apply-templates select="*" mode="facet"/>
      </dri:list>
  </xsl:template>

  <xsl:template match="*" mode="facet">
      <dri:item>
          <dri:xref rend="facet"> <!-- TODO: I don't think rend is allowed per definition -->
              <xsl:attribute name="target">
                  <xsl:value-of select="."/>
              </xsl:attribute>
              <xsl:value-of select="@name"/>
          </dri:xref>
      </dri:item>
  </xsl:template>

  <xsl:template match="str" mode="facet-field">
      <dri:item>
          <xsl:value-of select="."/>
      </dri:item>
  </xsl:template>

</xsl:stylesheet>
