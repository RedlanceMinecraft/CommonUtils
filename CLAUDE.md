# CommonUtils

Shared Java libraries of the Redlance projects (EmotecraftLibrary, enhanced-player-animations), published as
`org.redlance.common-utils:<module>`; the root project is a `java-platform` BOM over all modules.

## Build and test

- Gradle multi-project, modules are listed in `settings.gradle` (`legacy` stays in git but is not included). Java 25
  (`options.release = 25`); configuration cache and parallel builds are on.
- Versions: `mod_version` and library versions in `gradle.properties`, Kotlin and kotlinx in
  `gradle/libs.versions.toml`. Renovate (`renovate.json`) bumps dependencies; the user bumps `mod_version`.
- `./gradlew build`, `./gradlew :<module>:test`, `./gradlew :<module>:javadoc` (run it after touching Javadoc; broken
  `{@link}`s show up there).
- Tests use JUnit 6 with the `log4j` module on the classpath and run in `<module>/build/test-workdir` (HTTP cache and
  `log.txt` land there). System properties given to Gradle (`-Dkey=value`) are forwarded to the test JVM, which is how
  live tests gated by `@EnabledIfSystemProperty` get their tokens. With `-q` the per-test results are only in
  `<module>/build/test-results/test/*.xml`.
- `-x test` also drops explicitly named `:<module>:test` tasks, leaving stale results in place: run tests in an
  invocation of their own.

## Conventions

- Code, Javadoc, comments and commit messages are in English; the user talks in Russian.
- Records with final components and full Javadoc (`@param` for every component); JetBrains `@Nullable`/`@NotNull`,
  `@ApiStatus.Internal` for what other modules must not use.
- Jackson 3: `tools.jackson.*` databind while the annotations stay `com.fasterxml.jackson.annotation.*`; the shared
  mapper is `JacksonMappers.OBJECT_MAPPER` (module `jackson`).
- HTTP is Methanol on `java.net.http`: every service builds its own client with `HttpClients.newBuilder(name)` from
  `requester-base` (one executor per client, no disk cache unless added with `.cache(HttpClients.CACHE)`, only a
  connect timeout, so give requests `.timeout(...)`); the static `Requester` is deprecated. Scheduled executors come from
  `CommonExecutors` (module `common`), small persisted JSON state from `BaseCache` (module `cache`).
- Breaking API changes are fine when they make an API better: the user updates the consumers. Say what they need to
  change.
- Files commented out whole (`/* ... */`, usually still in the old `org.redlance.common.utils.*` packages) are dead
  code and not compiled: don't count them as callers or users of an API.

## Privacy

- When you handle real user data (e-mails, names, payments) in research or live tests, never put it into fixtures,
  test messages, commits or reports; fixtures are synthetic.
- Never print, log or store access tokens.

## Consumers

- EmotecraftLibrary (`../EmotecraftLibrary`) builds against this checkout, e.g.
  `cd ../EmotecraftLibrary && ./gradlew :server:compileKotlin --rerun --include-build ../CommonUtils --offline`.
- enhanced-player-animations (`~/Desktop/git/dima-dencep/enhanced-player-animations`) uses the published 1.3.9.

## Module notes

`requester-base/CLAUDE.md` and `requester-boosty/CLAUDE.md` hold the details of those modules; the other modules have
no notes yet.
