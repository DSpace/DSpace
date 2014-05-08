#!/usr/bin/env python
import sys, os; 

DSPACE_HOME = os.environ.get('DSPACE_HOME') or  "/dspace";
DSPACE_ASSETS = DSPACE_HOME + "/assetstore"; 


if (len(sys.argv) > 1): 
    for s in sys.argv[1:]: 
        dr = "%s/%s/%s" % (s[0:2], s[2:4], s[4:6])
        f = DSPACE_ASSETS + "/" + dr + "/" + s;
        os.system("ls " + f) 

