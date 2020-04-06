#Deployment

##Prerequsites for deploying
The files are for deploying Brage for Unit. 

Jenkinsfile-deploy.groovy is run via Jenkins. 
Set up a multibranch project in Jenkins. 
Be sure to add credentials in jenkins to use the brage deploy-key for gitlab. 
The private key is found on the jenkins-server.

### Jenkins
 - create credentials (as secret text) for
   - brage_vault_utvikle  
   - brage_vault_test  
   - brage_vault_produksjon
- Install necessary jenkins-plugins
- Install ansible
- Install HashiCorp Vault plugin for ansible (pip install hvac to use the hashi_vault lookup module.)

### Vault
Store these keys in vault (use structure: secret/service/brage/<fase>):    
- db_password   
 - db_username  
 - handle_username 
 - handle_password 

### Configfiles
The script-files uses configuration files found in out private GitLab repo "brage6-customizations". 

###Deployservers
install ansible



