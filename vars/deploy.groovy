#!/usr/bin/env groovy

def getServer() {
    def remote = [:]
    remote.name = 'manager node'
    remote.user = 'dev'
    remote.host = "${REMOTE_HOST}"
    remote.port = 22
    remote.identityFile = '/root/.ssh/id_rsa'
    remote.allowAnyHosts = true
    return remote
}

def getTestServer(){
    def remote = [:]
    remote.name = "192.168.4.10'"
    remote.host = '192.168.4.10'
    remote.port = 22
    remote.allowAnyHosts = true
    withCredentials([usernamePassword(credentialsId: '192.168.4.10', passwordVariable: 'password', usernameVariable: 'userName')]) {
        remote.user = "${userName}"
        remote.password = "${password}"
    }
    return remote
}

def call(Map map) {

    pipeline {
        agent {
            docker {
                image 'node'
            }
        }

        environment {
            REMOTE_HOST = "${map.REMOTE_HOST}"
            REPO_URL = "${map.REPO_URL}"
            BRANCH_NAME = "${map.BRANCH_NAME}"
            STACK_NAME = "${map.STACK_NAME}"
            COMPOSE_FILE_NAME = "docker-compose-" + "${map.STACK_NAME}" + "-" + "${map.BRANCH_NAME}" + ".yml"
        }

        stages {
            stage('获取代码') {
                steps {
                    git([url: "${REPO_URL}", branch: "${BRANCH_NAME}"])
                }
            }

            stage('编译代码') {
                steps {
                    sh "node --version"
                    sh 'npm config set registry http://registry.npm.taobao.org/'
                    sh 'npm install'
                    sh 'npm run build'
                }
            }

            stage('初始化测试环境配置') {
                steps {
                    script {
                        server = getTestServer()
                    }
                }
            }

            stage('测试环境部署') {
                steps {
                    // writeFile file: 'deploy.sh', text: """
                    // module='pwd'
                    // module='echo ${module%_*}'
                    // module='echo ${module##*/}'
                    // docker ps | grep ${module}:${BRANCH_NAME}-latest | awk '{print \$1}' | xargs docker kill || true
                    // docker images | grep ${module}:${BRANCH_NAME}-latest | awk '{print \$1":"\$2}' | xargs docker rmi -f || true
                    // docker pull ${module}:${BRANCH_NAME}-latest
                    // docker run -d ${module}:${BRANCH_NAME}-latest
                    // """
                    // sshScript remote: server, script: "deploy.sh"

                    sshCommand remote: server, command: "wget -O deploy.sh https://raw.githubusercontent.com/Lover103/jenkins-pipeline-library/master/resources/shell/deploy.sh; deploy.sh ${BRANCH_NAME} "
                }
            }

            stage('构建镜像') {
                steps {
                    sh "wget -O build.sh https://raw.githubusercontent.com/Lover103/jenkins-pipeline-library/master/resources/shell/build.sh"
                    sh "sh build.sh ${BRANCH_NAME} "
                }
            }

            stage('初始化发版配置') {
                when {
                    branch 'production'
                }
                steps {
                    script {
                        server = getServer()
                    }
                }
            }

            stage('执行发版') {
                when {
                    branch 'production'
                }
                steps {
                    writeFile file: 'deploy.sh', text: "wget -O ${COMPOSE_FILE_NAME} " +
                            " https://raw.githubusercontent.com/Lover103/jenkins-pipeline-library/master/resources/docker-compose/${COMPOSE_FILE_NAME} \n" +
                            "sudo docker stack deploy -c ${COMPOSE_FILE_NAME} ${STACK_NAME}"
                    sshScript remote: server, script: "deploy.sh"

                                        // def imgName = ${REGISTRY_DOMAIN}/${DOCKER_NAMESPACE}/${project_name}

                    // // 下载镜像
                    // sshCommand remote: server, command: "docker pull ${imgName}"

                    //  // 停止容器
                    // sshCommand remote: server, command: "docker stop ${project_name}"

                    // // 删除容器
                    // sshCommand remote: server, command: "docker rm -f ${project_name}"
                        
                    // // 启动容器
                    // sshCommand remote: server, command: "docker run -d --name ${project_name} -e TZ=Asia/Shanghai ${imgName}"
                    
                    // // 清理镜像
                    // def clearNoneSSH = "n=`docker images | grep  '<none>' | wc -l`; if [ \$n -gt 0 ]; then docker rmi `docker images | grep  '<none>' | awk '{print \$3}'`; fi"
                    
                    // sshCommand remote: server, command: "${clearNoneSSH}"
                }
            }
        }

        post {
            always {
                deleteDir()
                echo 'Test run completed'
                cucumber buildStatus: 'UNSTABLE', failedFeaturesNumber: 999, failedScenariosNumber: 999, failedStepsNumber: 3, fileIncludePattern: '**/*.json', skippedStepsNumber: 999
            }
            success {
                echo 'Successfully!'
                mail(
                    from: "quan.shi@zymobi.com",
                    to: "quan.shi@zymobi.com",
                    subject: "That build passed.",
                    body: "Nothing to see here"
                )
            }
            failure {
                echo 'Failed!'

                mail(
                    from: "quan.shi@zymobi.com",
                    to: "quan.shi@zymobi.com",
                    subject: "That build passed.",
                    body: "Nothing to see here"
                )
            }
            unstable {
                echo 'This will run only if the run was marked as unstable'
            }
            changed {
                echo 'This will run only if the state of the Pipeline has changed'
                echo 'For example, if the Pipeline was previously failing but is now successful'
            }
        }

        options {
            disableConcurrentBuilds()
            skipDefaultCheckout()
            timeout(time: 60, unit: 'MINUTES')
        }
    }
}
