package com.example.legal.document.template.publication;

import com.example.legal.document.storage.DocumentStorage;
import com.example.legal.document.storage.DocumentStorageException;
import com.example.legal.document.storage.StoredDocument;
import com.example.legal.document.template.DocumentTemplateFieldRepository;
import com.example.legal.document.template.DocumentTemplateRepository;
import com.example.legal.document.template.DocumentTemplateVersionRepository;
import com.example.legal.document.template.inspection.TemplateInspection;
import com.example.legal.document.template.inspection.TemplateInspectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplatePublicationServiceTest {

    private TemplateInspectionService inspectionService;
    private TemplateDocumentNormalizer normalizer;
    private TemplatePublicationValidator validator;
    private DocumentStorage storage;
    private DocumentTemplateRepository templateRepository;
    private DocumentTemplateVersionRepository versionRepository;
    private DocumentTemplateFieldRepository fieldRepository;
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        inspectionService = mock(TemplateInspectionService.class);
        normalizer = mock(TemplateDocumentNormalizer.class);
        validator = mock(TemplatePublicationValidator.class);
        storage = mock(DocumentStorage.class);
        templateRepository = mock(DocumentTemplateRepository.class);
        versionRepository = mock(DocumentTemplateVersionRepository.class);
        fieldRepository = mock(DocumentTemplateFieldRepository.class);
        transactionManager = mock(PlatformTransactionManager.class);
        when(inspectionService.inspect(any(), any())).thenReturn(new TemplateInspection(List.of()));
        when(validator.validate(any(), any())).thenReturn(Map.of());
        when(normalizer.normalize(any(), any())).thenReturn(new byte[]{4, 5, 6});
    }

    @Test
    void removesStoredBinaryWhenMetadataTransactionFails() {
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(storage.store(any())).thenReturn(new StoredDocument("stored-key", 3));
        when(templateRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fieldRepository.findAllByTemplateVersionIdOrderByDisplayOrder(any())).thenReturn(List.of());
        doThrow(new IllegalStateException("commit failed"))
                .when(transactionManager).commit(transactionStatus);

        assertThatThrownBy(() -> service().create(command()))
                .isInstanceOfSatisfying(TemplatePublicationException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(TemplatePublicationErrorCode.TEMPLATE_PERSISTENCE_FAILED));
        verify(storage).remove("stored-key");
    }

    @Test
    void reportsStorageFailureWithoutStartingMetadataTransaction() {
        when(storage.store(any())).thenThrow(new DocumentStorageException("store failed"));

        assertThatThrownBy(() -> service().create(command()))
                .isInstanceOfSatisfying(TemplatePublicationException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(TemplatePublicationErrorCode.TEMPLATE_STORAGE_FAILED));
    }

    private TemplatePublicationService service() {
        return new TemplatePublicationService(
                inspectionService, normalizer, validator, storage,
                templateRepository, versionRepository, fieldRepository, transactionManager
        );
    }

    private TemplatePublicationCommand command() {
        return new TemplatePublicationCommand(
                "模板", null, "template.docx", "application/test",
                new byte[]{1, 2, 3}, List.of()
        );
    }
}
