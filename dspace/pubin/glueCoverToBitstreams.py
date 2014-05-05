#!/usr/bin/env python
import sys, os, subprocess,  commands, shutil; 
from optparse import OptionParser

# 88435/dsp01qf85nb30h
DSPACE_HOME = os.environ.get('DSPACE_HOME') or  "/dspace";
DSPACE_CMD =  "/bin/dspace"
DSPACE_OPTS = " --include object,ITEM.handle,COLLECTION.id,COMMUNITY.id,internalId,BUNDLE.name,mimeType,ITEM.pu.pdf.coverpage " +  \
               "--format TXT "
DSPACE_LIST = " bulk-list --root ROOT --type BITSTREAM " +  DSPACE_OPTS; 
DSPACE_BITSTREAM = " bulk-bitstream --root ROOT --eperson EPERSON --bitstream COVEREDBITSTREAM " +  DSPACE_OPTS 

TMP_DIR = "tmp";

def doit(): 
    cmd = '';
    options = parseargs(); 
    if (options != None): 
        cmd =  options.dspace_cmd +  DSPACE_LIST.replace("ROOT", options.root);
        output = execCommand(cmd, options.verbose); 
        for line in output.split("\n"): 
            if (line[0] == '#'):
                continue;  # skip comment lines 

            bitstream = digestLine(line);  
            if ((bitstream['mimeType'] != "application/pdf") or (bitstream['BUNDLE.name'] != "ORIGINAL")):
                continue;  # skip ; only consider pdf bitstreams in ORIGINAL bundles 

            prtFile(options.bitstream_file, line + "\n", options.verbose); 

            if (bitstream['ITEM.pu.pdf.coverpage'] != ''): 
               continue;   # skip   - already has cover page

            ## add coverpage 
            bitstream['fileName'] = getDSpaceFileName(options.assetstore, bitstream['internalId']); 
            bitstream['pdfFileName'] = options.bitstream_covered_dir + "/" + bitstream['object'] + ".pdf"
            cmd = options.pdfAddCoverCmd;
            cmd = cmd.replace("ASSETSTOREFILE" ,  bitstream['fileName']); 
            cmd = cmd.replace("BITSTREAM" ,  bitstream['pdfFileName']); 
            try: 
               execCommand(cmd, options.verbose); 
               line = line + " pdf.COMMAND=SUCCESS" + " pdf.BITSTREAM=" + bitstream['pdfFileName']; 
            except Exception, e: 
               prtFile(options.bitstream_covered_file, line +  " pdf.COMMAND=ERROR\n", options.verbose); 
               continue; 

            ## import covered bitstream 
            cmd = options.dspace_cmd + " " + DSPACE_BITSTREAM.replace("COVEREDBITSTREAM", bitstream['pdfFileName']); 
            cmd = cmd.replace("ROOT", bitstream['object']); 
            cmd = cmd.replace("EPERSON", options.eperson);
            try: 
               execCommand(cmd, options.verbose); 
               prtFile(options.bitstream_covered_file, line +  " import.BITSTREAM=SUCCESS" + "\n", options.verbose); 
            except: 
               prtFile(options.bitstream_covered_file, line +  " import.BITSTREAM=ERROR" + "\n", options.verbose); 
               continue; 


               

def digestLine(line): 
    object = {};
    for prop in line.split():
        (name,value)= prop.split('=');
        object[name] = value;
    return object;

def getDSpaceFileName(assetstore, internalId): 
   dr = "/%s/%s/%s/" % (internalId[0:2], internalId[2:4], internalId[4:6])
   return assetstore  + dr + internalId; 
 
def prtFile(fle, txt, verbose):
    fle.write(txt); 
    if (False and verbose): 
        print txt;
    
def execCommand(cmd, verbose):    
        if (verbose): 
            print "# " + cmd; 
        (status, output) = commands.getstatusoutput( cmd ); 
        if (status != 0): 
            print >> sys.stderr, "Could not execute " + cmd; 
            print >> sys.stderr, output; 
            raise Exception(cmd + " failed with exit status " + str(status)) 
        return  output; 

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
    parser.add_option("-t", "--tmp", dest="tmpdir",
                  default=TMP_DIR,
                  help="directory containing tempoarry files, default: " + TMP_DIR); 
    parser.add_option("-r", "--root", dest="root",
                  help="DSpace community, collection, or itemf"); 
    parser.add_option("-c", "--cover", dest="cover",
                  help="Cover page pdf"); 
    parser.add_option("-d", "--dspace", dest="dhome",
                  default=DSPACE_HOME, 
                  help="DSPACE installation directory, default: " + DSPACE_HOME);
    parser.add_option("-e", "--eperson", dest="eperson",
                  help="DSPACE EPerson to be associated with operations ")
    parser.add_option("-v", "--verbose",
                  action="store_true", dest="verbose", default=False,
                  help="be verbose")

    (options, args) = parser.parse_args()
    if (not options.root or not options.cover or not options.eperson ): 
        parser.print_help();
        return None;

    try: 
        if (not os.path.isfile(options.cover)): 
            raise Exception, "Can't find cover " + options.cover; 

        options.dspace_cmd = options.dhome + DSPACE_CMD; 
        if (not os.path.isfile(options.dspace_cmd)): 
            raise Exception, "Can't find dspace executable " +  options.dspace_cmd; 

        options.assetstore =  options.dhome + "/assetstore"; 
        if (not os.path.isdir(options.assetstore)): 
            raise Exception, "Can't see assetstore " +  options.assetstore; 

        if (not os.path.isdir(options.tmpdir)): 
            if (options.verbose): 
                print "Creating temp directory " + options.tmpdir
            os.mkdir(options.tmpdir)
        options.bitstream_file =  open(options.tmpdir + "/bitstreams-original-pdf.txt", "w"); 
        options.bitstream_covered_file =  open(options.tmpdir + "/bitstreams-original-pdf_covered.txt", "w"); 

        options.bitstream_covered_dir =  options.tmpdir + "/bitstreams"; 
        if (not os.path.isdir(options.bitstream_covered_dir)): 
            if (options.verbose): 
                print "Creating directory tmp directory for covered bitstreams "  + options.bitstream_covered_dir;
            os.mkdir(options.bitstream_covered_dir)

        options.pdfAddCoverCmd = "pdftk %s ASSETSTOREFILE  output BITSTREAM" % (options.cover) 
    except Exception, ex: 
        print ex; 
        parser.print_help(); 
        return None;
    finally: 
        if (options.verbose): 
            print "# Root:\t" + options.root; 
            print "# Cover:\t" + options.cover; 
            print "# DSPACE:\t" + options.dhome; 
            print "# DSPACE_cmd:\t" + options.dspace_cmd; 
            print "# Temp Dir:\t" + options.tmpdir; 
            print "# Temp Dir for Bitstreams:\t" + options.bitstream_covered_dir; 
            print "# pdfAddCover command template:\t" + options.pdfAddCoverCmd; 

    return options; 

if __name__ == "__main__": 
    doit(); 



