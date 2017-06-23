job "zebedee-reader" {
  datacenters = ["eu-west-1"]
  region      = "eu"
  type        = "service"

  update {
    stagger      = "90s"
    max_parallel = 1
  }

  group "web" {
    count = 2

    constraint {
      distinct_hosts = true
    }

    constraint {
      attribute = "${node.class}"
      value     = "web"
    }

    task "zebedee-reader" {
      driver = "docker"

      artifact {
        source = "s3::https://s3-eu-west-1.amazonaws.com/{{DEPLOYMENT_BUCKET}}/zebedee-reader/{{REVISION}}.tar.gz"
      }

      config {
        command = "${NOMAD_TASK_DIR}/start-task"

        args = [
          "java",
          "-Xmx2048m",
          "-cp target/dependency/*:target/classes/",
          "-Drestolino.classes=target/classes",
          "-Drestolino.packageprefix=com.github.onsdigital.zebedee.reader.api",
          "com.github.davidcarboni.restolino.Main",
        ]

        image = "{{ECR_URL}}:concourse-{{REVISION}}"

        port_map {
          http = 8080
        }

        volumes = [
          "/var/babbage/site:/content:ro",
        ]
      }

      service {
        name = "zebedee-reader"
        port = "http"
        tags = ["web"]
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
        policies = ["zebedee-reader"]
      }
    }
  }
}
