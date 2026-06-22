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
              <delete id="deleteOrder">
                delete from order_archive where id = #{id}
              </delete>
            </mapper>
            """);

        List<SqlFragment> fragments = new MyBatisSqlExtractor().extract(repoRoot, List.of(xml));

        assertThat(fragments)
            .extracting(SqlFragment::mapperMethod)
            .containsExactly("insertOrder", "findOrder", "deleteOrder");
        assertThat(fragments.get(0).operationType()).isEqualTo("insert");
        assertThat(fragments.get(0).tables()).containsExactly("order_main");
        assertThat(fragments.get(1).operationType()).isEqualTo("select");
        assertThat(fragments.get(1).tables()).containsExactly("order_main", "user_account");
        assertThat(fragments.get(2).operationType()).isEqualTo("delete");
        assertThat(fragments.get(2).tables()).containsExactly("order_archive");
    }

    @Test
    void extractsJavaAnnotationSqlMyBatisPlusTablesAndJpaRepositoryTables() throws IOException {
        Path mapper = write("src/main/java/com/example/TipDataMapper.java", """
            package com.example;
            import com.baomidou.mybatisplus.core.mapper.BaseMapper;
            import org.apache.ibatis.annotations.Insert;
            import org.apache.ibatis.annotations.Mapper;
            import org.apache.ibatis.annotations.Select;

            @Mapper
            public interface TipDataMapper extends BaseMapper<TipDataDO> {
                @Select("select * from tip_data where id = #{id}")
                TipDataDO selectForExport(String id);

                @Insert({"insert into tip_data_archive (id)", "values (#{id})"})
                int archive(String id);
            }
            """);
        Path myBatisPlusEntity = write("src/main/java/com/example/TipDataDO.java", """
            package com.example;
            import com.baomidou.mybatisplus.annotation.TableName;

            @TableName(
                value = "tip_data",
                autoResultMap = true
            )
            public class TipDataDO {
            }
            """);
        Path repository = write("src/main/java/com/example/AuditLogRepository.java", """
            package com.example;
            import org.springframework.data.jpa.repository.JpaRepository;

            public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
            }
            """);
        Path jpaEntity = write("src/main/java/com/example/AuditLogEntity.java", """
            package com.example;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Table;

            @Entity
            @Table(name = "audit_log")
            public class AuditLogEntity {
            }
            """);

        List<SqlFragment> fragments = new MyBatisSqlExtractor()
            .extract(repoRoot, List.of(mapper, myBatisPlusEntity, repository, jpaEntity));

        assertThat(fragments).anySatisfy(fragment -> {
            assertThat(fragment.mapperNamespace()).isEqualTo("com.example.TipDataMapper");
            assertThat(fragment.mapperMethod()).isEqualTo("selectForExport");
            assertThat(fragment.operationType()).isEqualTo("select");
            assertThat(fragment.tables()).containsExactly("tip_data");
        });
        assertThat(fragments).anySatisfy(fragment -> {
            assertThat(fragment.mapperNamespace()).isEqualTo("com.example.TipDataMapper");
            assertThat(fragment.mapperMethod()).isEqualTo("archive");
            assertThat(fragment.operationType()).isEqualTo("insert");
            assertThat(fragment.tables()).containsExactly("tip_data_archive");
        });
        assertThat(fragments).anySatisfy(fragment -> {
            assertThat(fragment.mapperNamespace()).isEqualTo("com.example.TipDataMapper");
            assertThat(fragment.mapperMethod()).isEqualTo("BaseMapper<TipDataDO>");
            assertThat(fragment.operationType()).isEqualTo("mybatis-plus");
            assertThat(fragment.tables()).containsExactly("tip_data");
        });
        assertThat(fragments).anySatisfy(fragment -> {
            assertThat(fragment.mapperNamespace()).isEqualTo("com.example.AuditLogRepository");
            assertThat(fragment.mapperMethod()).isEqualTo("JpaRepository<AuditLogEntity>");
            assertThat(fragment.operationType()).isEqualTo("jpa");
            assertThat(fragment.tables()).containsExactly("audit_log");
        });
    }

    private Path write(String relativePath, String content) throws IOException {
        Path path = repoRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }
}
