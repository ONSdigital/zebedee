.PHONY: clean
clean:
	mvn clean package

.PHONY: local
local: clean
	java -jar target/collection-keyring-secrets-generator-0.0.1-SNAPSHOT.jar local

.PHONY: env
env: clean
	java -jar target/collection-keyring-secrets-generator-0.0.1-SNAPSHOT.jar env
