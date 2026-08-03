# HexaGlue v7 reactor build entry points.

.PHONY: help clean compile test format format-check quality checkstyle spotbugs pmd coverage mutation build quick verify ci all install
.DELETE_ON_ERROR:
.DEFAULT_GOAL := help

CYAN := \033[36m
GREEN := \033[32m
RESET := \033[0m

BUILD_DIR := build
REPORTS_DIR := target/quality

QUALITY_PREPARE = mkdir -p $(BUILD_DIR) && rm -rf $(REPORTS_DIR) target/reports
QUALITY_AGGREGATE = echo "$(CYAN)Generating aggregated reports...$(RESET)" \
	&& mvn checkstyle:checkstyle-aggregate pmd:aggregate-pmd jxr:aggregate -DskipTests -q \
	&& mv target/reports $(REPORTS_DIR)

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "$(CYAN)%-14s$(RESET) %s\n", $$1, $$2}'

clean: ## Clean all build artifacts
	mvn clean -q
	rm -rf $(REPORTS_DIR) $(BUILD_DIR)/build.log

compile: ## Compile without tests
	mvn compile -DskipTests -q

test: ## Run all tests
	mvn test

format: ## Apply Palantir Java Format
	mvn com.diffplug.spotless:spotless-maven-plugin:apply -q

format-check: ## Check code formatting
	mvn com.diffplug.spotless:spotless-maven-plugin:check

quality: ## Run all quality checks with aggregated reports
	$(QUALITY_PREPARE)
	mvn verify -Pquality -DskipTests 2>&1 | tee $(BUILD_DIR)/build.log
	$(QUALITY_AGGREGATE)

checkstyle: ## Run Checkstyle with aggregated HTML report
	rm -rf target/reports/checkstyle*
	mvn checkstyle:check checkstyle:checkstyle-aggregate -Pquality -q
	mkdir -p $(REPORTS_DIR) && cp -r target/reports/* $(REPORTS_DIR)/ 2>/dev/null || true

spotbugs: ## Run SpotBugs (XML reports per module)
	mvn spotbugs:check -Pquality

pmd: ## Run PMD with aggregated HTML report
	rm -rf target/reports/pmd*
	mvn pmd:check pmd:aggregate-pmd -Pquality -q
	mkdir -p $(REPORTS_DIR) && cp -r target/reports/* $(REPORTS_DIR)/ 2>/dev/null || true

coverage: ## Run tests and generate JaCoCo reports per module
	mvn verify
	@echo "$(GREEN)Coverage reports: <module>/target/site/jacoco/index.html$(RESET)"

# Single invocation: the testkit resolves hexaglue-model from the reactor, which
# requires the model to be built in the same Maven session as the PIT goal.
mutation: ## Run mutation testing on production modules
	mvn test org.pitest:pitest-maven:mutationCoverage -pl hexaglue-model,hexaglue-frontend,hexaglue-testkit,hexaglue-knowledge,hexaglue-engine

build: clean test ## Clean build with tests

quick: ## Quick rebuild without tests
	mvn clean install -DskipTests -q

verify: ## Tests + quality (incremental, no clean)
	$(QUALITY_PREPARE)
	mvn verify -Pquality 2>&1 | tee $(BUILD_DIR)/build.log
	$(QUALITY_AGGREGATE)

integration: ## Run the plugin integration tests only
	@mvn -pl hexaglue-maven-plugin -am install -DskipTests -q && mvn -pl hexaglue-maven-plugin invoker:install invoker:run

ci: clean verify ## Full CI pipeline

all: ci coverage ## Everything

install: ## Install artifacts into the local repository
	mvn clean install
