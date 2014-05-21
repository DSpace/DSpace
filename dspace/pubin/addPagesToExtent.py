#!/usr/bin/env python
import sys, os, subprocess,  commands, shutil
import re, string, datetime; 
from optparse import OptionParser

# 88435/dsp01qf85nb30h
DSPACE_HOME = os.environ.get('DSPACE_HOME') or  "/dspace";
DSPACE_CMD =  "/bin/dspace"
DSPACE_LIST = " bulk-list --root ROOT --type ITEM " +  \
              " --include object,DSPACE_METADATA_ELEM" + \
              " --format TSV "
DSPACE_METADATA = " bulk-meta-data --root ROOT " + \
              " --eperson EPERSON " + \
              " --format TSV "
            
DSPACE_METADATA_ELEM = "dc.format.extent"; 
DSPACE_METADATA_PATTERN = '.*pages$'; 
DSPACE_APPEND_VALUE = " pages"; 
STORE_DIR = "metaData"


def doit(): 
    cmd = '';
    options = parseargs(); 

    if (options != None): 
        doLog("", options.log_file); 
        doLog(" Date " +  str(datetime.datetime.now()), options.log_file); 
        doLog(" CWD " +   os.path.realpath(os.curdir), options.log_file); 
        doLog(" LogFile " +   options.log_file.name, options.log_file); 
        doLog("", options.log_file); 

        # list relevant ITEMS 
        cmd =  options.dspace_cmd +  DSPACE_LIST.replace("ROOT", options.root); 
        cmd = cmd.replace("DSPACE_METADATA_ELEM", options.metaElem); 
        if (options.workFlowItemsOnly): 
            cmd = cmd + " -W"; 
        output = execCommand(cmd, options.verbose); 
        for line in output.split("\n"): 
            if (line[0] == '#'):
                continue;  # skip comment lines 
            if (not line.startswith('ITEM')):  
                continue; # ignore anythig that is not related to items 

            if (options.verbose): 
                doLog(" " + line, options.log_file) 

            item = digestLine(line, 'object', options.metaElem); 
            if (item == None): 
                doLog("ERROR: Can digest line", options.log_file); 
                continue;

            result = "NOTHING-TO-DO";
            try: 

               if (not item[options.metaElem]):  
                    doFinalLog("MISSING-VALUE", item, options); 
                    continue;
               if (type(item[options.metaElem]) !=  str):  
                    doFinalLog("NON-STRING-VALUE", item, options); 
                    continue; 

               itMatches =  (re.match(options.valuePattern, item[options.metaElem])) 
               if (itMatches): 
                    result = "MATCH" 
               else: 
                    result = "NO-MATCH," + setField(item, options) 
               doFinalLog(result + ",SUCCESS", item, options); 
            except Exception, e: 
               doFinalLog(result + ",ERROR: " + str(e), item, options); 


def setField(item, options): 
    try: 
        result = "setField:"; 
        cmd = options.dspace_cmd +  DSPACE_METADATA.replace("ROOT", item['object'])
        cmd = cmd.replace("EPERSON", options.eperson); 
        # ADD  new value 
        addcmd = cmd + " -a ADD -m '%s=%s'" % (options.metaElem, item[options.metaElem] +  DSPACE_APPEND_VALUE ); 
        out = execCommand(addcmd, options.verbose, options.dryrun); 
        if (options.verbose): 
            doLogOut(out, options.log_file);
        result = result +  ",ADD-new";   

        # DEL  new value 
        delcmd = cmd + " -a DEL -m '%s=%s'" % (options.metaElem, item[options.metaElem]); 
        out = execCommand(delcmd, options.verbose, options.dryrun); 
        if (options.verbose): 
            doLogOut(out, options.log_file);
        result = result + ",DEL-old";   
    except Exception, e: 
        result = result + ",ERROR:" + str(e); 
    return result; 

def digestLine(line, key1, key2): 
    try: 
       object = {};
       prop = line.split('\t')
       object[key1] = prop[0];
       val = prop[1].strip()
       if val.startswith('"') and val.endswith('"'):
           val = val[1:-1]
       if (val.startswith("[")): 
            val = val[1:-1]; 
            object[key2]  = val.split(','); 
       else: 
           object[key2] = val;
       return object;
    except: 
       return None;

def doFinalLog(result, item, options): 
    line = item['object'] + " " + \
            " RESULT=" + result + "\t" +  \
            options.metaElem + "=" + str(item[options.metaElem]);
    doLog(line, options.log_file); 

def doLog(string, fle):
    fle.write(string + "\n"); 
    print "# " + string; 

def doLogOut(out, fle):
    if (len(out) > 0):
       out = out.replace("\n", "\n# ");
       fle.write(out + "\n");
       print out;

def execCommand(cmd, verbose, dryrun=False):    
        if (dryrun): 
            pre = "# dryrun: "; 
        else :
            pre = "# "; 
        
        if (verbose):
            print pre  + cmd; 
        if (not dryrun): 
            (status, output) = commands.getstatusoutput( cmd ); 
            if (status != 0): 
                print >> sys.stderr, "Could not execute " + cmd; 
                print >> sys.stderr, output; 
                raise Exception(cmd + " failed with exit status " + str(status)) 
            return  output; 
        return "";  # empty ouput 

def runProcess(exe, verbose):    
    if (verbose): 
        print "# " + exe; 
    p = subprocess.Popen(exe.split(), stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    while (True):
      retcode = p.poll() #returns None while subprocess is running
      line = p.stdout.readline()
      yield line
      if (retcode is not None):
        break


def runCmd(cmd, outfile,  verbose): 
    if (verbose): 
        print '# ' + cmd;
    cmdarray = cmd.split(); 
    process = subprocess.Popen(cmdarray, shell=False, stdout=outfile, stderr=subprocess.PIPE); 
    results =  process.communicate(); 
    if (process.returncode != 0): 
        sys.stderr.write("EXEC ERROR: " + cmd + "\n"); 
        sys.stderr.write(results[1]); 
        sys.exit(process.returncode); 
    return results; 

def parseargs(): 
    parser = OptionParser()
    parser.add_option("-d", "--dspace", dest="dhome",
                  default=DSPACE_HOME, 
                  help="DSPACE installation directory, default: " + DSPACE_HOME);
    parser.add_option("-e", "--eperson", dest="eperson",
                  help="Required: DSPACE EPerson to be associated with operations ")
    parser.add_option("-m", "--metaElem", dest="metaElem",
                  help="Metadata Elem to work with, default " + DSPACE_METADATA_ELEM );
    parser.add_option("-p", "--pattern", dest="valuePattern",
                  help="Pattern to match against metadata value, default " + DSPACE_METADATA_PATTERN);
    parser.add_option("-r", "--root", dest="root",
                  help="Required: DSpace community, collection, or item"); 
    parser.add_option("-s", "--store", dest="storedir",
                  help="directory containing trace files and generated bitstreams, default: " + STORE_DIR )
    parser.add_option( "-W", "--doWorkFlowItems", 
                  action="store_true", dest="workFlowItemsOnly", default=False,
                  help="Restrict to working on items in workflow "); 
    parser.add_option("-y", "--dryrun", 
                  action="store_true", dest="dryrun", default=False,
                  help="Dryrun only "); 
    parser.add_option("-v", "--verbose",
                  action="store_true", dest="verbose", default=False,
                  help="be verbose")

    (options, args) = parser.parse_args()
    if (not options.root or not options.eperson ): 
        parser.print_help();
        return None;

    try: 
        options.dspace_cmd = options.dhome + DSPACE_CMD; 
        if (not os.path.isfile(options.dspace_cmd)): 
            raise Exception, "Can't find dspace executable " +  options.dspace_cmd; 

        if (not options.storedir):
            options.storedir = STORE_DIR;
        if (not os.path.isdir(options.storedir)):
            if (options.verbose):
                print "Creating directory " + options.storedir
            os.makedirs(options.storedir);
        log_file =  options.storedir + "/log-root=" + options.root.replace("/","_") + "-pid=" + str(os.getpid()) + ".log"
        options.log_file =  open(log_file,  "w"); 

        if (not options.metaElem): 
            options.metaElem = DSPACE_METADATA_ELEM; 
        if (0 == len(options.metaElem)):  
            raise Exception, "Bad metaDataElem: " + options.metaElem; 

        if (not options.valuePattern): 
            options.valuePattern = DSPACE_METADATA_PATTERN
        if (0 == len(options.valuePattern)):  
            raise Exception, "Bad valuePattern: " + options.valuePattern; 
        prtOptions(options.log_file, options); 
        if (options.verbose): 
            prtOptions(sys.stdout, options); 
    except Exception, ex: 
        print ex; 
        parser.print_help(); 
        return None;
    return options; 

def prtOptions(dest, options): 
            print >> dest, "# Root:\t" + str(options.root); 
            print >> dest, "# DoWorkFlowItems:\t" + str(options.workFlowItemsOnly); 
            print >> dest, "# MetaDataElem:\t" + str(options.metaElem); 
            print >> dest, "# MetaDataValuePattern:\t" + str(options.valuePattern); 
            print >> dest, "# Dryrun:\t\t" + str(options.dryrun); 
            print >> dest, "# DSPACE:\t" + str(options.dhome); 
            print >> dest, "# DSPACE_cmd:\t" + str(options.dspace_cmd); 
            print >> dest, "# Store Dir:\t" + str(options.storedir); 
            print >> dest, "# Log File:\t" + str(options.log_file); 

if __name__ == "__main__": 
    doit(); 



