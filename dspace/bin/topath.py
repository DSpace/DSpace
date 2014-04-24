#!/usr/bin/env python
import sys, os, shutil

root = os.environ['DSPACE_HOME']; 
root = root + '/assetstore';

if len(sys.argv) <= 1: 
    for s in sys.stdin:
        s = s.rstrip(); 
        dr = "%s/%s/%s/%s/%s" % (root,s[0:2], s[2:4], s[4:6], s)
        print dr; 
else: 
    for s in sys.argv[1:]:
        print s; 
        dr = "%s/%s/%s/%s/%s" % (root,s[0:2], s[2:4], s[4:6], s)
        print dr; 

