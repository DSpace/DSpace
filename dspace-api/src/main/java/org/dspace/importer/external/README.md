- [Introduction](#Introduction)
	- [Features](#Features)
	- [Abstraction of input format](#Abstraction-input-format)
	- [What it can't do](#cant-do)
- [Implementation of an import source](#Example-implementation)
	- [Inherited methods](#Inherited-methods)
	- [Metadata mapping](#Mapping)
	- [Examples](#Examples)

# Introduction <a name="Introduction"></a> #

This documentation explains the features and the usage of the importer framework. 

## Features <a name="Features"></a> ##

- lookup publications from remote sources
- Support for multiple implementations 

## Abstraction of input format <a name="Abstraction-input-format"></a> ##

The importer framework does not enforce a specific input format. Each importer implementation defines which input format it expects from a remote source. 

## What it can't do <a name="cant-do"></a> ##

- import remote records as DSpace items

# Implementation of an import source <a name="Example-implementation"></a> #

Each importer implementation must at least implement interface *org.dspace.importer.external.service.other.Imports* and implement the inherited methods.

One can also choose to implement class *org.dspace.importer.external.service.other.Source* next to the Imports interface. This class contains functionality to handle request timeouts and to retry requests.

A third option is to implement class *org.dspace.importer.external.service.AbstractImportSourceService*. This class already implements both the Imports interface and Source class. AbstractImportSourceService has a generic type set 'RecordType'. In the importer implementation this type set should be the class of the records received from the remote source's response (e.g. when using axiom to get the records from the remote source's XML response, the importer implementation's type set is *org.apache.axiom.om.OMElement*). 

Implementing the AbstractImportSourceService allows the importer implementation to use the framework's build-in support to transform a record received from the remote source to an object of class *org.dspace.importer.external.datamodel.ImportRecord* containing DSpace metadata fields.

## Inherited methods <a name="Inherited-methods"></a> ##

Method getImportSource() should return a unique identifier. Importer implementations should not be called directly, but class *org.dspace.importer.external.service.ImportService* should be called instead. This class contains the same methods as the importer implementatons, but with an extra parameter 'url'. This url parameter should contain the same identifier that is returned by the getImportSource() method of the importer implementation you want to use.

The other inherited methods are used to query the remote source. 

## Metadata mapping <a name="Mapping"></a> ##

When using an implementation of AbstractImportSourceService, a mapping of remote record fields to DSpace metadata fields can be created. 

first create an implementation of class AbstractMetadataFieldMapping with the same type set used for the importer implementation.

Then create a spring configuration file in [dspace.dir]/config/spring/api.

Each DSpace metadata field that will be used for the mapping must first be configured as a spring bean of class *org.dspace.importer.external.metadatamapping.MetadataFieldConfig*.

```xml
	<bean id="dc.title" class="org.dspace.importer.external.metadatamapping.MetadataFieldConfig">
        <constructor-arg value="dc.title"/>
    </bean>
```

Now this metadata field can be used to create a mapping. To add a mapping for the "dc.title" field declared above, a new spring bean configuration of class *org.dspace.importer.external.metadatamapping.contributor.SimpleXpathMetadatumContributor* needs to be added. This bean expects 2 property values:

- field: A reference to the configured spring bean of the DSpace metadata field. e.g. the "dc.title" bean declared above. 
- query: The xpath expression used to select the record value returned by the remote source.

```xml
    <bean id="titleContrib" class="org.dspace.importer.external.metadatamapping.contributor.SimpleXpathMetadatumContributor">
        <property name="field" ref="dc.title"/>
        <property name="query" value="dc:title"/>
    </bean>
```

Multiple record fields can also be combined into one value. To implement a combined mapping first create a "*SimpleXpathMetadatumContributor*" as explained above for each part of the field. 

```xml
    <bean id="lastNameContrib" class="org.dspace.importer.external.metadatamapping.contributor.SimpleXpathMetadatumContributor">
        <property name="field" ref="dc.contributor.author"/>
        <property name="query" value="x:authors/x:author/x:surname"/>
    </bean>
    <bean id="firstNameContrib" class="org.dspace.importer.external.metadatamapping.contributor.SimpleXpathMetadatumContributor">
        <property name="field" ref="dc.contributor.author"/>
        <property name="query" value="x:authors/x:author/x:given-name"/>
    </bean>
```

Note that namespaces used in the xpath queries are configured in bean "FullprefixMapping" in the same spring file.

```xml
    <util:map id="FullprefixMapping" key-type="java.lang.String" value-type="java.lang.String">
        <description>Defines the namespace mappin for the SimpleXpathMetadatum contributors</description>
        <entry key="http://purl.org/dc/elements/1.1/" value="dc"/>
        <entry key="http://www.w3.org/2005/Atom" value="x"/>
    </util:map>
```

Then create a new list in the spring configuration containing references to all "*SimpleXpathMetadatumContributor*" beans that need to be combined.

```xml
	<util:list id="combinedauthorList" value-type="org.dspace.importer.external.metadatamapping.contributor.MetadataContributor" list-class="java.util.LinkedList">
        <ref bean="lastNameContrib"/>
        <ref bean="firstNameContrib"/>
	</util:list>{{/code}}
```

Finally create a spring bean configuration of class *org.dspace.importer.external.metadatamapping.contributor.CombinedMetadatumContributo*. This bean expects 3 values:

- field: A reference to the configured spring bean of the DSpace metadata field. e.g. the "dc.title" bean declared above. 
- metadatumContributors: A reference to the list containing all the single record field mappings that need to be combined. 
- separator: These characters will be added between each record field value when they are combined into one field. 

```xml
    <bean id="authorContrib" class="org.dspace.importer.external.metadatamapping.contributor.CombinedMetadatumContributor">
        <property name="separator" value=", "/>
        <property name="metadatumContributors" ref="combinedauthorList"/>
        <property name="field" ref="dc.contributor.author"/>
    </bean>
```

Each contributor must also be added to the "MetadataFieldMap" used by the "*MetadataFieldMapping*" implementation. Each entry of this map maps a metadata field bean to a contributor. For the contributors created above this results in the following configuration:

```xml
    <util:map id="org.dspace.importer.external.metadatamapping.MetadataFieldConfig"
              value-type="org.dspace.importer.external.metadatamapping.contributor.MetadataContributor">
        <entry key-ref="dc.title" value-ref="titleContrib"/>
        <entry key-ref="dc.contributor.author" value-ref="authorContrib"/>
    </util:map>
```

Note that the single field mappings used for the combined author mapping are not added to this list. 

## Example <a name="Example"></a> ##

An example of an importer implementation is *org.dspace.importer.external.scidir.ScidirImportSourceServiceImpl*.

An example of a spring configuration file can be found at [dspace.dir]/config/spring/api/scidir-services.xml.