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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyBatisSqlExtractor {
    private static final Set<String> SQL_TAGS = Set.of("select", "insert", "update", "delete");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\b(?:class|interface|record)\\s+(\\w+)");
    private static final Pattern SQL_ANNOTATION_PATTERN = Pattern.compile("@(?:[\\w.]+\\.)?(Select|Insert|Update|Delete)\\s*\\(");
    private static final Pattern METHOD_AFTER_ANNOTATION_PATTERN = Pattern.compile(
        "(?m)^\\s*(?:public\\s+|protected\\s+|private\\s+|default\\s+|static\\s+|final\\s+|abstract\\s+)*[\\w<>?,.\\[\\]]+(?:\\s+[\\w<>?,.\\[\\]]+)*\\s+(\\w+)\\s*\\("
    );
    private static final Pattern BASE_MAPPER_PATTERN = Pattern.compile(
        "\\b(?:class|interface)\\s+\\w+\\s+extends\\s+(?:[\\w.]+\\.)?BaseMapper\\s*<\\s*(\\w+)\\s*>"
    );
    private static final Pattern JPA_REPOSITORY_PATTERN = Pattern.compile(
        "\\b(?:class|interface)\\s+\\w+\\s+extends\\s+(?:[\\w.]+\\.)?(?:JpaRepository|CrudRepository|PagingAndSortingRepository)\\s*<\\s*(\\w+)\\s*,"
    );
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("@(?:[\\w.]+\\.)?TableName\\s*\\(");
    private static final Pattern JPA_TABLE_PATTERN = Pattern.compile("@(?:[\\w.]+\\.)?Table\\s*\\(");
    private static final Pattern JPA_TABLE_NAME_PATTERN = Pattern.compile("\\bname\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern TABLE_PATTERN = Pattern.compile(
        "\\b(from|join|into|update)\\s+([`\"]?[a-zA-Z_][a-zA-Z0-9_.$]*[`\"]?)",
        Pattern.CASE_INSENSITIVE
    );

    public List<SqlFragment> extract(Path repoRoot, List<Path> sourceFiles) {
        List<SqlFragment> fragments = new ArrayList<>();
        Path root = repoRoot.toAbsolutePath().normalize();
        Map<String, EntityTable> entityTables = entityTables(root, sourceFiles);
        for (Path sourceFile : sourceFiles) {
            Path normalized = sourceFile.toAbsolutePath().normalize();
            if (normalized.toString().endsWith(".xml")) {
                fragments.addAll(extractXmlFile(root, normalized));
            } else if (normalized.toString().endsWith(".java")) {
                fragments.addAll(extractJavaFile(root, normalized, entityTables));
            }
        }
        return fragments;
    }

    private List<SqlFragment> extractXmlFile(Path repoRoot, Path xmlFile) {
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
            tables.add(matcher.group(2).replace("`", "").replace("\"", ""));
        }
        return new ArrayList<>(tables);
    }

    private Map<String, EntityTable> entityTables(Path repoRoot, List<Path> sourceFiles) {
        Map<String, EntityTable> tables = new HashMap<>();
        for (Path sourceFile : sourceFiles) {
            Path normalized = sourceFile.toAbsolutePath().normalize();
            if (!normalized.toString().endsWith(".java")) {
                continue;
            }
            try {
                String source = Files.readString(normalized);
                Optional<String> typeName = typeName(source);
                if (typeName.isEmpty()) {
                    continue;
                }
                Optional<String> table = tableName(source, typeName.get());
                table.ifPresent(value -> tables.put(typeName.get(), new EntityTable(
                    typeName.get(),
                    value,
                    relativePath(repoRoot, normalized)
                )));
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse Java entity table metadata " + normalized, e);
            }
        }
        return tables;
    }

    private Optional<String> tableName(String source, String typeName) {
        Optional<String> myBatisPlusTable = annotationArgument(source, TABLE_NAME_PATTERN)
            .flatMap(this::firstStringLiteral);
        if (myBatisPlusTable.isPresent()) {
            return myBatisPlusTable;
        }

        Optional<String> jpaTable = annotationArgument(source, JPA_TABLE_PATTERN)
            .flatMap(this::jpaTableName);
        if (jpaTable.isPresent()) {
            return jpaTable;
        }

        if (source.contains("@Entity")) {
            return Optional.of(toSnakeCase(stripEntitySuffix(typeName)));
        }
        return Optional.empty();
    }

    private List<SqlFragment> extractJavaFile(Path repoRoot, Path javaFile, Map<String, EntityTable> entityTables) {
        try {
            String source = Files.readString(javaFile);
            Optional<String> typeName = typeName(source);
            if (typeName.isEmpty()) {
                return List.of();
            }
            String namespace = qualifiedName(packageName(source), typeName.get());
            List<SqlFragment> fragments = new ArrayList<>();
            fragments.addAll(extractAnnotationSql(repoRoot, javaFile, source, namespace));
            fragments.addAll(extractMyBatisPlusMapper(repoRoot, javaFile, source, namespace, entityTables));
            fragments.addAll(extractJpaRepository(repoRoot, javaFile, source, namespace, entityTables));
            return fragments;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Java SQL metadata " + javaFile, e);
        }
    }

    private List<SqlFragment> extractAnnotationSql(Path repoRoot, Path javaFile, String source, String namespace) {
        List<SqlFragment> fragments = new ArrayList<>();
        Matcher matcher = SQL_ANNOTATION_PATTERN.matcher(source);
        while (matcher.find()) {
            int bodyStart = matcher.end();
            int bodyEnd = findMatchingParenthesis(source, bodyStart - 1);
            if (bodyEnd < 0) {
                continue;
            }
            String operation = matcher.group(1).toLowerCase(Locale.ROOT);
            String sql = normalizeSql(String.join(" ", stringLiterals(source.substring(bodyStart, bodyEnd))));
            if (sql.isBlank()) {
                continue;
            }
            Optional<String> methodName = nextMethodName(source.substring(bodyEnd + 1));
            if (methodName.isEmpty()) {
                continue;
            }
            fragments.add(new SqlFragment(
                relativePath(repoRoot, javaFile),
                namespace,
                methodName.get(),
                sql,
                extractTables(sql),
                operation
            ));
        }
        return fragments;
    }

    private List<SqlFragment> extractMyBatisPlusMapper(
        Path repoRoot,
        Path javaFile,
        String source,
        String namespace,
        Map<String, EntityTable> entityTables
    ) {
        Matcher matcher = BASE_MAPPER_PATTERN.matcher(source);
        if (!matcher.find()) {
            return List.of();
        }
        String entityName = matcher.group(1);
        EntityTable table = entityTables.get(entityName);
        if (table == null) {
            return List.of();
        }
        return List.of(new SqlFragment(
            relativePath(repoRoot, javaFile),
            namespace,
            "BaseMapper<" + entityName + ">",
            "MyBatis Plus BaseMapper<" + entityName + "> -> " + table.tableName(),
            List.of(table.tableName()),
            "mybatis-plus"
        ));
    }

    private List<SqlFragment> extractJpaRepository(
        Path repoRoot,
        Path javaFile,
        String source,
        String namespace,
        Map<String, EntityTable> entityTables
    ) {
        Matcher matcher = JPA_REPOSITORY_PATTERN.matcher(source);
        if (!matcher.find()) {
            return List.of();
        }
        String entityName = matcher.group(1);
        EntityTable table = entityTables.get(entityName);
        if (table == null) {
            return List.of();
        }
        return List.of(new SqlFragment(
            relativePath(repoRoot, javaFile),
            namespace,
            "JpaRepository<" + entityName + ">",
            "Spring Data JPA repository<" + entityName + "> -> " + table.tableName(),
            List.of(table.tableName()),
            "jpa"
        ));
    }

    private Optional<String> annotationArgument(String source, Pattern annotationPattern) {
        Matcher matcher = annotationPattern.matcher(source);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int bodyStart = matcher.end();
        int bodyEnd = findMatchingParenthesis(source, bodyStart - 1);
        if (bodyEnd < 0) {
            return Optional.empty();
        }
        return Optional.of(source.substring(bodyStart, bodyEnd));
    }

    private Optional<String> firstStringLiteral(String value) {
        List<String> strings = stringLiterals(value);
        if (strings.isEmpty() || strings.getFirst().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(strings.getFirst().trim());
    }

    private Optional<String> jpaTableName(String arguments) {
        Matcher matcher = JPA_TABLE_NAME_PATTERN.matcher(arguments);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).trim());
        }
        return firstStringLiteral(arguments);
    }

    private Optional<String> nextMethodName(String tail) {
        Matcher matcher = METHOD_AFTER_ANNOTATION_PATTERN.matcher(tail);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }

    private Optional<String> typeName(String source) {
        Matcher matcher = TYPE_PATTERN.matcher(source);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private String packageName(String source) {
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String qualifiedName(String packageName, String typeName) {
        return packageName.isBlank() ? typeName : packageName + "." + typeName;
    }

    private String relativePath(Path repoRoot, Path file) {
        return repoRoot.relativize(file).toString().replace('\\', '/');
    }

    private String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private List<String> stringLiterals(String value) {
        List<String> literals = new ArrayList<>();
        int index = 0;
        while (index < value.length()) {
            if (startsWith(value, index, "\"\"\"")) {
                int end = value.indexOf("\"\"\"", index + 3);
                if (end < 0) {
                    break;
                }
                literals.add(value.substring(index + 3, end));
                index = end + 3;
            } else if (value.charAt(index) == '"') {
                StringBuilder literal = new StringBuilder();
                index++;
                while (index < value.length()) {
                    char current = value.charAt(index);
                    if (current == '\\' && index + 1 < value.length()) {
                        literal.append(value.charAt(index + 1));
                        index += 2;
                    } else if (current == '"') {
                        index++;
                        break;
                    } else {
                        literal.append(current);
                        index++;
                    }
                }
                literals.add(literal.toString());
            } else {
                index++;
            }
        }
        return literals;
    }

    private int findMatchingParenthesis(String source, int openIndex) {
        int depth = 0;
        int index = openIndex;
        while (index < source.length()) {
            if (startsWith(source, index, "\"\"\"")) {
                int end = source.indexOf("\"\"\"", index + 3);
                if (end < 0) {
                    return -1;
                }
                index = end + 3;
                continue;
            }

            char current = source.charAt(index);
            if (current == '"') {
                index = skipString(source, index + 1);
                continue;
            }
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
            index++;
        }
        return -1;
    }

    private int skipString(String source, int index) {
        while (index < source.length()) {
            char current = source.charAt(index);
            if (current == '\\') {
                index += 2;
            } else if (current == '"') {
                return index + 1;
            } else {
                index++;
            }
        }
        return source.length();
    }

    private boolean startsWith(String value, int index, String prefix) {
        return index + prefix.length() <= value.length()
            && value.startsWith(prefix, index);
    }

    private String stripEntitySuffix(String typeName) {
        if (typeName.endsWith("Entity")) {
            return typeName.substring(0, typeName.length() - "Entity".length());
        }
        return typeName;
    }

    private String toSnakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private record EntityTable(String entityName, String tableName, String relativeFile) {
    }
}
