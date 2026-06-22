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
