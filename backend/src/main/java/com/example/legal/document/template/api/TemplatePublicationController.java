package com.example.legal.document.template.api;

import com.example.legal.document.template.inspection.TemplateInspectionErrorCode;
import com.example.legal.document.template.inspection.TemplateInspectionException;
import com.example.legal.document.template.publication.PublishedTemplateVersion;
import com.example.legal.document.template.publication.TemplateDownload;
import com.example.legal.document.template.publication.TemplateFieldDefinition;
import com.example.legal.document.template.publication.TemplatePage;
import com.example.legal.document.template.publication.TemplatePublicationCommand;
import com.example.legal.document.template.publication.TemplatePublicationErrorCode;
import com.example.legal.document.template.publication.TemplatePublicationException;
import com.example.legal.document.template.publication.TemplatePublicationService;
import com.example.legal.document.template.publication.TemplateSummary;
import com.example.legal.document.template.publication.TemplateVersionSummary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/document-templates")
public class TemplatePublicationController {

    private final TemplatePublicationService service;

    public TemplatePublicationController(TemplatePublicationService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublishedTemplateVersion> create(
            @RequestPart("file") MultipartFile file,
            @RequestPart("publication") TemplatePublicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(command(file, request)));
    }

    @PostMapping(path = "/{templateId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublishedTemplateVersion> publishVersion(
            @PathVariable Long templateId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("publication") TemplateVersionPublicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.publishVersion(templateId, command(file, request)));
    }

    @GetMapping
    public TemplatePage<TemplateSummary> listTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.listTemplates(page, size);
    }

    @GetMapping("/{templateId}/versions")
    public TemplatePage<TemplateVersionSummary> listVersions(
            @PathVariable Long templateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.listVersions(templateId, page, size);
    }

    @GetMapping("/{templateId}/versions/{versionNumber}")
    public PublishedTemplateVersion getVersion(
            @PathVariable Long templateId,
            @PathVariable int versionNumber
    ) {
        return service.getVersion(templateId, versionNumber);
    }

    @GetMapping("/{templateId}/versions/{versionNumber}/content")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long templateId,
            @PathVariable int versionNumber
    ) {
        TemplateDownload download = service.download(templateId, versionNumber);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(download.content()));
    }

    private TemplatePublicationCommand command(MultipartFile file, TemplatePublicationRequest request) {
        if (request == null) {
            throw invalidPublicationRequest();
        }
        return command(file, request.name(), request.description(), request.toFields());
    }

    private TemplatePublicationCommand command(
            MultipartFile file,
            TemplateVersionPublicationRequest request
    ) {
        if (request == null) {
            throw invalidPublicationRequest();
        }
        return command(file, null, null, request.toFields());
    }

    private TemplatePublicationCommand command(
            MultipartFile file,
            String name,
            String description,
            List<TemplateFieldDefinition> fields
    ) {
        try {
            return new TemplatePublicationCommand(
                    name, description, file.getOriginalFilename(),
                    file.getContentType(), file.getBytes(), fields
            );
        } catch (IOException exception) {
            throw unreadableTemplate(exception);
        }
    }

    private TemplatePublicationException invalidPublicationRequest() {
        return new TemplatePublicationException(
                TemplatePublicationErrorCode.TEMPLATE_PUBLICATION_INVALID,
                HttpStatus.BAD_REQUEST,
                "Publication request is invalid"
        );
    }

    private TemplateInspectionException unreadableTemplate(IOException exception) {
        return new TemplateInspectionException(
                TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSUPPORTED,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Template content could not be read",
                exception
        );
    }
}
