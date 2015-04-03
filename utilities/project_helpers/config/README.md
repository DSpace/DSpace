LINDAT/CLARIN configuration
----------------------------

Rename `local.conf.dist` to `local.conf` and udpate the variables accordingly.

If you want to change configuration files in dist to suit your needs, copy them to this directory and update them. 
Otherwise, the configuration files from the dist directory will be used! 

Updating them in the dist directory would make them dirty for th VCS (git). 


The `_substituted` directory contains the final configuration with all variables substituted.

The template file config/template/lr.cfg is used to define custom configuration.
The final lr.cfg file after substitution is copied to sources/dspace/config/modules/lr.cfg at compile time.