# coding=utf-8
# See main file for licence
# pylint: disable=W0702,R0201

"""
  Settings module.
"""
import os
import re


settings = {


    # name
    "name": u"LINDAT/CLARIN file integrity checker",

    # logger config - read from _logger
    "logger_config": os.path.join(
        os.path.dirname( __file__ ), "logger.config" ),

    # check for db params
    "dspace_cfg_relative": "../../../config/dspace.cfg",
    "config_dist_relative": [
        "../../../../config/local.conf",
        "../../../../../config/local.conf",
    ],

    # assetstore structure
    "input_dir_glob": "*/*/*/*",

    "mime_type": {
        "application/zip": "unzip -t %s",
        "application/x-xz": "xz -t %s",
        "application/x-gzip": "gunzip -t %s",
        "application/x-bzip2": "bunzip2 -t %s",
        "application/x-tar": "tar -tvf %s",
        "image/png": "pngcheck %s",

        "text/plain": lambda x: (0, "<plain text>"),
    }

}  # settings

