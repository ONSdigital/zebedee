#!groovy

node {
    stage 'Checkout'
    checkout scm

    stage 'Build'
    sh "${tool 'm3'}/bin/mvn clean package dependency:copy-dependencies"
    sh 'git rev-parse --short HEAD | tee git_commit_id zebedee-reader/git_commit_id > /dev/null'

    stage 'Image'
    def branch   = env.JOB_NAME.replaceFirst('.+/', '')
    def revision = readFile('git_commit_id').trim()
    def registry = [
        'hub': [
            'login': 'docker login --username=$DOCKERHUB_USER --password=$DOCKERHUB_PASS',
            'images': [
                [
                    'name': "${env.DOCKERHUB_REPOSITORY}/zebedee",
                    'dir': '.',
                ],
                [
                    'name': "${env.DOCKERHUB_REPOSITORY}/zebedee-reader",
                    'dir': './zebedee-reader/',
                ],
            ],
            'tag': 'live',
            'uri': "https://${env.DOCKERHUB_REPOSITORY_URI}",
        ],
        'ecr': [
            'login': '$(aws ecr get-login)',
            'images': [
                [
                    'name': 'zebedee',
                    'dir': '.',
                ],
                [
                    'name': 'zebedee-reader',
                    'dir': './zebedee-reader/',
                ],
            ],
            'tag': revision,
            'uri': "https://${env.ECR_REPOSITORY_URI}",
        ],
    ][branch == 'live' ? 'hub' : 'ecr']

    docker.withRegistry(registry['uri'], { ->
        sh registry['login']

        for (image in registry['images']) {
            docker.build(image['name'], image['dir']).push(registry['tag'])
        }
    })

    if (branch != 'develop') return

    stage 'Bundle'
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

    stage 'Deploy'
    sh sprintf('aws deploy create-deployment %s %s %s,bundleType=tgz,key=%s.tar.gz', [
        '--application-name zebedee-reader',
        "--deployment-group-name ${env.CODEDEPLOY_FRONTEND_DEPLOYMENT_GROUP}",
        "--s3-location bucket=${env.S3_REVISIONS_BUCKET}",
        "zebedee-reader-${revision}",
    ])
    sh sprintf('aws deploy create-deployment %s %s %s,bundleType=tgz,key=%s.tar.gz', [
        '--application-name zebedee',
        "--deployment-group-name ${env.CODEDEPLOY_PUBLISHING_DEPLOYMENT_GROUP}",
        "--s3-location bucket=${env.S3_REVISIONS_BUCKET}",
        "zebedee-${revision}"
    ])
}
