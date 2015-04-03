# coding=utf-8
# This work is licensed!
# pylint: disable=W0702,R0201,C0111,W0613,R0914

"""
    We have a cursor to db database.
"""
from collections import defaultdict


def item2metadata( cursor, item_id ):
    cursor.execute( """
        select metadataschemaregistry.short_id,  metadatafieldregistry.element, metadatafieldregistry.qualifier, metadatavalue.text_value
             from metadatavalue
                inner join metadatafieldregistry on metadatavalue.metadata_field_id =  metadatafieldregistry.metadata_field_id
                inner join metadataschemaregistry on metadatafieldregistry.metadata_schema_id =  metadataschemaregistry.metadata_schema_id
             where
                metadatavalue.item_id = %d
    """ % item_id )
    objs = cursor.fetchall()
    vals = defaultdict(list)
    for s, e, q, v in objs:
        k = "%s.%s" % (s, e)
        if q is not None:
            k += "." + q
        vals[k].append(v)
    return vals 


def db2items( cursor ):
    cursor.execute( """
        select item.item_id, eperson.email, collection.name
             from item 
                inner join eperson on item.submitter_id = eperson.eperson_id
                inner join collection on item.owning_collection = collection.collection_id
    """ )
    objs = cursor.fetchall()
    items = dict([(item_id, 
                    { 
                        "submitter": ep,
                        "collection": col,
                    }
                  ) for item_id, ep, col in objs])
    return items


def do( cursor, _1, _2 ):
    """
        Do something with the database.
    """
    items = db2items( cursor )

    for k, v in items.iteritems():
        v["metadata"] = item2metadata(cursor, k)

    for i, k in enumerate( sorted(items, key=lambda x: len(items[x]["metadata"])) ):
        #if i > 100:
        #    break
        v = items[k]

        for km, vm in v["metadata"].iteritems():
            if len(vm) > 1 and km not in (
                "dc.description.provenance",
                "dc.subject",
                "dc.language.iso",
                "dc.contributor.author",
                "dc.relation.requires",
                "metashare.ResourceInfo#DistributionInfo#LicenseInfo.restrictionsOfUse",
                "metashare.ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo.projectName",
                "metashare.ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo.fundingType",
                "metashare.ResourceInfo#DistributionInfo#LicenseInfo.distributionAccessMedium",
                "metashare.ResourceInfo#ResourceDocumentationInfo.samplesLocation",
                "dc.publisher",
                "dc.contributor.other",


                #"dc.date.available",
                #"dc.rights.label",
                #"dc.date.accessioned",
                #"dc.source.uri",
            ):
                print 20 * "="
                print k, v["submitter"], v["collection"]
                print "\t", km, vm
