#!groovy

node {
    stage('Checkout') {
        checkout scm
        sh 'git clean -dfx'
        sh 'git rev-parse --short HEAD > git-commit'
        sh 'set +e && (git describe --exact-match HEAD || true) > git-tag'
    }

    def branch   = env.JOB_NAME.replaceFirst('.+/', '')
    def revision = revisionFrom(readFile('git-tag').trim(), readFile('git-commit').trim())

    stage('Build') {
        sh "${tool 'm3'}/bin/mvn clean package dependency:copy-dependencies"
    }

    stage('Image') {
        docker.withRegistry("https://${env.ECR_REPOSITORY_URI}", { ->
            for (image in images()) {
                docker.build(image['name'], image['dir']).push(revision)
            }
        })
    }

    stage('Bundle') {
        sh sprintf('sed -i -e %s -e %s -e %s -e %s -e %s appspec.yml zebedee-reader/appspec.yml scripts/codedeploy/* zebedee-reader/scripts/codedeploy/*', [
            "s/\\\${CODEDEPLOY_USER}/${env.CODEDEPLOY_USER}/g",
            "s/^CONFIG_BUCKET=.*/CONFIG_BUCKET=${env.S3_CONFIGURATIONS_BUCKET}/",
            "s/^ECR_REPOSITORY_URI=.*/ECR_REPOSITORY_URI=${env.ECR_REPOSITORY_URI}/",
            "s/^GIT_COMMIT=.*/GIT_COMMIT=${revision}/",
            "s/^AWS_REGION=.*/AWS_REGION=${env.AWS_DEFAULT_REGION}/",
        ])
        sh "tar -cvzf zebedee-${revision}.tar.gz appspec.yml scripts/codedeploy"
        sh "tar -cvzf zebedee-reader-${revision}.tar.gz -C zebedee-reader appspec.yml scripts/codedeploy"
        sh "aws s3 cp zebedee-${revision}.tar.gz s3://${env.S3_REVISIONS_BUCKET}/zebedee-${revision}.tar.gz"
        sh "aws s3 cp zebedee-reader-${revision}.tar.gz s3://${env.S3_REVISIONS_BUCKET}/zebedee-reader-${revision}.tar.gz"
    }

    def deploymentGroups = deploymentGroupsFor(branch)
    if (deploymentGroups.size() < 1) return

    stage('Deploy') {
        for (group in readerDeploymentGroupsFor(branch)) {
            sh sprintf('aws deploy create-deployment %s %s %s,bundleType=tgz,key=%s', [
                    '--application-name zebedee-reader',
                    "--deployment-group-name ${group}",
                    "--s3-location bucket=${env.S3_REVISIONS_BUCKET}",
                    "zebedee-reader-${revision}.tar.gz",
            ])
        }
        for (group in deploymentGroupsFor(branch)) {
            sh sprintf('aws deploy create-deployment %s %s %s,bundleType=tgz,key=%s', [
                    '--application-name zebedee',
                    "--deployment-group-name ${group}",
                    "--s3-location bucket=${env.S3_REVISIONS_BUCKET}",
                    "zebedee-${revision}.tar.gz"
            ])
        }
    }
}

def readerDeploymentGroupsFor(branch) {

    if (branch == 'develop') {
        return [env.CODEDEPLOY_FRONTEND_DEPLOYMENT_GROUP]
    }
    if (branch == 'dd-develop') {
        return [env.CODEDEPLOY_DISCOVERY_FRONTEND_DEPLOYMENT_GROUP]
    }
    if (branch == 'dd-master') {
        return [env.CODEDEPLOY_DISCOVERY_ALPHA_FRONTEND_DEPLOYMENT_GROUP]
    }
    return []
}

def deploymentGroupsFor(branch) {

    if (branch == 'develop') {
        return [env.CODEDEPLOY_PUBLISHING_DEPLOYMENT_GROUP]
    }
    if (branch == 'dd-develop') {
        return [env.CODEDEPLOY_DISCOVERY_PUBLISHING_DEPLOYMENT_GROUP]
    }
    if (branch == 'dd-master') {
        return [env.CODEDEPLOY_DISCOVERY_ALPHA_PUBLISHING_DEPLOYMENT_GROUP]
    }
    return []
}

def images() {
    [
        [
            name: 'zebedee',
            dir: '.',
        ],
        [
            name: 'zebedee-reader',
            dir: './zebedee-reader/',
        ],
    ]
}

@NonCPS
def revisionFrom(tag, commit) {
    def matcher = (tag =~ /^release\/(\d+\.\d+\.\d+(?:-rc\d+)?)$/)
    matcher.matches() ? matcher[0][1] : commit
}
