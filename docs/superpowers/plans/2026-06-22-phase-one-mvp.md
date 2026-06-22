# Phase One MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first runnable `springApiLens` MVP: a local Spring Boot service that scans one Spring Boot Git repository, stores endpoint/code/Git/SQL evidence in SQLite, and exposes API endpoints for a future React workbench.

**Architecture:** Create a Maven multi-module project with `spring-api-lens-core` for scanner/domain logic and `spring-api-lens-app` for the local HTTP API. The scanner uses lightweight JDK-based parsing for the first runnable MVP, XML parsing for MyBatis SQL, local Git commands for ownership evidence, and a persistence boundary that can be backed by SQLite once the driver is available.

**Tech Stack:** Java 21, Maven, JUnit 5, AssertJ, Spring Boot, JDK file/XML/process APIs.

---

## File Structure

Create these top-level files:

- `pom.xml` defines a Java 21 Maven reactor with `spring-api-lens-core` and `spring-api-lens-app`.
- `.gitignore` adds Java, Maven, SQLite, Node, IDE, and worktree ignores.
- `README.md` is expanded with build and run commands.

Create `spring-api-lens-core`:

- `spring-api-lens-core/pom.xml` declares JUnit 5 and AssertJ for tests; scanner code uses JDK APIs in the first MVP.
- `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/*.java` contains immutable scanner records.
- `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/*.java` contains repository validation, file discovery, Java parsing, endpoint extraction, call graph extraction, MyBatis parsing, Git ownership, and scan orchestration.
- `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/store/*.java` contains SQLite schema and persistence.
- `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/**` contains unit and integration tests with generated fixture repositories.

Create `spring-api-lens-app`:

- `spring-api-lens-app/pom.xml` declares Spring Boot Web, core module dependency, and test dependencies.
- `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/SpringApiLensApplication.java` boots the local service.
- `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/*.java` exposes scan and query APIs.
- `spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api/*.java` contains MVC tests.

## Task 1: Maven Reactor And Baseline Test Harness

**Files:**

- Modify: `pom.xml`
- Modify: `.gitignore`
- Modify: `README.md`
- Create: `spring-api-lens-core/pom.xml`
- Create: `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/BuildSmokeTest.java`
- Create: `spring-api-lens-app/pom.xml`
- Create: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/SpringApiLensApplication.java`
- Create: `spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/SpringApiLensApplicationTest.java`

- [ ] **Step 1: Write failing core smoke test**

Create `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/BuildSmokeTest.java`:

```java
package com.qiqua.springapilens.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuildSmokeTest {
    @Test
    void exposesProjectName() {
        assertThat(ProjectInfo.name()).isEqualTo("springApiLens");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -pl spring-api-lens-core test
```

Expected: FAIL because no Maven reactor/module or `ProjectInfo` class exists.

- [ ] **Step 3: Add Maven reactor and minimal core implementation**

Create top-level `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.qiqua</groupId>
    <artifactId>spring-api-lens</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>spring-api-lens-core</module>
        <module>spring-api-lens-app</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.13.3</junit.version>
        <assertj.version>3.27.3</assertj.version>
        <javaparser.version>3.27.0</javaparser.version>
        <sqlite.version>3.50.1.0</sqlite.version>
        <jackson.version>2.19.1</jackson.version>
        <spring-boot.version>3.5.3</spring-boot.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>3.5.3</version>
                    <configuration>
                        <useModulePath>false</useModulePath>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

Create `spring-api-lens-core/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.qiqua</groupId>
        <artifactId>spring-api-lens</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>spring-api-lens-core</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.github.javaparser</groupId>
            <artifactId>javaparser-core</artifactId>
            <version>${javaparser.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>${sqlite.version}</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/ProjectInfo.java`:

```java
package com.qiqua.springapilens.core;

public final class ProjectInfo {
    private ProjectInfo() {
    }

    public static String name() {
        return "springApiLens";
    }
}
```

Create `spring-api-lens-app/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.qiqua</groupId>
        <artifactId>spring-api-lens</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>spring-api-lens-app</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.qiqua</groupId>
            <artifactId>spring-api-lens-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

Create `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/SpringApiLensApplication.java`:

```java
package com.qiqua.springapilens.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringApiLensApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringApiLensApplication.class, args);
    }
}
```

Create `spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/SpringApiLensApplicationTest.java`:

```java
package com.qiqua.springapilens.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringApiLensApplicationTest {
    @Test
    void contextLoads() {
    }
}
```

Update `.gitignore`:

```gitignore
.worktrees/

target/
*.db
*.sqlite
*.sqlite3

.idea/
.vscode/
*.iml

node_modules/
dist/
```

Update `README.md` with:

```markdown
## Development

```powershell
mvn test
```

Run the local API:

```powershell
mvn -pl spring-api-lens-app spring-boot:run
```
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
mvn test
```

Expected: PASS for both modules.

- [ ] **Step 5: Commit**

Run:

```powershell
git add -- .gitignore README.md pom.xml spring-api-lens-core spring-api-lens-app
git commit -m "chore: scaffold Maven modules"
```

## Task 2: Repository Validation And File Discovery

**Files:**

- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/RepositoryInfo.java`
- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/RepositoryValidator.java`
- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/SourceFileDiscoverer.java`
- Create: `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/RepositoryValidatorTest.java`
- Create: `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/SourceFileDiscovererTest.java`

- [ ] **Step 1: Write failing repository validation test**

Create `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/RepositoryValidatorTest.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.RepositoryInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryValidatorTest {
    @TempDir
    Path tempDir;

    @Test
    void rejectsDirectoryWithoutGitMetadata() {
        RepositoryValidator validator = new RepositoryValidator();

        assertThatThrownBy(() -> validator.validate(tempDir))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(".git");
    }

    @Test
    void acceptsRepositoryRootAndReadsBasicInfo() throws IOException {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.writeString(tempDir.resolve(".git/HEAD"), "ref: refs/heads/main\n");
        Files.createDirectories(tempDir.resolve(".git/refs/heads"));
        Files.writeString(tempDir.resolve(".git/refs/heads/main"), "abc123\n");

        RepositoryInfo info = new RepositoryValidator().validate(tempDir);

        assertThat(info.rootPath()).isEqualTo(tempDir.toAbsolutePath().normalize());
        assertThat(info.repoName()).isEqualTo(tempDir.getFileName().toString());
        assertThat(info.currentBranch()).isEqualTo("main");
        assertThat(info.headCommit()).isEqualTo("abc123");
    }
}
```

- [ ] **Step 2: Run validation test to verify it fails**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=RepositoryValidatorTest test
```

Expected: FAIL because `RepositoryValidator` and `RepositoryInfo` do not exist.

- [ ] **Step 3: Implement repository validation**

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/RepositoryInfo.java`:

```java
package com.qiqua.springapilens.core.model;

import java.nio.file.Path;

public record RepositoryInfo(
    Path rootPath,
    String repoName,
    String currentBranch,
    String headCommit,
    boolean hasUncommittedChanges
) {
}
```

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/RepositoryValidator.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.RepositoryInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RepositoryValidator {
    public RepositoryInfo validate(Path rootPath) {
        Path normalized = rootPath.toAbsolutePath().normalize();
        Path gitDir = normalized.resolve(".git");
        if (!Files.isDirectory(gitDir)) {
            throw new IllegalArgumentException("Repository root must contain .git: " + normalized);
        }
        String branch = readBranch(gitDir);
        String headCommit = readHeadCommit(gitDir, branch);
        return new RepositoryInfo(
            normalized,
            normalized.getFileName().toString(),
            branch,
            headCommit,
            false
        );
    }

    private String readBranch(Path gitDir) {
        Path head = gitDir.resolve("HEAD");
        try {
            String content = Files.readString(head).trim();
            if (content.startsWith("ref: refs/heads/")) {
                return content.substring("ref: refs/heads/".length());
            }
            return "DETACHED";
        } catch (IOException e) {
            return "UNKNOWN";
        }
    }

    private String readHeadCommit(Path gitDir, String branch) {
        if ("UNKNOWN".equals(branch) || "DETACHED".equals(branch)) {
            return "UNKNOWN";
        }
        Path ref = gitDir.resolve("refs").resolve("heads").resolve(branch);
        try {
            return Files.readString(ref).trim();
        } catch (IOException e) {
            return "UNKNOWN";
        }
    }
}
```

- [ ] **Step 4: Run validation test to verify it passes**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=RepositoryValidatorTest test
```

Expected: PASS.

- [ ] **Step 5: Write failing file discovery test**

Create `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/SourceFileDiscovererTest.java`:

```java
package com.qiqua.springapilens.core.scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceFileDiscovererTest {
    @TempDir
    Path repoRoot;

    @Test
    void discoversJavaAndResourceFilesAndSkipsBuildOutput() throws IOException {
        write("src/main/java/com/example/OrderController.java");
        write("src/main/resources/mapper/OrderMapper.xml");
        write("src/main/resources/application.yml");
        write("target/generated/Generated.java");
        write("build/tmp/Other.java");

        List<Path> files = new SourceFileDiscoverer().discover(repoRoot);

        assertThat(files)
            .extracting(path -> repoRoot.relativize(path).toString().replace('\\', '/'))
            .containsExactly(
                "src/main/java/com/example/OrderController.java",
                "src/main/resources/application.yml",
                "src/main/resources/mapper/OrderMapper.xml"
            );
    }

    private void write(String relativePath) throws IOException {
        Path path = repoRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "");
    }
}
```

- [ ] **Step 6: Run file discovery test to verify it fails**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=SourceFileDiscovererTest test
```

Expected: FAIL because `SourceFileDiscoverer` does not exist.

- [ ] **Step 7: Implement file discovery**

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/SourceFileDiscoverer.java`:

```java
package com.qiqua.springapilens.core.scanner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class SourceFileDiscoverer {
    public List<Path> discover(Path repoRoot) {
        Path normalized = repoRoot.toAbsolutePath().normalize();
        try (var stream = Files.walk(normalized)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> isSupported(normalized, path))
                .sorted(Comparator.comparing(path -> normalized.relativize(path).toString()))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to discover source files under " + normalized, e);
        }
    }

    private boolean isSupported(Path root, Path file) {
        String relative = root.relativize(file).toString().replace('\\', '/');
        if (relative.startsWith("target/")
            || relative.startsWith("build/")
            || relative.startsWith(".idea/")
            || relative.startsWith(".gradle/")
            || relative.startsWith("node_modules/")) {
            return false;
        }
        return relative.startsWith("src/main/java/") && relative.endsWith(".java")
            || relative.startsWith("src/main/resources/") && (
                relative.endsWith(".xml")
                    || relative.endsWith(".yml")
                    || relative.endsWith(".yaml")
                    || relative.endsWith(".properties")
            );
    }
}
```

- [ ] **Step 8: Run core tests**

Run:

```powershell
mvn -pl spring-api-lens-core test
```

Expected: PASS.

- [ ] **Step 9: Commit**

Run:

```powershell
git add -- spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/RepositoryInfo.java spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/RepositoryValidator.java spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/SourceFileDiscoverer.java spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/RepositoryValidatorTest.java spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/SourceFileDiscovererTest.java
git commit -m "feat: validate repositories and discover source files"
```

## Task 3: Spring Endpoint Extraction

**Files:**

- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/ApiEndpoint.java`
- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/SpringEndpointExtractor.java`
- Create: `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/SpringEndpointExtractorTest.java`

- [ ] **Step 1: Write failing endpoint extraction test**

Create `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/SpringEndpointExtractorTest.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringEndpointExtractorTest {
    @TempDir
    Path repoRoot;

    @Test
    void extractsEndpointFromControllerClassAndMethodMappings() throws IOException {
        Path source = writeJava("""
            package com.example.order;

            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("/api/order")
            class OrderController {
                @PostMapping("/create")
                ApiResult<OrderVO> createOrder(@RequestBody CreateOrderRequest request) {
                    return null;
                }
            }
            """);

        List<ApiEndpoint> endpoints = new SpringEndpointExtractor().extract(repoRoot, List.of(source));

        assertThat(endpoints).singleElement().satisfies(endpoint -> {
            assertThat(endpoint.className()).isEqualTo("OrderController");
            assertThat(endpoint.methodName()).isEqualTo("createOrder");
            assertThat(endpoint.httpMethod()).isEqualTo("POST");
            assertThat(endpoint.path()).isEqualTo("/api/order/create");
            assertThat(endpoint.requestBodyType()).isEqualTo("CreateOrderRequest");
            assertThat(endpoint.responseType()).isEqualTo("ApiResult<OrderVO>");
            assertThat(endpoint.relativeFile()).isEqualTo("src/main/java/com/example/order/OrderController.java");
            assertThat(endpoint.lineStart()).isLessThan(endpoint.lineEnd());
        });
    }

    private Path writeJava(String content) throws IOException {
        Path source = repoRoot.resolve("src/main/java/com/example/order/OrderController.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, content);
        return source;
    }
}
```

- [ ] **Step 2: Run endpoint test to verify it fails**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=SpringEndpointExtractorTest test
```

Expected: FAIL because `ApiEndpoint` and `SpringEndpointExtractor` do not exist.

- [ ] **Step 3: Implement endpoint model and extractor**

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/ApiEndpoint.java`:

```java
package com.qiqua.springapilens.core.model;

public record ApiEndpoint(
    String relativeFile,
    String className,
    String methodName,
    String httpMethod,
    String path,
    String requestParamsJson,
    String requestBodyType,
    String responseType,
    int lineStart,
    int lineEnd
) {
}
```

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/SpringEndpointExtractor.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.qiqua.springapilens.core.model.ApiEndpoint;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpringEndpointExtractor {
    public List<ApiEndpoint> extract(Path repoRoot, List<Path> javaFiles) {
        List<ApiEndpoint> endpoints = new ArrayList<>();
        for (Path javaFile : javaFiles) {
            if (!javaFile.toString().endsWith(".java")) {
                continue;
            }
            endpoints.addAll(extractFromFile(repoRoot.toAbsolutePath().normalize(), javaFile.toAbsolutePath().normalize()));
        }
        return endpoints;
    }

    private List<ApiEndpoint> extractFromFile(Path repoRoot, Path javaFile) {
        try {
            CompilationUnit unit = StaticJavaParser.parse(javaFile);
            List<ApiEndpoint> endpoints = new ArrayList<>();
            for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                if (!hasAnnotation(type.getAnnotations(), "RestController") && !hasAnnotation(type.getAnnotations(), "Controller")) {
                    continue;
                }
                String classPath = annotationPath(type.getAnnotations(), "RequestMapping").orElse("");
                for (MethodDeclaration method : type.getMethods()) {
                    Mapping mapping = methodMapping(method.getAnnotations()).orElse(null);
                    if (mapping == null) {
                        continue;
                    }
                    String requestBodyType = method.getParameters().stream()
                        .filter(parameter -> hasAnnotation(parameter.getAnnotations(), "RequestBody"))
                        .map(parameter -> parameter.getType().asString())
                        .findFirst()
                        .orElse("");
                    endpoints.add(new ApiEndpoint(
                        repoRoot.relativize(javaFile).toString().replace('\\', '/'),
                        type.getNameAsString(),
                        method.getNameAsString(),
                        mapping.httpMethod(),
                        joinPaths(classPath, mapping.path()),
                        "[]",
                        requestBodyType,
                        method.getType().asString(),
                        method.getBegin().map(position -> position.line).orElse(0),
                        method.getEnd().map(position -> position.line).orElse(0)
                    ));
                }
            }
            return endpoints;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse Java file " + javaFile, e);
        }
    }

    private Optional<Mapping> methodMapping(List<AnnotationExpr> annotations) {
        for (AnnotationExpr annotation : annotations) {
            String name = annotation.getNameAsString();
            if ("GetMapping".equals(name)) {
                return Optional.of(new Mapping("GET", annotationPath(annotation).orElse("")));
            }
            if ("PostMapping".equals(name)) {
                return Optional.of(new Mapping("POST", annotationPath(annotation).orElse("")));
            }
            if ("PutMapping".equals(name)) {
                return Optional.of(new Mapping("PUT", annotationPath(annotation).orElse("")));
            }
            if ("DeleteMapping".equals(name)) {
                return Optional.of(new Mapping("DELETE", annotationPath(annotation).orElse("")));
            }
            if ("RequestMapping".equals(name)) {
                return Optional.of(new Mapping("REQUEST", annotationPath(annotation).orElse("")));
            }
        }
        return Optional.empty();
    }

    private boolean hasAnnotation(List<AnnotationExpr> annotations, String annotationName) {
        return annotations.stream().anyMatch(annotation -> annotation.getNameAsString().equals(annotationName));
    }

    private Optional<String> annotationPath(List<AnnotationExpr> annotations, String annotationName) {
        return annotations.stream()
            .filter(annotation -> annotation.getNameAsString().equals(annotationName))
            .findFirst()
            .flatMap(this::annotationPath);
    }

    private Optional<String> annotationPath(AnnotationExpr annotation) {
        if (annotation instanceof SingleMemberAnnotationExpr singleMember) {
            return stringValue(singleMember.getMemberValue());
        }
        if (annotation instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) {
                if ("value".equals(pair.getNameAsString()) || "path".equals(pair.getNameAsString())) {
                    return stringValue(pair.getValue());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> stringValue(Expression expression) {
        if (expression.isStringLiteralExpr()) {
            return Optional.of(expression.asStringLiteralExpr().asString());
        }
        return Optional.empty();
    }

    private String joinPaths(String classPath, String methodPath) {
        String joined = ("/" + classPath + "/" + methodPath).replaceAll("/+", "/");
        if (joined.length() > 1 && joined.endsWith("/")) {
            return joined.substring(0, joined.length() - 1);
        }
        return joined;
    }

    private record Mapping(String httpMethod, String path) {
    }
}
```

- [ ] **Step 4: Run endpoint test**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=SpringEndpointExtractorTest test
```

Expected: PASS.

- [ ] **Step 5: Run core tests and commit**

Run:

```powershell
mvn -pl spring-api-lens-core test
git add -- spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/ApiEndpoint.java spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/SpringEndpointExtractor.java spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/SpringEndpointExtractorTest.java
git commit -m "feat: extract Spring endpoints"
```

## Task 4: Deterministic Service And Mapper Call Edges

**Files:**

- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/CodeSymbol.java`
- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/CallEdge.java`
- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/JavaSymbolExtractor.java`
- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/CallEdgeExtractor.java`
- Create: `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/CallEdgeExtractorTest.java`

- [ ] **Step 1: Write failing call edge test**

Create `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/CallEdgeExtractorTest.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CallEdgeExtractorTest {
    @TempDir
    Path repoRoot;

    @Test
    void extractsControllerToServiceAndServiceToMapperEdges() throws IOException {
        Path controller = write("src/main/java/com/example/OrderController.java", """
            package com.example;
            class OrderController {
                private final OrderService orderService;
                OrderController(OrderService orderService) { this.orderService = orderService; }
                void create() { orderService.createOrder(); }
            }
            """);
        Path service = write("src/main/java/com/example/OrderService.java", """
            package com.example;
            class OrderService {
                private final OrderMapper orderMapper;
                OrderService(OrderMapper orderMapper) { this.orderMapper = orderMapper; }
                void createOrder() { orderMapper.insertOrder(); }
            }
            """);
        Path mapper = write("src/main/java/com/example/OrderMapper.java", """
            package com.example;
            interface OrderMapper {
                void insertOrder();
            }
            """);

        List<Path> files = List.of(controller, service, mapper);
        List<CodeSymbol> symbols = new JavaSymbolExtractor().extract(repoRoot, files);
        List<CallEdge> edges = new CallEdgeExtractor().extract(repoRoot, files, symbols);

        assertThat(edges)
            .extracting(edge -> edge.fromSignature() + " -> " + edge.toSignature())
            .contains(
                "OrderController.create() -> OrderService.createOrder()",
                "OrderService.createOrder() -> OrderMapper.insertOrder()"
            );
    }

    private Path write(String relativePath, String content) throws IOException {
        Path path = repoRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }
}
```

- [ ] **Step 2: Run call edge test to verify it fails**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=CallEdgeExtractorTest test
```

Expected: FAIL because symbol and call edge classes do not exist.

- [ ] **Step 3: Implement symbol and call edge extraction**

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/CodeSymbol.java`:

```java
package com.qiqua.springapilens.core.model;

public record CodeSymbol(
    String relativeFile,
    String symbolType,
    String className,
    String methodName,
    String signature,
    int lineStart,
    int lineEnd
) {
}
```

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/CallEdge.java`:

```java
package com.qiqua.springapilens.core.model;

public record CallEdge(
    String fromSignature,
    String toSignature,
    double confidence,
    String evidence
) {
}
```

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/JavaSymbolExtractor.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.qiqua.springapilens.core.model.CodeSymbol;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JavaSymbolExtractor {
    public List<CodeSymbol> extract(Path repoRoot, List<Path> javaFiles) {
        Path root = repoRoot.toAbsolutePath().normalize();
        List<CodeSymbol> symbols = new ArrayList<>();
        for (Path javaFile : javaFiles) {
            if (!javaFile.toString().endsWith(".java")) {
                continue;
            }
            try {
                CompilationUnit unit = StaticJavaParser.parse(javaFile);
                for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                    for (MethodDeclaration method : type.getMethods()) {
                        String signature = type.getNameAsString() + "." + method.getNameAsString() + "()";
                        symbols.add(new CodeSymbol(
                            root.relativize(javaFile.toAbsolutePath().normalize()).toString().replace('\\', '/'),
                            "METHOD",
                            type.getNameAsString(),
                            method.getNameAsString(),
                            signature,
                            method.getBegin().map(position -> position.line).orElse(0),
                            method.getEnd().map(position -> position.line).orElse(0)
                        ));
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to parse Java file " + javaFile, e);
            }
        }
        return symbols;
    }
}
```

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/CallEdgeExtractor.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CallEdgeExtractor {
    public List<CallEdge> extract(Path repoRoot, List<Path> javaFiles, List<CodeSymbol> symbols) {
        Map<String, CodeSymbol> byClassAndMethod = new HashMap<>();
        for (CodeSymbol symbol : symbols) {
            byClassAndMethod.put(symbol.className() + "." + symbol.methodName(), symbol);
        }

        List<CallEdge> edges = new ArrayList<>();
        for (Path javaFile : javaFiles) {
            if (!javaFile.toString().endsWith(".java")) {
                continue;
            }
            edges.addAll(extractFromFile(javaFile, byClassAndMethod));
        }
        return edges;
    }

    private List<CallEdge> extractFromFile(Path javaFile, Map<String, CodeSymbol> byClassAndMethod) {
        try {
            CompilationUnit unit = StaticJavaParser.parse(javaFile);
            List<CallEdge> edges = new ArrayList<>();
            for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                Map<String, String> variableTypes = fieldTypes(type);
                for (MethodDeclaration method : type.getMethods()) {
                    String fromSignature = type.getNameAsString() + "." + method.getNameAsString() + "()";
                    for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                        Optional<String> scope = call.getScope().map(Object::toString);
                        if (scope.isEmpty()) {
                            continue;
                        }
                        String targetClass = variableTypes.get(scope.get());
                        if (targetClass == null) {
                            continue;
                        }
                        CodeSymbol target = byClassAndMethod.get(targetClass + "." + call.getNameAsString());
                        if (target != null) {
                            edges.add(new CallEdge(
                                fromSignature,
                                target.signature(),
                                0.95,
                                scope.get() + "." + call.getNameAsString()
                            ));
                        }
                    }
                }
            }
            return edges;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse Java file " + javaFile, e);
        }
    }

    private Map<String, String> fieldTypes(ClassOrInterfaceDeclaration type) {
        Map<String, String> variableTypes = new HashMap<>();
        for (FieldDeclaration field : type.getFields()) {
            field.getVariables().forEach(variable ->
                variableTypes.put(variable.getNameAsString(), variable.getType().asString()));
        }
        type.getConstructors().forEach(constructor ->
            constructor.getParameters().forEach(parameter ->
                variableTypes.put(decapitalize(parameter.getType().asString()), parameter.getType().asString())));
        return variableTypes;
    }

    private String decapitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
```

- [ ] **Step 4: Run call edge test**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=CallEdgeExtractorTest test
```

Expected: PASS.

- [ ] **Step 5: Run core tests and commit**

Run:

```powershell
mvn -pl spring-api-lens-core test
git add -- spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/CodeSymbol.java spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/CallEdge.java spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/JavaSymbolExtractor.java spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/CallEdgeExtractor.java spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/CallEdgeExtractorTest.java
git commit -m "feat: extract deterministic call edges"
```

## Task 5: MyBatis SQL And Table Extraction

**Files:**

- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/SqlFragment.java`
- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/MyBatisSqlExtractor.java`
- Create: `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/MyBatisSqlExtractorTest.java`

- [ ] **Step 1: Write failing MyBatis extraction test**

Create `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/MyBatisSqlExtractorTest.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.SqlFragment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MyBatisSqlExtractorTest {
    @TempDir
    Path repoRoot;

    @Test
    void extractsSqlOperationAndTablesFromMapperXml() throws IOException {
        Path xml = repoRoot.resolve("src/main/resources/mapper/OrderMapper.xml");
        Files.createDirectories(xml.getParent());
        Files.writeString(xml, """
            <?xml version="1.0" encoding="UTF-8" ?>
            <mapper namespace="com.example.OrderMapper">
              <insert id="insertOrder">
                insert into order_main (id, user_id) values (#{id}, #{userId})
              </insert>
              <select id="findOrder">
                select * from order_main o join user_account u on o.user_id = u.id
              </select>
            </mapper>
            """);

        List<SqlFragment> fragments = new MyBatisSqlExtractor().extract(repoRoot, List.of(xml));

        assertThat(fragments)
            .extracting(SqlFragment::mapperMethod)
            .containsExactly("insertOrder", "findOrder");
        assertThat(fragments.get(0).operationType()).isEqualTo("insert");
        assertThat(fragments.get(0).tables()).containsExactly("order_main");
        assertThat(fragments.get(1).operationType()).isEqualTo("select");
        assertThat(fragments.get(1).tables()).containsExactly("order_main", "user_account");
    }
}
```

- [ ] **Step 2: Run MyBatis test to verify it fails**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=MyBatisSqlExtractorTest test
```

Expected: FAIL because SQL classes do not exist.

- [ ] **Step 3: Implement SQL fragment model and extractor**

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/SqlFragment.java`:

```java
package com.qiqua.springapilens.core.model;

import java.util.List;

public record SqlFragment(
    String relativeFile,
    String mapperNamespace,
    String mapperMethod,
    String sqlText,
    List<String> tables,
    String operationType
) {
}
```

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/MyBatisSqlExtractor.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.SqlFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyBatisSqlExtractor {
    private static final Set<String> SQL_TAGS = Set.of("select", "insert", "update", "delete");
    private static final Pattern TABLE_PATTERN = Pattern.compile(
        "\\b(from|join|into|update)\\s+([a-zA-Z_][a-zA-Z0-9_.$]*)",
        Pattern.CASE_INSENSITIVE
    );

    public List<SqlFragment> extract(Path repoRoot, List<Path> xmlFiles) {
        List<SqlFragment> fragments = new ArrayList<>();
        Path root = repoRoot.toAbsolutePath().normalize();
        for (Path xmlFile : xmlFiles) {
            if (!xmlFile.toString().endsWith(".xml")) {
                continue;
            }
            fragments.addAll(extractFile(root, xmlFile.toAbsolutePath().normalize()));
        }
        return fragments;
    }

    private List<SqlFragment> extractFile(Path repoRoot, Path xmlFile) {
        try {
            String xml = Files.readString(xmlFile);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setExpandEntityReferences(false);
            Element root = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)))
                .getDocumentElement();
            if (!"mapper".equals(root.getTagName())) {
                return List.of();
            }
            String namespace = root.getAttribute("namespace");
            List<SqlFragment> fragments = new ArrayList<>();
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node instanceof Element element && SQL_TAGS.contains(element.getTagName())) {
                    String sql = element.getTextContent().replaceAll("\\s+", " ").trim();
                    fragments.add(new SqlFragment(
                        repoRoot.relativize(xmlFile).toString().replace('\\', '/'),
                        namespace,
                        element.getAttribute("id"),
                        sql,
                        extractTables(sql),
                        element.getTagName().toLowerCase(Locale.ROOT)
                    ));
                }
            }
            return fragments;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse MyBatis XML " + xmlFile, e);
        }
    }

    private List<String> extractTables(String sql) {
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        LinkedHashSet<String> tables = new LinkedHashSet<>();
        while (matcher.find()) {
            tables.add(matcher.group(2));
        }
        return new ArrayList<>(tables);
    }
}
```

- [ ] **Step 4: Run MyBatis test**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=MyBatisSqlExtractorTest test
```

Expected: PASS.

- [ ] **Step 5: Run core tests and commit**

Run:

```powershell
mvn -pl spring-api-lens-core test
git add -- spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/SqlFragment.java spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/MyBatisSqlExtractor.java spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/MyBatisSqlExtractorTest.java
git commit -m "feat: extract MyBatis SQL tables"
```

## Task 6: Git Blame Ownership

**Files:**

- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/AuthorContribution.java`
- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/GitBlameAnalyzer.java`
- Create: `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/GitBlameAnalyzerTest.java`

- [ ] **Step 1: Write failing Git blame test**

Create `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/GitBlameAnalyzerTest.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.AuthorContribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitBlameAnalyzerTest {
    @TempDir
    Path repoRoot;

    @Test
    void calculatesAuthorRatioForLineRange() throws Exception {
        run("git", "init");
        run("git", "config", "user.name", "Zhang San");
        run("git", "config", "user.email", "zhang@example.com");
        Path source = repoRoot.resolve("OrderController.java");
        Files.writeString(source, "line1\nline2\n");
        run("git", "add", "OrderController.java");
        run("git", "commit", "-m", "first");

        run("git", "config", "user.name", "Li Si");
        run("git", "config", "user.email", "li@example.com");
        Files.writeString(source, "line1\nline2 changed\n");
        run("git", "add", "OrderController.java");
        run("git", "commit", "-m", "second");

        List<AuthorContribution> contributions = new GitBlameAnalyzer().analyze(repoRoot, source, 1, 2);

        assertThat(contributions).hasSize(2);
        assertThat(contributions)
            .extracting(AuthorContribution::name)
            .containsExactly("Zhang San", "Li Si");
        assertThat(contributions)
            .extracting(AuthorContribution::ratio)
            .containsExactly(0.5, 0.5);
    }

    private void run(String... command) throws Exception {
        Process process = new ProcessBuilder(command)
            .directory(repoRoot.toFile())
            .redirectErrorStream(true)
            .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();
        if (exit != 0) {
            throw new AssertionError(String.join(" ", command) + " failed: " + output);
        }
    }
}
```

- [ ] **Step 2: Run Git blame test to verify it fails**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=GitBlameAnalyzerTest test
```

Expected: FAIL because Git ownership classes do not exist.

- [ ] **Step 3: Implement Git blame analyzer**

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/AuthorContribution.java`:

```java
package com.qiqua.springapilens.core.model;

public record AuthorContribution(
    String name,
    String email,
    double ratio,
    int lineCount
) {
}
```

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/GitBlameAnalyzer.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.AuthorContribution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GitBlameAnalyzer {
    public List<AuthorContribution> analyze(Path repoRoot, Path file, int lineStart, int lineEnd) {
        String relativeFile = repoRoot.toAbsolutePath().normalize()
            .relativize(file.toAbsolutePath().normalize())
            .toString()
            .replace('\\', '/');
        String output = run(repoRoot, "git", "blame", "--line-porcelain", "-L", lineStart + "," + lineEnd, "--", relativeFile);
        Map<String, MutableContribution> counts = new LinkedHashMap<>();
        String currentAuthor = "";
        String currentEmail = "";
        for (String line : output.split("\\R")) {
            if (line.startsWith("author ")) {
                currentAuthor = line.substring("author ".length());
            } else if (line.startsWith("author-mail ")) {
                currentEmail = line.substring("author-mail ".length()).replace("<", "").replace(">", "");
            } else if (line.startsWith("\t")) {
                String key = currentAuthor + "\n" + currentEmail;
                counts.computeIfAbsent(key, ignored -> new MutableContribution(currentAuthor, currentEmail)).lineCount++;
            }
        }
        int total = counts.values().stream().mapToInt(value -> value.lineCount).sum();
        List<AuthorContribution> contributions = new ArrayList<>();
        for (MutableContribution value : counts.values()) {
            contributions.add(new AuthorContribution(
                value.name,
                value.email,
                total == 0 ? 0.0 : (double) value.lineCount / total,
                value.lineCount
            ));
        }
        return contributions;
    }

    private String run(Path repoRoot, String... command) {
        try {
            Process process = new ProcessBuilder(command)
                .directory(repoRoot.toFile())
                .redirectErrorStream(true)
                .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalArgumentException(String.join(" ", command) + " failed: " + output);
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to run " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running " + String.join(" ", command), e);
        }
    }

    private static final class MutableContribution {
        private final String name;
        private final String email;
        private int lineCount;

        private MutableContribution(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
```

- [ ] **Step 4: Run Git blame test**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=GitBlameAnalyzerTest test
```

Expected: PASS.

- [ ] **Step 5: Run core tests and commit**

Run:

```powershell
mvn -pl spring-api-lens-core test
git add -- spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/AuthorContribution.java spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/GitBlameAnalyzer.java spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/GitBlameAnalyzerTest.java
git commit -m "feat: calculate Git blame ownership"
```

## Task 7: Scan Orchestration And SQLite Persistence

**Files:**

- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/ScanResult.java`
- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/RepositoryScanner.java`
- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/store/SQLiteSchema.java`
- Create: `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/store/ScanResultRepository.java`
- Create: `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/RepositoryScannerTest.java`
- Create: `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/store/ScanResultRepositoryTest.java`

- [ ] **Step 1: Write failing scanner orchestration test**

Create `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/RepositoryScannerTest.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryScannerTest {
    @TempDir
    Path repoRoot;

    @Test
    void scansEndpointCallEdgeAndSqlFragment() throws Exception {
        initGit();
        write("src/main/java/com/example/OrderController.java", """
            package com.example;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;
            @RestController
            @RequestMapping("/api/order")
            class OrderController {
                private final OrderService orderService;
                OrderController(OrderService orderService) { this.orderService = orderService; }
                @PostMapping("/create")
                String create() { orderService.createOrder(); return "ok"; }
            }
            """);
        write("src/main/java/com/example/OrderService.java", """
            package com.example;
            class OrderService {
                private final OrderMapper orderMapper;
                OrderService(OrderMapper orderMapper) { this.orderMapper = orderMapper; }
                void createOrder() { orderMapper.insertOrder(); }
            }
            """);
        write("src/main/java/com/example/OrderMapper.java", """
            package com.example;
            interface OrderMapper { void insertOrder(); }
            """);
        write("src/main/resources/mapper/OrderMapper.xml", """
            <mapper namespace="com.example.OrderMapper">
              <insert id="insertOrder">insert into order_main (id) values (#{id})</insert>
            </mapper>
            """);
        run("git", "add", ".");
        run("git", "commit", "-m", "fixture");

        ScanResult result = new RepositoryScanner().scan(repoRoot);

        assertThat(result.repositoryInfo().repoName()).isEqualTo(repoRoot.getFileName().toString());
        assertThat(result.endpoints()).hasSize(1);
        assertThat(result.callEdges()).hasSize(2);
        assertThat(result.sqlFragments()).hasSize(1);
    }

    private void initGit() throws Exception {
        run("git", "init");
        run("git", "config", "user.name", "Zhang San");
        run("git", "config", "user.email", "zhang@example.com");
    }

    private void write(String relativePath, String content) throws Exception {
        Path path = repoRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private void run(String... command) throws Exception {
        Process process = new ProcessBuilder(command).directory(repoRoot.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();
        if (exit != 0) {
            throw new AssertionError(String.join(" ", command) + " failed: " + output);
        }
    }
}
```

- [ ] **Step 2: Run scanner orchestration test to verify it fails**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=RepositoryScannerTest test
```

Expected: FAIL because `RepositoryScanner` and `ScanResult` do not exist.

- [ ] **Step 3: Implement scan result and scanner orchestration**

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/ScanResult.java`:

```java
package com.qiqua.springapilens.core.model;

import java.util.List;

public record ScanResult(
    RepositoryInfo repositoryInfo,
    List<ApiEndpoint> endpoints,
    List<CodeSymbol> symbols,
    List<CallEdge> callEdges,
    List<SqlFragment> sqlFragments
) {
}
```

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/RepositoryScanner.java`:

```java
package com.qiqua.springapilens.core.scanner;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;
import com.qiqua.springapilens.core.model.RepositoryInfo;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.model.SqlFragment;

import java.nio.file.Path;
import java.util.List;

public class RepositoryScanner {
    public ScanResult scan(Path repoRoot) {
        RepositoryInfo repositoryInfo = new RepositoryValidator().validate(repoRoot);
        List<Path> files = new SourceFileDiscoverer().discover(repoRoot);
        List<ApiEndpoint> endpoints = new SpringEndpointExtractor().extract(repoRoot, files);
        List<CodeSymbol> symbols = new JavaSymbolExtractor().extract(repoRoot, files);
        List<CallEdge> callEdges = new CallEdgeExtractor().extract(repoRoot, files, symbols);
        List<SqlFragment> sqlFragments = new MyBatisSqlExtractor().extract(repoRoot, files);
        return new ScanResult(repositoryInfo, endpoints, symbols, callEdges, sqlFragments);
    }
}
```

- [ ] **Step 4: Run scanner orchestration test**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=RepositoryScannerTest test
```

Expected: PASS.

- [ ] **Step 5: Write failing SQLite persistence test**

Create `spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/store/ScanResultRepositoryTest.java`:

```java
package com.qiqua.springapilens.core.store;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.CallEdge;
import com.qiqua.springapilens.core.model.CodeSymbol;
import com.qiqua.springapilens.core.model.RepositoryInfo;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.model.SqlFragment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScanResultRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsScanResult() {
        Path dbPath = tempDir.resolve("scan.sqlite");
        ScanResultRepository repository = new ScanResultRepository(dbPath);
        ScanResult result = new ScanResult(
            new RepositoryInfo(tempDir, "demo", "main", "abc123", false),
            List.of(new ApiEndpoint("A.java", "AController", "create", "POST", "/a", "[]", "Request", "Response", 1, 5)),
            List.of(new CodeSymbol("A.java", "METHOD", "AController", "create", "AController.create()", 1, 5)),
            List.of(new CallEdge("AController.create()", "AService.create()", 0.95, "aService.create")),
            List.of(new SqlFragment("A.xml", "AMapper", "insertA", "insert into a_table values (?)", List.of("a_table"), "insert"))
        );

        repository.save(result);

        assertThat(repository.listEndpoints()).singleElement().satisfies(endpoint -> {
            assertThat(endpoint.path()).isEqualTo("/a");
            assertThat(endpoint.httpMethod()).isEqualTo("POST");
        });
    }
}
```

- [ ] **Step 6: Run SQLite test to verify it fails**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=ScanResultRepositoryTest test
```

Expected: FAIL because store classes do not exist.

- [ ] **Step 7: Implement SQLite schema and persistence**

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/store/SQLiteSchema.java`:

```java
package com.qiqua.springapilens.core.store;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class SQLiteSchema {
    private SQLiteSchema() {
    }

    static void initialize(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                create table if not exists repositories (
                    id integer primary key autoincrement,
                    root_path text not null,
                    repo_name text not null,
                    current_branch text not null,
                    head_commit text not null,
                    has_uncommitted_changes integer not null,
                    scanned_at text not null
                )
                """);
            statement.executeUpdate("""
                create table if not exists api_endpoints (
                    id integer primary key autoincrement,
                    relative_file text not null,
                    class_name text not null,
                    method_name text not null,
                    http_method text not null,
                    path text not null,
                    request_params_json text not null,
                    request_body_type text not null,
                    response_type text not null,
                    line_start integer not null,
                    line_end integer not null
                )
                """);
        }
    }
}
```

Create `spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/store/ScanResultRepository.java`:

```java
package com.qiqua.springapilens.core.store;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.ScanResult;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ScanResultRepository {
    private final Path dbPath;

    public ScanResultRepository(Path dbPath) {
        this.dbPath = dbPath;
    }

    public void save(ScanResult result) {
        try (Connection connection = open()) {
            SQLiteSchema.initialize(connection);
            insertRepository(connection, result);
            insertEndpoints(connection, result.endpoints());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save scan result", e);
        }
    }

    public List<ApiEndpoint> listEndpoints() {
        try (Connection connection = open()) {
            SQLiteSchema.initialize(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                select relative_file, class_name, method_name, http_method, path,
                       request_params_json, request_body_type, response_type, line_start, line_end
                from api_endpoints
                order by path
                """)) {
                ResultSet resultSet = statement.executeQuery();
                List<ApiEndpoint> endpoints = new ArrayList<>();
                while (resultSet.next()) {
                    endpoints.add(new ApiEndpoint(
                        resultSet.getString("relative_file"),
                        resultSet.getString("class_name"),
                        resultSet.getString("method_name"),
                        resultSet.getString("http_method"),
                        resultSet.getString("path"),
                        resultSet.getString("request_params_json"),
                        resultSet.getString("request_body_type"),
                        resultSet.getString("response_type"),
                        resultSet.getInt("line_start"),
                        resultSet.getInt("line_end")
                    ));
                }
                return endpoints;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load endpoints", e);
        }
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }

    private void insertRepository(Connection connection, ScanResult result) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            insert into repositories (root_path, repo_name, current_branch, head_commit, has_uncommitted_changes, scanned_at)
            values (?, ?, ?, ?, ?, ?)
            """)) {
            statement.setString(1, result.repositoryInfo().rootPath().toString());
            statement.setString(2, result.repositoryInfo().repoName());
            statement.setString(3, result.repositoryInfo().currentBranch());
            statement.setString(4, result.repositoryInfo().headCommit());
            statement.setInt(5, result.repositoryInfo().hasUncommittedChanges() ? 1 : 0);
            statement.setString(6, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    private void insertEndpoints(Connection connection, List<ApiEndpoint> endpoints) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            insert into api_endpoints
            (relative_file, class_name, method_name, http_method, path, request_params_json, request_body_type, response_type, line_start, line_end)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            for (ApiEndpoint endpoint : endpoints) {
                statement.setString(1, endpoint.relativeFile());
                statement.setString(2, endpoint.className());
                statement.setString(3, endpoint.methodName());
                statement.setString(4, endpoint.httpMethod());
                statement.setString(5, endpoint.path());
                statement.setString(6, endpoint.requestParamsJson());
                statement.setString(7, endpoint.requestBodyType());
                statement.setString(8, endpoint.responseType());
                statement.setInt(9, endpoint.lineStart());
                statement.setInt(10, endpoint.lineEnd());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
```

- [ ] **Step 8: Run SQLite test and all core tests**

Run:

```powershell
mvn -pl spring-api-lens-core -Dtest=ScanResultRepositoryTest test
mvn -pl spring-api-lens-core test
```

Expected: PASS.

- [ ] **Step 9: Commit**

Run:

```powershell
git add -- spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/model/ScanResult.java spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/scanner/RepositoryScanner.java spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/store/SQLiteSchema.java spring-api-lens-core/src/main/java/com/qiqua/springapilens/core/store/ScanResultRepository.java spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/scanner/RepositoryScannerTest.java spring-api-lens-core/src/test/java/com/qiqua/springapilens/core/store/ScanResultRepositoryTest.java
git commit -m "feat: orchestrate scans and persist endpoints"
```

## Task 8: Local Scan And Query HTTP API

**Files:**

- Create: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanController.java`
- Create: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanRequest.java`
- Create: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanResponse.java`
- Create: `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/EndpointResponse.java`
- Create: `spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api/ScanControllerTest.java`

- [ ] **Step 1: Write failing MVC test**

Create `spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api/ScanControllerTest.java`:

```java
package com.qiqua.springapilens.app.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScanControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void returnsEmptyEndpointListBeforeScan() throws Exception {
        mockMvc.perform(get("/api/endpoints"))
            .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run MVC test to verify it fails**

Run:

```powershell
mvn -pl spring-api-lens-app -Dtest=ScanControllerTest test
```

Expected: FAIL with 404 because the controller does not exist.

- [ ] **Step 3: Implement local API**

Create `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanRequest.java`:

```java
package com.qiqua.springapilens.app.api;

public record ScanRequest(String repoPath, String databasePath) {
}
```

Create `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanResponse.java`:

```java
package com.qiqua.springapilens.app.api;

public record ScanResponse(String repoName, int endpointCount, int callEdgeCount, int sqlFragmentCount) {
}
```

Create `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/EndpointResponse.java`:

```java
package com.qiqua.springapilens.app.api;

public record EndpointResponse(
    String httpMethod,
    String path,
    String className,
    String methodName,
    String requestBodyType,
    String responseType
) {
}
```

Create `spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api/ScanController.java`:

```java
package com.qiqua.springapilens.app.api;

import com.qiqua.springapilens.core.model.ApiEndpoint;
import com.qiqua.springapilens.core.model.ScanResult;
import com.qiqua.springapilens.core.scanner.RepositoryScanner;
import com.qiqua.springapilens.core.store.ScanResultRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ScanController {
    private final List<ApiEndpoint> inMemoryEndpoints = new ArrayList<>();

    @PostMapping("/scan")
    public ScanResponse scan(@RequestBody ScanRequest request) {
        ScanResult result = new RepositoryScanner().scan(Path.of(request.repoPath()));
        if (request.databasePath() != null && !request.databasePath().isBlank()) {
            new ScanResultRepository(Path.of(request.databasePath())).save(result);
        }
        inMemoryEndpoints.clear();
        inMemoryEndpoints.addAll(result.endpoints());
        return new ScanResponse(
            result.repositoryInfo().repoName(),
            result.endpoints().size(),
            result.callEdges().size(),
            result.sqlFragments().size()
        );
    }

    @GetMapping("/endpoints")
    public List<EndpointResponse> endpoints() {
        return inMemoryEndpoints.stream()
            .map(endpoint -> new EndpointResponse(
                endpoint.httpMethod(),
                endpoint.path(),
                endpoint.className(),
                endpoint.methodName(),
                endpoint.requestBodyType(),
                endpoint.responseType()
            ))
            .toList();
    }
}
```

- [ ] **Step 4: Run MVC test**

Run:

```powershell
mvn -pl spring-api-lens-app -Dtest=ScanControllerTest test
```

Expected: PASS.

- [ ] **Step 5: Run all tests and commit**

Run:

```powershell
mvn test
git add -- spring-api-lens-app/src/main/java/com/qiqua/springapilens/app/api spring-api-lens-app/src/test/java/com/qiqua/springapilens/app/api
git commit -m "feat: expose local scan API"
```

## Task 9: MVP Documentation And Verification

**Files:**

- Modify: `README.md`
- Create: `docs/mvp-api.md`

- [ ] **Step 1: Update README with MVP usage**

Modify `README.md` so it contains:

```markdown
# springApiLens

`springApiLens` is a local interface overview tool for Java/Spring Boot projects.

The first MVP scans one local Git repository, extracts Spring endpoints, deterministic Java call edges, MyBatis SQL fragments, and basic Git ownership evidence.

## Build

```powershell
mvn test
```

## Run

```powershell
mvn -pl spring-api-lens-app spring-boot:run
```

## Scan A Repository

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/scan -ContentType 'application/json' -Body '{"repoPath":"D:\\workspace\\demo-springboot","databasePath":"D:\\workspace\\spring-api-lens.sqlite"}'
```

## List Endpoints

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/endpoints
```

## Design

- [Interface Overview Tool Design](docs/superpowers/specs/2026-06-22-interface-overview-tool-design.md)
- [Phase One MVP Plan](docs/superpowers/plans/2026-06-22-phase-one-mvp.md)
```

- [ ] **Step 2: Add API docs**

Create `docs/mvp-api.md`:

```markdown
# MVP API

## POST /api/scan

Runs a local repository scan.

Request:

```json
{
  "repoPath": "D:\\workspace\\demo-springboot",
  "databasePath": "D:\\workspace\\spring-api-lens.sqlite"
}
```

Response:

```json
{
  "repoName": "demo-springboot",
  "endpointCount": 4,
  "callEdgeCount": 12,
  "sqlFragmentCount": 8
}
```

## GET /api/endpoints

Returns endpoints from the latest in-memory scan.

Response:

```json
[
  {
    "httpMethod": "POST",
    "path": "/api/order/create",
    "className": "OrderController",
    "methodName": "create",
    "requestBodyType": "CreateOrderRequest",
    "responseType": "ApiResult<OrderVO>"
  }
]
```
```

- [ ] **Step 3: Run final verification**

Run:

```powershell
mvn test
```

Expected: PASS.

- [ ] **Step 4: Commit**

Run:

```powershell
git add -- README.md docs/mvp-api.md docs/superpowers/plans/2026-06-22-phase-one-mvp.md
git commit -m "docs: document phase one MVP"
```

## Self-Review

Spec coverage:

- Local Git repository input is covered by Task 2 and Task 8.
- Spring Controller endpoint extraction is covered by Task 3.
- Deterministic Controller -> Service -> Mapper call chains are covered by Task 4 and Task 7.
- MyBatis XML SQL and table extraction are covered by Task 5.
- Git blame ownership groundwork is covered by Task 6.
- SQLite scan storage is covered by Task 7.
- Local HTTP API is covered by Task 8.
- MVP documentation is covered by Task 9.

Known first-phase gaps compared with the full design:

- Weighted endpoint author ratios are not yet joined into endpoint detail responses.
- AI analysis is not implemented in this phase.
- React UI is not implemented in this phase.
- Endpoint-table relation confidence is not fully persisted yet.

These gaps are intentional because this plan creates the scanner/backend foundation first. The next phase should add query models, author-filtered endpoint views, AI summary generation, and the React workbench.
