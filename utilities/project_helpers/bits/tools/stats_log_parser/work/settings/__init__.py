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
    "name": u"parse dspace logs for incorrect url",

    "apache_dir": (os.path.join( os.path.dirname(__file__),
                                "../../log_apache/access.log"),
                   os.path.join( os.path.dirname(__file__),
                                "../../log_apache/ssl-dspace-access.log"),
    ),

    "dspace_dir": (os.path.join( os.path.dirname(__file__),
                                "../../log_dspace/dspace.log.*"),
    ),

    # logger config - read from _logger
    "logger_config": os.path.join( os.path.dirname(__file__),
                                   "logger.config"),

    "indexer": {
        "autocommit": False,
        "backend_host": "http://localhost:8088/repository/solr/statistics/",
        "mode": "rw",
        "retry_timeout": 1,
    }

}  # settings
