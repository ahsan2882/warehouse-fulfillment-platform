# Warehouse Fulfillment Platform — Jenkins CI/CD Plan

**Status:** CI/CD learning and implementation plan
**Version:** 1.0
**CI server:** Jenkins
**Build:** Maven · Java 21

## 1. Objectives

Jenkins must validate the application and demonstrate practical CI/CD administration concepts.

The project should demonstrate:

- Controller and agent architecture.
- Labeled agents and workload separation.
- Jenkins users, roles, and least-privilege permissions.
- Declarative Pipeline and source-controlled Jenkinsfiles.
- Maven/JDK tool configuration.
- Upstream and downstream jobs.
- Build parameters and triggers.
- Credentials management.
- Parallel execution.
- Test reports and artifact archiving.
- Container image build and publication.
- Gated deployment and smoke tests.
- Reusable pipeline logic when justified.

Do not implement the complete pipeline before the corresponding application stages exist.

## 2. Controller and agent architecture

The controller coordinates jobs, stores configuration, and schedules builds. Build and test workloads should execute on agents.

Recommended logical labels:

| Label              | Purpose                                        |
| ------------------ | ---------------------------------------------- |
| `java`             | Maven compilation, unit tests, packaging       |
| `integration-test` | Testcontainers and integration test workloads  |
| `docker`           | Container image build and registry publication |
| `deployment`       | Controlled test-environment deployment         |

For the local learning setup, agents may initially be separate processes or VMs on the same workstation. This demonstrates Jenkins scheduling and agent configuration, but it does not provide additional physical compute capacity if all agents share the same host.

As the project grows, use distinct VMs or machines where possible. Do not run routine builds on controller executors.

### Agent safety

- Use dedicated, unprivileged OS accounts.
- Restrict agent workspaces.
- Do not give agents unrestricted `sudo`.
- Treat Docker socket access as highly privileged.
- Do not expose production credentials to general build jobs.
- Use labels and job permissions to control where work runs.
- Set sensible executor counts to avoid exhausting workstation resources.

## 3. Jenkins users, roles, and permissions

Demonstrate at least:

| Role             | Intended access                                      |
| ---------------- | ---------------------------------------------------- |
| Administrator    | Jenkins configuration, agents, credentials, security |
| Developer        | Build and inspect permitted project jobs             |
| Release operator | Run approved release/deployment jobs                 |
| Read-only viewer | View job status and reports                          |

Use the authorization strategy supported by the installed Jenkins setup. A role-based authorization plugin may be used if available and appropriate.

Apply least privilege:

- Restrict job configuration to trusted maintainers.
- Restrict credential administration.
- Restrict agent administration.
- Limit who can approve deployment stages.
- Avoid giving every user Overall/Administer.

A pipeline must not be able to print credentials to logs.

## 4. Jenkins job topology

Start with a small topology that makes upstream/downstream behavior visible.

```text
                    SCM Change
                        |
                        v
                  warehouse-ci
                        |
          +-------------+-------------+
          |             |             |
          v             v             v
      catalog-ci   inventory-ci    order-ci
          |             |             |
          +-------------+-------------+
                        |
                        v
              warehouse-integration
                        |
                        v
                 warehouse-release
                 (manual approval)
```

Suggested jobs:

| Job                     | Responsibility                                         |
| ----------------------- | ------------------------------------------------------ |
| `warehouse-ci`          | Checkout, validate repository, coordinate service jobs |
| `catalog-ci`            | Build/test Catalog Service                             |
| `inventory-ci`          | Build/test Inventory Service                           |
| `order-ci`              | Build/test Order Service                               |
| `warehouse-integration` | Run cross-service workflow tests                       |
| `warehouse-release`     | Publish approved artifacts and deploy                  |

The topology is a learning exercise. Compare it with a single multibranch pipeline after the basics are working. Avoid maintaining duplicated build logic indefinitely.

### Upstream/downstream demonstration

- A successful `warehouse-ci` run triggers service jobs.
- The integration job runs only when required service jobs succeed.
- The release job requires successful validation and an explicit approval initially.
- Pass the source revision/build reference between jobs.
- Do not let downstream jobs silently rebuild a different commit.

## 5. Pipeline stages

Target pipeline:

```text
Checkout
   |
   v
Compile
   |
   v
Unit Tests
   |
   v
Static / Dependency Checks
   |
   v
Integration Tests
   |
   v
Package JAR
   |
   v
Archive / Publish Artifact
   |
   v
Build Container Image
   |
   v
Deploy to Test
   |
   v
Smoke / Contract Tests
```

Introduce stages incrementally:

| Stage                                 | First useful milestone |
| ------------------------------------- | ---------------------- |
| Checkout and compile                  | Milestone 0            |
| Unit tests                            | Milestone 0/1          |
| Test report publishing                | Milestone 1            |
| PostgreSQL integration tests          | Milestone 2            |
| Parallel service builds               | Milestone 3            |
| Container image build                 | Milestone 2/4          |
| Test deployment and smoke tests       | Milestone 4            |
| Registry publication and release gate | Milestone 6            |

## 6. Declarative Pipeline conventions

Keep a `Jenkinsfile` in source control.

Use Declarative Pipeline features where useful:

- `pipeline`
- `agent`
- `stages`
- `stage`
- `environment`
- `parameters`
- `options`
- `when`
- `parallel`
- `post`
- `input`
- `tools`

Keep the pipeline readable. Move repeated logic into a Shared Library only after more than one pipeline has demonstrated a stable reuse requirement.

Example starter pipeline:

```groovy
pipeline {
    agent { label 'java' }

    tools {
        jdk 'jdk-21'
        maven 'maven-3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn -B -DskipTests compile'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B package'
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: '**/target/*.jar',
                             fingerprint: true,
                             allowEmptyArchive: true
        }
        failure {
            echo 'Build failed. Review the stage output and test reports.'
        }
        always {
            cleanWs()
        }
    }
}
```

This is a teaching baseline. Confirm that the named JDK/Maven installations and plugins exist in the actual Jenkins environment before using it. For a multi-module repository, refine test and artifact paths to avoid archiving unrelated files.

## 7. Maven integration

Use a controlled JDK 21 and Maven version. Configure tools in Jenkins or use a pinned build image.

Common commands:

```bash
mvn -B clean verify

mvn -B -pl services/catalog-service -am verify

mvn -B -pl services/inventory-service -am verify
```

Use `verify` as the normal CI quality boundary because it executes the relevant preceding Maven lifecycle phases and verification checks.

Publish:

- Surefire unit test reports.
- Failsafe integration test reports, when configured.
- JAR artifacts.
- Build fingerprints or immutable version metadata.

Do not make a build pass by skipping failing tests.

## 8. Parallel execution

Once services are independently buildable, run independent builds in parallel.

Example concept:

```groovy
stage('Service Builds') {
    parallel {
        stage('Catalog') {
            agent { label 'java' }
            steps {
                sh 'mvn -B -pl services/catalog-service -am verify'
            }
        }
        stage('Inventory') {
            agent { label 'java' }
            steps {
                sh 'mvn -B -pl services/inventory-service -am verify'
            }
        }
    }
}
```

Avoid running resource-heavy Testcontainers workloads concurrently if they exhaust local CPU, memory, disk, or Docker capacity. Use separate labels or throttle concurrency where needed.

## 9. Credentials and secrets

Use Jenkins Credentials for:

- Private Git repository credentials, if needed.
- Container registry credentials.
- Deployment credentials.
- Service integration credentials used by CI.

Rules:

- Bind a credential only in the stage that needs it.
- Use credential IDs, not literal secret values.
- Never echo tokens or passwords.
- Do not write secrets into archived artifacts.
- Do not commit `.env` files containing real credentials.
- Prefer short-lived or narrowly scoped credentials.
- Keep deployment credentials unavailable to untrusted pull-request builds.

## 10. Build triggers and parameters

Demonstrate:

- SCM webhook trigger where available.
- Poll SCM as a fallback learning exercise.
- Manual build.
- Parameterized release/deployment job.

Useful parameters may include:

- Target environment (`dev`, `test`).
- Artifact version or approved build number.
- Whether to run a costly optional test suite.

Do not allow user parameters to become arbitrary shell commands or unvalidated deployment targets.

## 11. Artifact management

- Archive test reports and JARs.
- Fingerprint artifacts when useful.
- Use immutable version identifiers.
- Pass exact artifact references between jobs.
- Avoid rebuilding source in a release job if a validated artifact already exists.
- Later publish Docker images with explicit version tags.
- Avoid relying on `latest` as the only release identifier.

A release should promote a previously validated artifact rather than create a different artifact from an unverified source revision.

## 12. Container image pipeline

Introduce Docker after service packaging and local execution are stable.

Target stages:

1. Build service JAR.
2. Build a minimal runtime image.
3. Scan image/dependencies using an approved tool.
4. Tag with commit/build version.
5. Publish to registry using Jenkins Credentials.
6. Deploy to a test environment.
7. Run health and smoke tests.

Use non-root containers where practical. Do not place credentials in Docker build arguments or image layers.

## 13. Deployment and approval

Initially deploy only to a disposable local/test environment.

A release pipeline should:

- Require a successful validation build.
- Use an explicit target environment.
- Require approval before deployment.
- Record the source commit and artifact version.
- Perform health checks after deployment.
- Run smoke tests.
- Fail visibly if deployment or verification fails.
- Document rollback to the previous known-good artifact.

Do not connect the learning pipeline to production systems.

## 14. Quality gates

A build must fail on:

- Compilation errors.
- Failed unit tests.
- Failed required integration tests.
- Failed configured static/format checks.
- Invalid packaging.
- Failed required smoke tests.

Coverage thresholds can be introduced after meaningful test coverage exists. Coverage is a signal, not a substitute for tests of business invariants.

## 15. Shared Library

Do not create a Shared Library at the beginning.

Introduce it when multiple pipelines repeat stable logic, such as:

- Maven verification.
- Test report publishing.
- Artifact archiving.
- Container image publication.

Keep application-specific behavior in the service pipeline. Version and review Shared Library changes because they can affect many jobs.

## 16. Suggested learning sequence

1. Run one Freestyle job to understand job configuration and workspaces.
2. Create a Declarative Pipeline for Catalog Service.
3. Configure JDK and Maven tools.
4. Publish JUnit reports and archive JARs.
5. Add a second agent and target it with a label.
6. Configure developer and administrator access.
7. Add an upstream/downstream job chain.
8. Add parallel service builds.
9. Store and consume a credential safely.
10. Add Testcontainers integration tests.
11. Build and publish a container image.
12. Add approval-gated test deployment and smoke tests.
13. Compare multi-job orchestration with a Multibranch Pipeline.
14. Introduce a Shared Library only if duplication justifies it.

## 17. Operational checklist

Before enabling a job:

- Confirm its agent label and executor capacity.
- Confirm its workspace and cleanup behavior.
- Confirm tool versions.
- Confirm required credentials exist and are scoped correctly.
- Confirm no privileged host access is required.
- Confirm test reports are published.
- Confirm artifacts are traceable to a commit.
- Confirm failure is visible and does not trigger deployment.
- Confirm deployment targets are non-production unless separately approved.
