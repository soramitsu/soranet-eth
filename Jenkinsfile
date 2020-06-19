def dockerVolumes = '-v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp'
def dockerRunArgs = '-e JVM_OPTS="-Xmx3200m" -e TERM="dumb"'
def tagPattern = /(master|develop|reserved)/

pipeline {
    environment { DOCKER_NETWORK = '' }
    agent { label 'd3-build-agent' }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
        disableConcurrentBuilds()
    }
    stages {
        stage('Tests') {
            environment {
                SORANET_DOCKER = credentials('bot-soranet-ro')
                D3_DOCKER = credentials('nexus-d3-docker')
                SONAR_TOKEN = credentials('SONAR_TOKEN')
            }
            steps {
                script {
    
                    env.WORKSPACE = pwd()

                    DOCKER_NETWORK = "${env.CHANGE_ID}-${env.GIT_COMMIT}-${BUILD_NUMBER}"
                    def dockerNetArgs = "--network='d3-${DOCKER_NETWORK}'"
                    writeFile file: ".env", text: "SUBNET=${DOCKER_NETWORK}"

                    sh "docker login docker.soramitsu.co.jp -u ${SORANET_DOCKER_USR} -p '${SORANET_DOCKER_PSW}'"
                    sh "docker login nexus.iroha.tech:19002 -u ${D3_DOCKER_USR} -p '${D3_DOCKER_PSW}'"

                    sh "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml pull"
                    sh "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml up --build -d"


                    docker.withRegistry('https://docker.soramitsu.co.jp/', 'bot-build-tools-ro'){

                        iC = docker.image("docker.soramitsu.co.jp/build-tools/openjdk-8:latest")
                        iC.inside("${dockerNetArgs} ${dockerRunArgs} ${dockerVolumes}") {

                            sh "docker login docker.soramitsu.co.jp -u ${SORANET_DOCKER_USR} -p '${SORANET_DOCKER_PSW}'"
                            sh "docker login nexus.iroha.tech:19002 -u ${D3_DOCKER_USR} -p '${D3_DOCKER_PSW}'"

                            sh "./gradlew dependencies"
                            sh "./gradlew test --info"
                            // We need this to test containers
                            sh "./gradlew dockerfileCreate"
                            sh "./gradlew compileIntegrationTestKotlin --info"
                            sh "./gradlew integrationTest --info"
                            sh "./gradlew d3TestReport"
                        }
                    }

                    if (env.BRANCH_NAME == 'develop') {
                        iC.inside("${dockerNetArgs} ${dockerRunArgs}") {
                            sh "./gradlew sonarqube -x test --configure-on-demand \
                                -Dsonar.links.ci=${BUILD_URL} \
                                -Dsonar.github.pullRequest=${env.CHANGE_ID} \
                                -Dsonar.github.disableInlineComments=true \
                                -Dsonar.host.url=https://sonar.soramitsu.co.jp \
                                -Dsonar.login=${SONAR_TOKEN}"
                        }
                    }

                    publishHTML (target: [
                        allowMissing: false,
                        alwaysLinkToLastBuild: false,
                        keepAll: true,
                        reportDir: 'build/reports',
                        reportFiles: 'd3-test-report.html',
                        reportName: "D3 test report"
                    ])

                    // scan smartcontracts only on pull requests to master
                    try {
                        if (env.CHANGE_TARGET == "master") {
                            docker.image("mythril/myth").inside("--entrypoint=''") {
                                sh "echo 'Smart contracts scan results' > mythril.txt"
                                // using mythril to scan all solidity files
                                sh "find . -name '*.sol' -exec \
                                    myth --execution-timeout 900 --create-timeout 900 -x {} \\; | \
                                    tee mythril.txt"
                            }
                            // save results as a build artifact
                            zip archive: true, dir: '', glob: 'mythril.txt', zipFile: 'smartcontracts-scan-results.zip'
                        }
                    } catch(MissingPropertyException e) { }
                }
            }

            post {
                always {
                    junit allowEmptyResults: true, keepLongStdio: true, testResults: 'build/test-results/**/*.xml'
                }
                cleanup {
                    sh ".jenkinsci/prepare-logs.sh"
                    archiveArtifacts artifacts: 'build-logs/*.gz'
                    sh "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml down"
                }
            }

        }

        stage('Build and push docker images') {
            environment {
                SORANET_DOCKER = credentials('bot-soranet-rw')
            }
            when {
                expression { return (env.BRANCH_NAME ==~ tagPattern || env.TAG_NAME) }
            }
            steps {
                script {
                    env.DOCKER_TAG = env.TAG_NAME ? env.TAG_NAME : env.BRANCH_NAME

                    def dockerPushConfig =  " -e DOCKER_REGISTRY_URL='https://docker.soramitsu.co.jp'" +
                                            " -e DOCKER_REGISTRY_USERNAME='${SORANET_DOCKER_USR}'" +
                                            " -e DOCKER_REGISTRY_PASSWORD='${SORANET_DOCKER_PSW}'" +
                                            " -e TAG='${DOCKER_TAG}'"
                    
                    iC = docker.image("gradle:4.10.2-jdk8-slim")
                    iC.inside("${dockerRunArgs} ${dockerVolumes} ${dockerPushConfig}") {
                        sh "gradle shadowJar"
                        sh "gradle dockerPush"
                    }
                }
            }
        }
    }
    post {
        cleanup { cleanWs() }
    }
}
