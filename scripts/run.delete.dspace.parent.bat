rem set those to your local paths in .gitignored file /envs/__dspace.parent.basic.bat.
rem you HAVE TO CREATE /envs/__dspace.parent.basic.bat with following variables in the same directory

rem start of variables expected in /envs/__dspace.parent.basic.bat
set m2_source=
set dspace_source=

call envs\__dspace.parent.basic.bat

set dspace_parent=%m2_source%\repository\org\dspace\dspace-parent
call delete.dspace.parent.bat