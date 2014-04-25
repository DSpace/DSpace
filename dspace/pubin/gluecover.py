#!/usr/bin/env python
import sys, os, subprocess, shutil; 
from optparse import OptionParser

# 88435/dsp01qf85nb30h

def doit(): 
    options = parseargs(); 
    if (False and options.verbose): 
        print "ARGS=" + str(options); 

    cmdTemplate = "pdftk %s %s/BITSTREAM output %s/BITSTREAM" % (options.cover, options.indir, options.outdir) 

    for filename in os.listdir(options.indir): 
         cmd = cmdTemplate.replace("BITSTREAM", filename); 
         runCmd(cmd, options.verbose); 

def runCmd(cmd, verbose): 
    if (verbose): 
        print cmd;
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

def parseargs(): 
    parser = OptionParser()
    parser.add_option("-i", "--in", dest="indir",
                  help="directory containing pdf files"); 
    parser.add_option("-o", "--out", dest="outdir",
                  help="directory where to place pdf files with cover page");  
    parser.add_option("-c", "--cover", dest="cover",
                  help="Cover page pdf"); 
    parser.add_option("-v", "--verbose",
                  action="store_true", dest="verbose", default=False,
                  help="be verbose")

    (options, args) = parser.parse_args()
    if (not options.indir or not options.outdir or not options.cover ): 
        parser.print_help();

    return options; 

if __name__ == "__main__": 
    doit(); 



