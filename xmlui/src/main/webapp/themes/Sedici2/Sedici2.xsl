<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright (C) 2011 SeDiCI <info@sedici.unlp.edu.ar>

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<!--
    TODO: Describe this XSL file
    Author: Alexey Maslov

-->

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

    <xsl:import href="../Mirage/Mirage.xsl"/>
    <xsl:import href="lib/xsl/core/constant.xsl"/>
	<xsl:import href="lib/xsl/aspect/artifactbrowser/community-hierarchical-list.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/collection-list.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/item-view.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/collection-view-append.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/community-view-append.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/community-collection-search.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/authors_table.xsl"/>
      <xsl:import href="lib/xsl/aspect/artifactbrowser/community_table.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/recent_submissions.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/discovery_search.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/submissions-show.xsl"/>
    <xsl:import href="lib/xsl/core/utils.xsl"/>
    <xsl:import href="lib/xsl/core/forms.xsl"/>
    <xsl:import href="lib/xsl/core/page-structure.xsl"/>
    <xsl:import href="lib/xsl/core/feedback.xsl"/>
    <xsl:import href="lib/xsl/aspect/general/choice-authority-control.xsl"/>
    <xsl:import href="lib/xsl/aspect/news/news-list.xsl"/>
    <xsl:import href="lib/xsl/core/menu-superior.xsl"/>
    <xsl:import href="lib/xsl/core/menu-lateral.xsl"/>
    <xsl:import href="lib/xsl/core/paginas_estaticas.xsl"/>
    <xsl:import href="lib/xsl/core/templatesEspecificos/profile.xsl"/>
    <xsl:import href="lib/xsl/core/templatesEspecificos/form_fecha_publicacion.xsl"/>
    <xsl:import href="lib/xsl/core/groups_in_workflow.xsl"/>
    <xsl:output indent="yes"/>
    

</xsl:stylesheet>
