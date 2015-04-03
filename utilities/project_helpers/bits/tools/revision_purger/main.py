#!/usr/bin/env python
# coding=utf-8
"""
    python 2.7*
        - run from this dir
"""
#
#
import fnmatch
import os
import sys
import re

settings = {
    "dir": os.path.join( "..", "..", "sources" ),
    "lookfor": "$Revision:",
    "infiles": "*.java",
}

if sys.version_info < (2, 7) or sys.version_info >= (3, 0):
    print "Sorry, not supported python version, get yourself python v2.7"
    sys.exit(1)


#
#

if __name__ == '__main__':

    print "Looking in [%s]" % settings["dir"]

    to_process = []
    for root, dirname, filenames in os.walk(settings["dir"]):
        for filename in fnmatch.filter(filenames, settings["infiles"]):
            to_process.append(os.path.join(root, filename))

    print "Processing [%d] files" % len(to_process)

    look_for = settings["lookfor"]
    found = 0
    for filename in to_process:
        contents = open(filename, "rb").read()
        lookfor = settings["lookfor"]
        if lookfor in contents:
            print "Changing [%s]" % filename
            contents = re.sub( u"\$Revision: .*\$", "$Revision$", contents )
            f = open( filename, 'wb' )
            f.write(contents)
            f.flush()
            f.close()
