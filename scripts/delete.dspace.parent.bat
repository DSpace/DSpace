IF EXIST %dspace_parent% rmdir %dspace_parent% /q /s
cd %dspace_source% || echo 'failure'
call mvn install