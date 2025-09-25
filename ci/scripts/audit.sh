#!/bin/bash -eux

# Ensure required environment variables are set
if [[ -z "${OSSINDEX_USERNAME:-}" || -z "${OSSINDEX_TOKEN:-}" ]]; then
  echo "Error: OSSINDEX_USERNAME and OSSINDEX_TOKEN must be set" >&2
  exit 1
fi

# Create Maven settings directory and file
mkdir -p ~/.m2
cat > ~/.m2/settings.xml << EOF
<settings>
  <servers>
    <server>
      <id>ossindex</id>
      <username>${OSSINDEX_USERNAME}</username>
      <password>${OSSINDEX_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

# Secure the file
chmod 600 ~/.m2/settings.xml

pushd zebedee
    if [[ "$APPLICATION" == "zebedee" ]]; then
        make audit-cms
    elif [[ "$APPLICATION" == "zebedee-reader" ]]; then
        make audit-reader
    fi
popd
