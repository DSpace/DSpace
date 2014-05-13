DSpace-SEDICI-svn
=================

# DSpace 1.8.2 @ SEDICI (UNLP) SVN mirror

## Descripción
Copia del repositorio SVN de desarrollo de http://sedici.unlp.edu.ar, con casi de 2 años de adaptaciones y actualizaciones sobre la rama de 1.8.x de DSpace 
Este repositorio se creó para punto de transición para la actualización de SEDICI de la versión de DSpace 1.8.2 a la 4.0.


*SEDICI* es el Repositorio Institucional de la Universidad Nacional de La Plata, un servicio libre y gratuito creado para albergar, preservar y dar visibilidad a las producciones de las Unidades Académicas de la Universidad.

**Visite [sedici.unlp.edu.ar](http://sedici.unlp.edu.ar.) para verlo en vivo.**

![Screenshot](screenshot_sedici.png?raw=true)

### Instalación

git clone git@github.com:sedici/DSpace-SEDICI-svn.git DSpace-SEDICI-svn
cd DSpace-SEDICI-svn/
svn co https://svn.duraspace.org/dspace/dspace/tags/dspace-1.8.2/dspace distribution/dspace-tag-files
mvn eclipse:eclipse
./build.bash install

# License
[Apache License v2](http://www.apache.org/licenses/LICENSE-2.0.html)

