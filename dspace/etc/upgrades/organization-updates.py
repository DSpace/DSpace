#!/usr/bin/env python

# Updates conceptmetadatavalue table to use the latest schemas as of August 2016.

__author__ = 'daisieh'

import re
import os
import sys
import shutil
import hashlib

def dict_from_query(sql):
    # Now execute it
    cmd = "psql -A -U dryad_app dryad_repo -c \"%s\"" % sql
    output = [line.strip().split('|') for line in os.popen(cmd).readlines()]
    if len(output) <= 2: # the output should have at least 3 lines: header, body rows, number of rows
        return None
    else:
        return dict(zip(output[0],output[1]))

def update_field_id(organization_field_id, journal_field_id):
    sql = "update conceptmetadatavalue set field_id=%s where field_id=%s" % (organization_field_id, journal_field_id)
    cmd = "psql -U dryad_app dryad_repo -c \"%s\"" % sql
    print os.popen(cmd).read()

def delete_field_id(journal_field_id):
    sql = "delete from metadatafieldregistry where metadata_field_id=%s" % (journal_field_id)
    cmd = "psql -U dryad_app dryad_repo -c \"%s\"" % sql
    print os.popen(cmd).read()

def main():
    organization_schema = dict_from_query("select metadata_schema_id from metadataschemaregistry where short_id = 'organization'")['metadata_schema_id']
    journal_schema = dict_from_query("select metadata_schema_id from metadataschemaregistry where short_id = 'journal'")['metadata_schema_id']

    # update the field_id for full names:
    journal_field_id = dict_from_query("select metadata_field_id from metadatafieldregistry where metadata_schema_id=%s and element='fullname'" % journal_schema)
    if journal_field_id is not None:
        journal_field_id = journal_field_id['metadata_field_id']
        organization_field_id = dict_from_query("select metadata_field_id from metadatafieldregistry where metadata_schema_id=%s and element='fullName'" % organization_schema)['metadata_field_id']
        update_field_id(organization_field_id, journal_field_id)
        delete_field_id(journal_field_id)

    # update the field_id for payment plans:
    journal_field_id = dict_from_query("select metadata_field_id from metadatafieldregistry where metadata_schema_id=%s and element='paymentPlanType'" % journal_schema)
    if journal_field_id is not None:
        journal_field_id = journal_field_id['metadata_field_id']
        organization_field_id = dict_from_query("select metadata_field_id from metadatafieldregistry where metadata_schema_id=%s and element='paymentPlanType'" % organization_schema)['metadata_field_id']
        update_field_id(organization_field_id, journal_field_id)
        delete_field_id(journal_field_id)

    # update the field_id for customer IDs:
    journal_field_id = dict_from_query("select metadata_field_id from metadatafieldregistry where metadata_schema_id=%s and element='customerID'" % journal_schema)
    if journal_field_id is not None:
        journal_field_id = journal_field_id['metadata_field_id']
        organization_field_id = dict_from_query("select metadata_field_id from metadatafieldregistry where metadata_schema_id=%s and element='customerID'" % organization_schema)['metadata_field_id']
        update_field_id(organization_field_id, journal_field_id)
        delete_field_id(journal_field_id)

    # update the field_id for descriptions:
    journal_field_id = dict_from_query("select metadata_field_id from metadatafieldregistry where metadata_schema_id=%s and element='description'" % journal_schema)
    if journal_field_id is not None:
        journal_field_id = journal_field_id['metadata_field_id']
        organization_field_id = dict_from_query("select metadata_field_id from metadatafieldregistry where metadata_schema_id=%s and element='description'" % organization_schema)['metadata_field_id']
        update_field_id(organization_field_id, journal_field_id)
        delete_field_id(journal_field_id)

    # update the field_id for websites:
    journal_field_id = dict_from_query("select metadata_field_id from metadatafieldregistry where metadata_schema_id=%s and element='website'" % journal_schema)
    if journal_field_id is not None:
        journal_field_id = journal_field_id['metadata_field_id']
        organization_field_id = dict_from_query("select metadata_field_id from metadatafieldregistry where metadata_schema_id=%s and element='website'" % organization_schema)['metadata_field_id']
        update_field_id(organization_field_id, journal_field_id)
        delete_field_id(journal_field_id)

    # update the journal view:
    sql = "CREATE OR REPLACE VIEW journal_name_view AS SELECT journal_id.parent_id as organization_id, journal_id.text_value as name FROM conceptmetadatavalue as journal_id, metadataschemaregistry journalschema INNER JOIN metadatafieldregistry jid on journalschema.short_id='organization' and jid.metadata_schema_id=journalschema.metadata_schema_id and jid.element = 'fullName' WHERE jid.metadata_field_id = journal_id.field_id"
    cmd = "psql -U dryad_app dryad_repo -c \"%s\"" % sql
    print os.popen(cmd).read()

    sql = "CREATE or replace VIEW journal AS SELECT journal_code_view.organization_id as concept_id, code, name, issn from journal_code_view inner join journal_name_view on journal_name_view.organization_id = journal_code_view.organization_id left join journal_issn_view on journal_issn_view.organization_id = journal_code_view.organization_id"
    cmd = "psql -U dryad_app dryad_repo -c \"%s\"" % sql
    print os.popen(cmd).read()

    sql = "DROP VIEW organization"
    cmd = "psql -U dryad_app dryad_repo -c \"%s\"" % sql
    print os.popen(cmd).read()
if __name__ == '__main__':
    main()

