# GitHub Actions CI/CD Workflows

This project uses GitHub Actions for continuous integration, quality analysis, and Maven Central release automation.

## Workflows

### CI Build & Test (`ci.yml`)

Automated pipeline triggered on:
- Pushes to `main`
- Pull requests targeting `main`
- Manual runs via `workflow_dispatch`

The workflow uses concurrency cancellation so only the latest run per ref keeps executing.

#### Jobs

##### 1. Preflight
- Checks out the repository with full history
- Sets up Java 21 (Temurin)
- Validates the Gradle wrapper

##### 2. Build
- Runs `./gradlew assemble`
- Uploads build artifacts from both:
  - `build/`
  - `codeprompt/build/`
- Uploads build logs on failure

##### 3. Test & Coverage
- Runs `./gradlew test jacocoTestReport`
- Publishes JUnit results from `codeprompt/build/test-results/`
- Uploads:
  - `test-results`
  - `jacoco-report`
  - `test-reports`
  - `sonar-inputs`
- Adds a job summary for the `codeprompt` module

##### 4. Code Quality Checks
- Runs `./gradlew check`
- Uploads reports from root and `codeprompt`
- Non-blocking on pull requests to keep contributor feedback flowing

##### 5. SonarQube Analysis
- Reuses compiled outputs from the test stage when available
- Falls back to rebuilding `codeprompt` classes if needed
- Publishes coverage and binary paths for the `codeprompt` module
- Skips fork PRs because secrets are unavailable there

##### 6. Dependency Submission
- Submits the Gradle dependency graph to GitHub
- Runs on pushes to `main`

##### 7. CI Summary
- Adds a workflow summary to the run
- Posts a PR comment with per-stage status for internal PRs

### Release Draft (`release-draft.yml`)

Automated release draft workflow triggered on:
- Pushes to `main`
- Manual runs via `workflow_dispatch`

What it does:
- Resolves the current project version
- Builds the repository and stages the `codeprompt` Maven publication into `build/staging-deploy/`
- Creates or refreshes a GitHub release draft through JReleaser
- Uploads JReleaser logs as workflow artifacts

### Release to Maven Central (`release.yml`)

Release workflow triggered on:
- Push tags matching `v*`
- Manual runs with a `releaseVersion` input

What it does:
- Resolves the release version from the tag or workflow input
- Verifies the build with `clean test`
- Publishes the staged `codeprompt` module to Maven Central using JReleaser
- Uploads JReleaser logs as workflow artifacts

## Artifacts

### Main artifacts
- `build-folder`: assembled outputs from root and `codeprompt`
- `build-folder-tested`: tested outputs reused by downstream jobs
- `test-results`: raw JUnit XML from `codeprompt/build/test-results/`
- `jacoco-report`: JaCoCo XML coverage report from `codeprompt/build/reports/jacoco/test/`
- `test-reports`: HTML test report from `codeprompt/build/reports/tests/`
- `code-quality-reports`: Gradle reports from root and `codeprompt`
- `jreleaser-draft-logs`: release draft logs and generated properties
- `jreleaser-logs`: release deployment logs and generated properties

### Failure artifacts
- `build-logs`
- `test-build-logs`

## Local verification

Before pushing, these are the closest local equivalents to CI:

```bash
./gradlew assemble
./gradlew test jacocoTestReport
./gradlew check
```

For SonarQube analysis, provide the usual Sonar environment variables or Gradle properties before running:

```bash
./gradlew sonar
```

For release automation smoke tests, the safest local checks are:

```bash
./gradlew :codeprompt:publishMavenJavaPublicationToStagingRepository --dry-run
./gradlew jreleaserDeploy --dry-run
```

## Notes for this repository

- The repository root is a Gradle parent project.
- The main application code and most CI reports live under the `codeprompt` subproject.
- Maven Central publishing is staged from `codeprompt`, while JReleaser runs from the repository root and deploys the shared staging directory at `build/staging-deploy/`.
- Release workflows assume these GitHub Actions secrets are configured: `MODEL_TOKEN`, `SIGNING_KEY`, `SIGNING_PASSWORD`, `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, plus the Sonar secrets used by `ci.yml`.

## Troubleshooting

### Test reports are missing
Check whether the `codeprompt` test task ran and whether files were generated under:
- `codeprompt/build/test-results/`
- `codeprompt/build/reports/tests/`

### SonarQube job fails with missing binaries
The workflow expects compiled classes under `codeprompt/build/classes/`. If artifact reuse fails, the Sonar job rebuilds `:codeprompt:classes` and `:codeprompt:testClasses` as a fallback.

### Release workflow fails before deployment
Check that:
- `build/staging-deploy/` contains the staged publication
- the signing and Maven Central secrets are present
- the requested release version is semver-compatible

### PR comment not created
PR comments are skipped for forked pull requests because repository secrets and write permissions are not available.

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle Testing Guide](https://docs.gradle.org/current/userguide/testing.html)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/)
- [SonarQube Gradle Scanner](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/scanners/sonarscanner-for-gradle/)
- [JReleaser Documentation](https://jreleaser.org/guide/latest/index.html)
