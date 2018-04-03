job "zebedee-reader" {
  datacenters = ["eu-west-1"]
  region      = "eu"
  type        = "service"

  constraint {
    attribute = "${meta.has_disk}"
    value     = true
  }

  update {
    min_healthy_time = "30s"
    healthy_deadline = "2m"
    max_parallel     = 1
    auto_revert      = true
    stagger          = "150s"
  }

  group "web" {
    count = "{{WEB_TASK_COUNT}}"

    constraint {
      distinct_hosts = true
    }

    constraint {
      attribute = "${node.class}"
      operator  = "regexp"
      value     = "web.*"
    }

    task "zebedee-reader" {
      driver = "docker"

      artifact {
        source = "s3::https://s3-eu-west-1.amazonaws.com/{{DEPLOYMENT_BUCKET}}/zebedee/{{REVISION}}.tar.gz"
      }

      config {
        command = "${NOMAD_TASK_DIR}/start-task"

        args = [
          "java",
          "-server",
          "-Xms{{WEB_RESOURCE_HEAP_MEM}}m",
          "-Xmx{{WEB_RESOURCE_HEAP_MEM}}m",
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
        cpu    = "{{WEB_RESOURCE_CPU}}"
        memory = "{{WEB_RESOURCE_MEM}}"

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
