#!/usr/bin/env python
import sys, os, subprocess,  commands, shutil, string, datetime; 
from optparse import OptionParser

# 88435/dsp01qf85nb30h
DSPACE_METADATA_ELEMENT = "pu.pdf.coverpage"; 
DSPACE_HOME = os.environ.get('DSPACE_HOME') or  "/dspace";
DSPACE_CMD =  "/bin/dspace"
DSPACE_OPTS = " --include object,ITEM.handle,internalId,BUNDLE.name,BUNDLE.id,mimeType," + \
              "ITEM." + DSPACE_METADATA_ELEMENT +  ",name " + \
               "--format TXT "
DSPACE_LIST =      " bulk-list --root ROOT --type BITSTREAM " +  DSPACE_OPTS; 
DSPACE_BITSTREAM = " bulk-bitstream --root ROOT --eperson EPERSON --bitstream COVEREDBITSTREAM " +  DSPACE_OPTS 
DSPACE_METADATA  = " bulk-meta-data --root ITEM --eperson EPERSON --action ADD --meta_data " + DSPACE_METADATA_ELEMENT + "=METADATA"; 

STORE_DIR = "glued"

def doit(): 
    cmd = '';
    options = parseargs(); 
    doLog("", options.log_file); 
    doLog("# Date " +  str(datetime.datetime.now()), options.log_file); 
    doLog("# CWD " +   os.path.realpath(os.curdir), options.log_file); 
    doLog("# LogFile " +   options.log_file.name, options.log_file); 
    doLog("", options.log_file); 

    if (options != None): 
        # iter over relevant bitstreams 
        cmd =  options.dspace_cmd +  DSPACE_LIST.replace("ROOT", options.root);
        output = execCommand(cmd, options.verbose); 
        for line in output.split("\n"): 
            if (line[0] == '#'):
                continue;  # skip comment lines 

            doLog("", options.log_file) 
            doLog("# " + line, options.log_file) 
            bitstream = digestLine(line);  
            if (bitstream == None): 
                doLog("ERROR: Can digest line", options.log_file); 
                continue;

            if ((bitstream['mimeType'] != "application/pdf") or (bitstream['BUNDLE.name'] != "ORIGINAL")):
                   continue;  # skip ; only consider pdf bitstreams in ORIGINAL bundles 

            try: 
               stages = [];
               result = "NOTHING-TO-DO";
               bitstream['fileName'] = getDSpaceFileName(options.assetstore, bitstream['internalId']); 
               bitstream['pdfFileName'] = options.bitstream_covered_dir + "/" + \
                                          "BUNDLE."  + bitstream['BUNDLE.id'] +":" + bitstream['name']
               doLog(bitstream['pdfFileName'] + " checkMetaData " + line, options.log_file) 
               stages.append("checkMetaData");
               hasMetaData = (bitstream['ITEM.' + DSPACE_METADATA_ELEMENT ] != '')
               
               ## check whether  bitstream['pdfFileName'] already exists 
               ## if hasMetData - should see file 
               ## if not hasMetData - there should not be a file 
               ## otherwise - there maybe a problem 
               doLog(bitstream['pdfFileName'] + " checkFileExists ", options.log_file); 
               stages.append("checkFileExists"); 
               fileExists = os.path.isfile(bitstream['pdfFileName']) 
               if (fileExists and not hasMetaData): 
                   raise Exception,  bitstream['pdfFileName'] + " exists but " + DSPACE_METADATA_ELEMENT + " undefined; " + \
                                                                "afraid to double cover"
               if (not fileExists and hasMetaData): 
                   raise Exception, bitstream['pdfFileName'] + " does not exist but " + DSPACE_METADATA_ELEMENT + " is set; " + \
                                                                "may have missed this bitstream "; 
               if (hasMetaData): 
                  continue;
 
               ## add coverpage 
               doLog(bitstream['pdfFileName'] + " addCover " + options.cover , options.log_file); 
               stages.append("addCover"); 
               cmd = options.pdfAddCoverCmd;
               cmd = cmd.replace("ASSETSTOREFILE" ,  bitstream['fileName']); 
               cmd = cmd.replace("BITSTREAM" ,  bitstream['pdfFileName']); 
               execCommand(cmd, options.verbose, options.dryrun); 

               ## import covered bitstream  - dryrun if necessary 
               doLog(bitstream['pdfFileName'] + " importBitstream ", options.log_file); 
               stages.append("importBitstream"); 
               cmd = options.dspace_cmd + " " + DSPACE_BITSTREAM.replace("COVEREDBITSTREAM", bitstream['pdfFileName']); 
               cmd = cmd.replace("ROOT", bitstream['object']); 
               cmd = cmd.replace("EPERSON", options.eperson);
               execCommand(cmd, options.verbose, options.dryrun);  # can't execute with dryrun options - do not have covered bitstream file 

               ## set metadata value 
               doLog(bitstream['pdfFileName'] + " setMetaData " + DSPACE_METADATA_ELEMENT + "=" + options.metavalue, options.log_file); 
               stages.append("setMetaData"); 
               cmd = options.dspace_cmd + " " + DSPACE_METADATA.replace("ITEM", bitstream['ITEM.handle']); 
               cmd = cmd.replace("METADATA", options.metavalue); 
               cmd = cmd.replace("EPERSON", options.eperson);
               if (options.dryrun):
                  cmd = cmd + " --dryrun"; 
               execCommand(cmd, options.verbose)

               result = "SUCCESS"; 
            except Exception, e: 
               result = "ERROR: " + str(e.message); 
            finally: 
               doLog(bitstream['pdfFileName'] + " STAGES=" + string.join(stages, ","), options.log_file);
               doLog(bitstream['pdfFileName'] + " RESULT=" + result, options.log_file);


def digestLine(line): 
    try: 
       object = {};
       # name is last 
       i = line.rfind(" name=")
       for prop in line[:i].split():
           (name,value)= prop.split('=');
           object[name] = value;
       (name,value)= line[i+1:].split('=');
       object[name] = value.replace(" ", "_"); 
       return object;
    except: 
        return None;

def getDSpaceFileName(assetstore, internalId): 
   dr = "/%s/%s/%s/" % (internalId[0:2], internalId[2:4], internalId[4:6])
   return assetstore  + dr + internalId; 
 
def doLog(txt, fle):
    fle.write(txt + "\n"); 
    print txt; 

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
    parser.add_option("-c", "--cover", dest="cover",
                  help="Required: Cover page pdf"); 
    parser.add_option("-d", "--dspace", dest="dhome",
                  default=DSPACE_HOME, 
                  help="DSPACE installation directory, default: " + DSPACE_HOME);
    parser.add_option("-e", "--eperson", dest="eperson",
                  help="Required: DSPACE EPerson to be associated with operations ")
    parser.add_option("-m", "--metavalue", dest="metavalue",
                  help="Metadata Value for " + DSPACE_METADATA_ELEMENT  + ", default derives from --cover options")
    parser.add_option("-r", "--root", dest="root",
                  help="Required: DSpace community, collection, or item"); 
    parser.add_option("-s", "--store", dest="storedir",
                  help="directory containing trace files and generated bitstreams, default: " + STORE_DIR  + "/<ROOT_PARAM>" )
    parser.add_option("-y", "--dryrun", 
                  action="store_true", dest="dryrun", default=False,
                  help="Dryrun only "); 
    parser.add_option("-v", "--verbose",
                  action="store_true", dest="verbose", default=False,
                  help="be verbose")

    (options, args) = parser.parse_args()
    if (not options.root or not options.cover or not options.eperson ): 
        parser.print_help();
        return None;

    try: 
        options.bitstream_covered_dir =  None;
        options.pdfAddCoverCmd = None;

        if (not os.path.isfile(options.cover)): 
            raise Exception, "Can't find cover " + options.cover; 

        options.dspace_cmd = options.dhome + DSPACE_CMD; 
        if (not os.path.isfile(options.dspace_cmd)): 
            raise Exception, "Can't find dspace executable " +  options.dspace_cmd; 

        options.assetstore =  options.dhome + "/assetstore"; 
        if (not os.path.isdir(options.assetstore)): 
            raise Exception, "Can't see assetstore " +  options.assetstore; 

        if (not options.storedir): 
            options.storedir = STORE_DIR; 
        if (not os.path.isdir(options.storedir)): 
            if (options.verbose): 
                print "Creating directory " + options.storedir
            os.makedirs(options.storedir);
        log_file =  options.storedir + "/log-root=" + options.root.replace("/","_") + "-pid=" + str(os.getpid()) + ".log"
        options.log_file =  open(log_file,  "w"); 

        if (not options.metavalue):
            options.metavalue = os.path.splitext(os.path.basename(options.cover))[0];
        if (0 == len(options.metavalue)): 
            raise Exception, "Can't derive metadata value from " + options.cover; 

        options.bitstream_covered_dir =  options.storedir + "/bitstreams"; 
        if (not os.path.isdir(options.bitstream_covered_dir)): 
            if (options.verbose): 
                print "Creating directory for covered bitstreams "  + options.bitstream_covered_dir;
            os.mkdir(options.bitstream_covered_dir)

        options.pdfAddCoverCmd = "pdftk %s ASSETSTOREFILE  output BITSTREAM" % (options.cover) 
        prtOptions(options.log_file, options); 
    except Exception, ex: 
        print ex; 
        parser.print_help(); 
        return None;
    finally: 
        if (options.verbose): 
            prtOptions(sys.stdout, options); 
    return options; 

def prtOptions(dest, options): 
            print >> dest, "# Root:\t" + str(options.root); 
            print >> dest, "# Cover:\t" + str(options.cover); 
            print >> dest, "# CoverMetaDataValue:\t" + str(options.metavalue); 
            print >> dest, "# Dryrun:\t\t" + str(options.dryrun); 
            print >> dest, "# DSPACE:\t" + str(options.dhome); 
            print >> dest, "# DSPACE_cmd:\t" + str(options.dspace_cmd); 
            print >> dest, "# Store Dir:\t" + str(options.storedir); 
            print >> dest, "# Store Dir for Bitstreams:\t" + str(options.bitstream_covered_dir); 
            print >> dest, "# Log File:\t" + str(options.log_file); 
            print >> dest, "# pdfAddCover command template:\t" + str(options.pdfAddCoverCmd); 

if __name__ == "__main__": 
    doit(); 



