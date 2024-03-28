call ..\envs\__basic.bat

rm -rf %dspace_solr%server\solr\configsets\authority %dspace_solr%server\solr\configsets\oai dspace_solr%server\solr\configsets\search dspace_solr%server\solr\configsets\statistics
xcopy /e /h /i /q /y %dspace_application%solr\ %dspace_solr%server\solr\configsets\

