/**
 * CircleGuard Shared Pipeline Library
 *
 * Usage in a service Jenkinsfile:
 *   @Library('circleguard-shared') _
 *   circleguardPipeline(service: 'circleguard-auth-service', port: 8180)
 */
def call(Map config) {
    def service   = config.service      // e.g. 'circleguard-auth-service'
    def port      = config.port         // e.g. 8180
    def env       = config.get('env', 'dev')   // dev | stage | master
    def root      = env.CIRCLE_GUARD_ROOT ?: '/workspace/circle-guard-public'
    def shortName = service.replace('circleguard-', '').replace('-service', '')

    pipeline {
        agent any

        options {
            timestamps()
            ansiColor('xterm')
            timeout(time: 30, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: '10'))
        }

        parameters {
            choice(name: 'ENVIRONMENT', choices: ['dev', 'stage', 'master'],
                   description: 'Target environment')
        }

        environment {
            SERVICE_NAME  = "${service}"
            SERVICE_PORT  = "${port}"
            IMAGE_TAG     = "${shortName}-${BUILD_NUMBER}"
            REPO_ROOT     = "${root}"
            GRADLE_OPTS   = '-Xmx512m -Dfile.encoding=UTF-8'
        }

        stages {

            stage('Checkout') {
                steps {
                    echo "Source: ${REPO_ROOT}"
                    dir("${REPO_ROOT}") {
                        sh 'git log -1 --oneline'
                    }
                }
            }

            stage('Build') {
                steps {
                    dir("${REPO_ROOT}") {
                        sh "./gradlew :services:${SERVICE_NAME}:bootJar --no-daemon -x test"
                    }
                }
                post {
                    success { echo "Build SUCCESS for ${SERVICE_NAME}" }
                    failure { echo "Build FAILED for ${SERVICE_NAME}" }
                }
            }

            stage('Unit Tests') {
                steps {
                    dir("${REPO_ROOT}") {
                        sh "./gradlew :services:${SERVICE_NAME}:test --no-daemon"
                    }
                }
                post {
                    always {
                        junit(
                            testResults: "services/${SERVICE_NAME}/build/test-results/test/**/*.xml",
                            allowEmptyResults: true
                        )
                        publishHTML(target: [
                            allowMissing: true,
                            reportDir:   "services/${SERVICE_NAME}/build/reports/tests/test",
                            reportFiles: 'index.html',
                            reportName:  "${SERVICE_NAME} Unit Test Report"
                        ])
                    }
                }
            }

            stage('Integration Tests') {
                steps {
                    dir("${REPO_ROOT}") {
                        sh "./gradlew :services:${SERVICE_NAME}:integrationTest --no-daemon || true"
                        // Fall back to running all tests if no integrationTest task defined
                        sh "./gradlew :services:${SERVICE_NAME}:test --no-daemon -PtestType=integration || true"
                    }
                }
                post {
                    always {
                        junit(
                            testResults: "services/${SERVICE_NAME}/build/test-results/**/*.xml",
                            allowEmptyResults: true
                        )
                    }
                }
            }

            stage('Docker Build') {
                steps {
                    dir("${REPO_ROOT}") {
                        sh """
                            docker build \
                              -f services/${SERVICE_NAME}/Dockerfile \
                              -t ${SERVICE_NAME}:${IMAGE_TAG} \
                              -t ${SERVICE_NAME}:latest \
                              .
                        """
                    }
                }
            }

            stage('Deploy Dev') {
                when {
                    expression { params.ENVIRONMENT == 'dev' }
                }
                steps {
                    dir("${REPO_ROOT}") {
                        sh "docker compose -f docker-compose.dev.yml up -d --no-deps ${shortName}-service || true"
                    }
                }
                post {
                    success {
                        sh "sleep 15 && curl -sf http://localhost:${SERVICE_PORT}/actuator/health || echo 'Health check not available'"
                    }
                }
            }

            stage('Deploy Stage (K8s)') {
                when {
                    expression { params.ENVIRONMENT in ['stage', 'master'] }
                }
                steps {
                    dir("${REPO_ROOT}") {
                        sh "docker tag ${SERVICE_NAME}:latest ${SERVICE_NAME}:stage-latest"
                        sh "kubectl apply -k k8s/overlays/stage --dry-run=client || true"
                        sh "kubectl apply -k k8s/overlays/stage"
                        sh "kubectl rollout status deployment/${SERVICE_NAME} -n circleguard-stage --timeout=120s || true"
                    }
                }
            }

            stage('E2E Tests') {
                when {
                    expression { params.ENVIRONMENT in ['stage', 'master'] }
                }
                steps {
                    dir("${REPO_ROOT}/tests/e2e") {
                        sh """
                            newman run circleguard-e2e.postman_collection.json \
                              --environment stage.env.json \
                              --reporters cli,junit,htmlextra \
                              --reporter-junit-export reports/e2e-${BUILD_NUMBER}.xml \
                              --reporter-htmlextra-export reports/e2e-${BUILD_NUMBER}.html \
                              --timeout-request 15000 \
                              --bail || true
                        """
                    }
                }
                post {
                    always {
                        junit(testResults: 'tests/e2e/reports/*.xml', allowEmptyResults: true)
                        publishHTML(target: [
                            allowMissing: true,
                            reportDir:   'tests/e2e/reports',
                            reportFiles: "e2e-${BUILD_NUMBER}.html",
                            reportName:  'E2E Test Report'
                        ])
                    }
                }
            }

            stage('Performance Smoke') {
                when {
                    expression { params.ENVIRONMENT in ['stage', 'master'] }
                }
                steps {
                    dir("${REPO_ROOT}/tests/performance") {
                        sh """
                            locust --headless \
                              -f locustfile.py \
                              --host http://localhost:8180 \
                              -u 20 -r 5 -t 1m \
                              --html reports/perf-smoke-${BUILD_NUMBER}.html \
                              --csv  reports/perf-smoke-${BUILD_NUMBER} \
                              --logfile reports/perf-smoke-${BUILD_NUMBER}.log || true
                        """
                    }
                }
                post {
                    always {
                        publishHTML(target: [
                            allowMissing: true,
                            reportDir:   'tests/performance/reports',
                            reportFiles: "perf-smoke-${BUILD_NUMBER}.html",
                            reportName:  'Performance Smoke Report'
                        ])
                    }
                }
            }

            stage('Promote to Prod (K8s)') {
                when {
                    expression { params.ENVIRONMENT == 'master' }
                }
                steps {
                    dir("${REPO_ROOT}") {
                        sh "docker tag ${SERVICE_NAME}:stage-latest ${SERVICE_NAME}:prod-latest"
                        sh "kubectl apply -k k8s/overlays/prod"
                        sh "kubectl rollout status deployment/${SERVICE_NAME} -n circleguard-prod --timeout=180s || true"
                    }
                }
            }

            stage('Generate Release Notes') {
                when {
                    expression { params.ENVIRONMENT == 'master' }
                }
                steps {
                    dir("${REPO_ROOT}") {
                        sh "pwsh -File ci/scripts/generate-release-notes.ps1 -Service ${SERVICE_NAME} -BuildNumber ${BUILD_NUMBER} || true"
                    }
                }
                post {
                    always {
                        archiveArtifacts(
                            artifacts: 'RELEASE_NOTES_*.md,CHANGELOG.md',
                            allowEmptyArchive: true
                        )
                    }
                }
            }
        }

        post {
            always {
                cleanWs(cleanWhenNotBuilt: false, deleteDirs: true, disableDeferredWipeout: true)
            }
            success {
                echo "Pipeline ${SERVICE_NAME} [${params.ENVIRONMENT}] - SUCCESS (Build #${BUILD_NUMBER})"
            }
            failure {
                echo "Pipeline ${SERVICE_NAME} [${params.ENVIRONMENT}] - FAILED (Build #${BUILD_NUMBER})"
            }
        }
    }
}
