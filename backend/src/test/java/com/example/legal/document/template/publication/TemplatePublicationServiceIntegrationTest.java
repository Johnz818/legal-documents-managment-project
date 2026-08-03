package com.example.legal.document.template.publication;

import com.example.legal.document.template.DocumentFieldDefaultSource;
import com.example.legal.document.template.DocumentFieldValueType;
import com.example.legal.document.template.inspection.TemplateMarker;
import com.example.legal.document.template.inspection.TemplateMarkerKind;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TemplatePublicationServiceIntegrationTest {

    private final TemplatePublicationService service;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    TemplatePublicationServiceIntegrationTest(
            TemplatePublicationService service,
            JdbcTemplate jdbcTemplate
    ) {
        this.service = service;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM document_template_fields");
        jdbcTemplate.update("DELETE FROM document_template_versions");
        jdbcTemplate.update("DELETE FROM document_templates");
    }

    @Test
    void createsNormalizesReadsListsAndDownloadsAnImmutableVersion() throws Exception {
        TemplatePublicationCommand command = command(
                "律师事务所函",
                "{{案号}} 与 {{案件编号}} / {{court_name}}",
                List.of(
                        field("case_number", "案号", DocumentFieldValueType.TEXT,
                                DocumentFieldDefaultSource.CASE_FIELD, "caseNumber",
                                marker(TemplateMarkerKind.CHINESE, "案号"),
                                marker(TemplateMarkerKind.CHINESE, "案件编号")),
                        field("court_name", "法院", DocumentFieldValueType.TEXT,
                                DocumentFieldDefaultSource.CASE_FIELD, "courtName",
                                marker(TemplateMarkerKind.CANONICAL, "court_name"))
                )
        );

        PublishedTemplateVersion created = service.create(command);
        PublishedTemplateVersion retrieved = service.getVersion(created.templateId(), 1);
        TemplateDownload download = service.download(created.templateId(), 1);
        byte[] stored = download.content().readAllBytes();

        assertThat(created.versionNumber()).isEqualTo(1);
        assertThat(created.fields()).extracting(PublishedTemplateField::fieldKey)
                .containsExactly("case_number", "court_name");
        assertThat(retrieved).isEqualTo(created);
        assertThat(service.listTemplates(0, 20).items()).singleElement()
                .satisfies(template -> assertThat(template.name()).isEqualTo("律师事务所函"));
        assertThat(service.listVersions(created.templateId(), 0, 20).items())
                .extracting(TemplateVersionSummary::versionNumber)
                .containsExactly(1);
        assertThat(stored).hasSize((int) created.fileSize());
        assertThat(stored).isNotEqualTo(command.content());
    }

    @Test
    void permitsEmptyTemplateContractAndPublishesLaterVersions() {
        PublishedTemplateVersion first = service.create(command("空字段模板", "固定正文", List.of()));
        PublishedTemplateVersion second = service.publishVersion(
                first.templateId(),
                command(null, "更新后的固定正文", List.of())
        );

        assertThat(first.fields()).isEmpty();
        assertThat(second.versionNumber()).isEqualTo(2);
        assertThat(service.listVersions(first.templateId(), 0, 20).items())
                .extracting(TemplateVersionSummary::versionNumber)
                .containsExactly(2, 1);
        assertThat(service.getVersion(first.templateId(), 1).contentSha256())
                .isEqualTo(first.contentSha256());
    }

    @Test
    void serializesConcurrentVersionAllocationWithTemplateRowLock() throws Exception {
        PublishedTemplateVersion first = service.create(command("并发模板", "固定正文", List.of()));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var left = executor.submit(() -> service.publishVersion(
                    first.templateId(), command(null, "版本甲", List.of())
            ));
            var right = executor.submit(() -> service.publishVersion(
                    first.templateId(), command(null, "版本乙", List.of())
            ));

            assertThat(Set.of(left.get().versionNumber(), right.get().versionNumber()))
                    .containsExactlyInAnyOrder(2, 3);
        }
    }

    @Test
    void rejectsMissingResourcesInvalidPaginationAndInvalidMetadata() {
        assertThatThrownBy(() -> service.getVersion(Long.MAX_VALUE, 1))
                .isInstanceOfSatisfying(TemplatePublicationException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(TemplatePublicationErrorCode.TEMPLATE_NOT_FOUND));
        assertThatThrownBy(() -> service.listTemplates(-1, 101))
                .isInstanceOf(TemplatePublicationException.class);
        assertThatThrownBy(() -> service.create(command(" ", "固定正文", List.of())))
                .isInstanceOf(TemplatePublicationException.class);
    }

    @Test
    void ordersTemplatesDeterministicallyWhenCreationTimestampsTie() {
        PublishedTemplateVersion first = service.create(command("模板甲", "固定正文", List.of()));
        PublishedTemplateVersion second = service.create(command("模板乙", "固定正文", List.of()));
        PublishedTemplateVersion third = service.create(command("模板丙", "固定正文", List.of()));
        jdbcTemplate.update(
                "UPDATE document_templates SET created_at = '2026-08-03 00:00:00.000000'"
        );

        assertThat(service.listTemplates(0, 2).items())
                .extracting(TemplateSummary::id)
                .containsExactly(third.templateId(), second.templateId());
        assertThat(service.listTemplates(1, 2).items())
                .extracting(TemplateSummary::id)
                .containsExactly(first.templateId());
    }

    private TemplatePublicationCommand command(
            String name,
            String paragraph,
            List<TemplateFieldDefinition> fields
    ) {
        return new TemplatePublicationCommand(
                name, null, "template.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docx(paragraph), fields
        );
    }

    private TemplateFieldDefinition field(
            String key,
            String displayName,
            DocumentFieldValueType type,
            DocumentFieldDefaultSource source,
            String sourceKey,
            TemplateMarker... markers
    ) {
        return new TemplateFieldDefinition(
                key, displayName, null, type, true, source, sourceKey, List.of(markers)
        );
    }

    private TemplateMarker marker(TemplateMarkerKind kind, String value) {
        return new TemplateMarker(kind, value);
    }

    private byte[] docx(String paragraph) {
        try {
            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
            wordPackage.getMainDocumentPart().addParagraphOfText(paragraph);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            wordPackage.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
