job "zebedee" {
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

  group "publishing" {
    count = "{{PUBLISHING_TASK_COUNT}}"

    constraint {
      distinct_hosts = true
    }

    constraint {
      attribute = "${node.class}"
      operator  = "regexp"
      value     = "publishing.*"
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
          "-server",
          "-Xms{{PUBLISHING_RESOURCE_HEAP_MEM}}m",
          "-Xmx{{PUBLISHING_RESOURCE_HEAP_MEM}}m",
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
        cpu    = "{{PUBLISHING_RESOURCE_CPU}}"
        memory = "{{PUBLISHING_RESOURCE_MEM}}"

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
