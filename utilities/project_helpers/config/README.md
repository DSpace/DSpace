LINDAT/CLARIN configuration
----------------------------

Rename `local.conf.dist` to `local.conf` and update the variables accordingly.

If you want to change a configuration file, copy and delete the `.example` from the name.
Deployment framework first tries to find the configuration file without `.example` and only if
it file does not exist it tries with `.example`.  

Updating raw configuration files in this directory make them dirty for git. 

The `_substituted` directory contains the final configuration with all variables substituted.
