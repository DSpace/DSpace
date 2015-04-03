# -*- coding: utf-8 -*-
"""
    oval
    ~~~~

    BASE OAI-PMH Validity Checker

    :copyright: Copyright 2011 Mathias Loesch.
"""

import os

this_dir, this_filename = os.path.split(__file__)

DATA_PATH = os.path.join(this_dir, "data")
__version__ = '0.2.0'
OAI_PMH_VERSION = '2.0'


def compress(_list):
    _list.remove(_list[0])
    return [item for item in _list if item != '']

iso_639_file = open(os.path.join(DATA_PATH, 'iso-639-3.tab'), 'r')
ISO_639_3_CODES = compress([line.split('\t')[0] for line in iso_639_file])
iso_639_file.seek(0)
ISO_639_2B_CODES = compress([line.split('\t')[1] for line in iso_639_file])
iso_639_file.seek(0)
ISO_639_2T_CODES = compress([line.split('\t')[2] for line in iso_639_file])
iso_639_file.seek(0)
ISO_639_1_CODES = compress([line.split('\t')[3] for line in iso_639_file])

