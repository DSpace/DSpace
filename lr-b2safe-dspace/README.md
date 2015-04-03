# Business to Safe DSpace Module

This DSpace module can be used to automatically replicate each submission after it has been approved. The submission is converted to AIP format which is uploaded to the iRods server. We use our PID in the name of each AIP e.g.

> irods://XXX@replication.server:XX/Home/dspace/11858_00-097C-0000-0001-487A-4.zip
> irods://XXX@replication.server:XX/Home/dspace/11858_00-097C-0000-0001-487E-B.zip


### Requirements
To use this package you need to first install lr-b2safe-core package
```
git clone https://github.com/ufal/lr-b2safe-core.git
cd lr-b2safe-core
mvn install -Dmaven.test.skip=true
```

### Configuration

To add the module to your DSpace. Clone this git repository into the sources folder of DSpace, and update the following files.

* Change the parent version in lr-b2safe-dspace/pom.xml to match your DSpace version.
* Add the module in the root pom.xml of DSpace.
```
<profile>
  <id>lr-b2safe-dspace</id>
  <activation>
    <activeByDefault>true</activeByDefault>
  </activation>
  <modules>
    <module>lr-b2safe-dspace</module>
  </modules>
</profile>
```
* Add the dependency in root pom.xml, dspace/pom.xml and dspace-xmlui/dspace-xmlui-api/pom.xml (1.8.x) or dspace-xmlui/pom.xml (4.x)
```
<dependency>
  <groupId>cz.cuni.mff.ufal.dspace</groupId>
  <artifactId>b2safe-dspace</artifactId>
  <version>${project.version}</version>
</dependency>
```
* Add a new configuration module "lr" in dspace/config/modules with following properties.
```
lr.replication.eudat.on=
lr.replication.eudat.host=
lr.replication.eudat.port=
lr.replication.eudat.username=
lr.replication.eudat.password=
lr.replication.eudat.homedirectory=
lr.replication.eudat.replicadirectory=
lr.replication.eudat.zone=
lr.replication.eudat.defaultstorage=
lr.replication.eudat.notification_email=
```
* Finally add the event listener to dspace.cfg, e.g. named replication
```
# list of event listeners
event.dispatcher.default.consumers = search, browse, discovery, eperson, harvester, replication 
# consumer to maintain the browse index
event.consumer.replication.class = cz.cuni.mff.ufal.dspace.b2safe.ItemModifyConsumer
event.consumer.replication.filters = Community|Collection|Item+Create|Modify 
```

### Control Panel

To activate the Replication tab in Control Panel, apply the patch according to dspace version in patch folder.
`git apply ControlPanel_dspace4.patch`


### What is replicateable

Each submission is automatically replicated after it has been approved, provided that the item is PUB (dc.rights.label). The submission is converted to AIP format that is uploaded to the iRods server. We use our PID in the name of each AIP e.g., 
```
irods://XXX@irods.server:XX/IRODSZone/home/dspace_1.8.2/11858_00-097C-0000-0001-487A-4-6451959456007568280.zip
irods://XXX@irods.server:XX/IRODSZone/home/dspace_1.8.2/11858_00-097C-0000-0001-487E-B-6544474914476974525.zip
```
After uploading the file, we fill out its metadata e.g.,
```
EUDAT_ROR : http://hdl.handle.net/11858/00-097Z-0000-0022-E46B-E
OTHER_AckEmail : email@ufal.mff.cuni.cz
OTHER_From : https://ufal-point-dev.ms.mff.cuni.cz/jm/xmlui
```
