#!groovy

node {
    stage 'Checkout'
    checkout scm

    stage 'Build'
    def mvn = "${tool 'm3'}/bin/mvn"
    sh "${mvn} -Dmaven.test.skip=true clean package dependency:copy-dependencies"

    stage 'Image'
    sh 'git rev-parse --short HEAD | tee git_commit_id zebedee-reader/git_commit_id > /dev/null'
    commit = readFile('git_commit_id').trim()
    def img = docker.build "${env.ECR_REPOSITORY_URI}/zebedee:${commit}"
    def readerImg = docker.build "${env.ECR_REPOSITORY_URI}/zebedee-reader:${commit}", 'zebedee-reader/'

    stage 'Push'
    sh '$(aws ecr get-login)'
    img.push()
    readerImg.push()
}
