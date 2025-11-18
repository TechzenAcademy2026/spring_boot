package student.management.api_app.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OpenApiConfig {

    // ===== Info =====
    @Value("${openapi.title}")
    String title;
    @Value("${openapi.description}")
    String description;
    @Value("${openapi.version}")
    String version;

    // ===== Servers =====
    @Value("${openapi.servers[0].url}")
    String server0Url;
    @Value("${openapi.servers[0].description}")
    String server0Desc;

    @Value("${openapi.servers[1].url}")
    String server1Url;
    @Value("${openapi.servers[1].description}")
    String server1Desc;

    @Value("${openapi.servers[2].url}")
    String server2Url;
    @Value("${openapi.servers[2].description}")
    String server2Desc;

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(version))
                .servers(List.of(
                        new Server().url(server0Url).description(server0Desc),
                        new Server().url(server1Url).description(server1Desc),
                        new Server().url(server2Url).description(server2Desc)
                ));
    }

    // ===== Groups =====
    @Value("${openapi.groups.persons.name}")
    String personsGroupName;
    @Value("${openapi.groups.persons.packages}")
    String[] personsPackages;

    @Value("${openapi.groups.students.name}")
    String studentsGroupName;
    @Value("${openapi.groups.students.packages}")
    String[] studentsPackages;

    @Value("${openapi.groups.majors.name}")
    String majorsGroupName;
    @Value("${openapi.groups.majors.packages}")
    String[] majorsPackages;

    @Bean
    public GroupedOpenApi personsGroup() {
        return GroupedOpenApi.builder()
                .group(personsGroupName)
                .packagesToScan(personsPackages)
                .build();
    }

    @Bean
    public GroupedOpenApi studentsGroup() {
        return GroupedOpenApi.builder()
                .group(studentsGroupName)
                .packagesToScan(studentsPackages)
                .build();
    }

    @Bean
    public GroupedOpenApi majorsGroup() {
        return GroupedOpenApi.builder()
                .group(majorsGroupName)
                .packagesToScan(majorsPackages)
                .build();
    }
}
