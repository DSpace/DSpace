- [Introduction](#Introduction)
- [Additional Config](#Additional-config)
- [Pubmed specific Config](#Pubmed-specific)
	- [Metadata mapping classes](#Metadata-classes)
	- [Service classes](#Service-classes)


# Introduction <a name="Introduction"></a> #

**[First read the base documentation on external importing](../README.md)**
This documentation explains the implementation of the importer framework using pubmed as an example.

The configuration done for pubmed specifically is located at pubmed-integration.xml in dspace/config/spring/api
I will not go into detail to what exactly is configured for the pubmed integration as it is simply a usage of the classes explained [here](../README.md)

# Pubmed specific classes Config <a name="Pubmed-specific"></a> #

These classes are simply implementations based of the base classes defined in importer/external. They add characteristic behaviour for services/mapping for the pubmed specific data.

## Metadata mapping classes <a name="Metadata-classes"></a> ##

- "PubmedFieldMapping". An implementation of AbstractMetadataFieldMapping, linking to the bean that serves as the entry point of other metadata mapping
- "PubmedDateMetadatumContributor"/"PubmedLanguageMetadatumContributor". Pubmed specific implementations of the "MetadataContributor" interface

## Service classes <a name="Service-classes"></a> ##

- "GeneratePubmedQueryService". Generates the pubmed query which is used to retrieve the records. This is based on a given item.
- "PubmedImportMetadataSourceServiceImpl". Child class of "AbstractImportMetadataSourceService", retrieving the records from pubmed.
