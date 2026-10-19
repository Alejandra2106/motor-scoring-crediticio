package com.crediticio.acceptance;

import io.karatelabs.junit6.Karate;
import org.junit.jupiter.api.DynamicNode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(statements = "ALTER TABLE solicitante ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP")
class SolicitantesKarateTest {

    @LocalServerPort
    private int port;

    @Karate.Test
    Iterable<DynamicNode> acceptanceTests() {
        return Karate.run("classpath:com/crediticio/acceptance/solicitantes.feature")
                .systemProperty("acceptance.baseUrl", "http://localhost:" + port)
                .outputJunitXml(true)
                .outputCucumberJson(true);
    }
}
