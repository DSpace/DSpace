pipeline {
    agent any

    tools {
        maven 'Maven 3.6.1'
    }

    environment {
        VERSION = "${env.BRANCH_NAME}".replaceAll('/', '_').toLowerCase()
        CUSTOMZ = "customizations"
    }

    stages {

        stage('Checkout Brage6 customizations') {
            steps {
                script {
                    brageVars = checkout scm
                    dir("${CUSTOMZ}") {
                        //configVars = checkout scm
                        git url: 'git@git.bibsys.no:team-rosa/brage6-customizations.git'
                    }
                }
            }
        }

        stage('Provide input parameters to confirm deployment') {
            steps {
                script {
                    def institusjoner = readYaml file: "${CUSTOMZ}/institusjoner.yml"
                    def kunder = []

                    institusjoner.each { prop, val ->
                        kunder << prop
                    }

                    try {
                        timeout(activity: true, time: 120, unit: 'SECONDS') {
                            inputResult = input(id: 'phaseInput', message: 'Velg parametre', parameters: [
                                    choice(choices: ["utvikle", "test", "produksjon"], name: 'devstep', description: 'Utviklingsfase:'),
                                    choice(choices: kunder, name: 'kunde', description: "Kunde:")
                            ])
                        }
                    } catch (err) {
                        echo "Release aborted"
                        throw err
                    }
                }
            }
        }

        stage('Bootstrap workspace') {
            steps {
                dir("${env.WORKSPACE}/deployscripts") {
                    withCredentials([string(credentialsId: 'brage_vault_' + inputResult.devstep, variable: 'VAULTSECRET')]) {
                        ansiblePlaybook(
                                playbook: 'pre-build.yml',
                                inventory: 'localhost,',
                                extraVars: [
                                        fase             : inputResult.devstep,
                                        jenkins_workspace: env.WORKSPACE,
                                        kunde            : inputResult.kunde,
                                        vault_secret     : "$VAULTSECRET"
                                ]
                        )
                    }
                }
            }
        }

        stage('Maven Build') {
            steps {
                echo "Building with maven"
                sh 'mvn package -Dmirage2.on=true -P !dspace-lni,!dspace-sword,!dspace-jspui,!dspace-rdf'
            }
        }

		stage('Deploy Brage') {
			steps {
				println("Deploying branch $VERSION for ${inputResult.kunde} to ${inputResult.devstep}")
				dir("${env.WORKSPACE}/deployscripts") {
					ansiblePlaybook(
					playbook: 'deploy-brage.yml',
					inventory: 'hosts',
					extraVars: [
							fase: inputResult.devstep,
							jenkins_workspace: env.WORKSPACE,
							kunde: inputResult.kunde
						]
					)
				}
			}
		}

        stage('Cleanup') {
            steps {
                cleanWs()
            }
        }
    }

}

