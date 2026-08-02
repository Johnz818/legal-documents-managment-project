package com.example.legal.document.template.api;

import com.example.legal.document.template.inspection.TemplateInspectionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/document-templates/inspections")
public class TemplateInspectionController {

    private final TemplateInspectionService inspectionService;

    public TemplateInspectionController(TemplateInspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TemplateInspectionResponse inspect(@RequestPart("file") MultipartFile file) {
        return TemplateInspectionResponse.from(inspectionService.inspect(file));
    }
}
