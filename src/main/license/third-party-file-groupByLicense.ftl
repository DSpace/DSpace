<#--
  #%L
  License Maven Plugin
  %%
  Copyright (C) 2012 Codehaus, Tony Chemit
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Lesser Public License for more details.

  You should have received a copy of the GNU General Lesser Public
  License along with this program.  If not, see
  <http://www.gnu.org/licenses/lgpl-3.0.html>.
  #L%
  -->
<#-- Minor tweaks for DSpace.
     ORIGINAL BORROWED FROM:
     https://fisheye.codehaus.org/browse/mojo/trunk/mojo/license-maven-plugin/src/main/resources/org/codehaus/mojo/license
  -->

<#-- To render the third-party file.
 Available context :
 - dependencyMap a collection of Map.Entry with
   key are dependencies (as a MavenProject) (from the maven project)
   values are licenses of each dependency (array of string)

 - licenseMap a collection of Map.Entry with
   key are licenses of each dependency (array of string)
   values are all dependencies using this license
-->
<#function artifactFormat p>
    <#if p.name?index_of('Unnamed') &gt; -1>
        <#return p.artifactId + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    <#else>
        <#return p.name + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    </#if>
</#function>

<#if licenseMap?size == 0>
The project has no dependencies.
<#else>
DSpace uses third-party libraries which may be distributed under different 
licenses. We have listed all of these third party libraries and their licenses
below. This file can be regenerated at any time by simply running:
    
    mvn clean verify -Dthird.party.licenses=true

You must agree to the terms of these licenses, in addition to the DSpace 
source code license, in order to use this software.

---------------------------------------------------
Third party Java libraries listed by License type.

PLEASE NOTE: Some dependencies may be listed under multiple licenses if they
are dual-licensed. This is especially true of anything listed as 
"GNU General Public Library" below, as DSpace actually does NOT allow for any 
dependencies that are solely released under GPL terms. For more info see:
https://wiki.duraspace.org/display/DSPACE/Code+Contribution+Guidelines
---------------------------------------------------
    <#list licenseMap as e>
        <#assign license = e.getKey()/>
        <#assign projects = e.getValue()/>
        <#if projects?size &gt; 0>

    ${license}:

        <#list projects as project>
        * ${artifactFormat(project)}
        </#list>
        </#if>
    </#list>
</#if>
