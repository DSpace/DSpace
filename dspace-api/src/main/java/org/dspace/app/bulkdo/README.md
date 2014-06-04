# org.dspace.app.bulkdo package

The org.dspace.app.bulkdo package contains four dspace main commands:
* bulk-list: list dspace objects contained in a the 'tree' rooted at a community, collection, item, bundle, or bitstream
* bulk-pol: add a policy described by a dspace action and an Eperson or Group to all dspace objects contained in a
 'tree' at given by dspace object; likewise delete a given policy
* bulk-meta-data: add a metadata value setting to  or delete the setting from all dspace objects contained in a the
'tree' at given by dspace object;
* bulk-bitstream: replace the bitstream file in a given BITSTREAM  dspace object

bulk-list is a powerful list command. Based on its root  parameter, a dspace object, it lists all contained dspace objects
of a given type including properties selected by command line parameters. The root object can be designated by its
handle or by its type and id, eg BITSTREAM.1239, ITEM.54, 95678/dspiu738.  Properties may include database ids,
handle, name, metaData values, policy settings of the listed object itself or of dspace objects that are enclosing
the listed object. A list of bitstreams can be configured to include the bitstreams id, mimeType, the name of the enclosing bundle,
as well as the handles of the enclosing item and collection.

bulk-pol may be used to ADD or DEL authorization policies. bulk-meta-data modifies individual metadata value settings.
Both commands use the same logic of selecting which dspace objects to operate on as the bulk-list command.
Both commands list the results of their actions. A verbose option gives more details. It is also easy to use bulk-list
to view the status after running either of the two commands.

bulk-bitstream  can replace the file in a single bitstream; I kept the prefix bulk- to indicate that the command
 is related to the other bulk-\* commands. In itself it is not very useful to be able to replace single bitstream
 from the command line. But together with the other bulk-\* commands, especially the lister it becomes possible
 to write scripts that manipulate bitstreams in bulk.

For example using bulk-list, a python script that interprets its output, and bulk-bitstream, I build a utility that grabs all
pdf bitstreams from a community, prepends a cover pages to each of them, and replaces the original bitstream with the one
that includes the cover page.

# Examples

## bulk-list; listing dspace objects and their properties

** list all items  under collection given by its handle **

~~~~
> dspace bulk-list -r 88435/1100 -t ITEM
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 15 items
# org.dspace.app.bulkdo.Lister: # 15 type=2
 object=ITEM.866 parent=COLLECTION.296
 object=ITEM.867 parent=COLLECTION.296
 object=ITEM.868 parent=COLLECTION.296
 object=ITEM.869 parent=COLLECTION.296
 object=ITEM.870 parent=COLLECTION.296
 object=ITEM.871 parent=COLLECTION.296
 object=ITEM.872 parent=COLLECTION.296
 object=ITEM.874 parent=COLLECTION.296
 object=ITEM.875 parent=COLLECTION.296
 object=ITEM.876 parent=COLLECTION.296
 object=ITEM.877 parent=COLLECTION.296
 object=ITEM.878 parent=COLLECTION.296
 object=ITEM.879 parent=COLLECTION.296
 object=ITEM.880 parent=COLLECTION.296
 object=ITEM.881 parent=COLLECTION.296
~~~~

you get the same result for
~~~~
> dspace bulk-list -r COLLECTION.296 -t ITEM
~~~~

** again list all items  under 88435/1100 and  tweak list with the properties to print **

~~~~
> dspace bulk-list -r 88435/1100 -t ITEM --include 'handle,COLLECTION.name,dc.contributor.author'
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 15 items
# org.dspace.app.bulkdo.Lister: # 15 type=2
handle=88435/dsp014t64gn29q COLLECTION.name=History dc.contributor.author=Flower, Isabel
handle=88435/dsp011544bp17f COLLECTION.name=History dc.contributor.author=Magagna, Sarah
handle=88435/dsp01wd375w370 COLLECTION.name=History dc.contributor.author=Metts, Elizabeth
handle=88435/dsp01rn3011459 COLLECTION.name=History dc.contributor.author=Piacente, Nicholas
handle=88435/dsp01sn009x84r COLLECTION.name=History dc.contributor.author=Beimfohr, Margaret
handle=88435/dsp01nv9352924 COLLECTION.name=History dc.contributor.author=Garfing, Hana
handle=88435/dsp013484zg99f COLLECTION.name=History dc.contributor.author=Cheezem, Tiffany
handle=88435/dsp0112579s387 COLLECTION.name=History dc.contributor.author=Preston, Laura
handle=88435/dsp01w9505058n COLLECTION.name=History dc.contributor.author=Udogwu, Ugo
handle=88435/dsp012514nk573 COLLECTION.name=History dc.contributor.author=Dammers, Kathryn
handle=88435/dsp01xd07gs760 COLLECTION.name=History dc.contributor.author=Manohar, Mohit
handle=88435/dsp01j3860700c COLLECTION.name=History dc.contributor.author=Spinazzi, Micol
handle=88435/dsp01db78tc09w COLLECTION.name=History dc.contributor.author=Gregory, Katherine
handle=88435/dsp018p58pd01w COLLECTION.name=History dc.contributor.author=Holubar, Grayden
handle=88435/dsp014x51hj109 COLLECTION.name=History dc.contributor.author=Li, Maximilian
~~~~

** list items in the workflow of collection **

~~~
> dspace bulk-list --root 88435/1100 -t ITEM  --doWorkFlowItems
                   --include 'object,handle,COLLECTION.name,dc.contributor.*,dc.title' --format tsv
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 1 items
# org.dspace.app.bulkdo.Lister: # 1 type=2
object	handle	COLLECTION.name	dc.contributor.*	dc.title
ITEM.1970		"Art and Archaeology"	"[Finwinkel, Wayne, Georgia, O'Water]"	"Garish Gardens"
~~~

As you can see, this item does not have a handle yet, since it still sits in the workflow.
Note that you may use the \* notation for metadata value selection in the --include parameter.


**list all bitstreams in a given collection, show related bundle names and mimeTypes**
~~~~
> dspace bulk-list -r $RROOT -t BITSTREAM --include 'object,ITEM.object,BUNDLE.name,mimeType' --format TSV
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 2 items
# org.dspace.app.bulkdo.Lister: Found 4 bundles
# org.dspace.app.bulkdo.Lister: Found 4 bitstreams
# org.dspace.app.bulkdo.Lister: # 4 type=0
object	ITEM.object	BUNDLE.name	mimeType
BITSTREAM.2393	ITEM.1968	ORIGINAL	application/pdf
BITSTREAM.2281	ITEM.1968	LICENSE	"text/plain; charset=utf-8"
BITSTREAM.2338	ITEM.1967	ORIGINAL	application/pdf
BITSTREAM.2278	ITEM.1967	LICENSE	"text/plain; charset=utf-8"
~~~~

** include  dc.language.iso metadata value of enclosing ITEM and
print  in TSV format **
~~~~
> dspace bulk-list -r handle/1398695916393 -t BITSTREAM --include 'object,ITEM.object,BUNDLE.name,ITEM.dc.language.iso' --format TSV
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 2 items
# org.dspace.app.bulkdo.Lister: Found 4 bundles
# org.dspace.app.bulkdo.Lister: Found 4 bitstreams
# org.dspace.app.bulkdo.Lister: # 4 type=0
object	ITEM.object	BUNDLE.name	ITEM.dc.language.iso
BITSTREAM.2393	ITEM.1968	ORIGINAL	en_US
BITSTREAM.2281	ITEM.1968	LICENSE	en_US
BITSTREAM.2338	ITEM.1967	ORIGINAL	en_US
BITSTREAM.2278	ITEM.1967	LICENSE	en_US
~~~~

** include policy information for READ, WRITE, ADD, REMOVE actions **
~~~~
> dspace bulk-list -r handle/1398695916393 -t BITSTREAM --include 'object,ITEM.object,POLICY.READ,POLICY.WRITE,POLICY.ADD,POLICY.REMOVE' --format TSV
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 2 items
# org.dspace.app.bulkdo.Lister: Found 4 bundles
# org.dspace.app.bulkdo.Lister: Found 4 bitstreams
# org.dspace.app.bulkdo.Lister: # 4 type=0
object	ITEM.object	POLICY.READ	POLICY.WRITE	POLICY.ADD	POLICY.REMOVE
BITSTREAM.2393	ITEM.1968	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[]	[POLICY(GROUP.Anonymous)]
BITSTREAM.2281	ITEM.1968	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[]	[POLICY(GROUP.Anonymous)]
BITSTREAM.2338	ITEM.1967	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[]	[]
BITSTREAM.2278	ITEM.1967	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[]	[]
~~~~


## bulk-pols: change authorization policies

** delete the Anonymous group's read authorization  **
~~~~
> dspace bulk-pols -r $RROOT -t BITSTREAM --format TSV -a DEL -w GROUP.Anonymous -d REMOVE
# D policy.REMOVE for 4 DSPaceObjects
object	parent	POLICY.REMOVE
BITSTREAM.2393	ITEM.1968	[]
BITSTREAM.2281	ITEM.1968	[]
BITSTREAM.2338	ITEM.1967	[]
BITSTREAM.2278	ITEM.1967	[]
~~~~

** the list command shows that the policies were removed **
~~~~
> dspace bulk-list -r handle/1398695916393 -t BITSTREAM
                 --include 'object,ITEM.object,POLICY.READ,POLICY.WRITE,POLICY.ADD,POLICY.REMOVE'
                 --format TSV
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 2 items
# org.dspace.app.bulkdo.Lister: Found 4 bundles
# org.dspace.app.bulkdo.Lister: Found 4 bitstreams
# org.dspace.app.bulkdo.Lister: # 4 type=0
object  ITEM.object POLICY.READ POLICY.WRITE    POLICY.ADD  POLICY.REMOVE
BITSTREAM.2393  ITEM.1968   [POLICY(GROUP.Anonymous)]   [POLICY(EPERSON.monikam)]   []  []
BITSTREAM.2281  ITEM.1968   [POLICY(GROUP.Anonymous)]   [POLICY(EPERSON.monikam)]   []  []
BITSTREAM.2338  ITEM.1967   [POLICY(GROUP.Anonymous)]   [POLICY(EPERSON.monikam)]   []  []
BITSTREAM.2278  ITEM.1967   [POLICY(GROUP.Anonymous)]   [POLICY(EPERSON.monikam)]   []  []
~~~~


** to give WRITE, ADD and REMOVE authorization to the EPerson monikam, for  all BITSTREAM under handle/1398695916393 run the following commands **
~~~~
> dspace bulk-pols -r handle/1398695916393 -t BITSTREAM
                 -a ADD -d WRITE -w EPERSON.monikam --format TSV
# A policy.WRITE for 4 DSPaceObjects
object	parent	POLICY.WRITE
BITSTREAM.2393	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2281	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2338	ITEM.1967	[POLICY(EPERSON.monikam)]
BITSTREAM.2278	ITEM.1967	[POLICY(EPERSON.monikam)]
~~~~
~~~~
dspace bulk-pols -r handle/1398695916393 -t BITSTREAM
                 -a ADD -d ADD  -w EPERSON.monikam --format TSV
# A policy.ADD for 4 DSPaceObjects
object	parent	POLICY.ADD
BITSTREAM.2393	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2281	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2338	ITEM.1967	[POLICY(EPERSON.monikam)]
BITSTREAM.2278	ITEM.1967	[POLICY(EPERSON.monikam)]
~~~~
~~~~
dspace bulk-pols -r handle/1398695916393 -t BITSTREAM
                 -a ADD -d REMOVE  -w EPERSON.monikam --format TSV
# A policy.REMOVE for 4 DSPaceObjects
object	parent	POLICY.REMOVE
BITSTREAM.2393	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2281	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2338	ITEM.1967	[POLICY(EPERSON.monikam)]
BITSTREAM.2278	ITEM.1967	[POLICY(EPERSON.monikam)]
~~~~


** repeating the list command shows the new settings **
~~~~
> dspace bulk-list -r handle/1398695916393 -t BITSTREAM
                   --include 'object,ITEM.object,POLICY.READ,POLICY.WRITE,POLICY.ADD,POLICY.REMOVE'
                   --format TSV
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 2 items
# org.dspace.app.bulkdo.Lister: Found 4 bundles
# org.dspace.app.bulkdo.Lister: Found 4 bitstreams
# org.dspace.app.bulkdo.Lister: # 4 type=0
object	ITEM.object	POLICY.READ	POLICY.WRITE	POLICY.ADD	POLICY.REMOVE
BITSTREAM.2393	ITEM.1968	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]
BITSTREAM.2281	ITEM.1968	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]
BITSTREAM.2338	ITEM.1967	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]
BITSTREAM.2278	ITEM.1967	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]
~~~~


## bulk-meta-data: working with metadata

To make sure make that the  pu.department field  on all items in a collection reads 'Art and Archaeology'
list first the setting first, delete the unwanted values and insert the correct setting.

**list pu.department metadata values
**
~~~~
> dspace bulk-list -r $RROOT --type ITEM --include 'object,parent,pu.department'
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 15 items
# org.dspace.app.bulkdo.Lister: # 15 type=2
 object=ITEM.867 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.869 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.870 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.871 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.872 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.874 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.875 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.876 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.877 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.878 parent=COLLECTION.296 pu.department=Art and Archaeology
 object=ITEM.879 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.880 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.881 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.868 parent=COLLECTION.296 pu.department=Art and Archaeology
 object=ITEM.866 parent=COLLECTION.296 pu.department=Arts and Archaeologie
~~~~

**delete pu.department=Arts and Archaeologie   settings
**
~~~~
> dspace bulk-meta-data -r $RROOT --type ITEM --eperson monikam -a DEL -m 'pu.department=Arts and Archaeologie'
# org.dspace.app.bulkdo.MetaData: pu.department=Arts and Archaeologie > COLLECTION.296
# org.dspace.app.bulkdo.MetaData: apply: DEL dryRun=false
 object=ITEM.867 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.869 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.870 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.871 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.872 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.874 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.875 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.876 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.877 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.878 parent=COLLECTION.296 pu.department=Art and Archaeology changed=false
 object=ITEM.879 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.880 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.881 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.868 parent=COLLECTION.296 pu.department=Art and Archaeology changed=false
 object=ITEM.866 parent=COLLECTION.296 pu.department= changed=true
~~~~

**add  pu.department=Art and Archaeology where not yet set
**
~~~~
> dspace bulk-meta-data -r $RROOT --type ITEM --eperson monikam -a ADD  -m 'pu.department=Art and Archaeology'
# org.dspace.app.bulkdo.MetaData: pu.department=Art and Archaeology > COLLECTION.296
# org.dspace.app.bulkdo.MetaData: apply: ADD dryRun=false
 object=ITEM.869 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.867 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.870 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.871 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.872 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.874 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.875 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.876 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.877 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.879 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.878 parent=COLLECTION.296 pu.department=Art and Archaeology changed=false
 object=ITEM.880 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.881 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.866 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.868 parent=COLLECTION.296 pu.department=Art and Archaeology changed=false
~~~~

## bulk-bitstream: replacing a bitstream

replace the bitstream in an item's original BUNDLE
list and note the BITSTREAM that needs to be replaced

** list bitstreams in a given item   **
~~~~
> dspace bulk-list -r 88435/dsp01s4655g69q -t BITSTREAM --include 'object,BUNDLE.name,name'
# org.dspace.app.bulkdo.Lister: Found 0 collections
# org.dspace.app.bulkdo.Lister: Found 1 items
# org.dspace.app.bulkdo.Lister: Found 1 bundles
# org.dspace.app.bulkdo.Lister: Found 1 bitstreams
# org.dspace.app.bulkdo.Lister: # 1 type=0
 object=BITSTREAM.1133 BUNDLE.name=ORIGINAL name=minkin_daniel.pdf
~~~~

** replace with bitstream with a new version **

~~~~
> dspace bulk-bitstream -r BITSTREAM.1133 -t BITSTREAM -b minkin_daniel_fixed.pdf -e monikam
# org.dspace.app.bulkdo.Bitstreams: minkin_daniel_fixed.pdf(application/pdf) --> BITSTREAM.1133
 object=BITSTREAM.1133 parent= BUNDLE.name= replace=minkin_daniel_fixed.pdf replace.mimeType=application/pdf success= SUCCESS bundles=[BUNDLE.1111]
~~~~
# Examples

## bulk-list; listing dspace objects and their properties

** list all items  under handle collection **

~~~~
> dspace bulk-list -r 88435/1100 -t ITEM
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 15 items
# org.dspace.app.bulkdo.Lister: # 15 type=2
 object=ITEM.866 parent=COLLECTION.296
 object=ITEM.867 parent=COLLECTION.296
 object=ITEM.868 parent=COLLECTION.296
 object=ITEM.869 parent=COLLECTION.296
 object=ITEM.870 parent=COLLECTION.296
 object=ITEM.871 parent=COLLECTION.296
 object=ITEM.872 parent=COLLECTION.296
 object=ITEM.874 parent=COLLECTION.296
 object=ITEM.875 parent=COLLECTION.296
 object=ITEM.876 parent=COLLECTION.296
 object=ITEM.877 parent=COLLECTION.296
 object=ITEM.878 parent=COLLECTION.296
 object=ITEM.879 parent=COLLECTION.296
 object=ITEM.880 parent=COLLECTION.296
 object=ITEM.881 parent=COLLECTION.296
~~~~

you get the same result for
~~~~
> dspace bulk-list -r COLLECTION.296 -t ITEM
~~~~

** again list all items  under 88435/1100 and  tweak list with the properties to print **

~~~~
> dspace bulk-list -r 88435/1100 -t ITEM --include 'handle,COLLECTION.name,dc.contributor.author'
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 15 items
# org.dspace.app.bulkdo.Lister: # 15 type=2
handle=88435/dsp014t64gn29q COLLECTION.name=History dc.contributor.author=Flower, Isabel
handle=88435/dsp011544bp17f COLLECTION.name=History dc.contributor.author=Magagna, Sarah
handle=88435/dsp01wd375w370 COLLECTION.name=History dc.contributor.author=Metts, Elizabeth
handle=88435/dsp01rn3011459 COLLECTION.name=History dc.contributor.author=Piacente, Nicholas
handle=88435/dsp01sn009x84r COLLECTION.name=History dc.contributor.author=Beimfohr, Margaret
handle=88435/dsp01nv9352924 COLLECTION.name=History dc.contributor.author=Garfing, Hana
handle=88435/dsp013484zg99f COLLECTION.name=History dc.contributor.author=Cheezem, Tiffany
handle=88435/dsp0112579s387 COLLECTION.name=History dc.contributor.author=Preston, Laura
handle=88435/dsp01w9505058n COLLECTION.name=History dc.contributor.author=Udogwu, Ugo
handle=88435/dsp012514nk573 COLLECTION.name=History dc.contributor.author=Dammers, Kathryn
handle=88435/dsp01xd07gs760 COLLECTION.name=History dc.contributor.author=Manohar, Mohit
handle=88435/dsp01j3860700c COLLECTION.name=History dc.contributor.author=Spinazzi, Micol
handle=88435/dsp01db78tc09w COLLECTION.name=History dc.contributor.author=Gregory, Katherine
handle=88435/dsp018p58pd01w COLLECTION.name=History dc.contributor.author=Holubar, Grayden
handle=88435/dsp014x51hj109 COLLECTION.name=History dc.contributor.author=Li, Maximilian
~~~~

**list all bitstreams in a given collection, show related bundle names and mimeTypes**
~~~~
> dspace bulk-list -r $RROOT -t BITSTREAM --include 'object,ITEM.object,BUNDLE.name,mimeType' --format TSV
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 2 items
# org.dspace.app.bulkdo.Lister: Found 4 bundles
# org.dspace.app.bulkdo.Lister: Found 4 bitstreams
# org.dspace.app.bulkdo.Lister: # 4 type=0
object	ITEM.object	BUNDLE.name	mimeType
BITSTREAM.2393	ITEM.1968	ORIGINAL	application/pdf
BITSTREAM.2281	ITEM.1968	LICENSE	"text/plain; charset=utf-8"
BITSTREAM.2338	ITEM.1967	ORIGINAL	application/pdf
BITSTREAM.2278	ITEM.1967	LICENSE	"text/plain; charset=utf-8"
~~~~

** include  dc.language.iso metadata value of enclosing ITEM and
print  in TSV format **
~~~~
> dspace bulk-list -r handle/1398695916393 -t BITSTREAM --include 'object,ITEM.object,BUNDLE.name,ITEM.dc.language.iso' --format TSV
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 2 items
# org.dspace.app.bulkdo.Lister: Found 4 bundles
# org.dspace.app.bulkdo.Lister: Found 4 bitstreams
# org.dspace.app.bulkdo.Lister: # 4 type=0
object	ITEM.object	BUNDLE.name	ITEM.dc.language.iso
BITSTREAM.2393	ITEM.1968	ORIGINAL	en_US
BITSTREAM.2281	ITEM.1968	LICENSE	en_US
BITSTREAM.2338	ITEM.1967	ORIGINAL	en_US
BITSTREAM.2278	ITEM.1967	LICENSE	en_US
~~~~

** include policy information for READ, WRITE, ADD, REMOVE actions **
~~~~
> dspace bulk-list -r handle/1398695916393 -t BITSTREAM --include 'object,ITEM.object,POLICY.READ,POLICY.WRITE,POLICY.ADD,POLICY.REMOVE' --format TSV
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 2 items
# org.dspace.app.bulkdo.Lister: Found 4 bundles
# org.dspace.app.bulkdo.Lister: Found 4 bitstreams
# org.dspace.app.bulkdo.Lister: # 4 type=0
object	ITEM.object	POLICY.READ	POLICY.WRITE	POLICY.ADD	POLICY.REMOVE
BITSTREAM.2393	ITEM.1968	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[]	[POLICY(GROUP.Anonymous)]
BITSTREAM.2281	ITEM.1968	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[]	[POLICY(GROUP.Anonymous)]
BITSTREAM.2338	ITEM.1967	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[]	[]
BITSTREAM.2278	ITEM.1967	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[]	[]
~~~~


## bulk-pols: change authorization policies

** delete the Anonymous group's read authorization  **
~~~~
> dspace bulk-pols -r $RROOT -t BITSTREAM --format TSV -a DEL -w GROUP.Anonymous -d REMOVE
# D policy.REMOVE for 4 DSPaceObjects
object	parent	POLICY.REMOVE
BITSTREAM.2393	ITEM.1968	[]
BITSTREAM.2281	ITEM.1968	[]
BITSTREAM.2338	ITEM.1967	[]
BITSTREAM.2278	ITEM.1967	[]
~~~~

** the list command shows that the policies were removed **
~~~~
> dspace bulk-list -r handle/1398695916393 -t BITSTREAM
                 --include 'object,ITEM.object,POLICY.READ,POLICY.WRITE,POLICY.ADD,POLICY.REMOVE'
                 --format TSV
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 2 items
# org.dspace.app.bulkdo.Lister: Found 4 bundles
# org.dspace.app.bulkdo.Lister: Found 4 bitstreams
# org.dspace.app.bulkdo.Lister: # 4 type=0
object  ITEM.object POLICY.READ POLICY.WRITE    POLICY.ADD  POLICY.REMOVE
BITSTREAM.2393  ITEM.1968   [POLICY(GROUP.Anonymous)]   [POLICY(EPERSON.monikam)]   []  []
BITSTREAM.2281  ITEM.1968   [POLICY(GROUP.Anonymous)]   [POLICY(EPERSON.monikam)]   []  []
BITSTREAM.2338  ITEM.1967   [POLICY(GROUP.Anonymous)]   [POLICY(EPERSON.monikam)]   []  []
BITSTREAM.2278  ITEM.1967   [POLICY(GROUP.Anonymous)]   [POLICY(EPERSON.monikam)]   []  []
~~~~


** to give WRITE, ADD and REMOVE authorization to the EPerson monikam, for  all BITSTREAM under handle/1398695916393 run the following commands **
~~~~
> dspace bulk-pols -r handle/1398695916393 -t BITSTREAM
                 -a ADD -d WRITE -w EPERSON.monikam --format TSV
# A policy.WRITE for 4 DSPaceObjects
object	parent	POLICY.WRITE
BITSTREAM.2393	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2281	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2338	ITEM.1967	[POLICY(EPERSON.monikam)]
BITSTREAM.2278	ITEM.1967	[POLICY(EPERSON.monikam)]
~~~~
~~~~
dspace bulk-pols -r handle/1398695916393 -t BITSTREAM
                 -a ADD -d ADD  -w EPERSON.monikam --format TSV
# A policy.ADD for 4 DSPaceObjects
object	parent	POLICY.ADD
BITSTREAM.2393	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2281	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2338	ITEM.1967	[POLICY(EPERSON.monikam)]
BITSTREAM.2278	ITEM.1967	[POLICY(EPERSON.monikam)]
~~~~
~~~~
dspace bulk-pols -r handle/1398695916393 -t BITSTREAM
                 -a ADD -d REMOVE  -w EPERSON.monikam --format TSV
# A policy.REMOVE for 4 DSPaceObjects
object	parent	POLICY.REMOVE
BITSTREAM.2393	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2281	ITEM.1968	[POLICY(EPERSON.monikam)]
BITSTREAM.2338	ITEM.1967	[POLICY(EPERSON.monikam)]
BITSTREAM.2278	ITEM.1967	[POLICY(EPERSON.monikam)]
~~~~


** repeating the list command shows the new settings **
~~~~
> dspace bulk-list -r handle/1398695916393 -t BITSTREAM
                   --include 'object,ITEM.object,POLICY.READ,POLICY.WRITE,POLICY.ADD,POLICY.REMOVE'
                   --format TSV
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 2 items
# org.dspace.app.bulkdo.Lister: Found 4 bundles
# org.dspace.app.bulkdo.Lister: Found 4 bitstreams
# org.dspace.app.bulkdo.Lister: # 4 type=0
object	ITEM.object	POLICY.READ	POLICY.WRITE	POLICY.ADD	POLICY.REMOVE
BITSTREAM.2393	ITEM.1968	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]
BITSTREAM.2281	ITEM.1968	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]
BITSTREAM.2338	ITEM.1967	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]
BITSTREAM.2278	ITEM.1967	[POLICY(GROUP.Anonymous)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]	[POLICY(EPERSON.monikam)]
~~~~


## bulk-meta-data: working with metadata

To make sure make that the  pu.department field  on all items in a collection reads 'Art and Archaeology'
list first the setting first, delete the unwanted values and insert the correct setting.

**list pu.department metadata values
**
~~~~
> dspace bulk-list -r $RROOT --type ITEM --include 'object,parent,pu.department'
# org.dspace.app.bulkdo.Lister: Found 1 collections
# org.dspace.app.bulkdo.Lister: Found 15 items
# org.dspace.app.bulkdo.Lister: # 15 type=2
 object=ITEM.867 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.869 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.870 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.871 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.872 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.874 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.875 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.876 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.877 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.878 parent=COLLECTION.296 pu.department=Art and Archaeology
 object=ITEM.879 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.880 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.881 parent=COLLECTION.296 pu.department=Arts and Archaeologie
 object=ITEM.868 parent=COLLECTION.296 pu.department=Art and Archaeology
 object=ITEM.866 parent=COLLECTION.296 pu.department=Arts and Archaeologie
~~~~

**delete pu.department=Arts and Archaeologie   settings
**
~~~~
> dspace bulk-meta-data -r $RROOT --type ITEM --eperson monikam -a DEL -m 'pu.department=Arts and Archaeologie'
# org.dspace.app.bulkdo.MetaData: pu.department=Arts and Archaeologie > COLLECTION.296
# org.dspace.app.bulkdo.MetaData: apply: DEL dryRun=false
 object=ITEM.867 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.869 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.870 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.871 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.872 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.874 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.875 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.876 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.877 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.878 parent=COLLECTION.296 pu.department=Art and Archaeology changed=false
 object=ITEM.879 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.880 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.881 parent=COLLECTION.296 pu.department= changed=true
 object=ITEM.868 parent=COLLECTION.296 pu.department=Art and Archaeology changed=false
 object=ITEM.866 parent=COLLECTION.296 pu.department= changed=true
~~~~

**add  pu.department=Art and Archaeology where not yet set
**
~~~~
> dspace bulk-meta-data -r $RROOT --type ITEM --eperson monikam -a ADD  -m 'pu.department=Art and Archaeology'
# org.dspace.app.bulkdo.MetaData: pu.department=Art and Archaeology > COLLECTION.296
# org.dspace.app.bulkdo.MetaData: apply: ADD dryRun=false
 object=ITEM.869 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.867 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.870 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.871 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.872 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.874 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.875 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.876 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.877 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.879 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.878 parent=COLLECTION.296 pu.department=Art and Archaeology changed=false
 object=ITEM.880 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.881 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.866 parent=COLLECTION.296 pu.department=Art and Archaeology changed=true
 object=ITEM.868 parent=COLLECTION.296 pu.department=Art and Archaeology changed=false
~~~~

## bulk-bitstream: replacing a bitstream

replace the bitstream in an item's original BUNDLE
list and note the BITSTREAM that needs to be replaced

** list bitstreams in a given item   **
~~~~
> dspace bulk-list -r 88435/dsp01s4655g69q -t BITSTREAM --include 'object,BUNDLE.name,name'
# org.dspace.app.bulkdo.Lister: Found 0 collections
# org.dspace.app.bulkdo.Lister: Found 1 items
# org.dspace.app.bulkdo.Lister: Found 1 bundles
# org.dspace.app.bulkdo.Lister: Found 1 bitstreams
# org.dspace.app.bulkdo.Lister: # 1 type=0
 object=BITSTREAM.1133 BUNDLE.name=ORIGINAL name=minkin_daniel.pdf
~~~~

** replace with bitstream with a new version **

~~~~
> dspace bulk-bitstream -r BITSTREAM.1133 -t BITSTREAM -b minkin_daniel_fixed.pdf -e monikam
# org.dspace.app.bulkdo.Bitstreams: minkin_daniel_fixed.pdf(application/pdf) --> BITSTREAM.1133
 object=BITSTREAM.1133 parent= BUNDLE.name= replace=minkin_daniel_fixed.pdf replace.mimeType=application/pdf success= SUCCESS bundles=[BUNDLE.1111]
~~~~
# Command Usage

## bulk-list
```

 usage: org.dspace.app.bulkdo.Lister

 -y,--dryrun            dryrun - do not actually change anything; default
                        is false
 -W,--doWorkFlowItems   list items in workflow
 -e,--eperson           dspace user account (email or netid) used for
                        authorization to dspace app
 -f,--format            output format: tsv or txt
 -h,--help              help
 -i,--include           include listed object keys/properties in output;
                        give as comma separated list
 -r,--root              handle / type.ID
 -t,--type              type: collection, item, bundle, or bitstream
 -v,--verbose           verbose

List dspaceObjects of given type contained in root, printing properties
designated by include keys

OPTION include:
    Default Print Keys: [object, parent]
    Available Keys depend on the type of object being printed
        COLLECTION:[object, id, type, exception, name, handle, template]
        ITEM:[object, id, type, exception, isWithdrawn, handle, name]any
              metadafield, POLICY.dspace_action
        BUNDLE:[object, id, type, exception, isEmbargoed, name]
        BITSTREAM:[object, id, type, exception, mimeType, name, size,
              internalId, checksum, checksumAlgo]
        where dspace_action may be one of:  [READ, WRITE, OBSOLETE (DELETE),
              ADD, REMOVE, WORKFLOW_STEP_1, WORKFLOW_STEP_2, WORKFLOW_STEP_3,
              WORKFLOW_ABORT, DEFAULT_BITSTREAM_READ, DEFAULT_ITEM_READ, ADMIN]

```


## bulk-pols

```
usage: org.dspace.app.bulkdo.Policies

 -y,--dryrun            dryrun - do not actually change anything; default
                        is false
 -W,--doWorkFlowItems   list items in workflow
 -a,--action            what to do, available ADD, DEL
 -d,--dspace_action     one of [READ, WRITE, OBSOLETE (DELETE), ADD,
                        REMOVE, WORKFLOW_STEP_1, WORKFLOW_STEP_2,
                        WORKFLOW_STEP_3, WORKFLOW_ABORT,
                        DEFAULT_BITSTREAM_READ, DEFAULT_ITEM_READ, ADMIN]
                        default is READ
 -e,--eperson           dspace user account (email or netid) used for
                        authorization to dspace app
 -f,--format            output format: tsv or txt
 -h,--help              help
 -i,--include           include listed object keys/properties in output;
                        give as comma separated list
 -r,--root              handle / type.ID
 -t,--type              type: collection, item, bundle, or bitstream
 -v,--verbose           verbose
 -w,--who               group/eperson used in policies, give as
                        GROUP.<name>, EPERSON.<netid>, or EPERSON.<email>

ADD policy to or DELete policy from all dspaceObjects of given type contained in root

OPTION include:
    Default Print Keys: [object, parent]
    Available Keys depend on the type of object being printed
        COLLECTION:[object, id, type, exception, name, handle, template]
        ITEM:[object, id, type, exception, isWithdrawn, handle, name]any
              metadafield, POLICY.dspace_action
        BUNDLE:[object, id, type, exception, isEmbargoed, name]
        BITSTREAM:[object, id, type, exception, mimeType, name, size,
              internalId, checksum, checksumAlgo]
        where dspace_action may be one of:  [READ, WRITE, OBSOLETE (DELETE),
              ADD, REMOVE, WORKFLOW_STEP_1, WORKFLOW_STEP_2, WORKFLOW_STEP_3,
              WORKFLOW_ABORT, DEFAULT_BITSTREAM_READ, DEFAULT_ITEM_READ, ADMIN]
```


## bulk-meta-data
```
usage: org.dspace.app.bulkdo.MetaData

 -y,--dryrun            dryrun - do not actually change anything; default
                        is false
 -W,--doWorkFlowItems   list items in workflow
 -a,--action            what to do, available ADD, DEL
 -e,--eperson           dspace user account (email or netid) used for
                        authorization to dspace app
 -f,--format            output format: tsv or txt
 -h,--help              help
 -i,--include           include listed object keys/properties in output;
                        give as comma separated list
 -m,--meta_data         metadata setting of the form
                        'schema.ualifier.name=value'
 -r,--root              handle / type.ID
 -t,--type              type: collection, item, bundle, or bitstream
 -v,--verbose           verbose

OPTION include:
    Default Print Keys: [object, parent]
    Available Keys depend on the type of object being printed
        COLLECTION:[object, id, type, exception, name, handle, template]
        ITEM:[object, id, type, exception, isWithdrawn, handle, name]any
              metadafield, POLICY.dspace_action
        BUNDLE:[object, id, type, exception, isEmbargoed, name]
        BITSTREAM:[object, id, type, exception, mimeType, name, size,
              internalId, checksum, checksumAlgo]
        where dspace_action may be one of:  [READ, WRITE, OBSOLETE (DELETE),
              ADD, REMOVE, WORKFLOW_STEP_1, WORKFLOW_STEP_2, WORKFLOW_STEP_3,
              WORKFLOW_ABORT, DEFAULT_BITSTREAM_READ, DEFAULT_ITEM_READ, ADMIN]
```

## bulk-bitstream
```
usage: org.dspace.app.bulkdo.Bitstreams

 -y,--dryrun            dryrun - do not actually change anything; default
                        is false
 -g,--GO-GO-GO          ignore file format incompatibilities
 -W,--doWorkFlowItems   list items in workflow
 -b,--bitstream         file to replace given bitstream
 -e,--eperson           dspace user account (email or netid) used for
                        authorization to dspace app
 -f,--format            output format: tsv or txt
 -h,--help              help
 -i,--include           include listed object keys/properties in output;
                        give as comma separated list
 -r,--root              handle / type.ID
 -t,--type              type: collection, item, bundle, or bitstream
 -v,--verbose           verbose

Replace bitstream file in BITSTREAM object defined by root

OPTION include:
    Default Print Keys: [object, parent]
    Available Keys depend on the type of object being printed
        COLLECTION:[object, id, type, exception, name, handle, template]
        ITEM:[object, id, type, exception, isWithdrawn, handle, name]any
              metadafield, POLICY.dspace_action
        BUNDLE:[object, id, type, exception, isEmbargoed, name]
        BITSTREAM:[object, id, type, exception, mimeType, name, size,
              internalId, checksum, checksumAlgo]
        where dspace_action may be one of:  [READ, WRITE, OBSOLETE (DELETE),
              ADD, REMOVE, WORKFLOW_STEP_1, WORKFLOW_STEP_2, WORKFLOW_STEP_3,
              WORKFLOW_ABORT, DEFAULT_BITSTREAM_READ, DEFAULT_ITEM_READ, ADMIN]
```
