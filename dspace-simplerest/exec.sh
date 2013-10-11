timeout 5 echo "We're moving simplerest.war to source-simplerest folder, type y to continue "
read -p "We're moving simplerest.war to source-simplerest folder, type y to continue " -n 1 -r 
echo

if [[ $REPLY =~ ^[Yy]$ ]]
then
    if [ ! -d /data/source-simplerest/ ]; then
     echo "creating source-simplerest folder"
     mkdir /data/source-simplerest/
    fi
    cp -r target/simplerest.war /data/source-simplerest/
    chown -R tomcat:tomcat /data/source-simplerest/
else
    echo "File not moved"
fi
