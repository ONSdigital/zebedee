job "zebedee" {
  datacenters = ["eu-west-1"]
  region      = "eu"
  type        = "service"

  update {
    stagger      = "90s"
    max_parallel = 1
  }

  group "publishing" {
    count = 1

    constraint {
      distinct_hosts = true
    }

    constraint {
      attribute = "${node.class}"
      value     = "publishing"
    }

    task "zebedee" {
      driver = "docker"

      artifact {
        source = "s3::https://s3-eu-west-1.amazonaws.com/{{DEPLOYMENT_BUCKET}}/zebedee/{{REVISION}}.tar.gz"
      }

      config {
        command = "${NOMAD_TASK_DIR}/start-task"

        args = [
          "java",
          "-Xmx2048m",
          "-cp target/dependency/*:target/classes/",
          "-Drestolino.classes=target/classes",
          "-Drestolino.packageprefix=com.github.onsdigital.zebedee.api",
          "com.github.davidcarboni.restolino.Main",
        ]

        image = "{{ECR_URL}}:concourse-{{REVISION}}"

        port_map {
          http = 8080
        }

        volumes = [
          "/var/florence:/content",
        ]
      }

      service {
        name = "zebedee"
        port = "http"
        tags = ["publishing"]
      }

      resources {
        cpu    = 2000
        memory = 2048

        network {
          port "http" {}
        }
      }

      template {
        source      = "${NOMAD_TASK_DIR}/vars-template"
        destination = "${NOMAD_TASK_DIR}/vars"
      }

      vault {
        policies = ["zebedee"]
      }
    }
  }
}