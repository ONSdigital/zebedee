OSSINDEX_ERRORS = "Unable to contact OSS Index|authentication failed|401 Unauthorized|403 Forbidden|429 Too Many Requests|Too many requests|Rate limit|Unknown host|Connection refused|timed out|unreachable|402 Payment Required"

.PHONY: all
all: audit test build

.PHONY: audit audit-cms audit-reader

audit: audit-cms audit-reader

audit-cms:
	@echo "🔍 Running OSS Index audit for CMS zebedee-cms..."
	@mvn -B -Dmaven.test.skip -Dossindex.skip=true -pl zebedee-cms -am install
	@mkdir -p target
	@mvn -B -Dmaven.test.skip -pl zebedee-cms ossindex:audit > target/ossindex-audit-zebedee-cms.log 2>&1; status=$$?; \
	cat target/ossindex-audit-zebedee-cms.log; \
	[ $$status -eq 0 ] && grep -Eiqn $(OSSINDEX_ERRORS) target/ossindex-audit-zebedee-cms.log && \
		{ echo "❌ OSS Index API/auth/network error (CMS) — see target/ossindex-audit-zebedee-cms.log"; exit 1; }; \
	exit $$status

audit-reader:
	@echo "🔍 Running OSS Index audit for zebedee-reader..."
	@mvn -B -Dmaven.test.skip -Dossindex.skip=true -pl zebedee-reader -am install
	@mkdir -p target
	@mvn -B -Dmaven.test.skip -pl zebedee-reader ossindex:audit > target/ossindex-audit-zebedee-reader.log 2>&1; status=$$?; \
	cat target/ossindex-audit-zebedee-reader.log; \
	[ $$status -eq 0 ] && grep -Eiqn $(OSSINDEX_ERRORS) target/ossindex-audit-zebedee-reader.log && \
		{ echo "❌ OSS Index API/auth/network error (Reader) — see target/ossindex-audit-zebedee-reader.log"; exit 1; }; \
	exit $$status

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
