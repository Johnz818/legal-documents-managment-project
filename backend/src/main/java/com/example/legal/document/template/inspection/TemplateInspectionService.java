package com.example.legal.document.template.inspection;

import com.example.legal.document.template.docx.TemplatePackageLimits;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Service
public class TemplateInspectionService {

    private final TemplateDocumentInspector inspector;
    private final TemplatePackageLimits limits;

    public TemplateInspectionService(
            TemplateDocumentInspector inspector,
            TemplatePackageLimits limits
    ) {
        this.inspector = inspector;
        this.limits = limits;
    }

    public TemplateInspection inspect(MultipartFile file) {
        if (file == null) {
            validateFileMetadata(null, 0, true);
        }
        validateFileMetadata(file.getOriginalFilename(), file.getSize(), file.isEmpty());
        try {
            return inspect(file.getOriginalFilename(), file.getBytes());
        } catch (IOException exception) {
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSUPPORTED,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Template content could not be read",
                    exception
            );
        }
    }

    public TemplateInspection inspect(String fileName, byte[] content) {
        validateFileMetadata(fileName, content == null ? 0 : content.length, content == null || content.length == 0);
        return inspector.inspect(content);
    }

    private void validateFileMetadata(String fileName, long size, boolean empty) {
        if (empty) {
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_FILE_EMPTY,
                    HttpStatus.BAD_REQUEST,
                    "Template file must not be empty"
            );
        }
        if (size > limits.maximumFileSize()) {
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_FILE_TOO_LARGE,
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Template file exceeds the configured size limit"
            );
        }
        if (fileName == null
                || fileName.isBlank()
                || fileName.length() > 255
                || fileName.indexOf('/') >= 0
                || fileName.indexOf('\\') >= 0
                || fileName.chars().anyMatch(Character::isISOControl)
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_FILE_NOT_DOCX,
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Template file must use a valid DOCX filename"
            );
        }
    }
}
