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
