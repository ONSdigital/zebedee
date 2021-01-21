.PHONY: all
all: audit test build

.PHONY: audit
audit : audit-cms

.PHONY: audit-cms
audit-cms:
	mvn install -pl zebedee-reader -Dmaven.test.skip -Dossindex.skip=true
	mvn ossindex:audit

.PHONY: audit-reader
audit-reader:
	mvn -pl zebedee-reader -Dossindex.skip=true test
	mvn -pl zebedee-reader ossindex:audit

.PHONY: build
build: build-cms

.PHONY: build-cms
build-cms:
	mvn -Dmaven.test.skip -Dossindex.skip=true clean package dependency:copy-dependencies

.PHONY: build-reader
build-reader:
	mvn -pl zebedee-reader -Dmaven.test.skip -Dossindex.skip=true clean package dependency:copy-dependencies

.PHONY: debug-cms
debug-cms:
	./run.sh

.PHONY: debug-reader
debug-reader:
	./run-reader.sh

.PHONY: test
test: test-cms

.PHONY: test-cms
test-cms:
	mvn -Dossindex.skip=true test

.PHONY: test-reader
test-reader:
	mvn -pl zebedee-reader -Dossindex.skip=true test