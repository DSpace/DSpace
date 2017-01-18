#!/usr/bin/env python

# Updates conceptmetadatavalue table to use the latest schemas as of August 2016.

__author__ = 'daisieh'

import re
import os
import sys
import shutil
import hashlib

def rows_from_query(sql):
    # Now execute it
    cmd = "psql -A -U dryad_app dryad_repo -c \"%s\"" % sql
    output = [line.strip().split('|') for line in os.popen(cmd).readlines()]
    if len(output) <= 2: # the output should have at least 3 lines: header, body rows, number of rows
        return None
    else:
        return output[1:-1]

def update_funder(metadata_value_id, fundingEntity):
    m = re.search('(.*)@National Science Foundation \(United States\)', fundingEntity)
    if m is None:
        new_val = fundingEntity + "@National Science Foundation (United States)"
#         print new_val
        sql = "update metadatavalue set text_value = '%s' where metadata_value_id =%s" % (new_val, metadata_value_id)
        cmd = "psql -U dryad_app dryad_repo -c \"%s\"" % sql
        print os.popen(cmd).read()

def main():
    fundingEntities = rows_from_query("select text_value, metadata_value_id from metadatavalue where metadata_field_id = 146")
    for funder in fundingEntities:
        update_funder(funder[1], funder[0])

if __name__ == '__main__':
    main()

