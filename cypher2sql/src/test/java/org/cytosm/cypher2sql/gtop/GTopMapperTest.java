package org.cytosm.cypher2sql.gtop;

import org.cytosm.common.gtop.GTop;
import org.cytosm.common.gtop.RelationalGTopInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import us.fatehi.chinook_database.DatabaseType;

import java.sql.Connection;
import java.sql.SQLException;

import static us.fatehi.chinook_database.ChinookDatabaseUtils.createChinookDatabase;

@Testcontainers(disabledWithoutDocker = true)
public class GTopMapperTest {
    @Container
    public final MySQLContainer<?> sakilaContainer = new MySQLContainer<>("mysql:8.1")
            .withDatabaseName("sakila")
            .withUsername("test")
            .withPassword("test")
            .withEnv("MYSQL_ROOT_PASSWORD", "test")
            // see - https://github.com/testcontainers/testcontainers-java/issues/570
            // another possible solution is to use flyway/liquibase
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("data/mysql/sakila/sakila-both.sql"),
                    "/docker-entrypoint-initdb.d/schema.sql"
            )
            .withExposedPorts(3306);
    @Container
    public final MySQLContainer<?> chinookContainer = new MySQLContainer<>("mysql:8.1")
            .withDatabaseName("chinook")
            .withUsername("test")
            .withPassword("test")
            .withEnv("MYSQL_ROOT_PASSWORD", "test")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("data/mysql/chinook/Chinook_MySql.sql"),
                    "/docker-entrypoint-initdb.d/schema.sql"
            )
            .withExposedPorts(3306);

    @Test
    public void sakilaTest() {
        try (Connection connection = sakilaContainer.createConnection("")) {
            GTopMapper gTopMapper = new GTopMapper(connection);
            GTop gTop = gTopMapper.mapSchema("sakila");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    // TODO: use GenericContainer and parameterized tests instead of `MySQLContainer`
//    @ParameterizedTest
    @EnumSource(value = DatabaseType.class, names = {"mysql", "postgresql"})
    public void chinookTest(
//            DatabaseType databaseType
    ) {
        try (Connection connection = chinookContainer.createConnection("")) {
            createChinookDatabase(DatabaseType.mysql, connection);
            GTopMapper gTopMapper = new GTopMapper(connection);
            GTop gTop = gTopMapper.mapSchema(null);
            System.out.println(RelationalGTopInterface.toPrettyString(gTop));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
