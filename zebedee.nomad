job "zebedee" {
  datacenters = ["eu-west-1"]
  region      = "eu"
  type        = "service"

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
      value     = "publishing-mount"
    }

    restart {
      attempts = 3
      delay    = "15s"
      interval = "1m"
      mode     = "delay"
    }

    task "zebedee" {
      driver = "docker"

      artifact {
        source = "s3::https://s3-eu-west-1.amazonaws.com/{{DEPLOYMENT_BUCKET}}/zebedee/{{TARGET_ENVIRONMENT}}/{{RELEASE}}.tar.gz"
      }

      config {
        command     = "${NOMAD_TASK_DIR}/start-task"
        image       = "{{ECR_URL}}:concourse-{{REVISION}}"
        userns_mode = "host"

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

        check {
          type     = "http"
          path     = "/health"
          interval = "10s"
          timeout  = "2s"
        }
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
