#!/usr/bin/env python
import sys, os, subprocess, shutil; 
from optparse import OptionParser

# 88435/dsp01qf85nb30h

def doit(): 
    options = parseargs(); 
    DSPACE = dspaceSettings(); 
    if (False and options.verbose): 
        print "DSPACE=" + str(DSPACE); 
        print "ARGS=" + str(options); 

    cmd = DSPACE['lister'].replace('ROOT', options.root) + " --type BITSTREAM --include object,mimeType,internalId"; 
    results = runCmd(cmd); 

    for line in results[0].split("\n"): 
       if (line.startswith("BITSTREAM")): 
            (bit, mime, internal) = line.split("\t"); 
            dontInclude = (options.mime != None)  and (options.mime != mime); 
            if (dontInclude): 
                if (options.verbose): 
                    print "#SKIP " + line; 
            else:  
                bitpath = topath(DSPACE, internal); 
                tofile = options.dest + "/" + bit;
                print  "#COPY " + line + " " + tofile 
                shutil.copy(bitpath, tofile); 
       elif (options.verbose): 
            print >> sys.stderr,  "#" + line; 
       

def runCmd(cmd): 
    cmdarray = cmd.split(); 
    process = subprocess.Popen(cmd.split(), shell=False, stdout=subprocess.PIPE, stderr=subprocess.PIPE); 
    results =  process.communicate(); 
    if (process.returncode != 0): 
        sys.stderr.write("EXEC ERROR: " + cmd + "\n"); 
        sys.stderr.write(results[1]); 
        if (options.verbose): 
            sys.stderr.write(results[0])
        sys.exit(process.returncode); 
    return results; 

def topath(DSPACE, internalId): 
    internalId = internalId.rstrip(); 
    dr = "%s/%s/%s/%s/%s" % (DSPACE['HOME'],internalId[0:2], internalId[2:4], internalId[4:6], internalId)
    return  dr; 

def dspaceSettings():  
    dspace = { 'HOME' : os.environ['DSPACE_HOME'] } ; 
    dspace['assetstore'] = dspace['HOME'] + "/assetstore"; 
    dspace['lister'] = dspace['HOME'] + "/bin/dspace bulk-list -r ROOT  --format tsv"; 
    return dspace; 
     
def parseargs(): 
    parser = OptionParser()
    parser.add_option("-r", "--root", dest="root",
                  help="DSpaceObject given as <TYPE>.<ID> or handfle, REQUIRED"); 
    parser.add_option("-m", "--mime", dest="mime",
                  help="copy only Bitstream with given mimeType"); 
    parser.add_option("-v", "--verbose",
                  action="store_true", dest="verbose", default=False,
                  help="be verbose")
    parser.add_option("-d", "--dir", dest="dest",
                  help="Directory where to store bitstream files (default '.'"); 

    (options, args) = parser.parse_args()
    if (not options.root ): 
        parser.print_help();

    if (not options.dest ): 
        options.dest = "."; 


    return options; 

if __name__ == "__main__": 
    doit(); 



