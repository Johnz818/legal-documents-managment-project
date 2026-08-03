package com.example.legal.document.template.docx;

import com.example.legal.document.template.inspection.TemplateMarker;
import com.example.legal.document.template.publication.TemplateDocumentNormalizer;
import com.example.legal.document.template.publication.TemplatePublicationErrorCode;
import com.example.legal.document.template.publication.TemplatePublicationException;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Docx4jTemplateDocumentNormalizer implements TemplateDocumentNormalizer {

    private final TemplatePackagePreflight preflight;
    private final TemplateMarkerParser markerParser = new TemplateMarkerParser();
    private final Docx4jTextTokenReplacer tokenReplacer = new Docx4jTextTokenReplacer();

    public Docx4jTemplateDocumentNormalizer(TemplatePackagePreflight preflight) {
        this.preflight = preflight;
    }

    @Override
    public byte[] normalize(byte[] source, Map<TemplateMarker, String> markerMappings) {
        preflight.validate(source);
        try {
            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.load(
                    new ByteArrayInputStream(source)
            );
            tokenReplacer.replace(wordPackage, paragraphText -> replacements(paragraphText, markerMappings));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            wordPackage.save(output);
            return output.toByteArray();
        } catch (Docx4JException | RuntimeException exception) {
            if (exception instanceof TemplatePublicationException publicationException) {
                throw publicationException;
            }
            throw new TemplatePublicationException(
                    TemplatePublicationErrorCode.TEMPLATE_NORMALIZATION_FAILED,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Template could not be normalized",
                    exception
            );
        }
    }

    private List<Docx4jTextTokenReplacer.TextReplacement> replacements(
            String paragraphText,
            Map<TemplateMarker, String> mappings
    ) {
        List<TemplateMarker> markers = markerParser.parse(paragraphText, "MAIN_BODY");
        int searchFrom = 0;
        List<Docx4jTextTokenReplacer.TextReplacement> replacements = new ArrayList<>();
        for (TemplateMarker marker : markers) {
            String token = "{{" + marker.value() + "}}";
            int start = paragraphText.indexOf(token, searchFrom);
            String canonicalKey = mappings.get(marker);
            if (start < 0 || canonicalKey == null) {
                throw invalidMapping(marker);
            }
            replacements.add(new Docx4jTextTokenReplacer.TextReplacement(
                    start, start + token.length(), "{{" + canonicalKey + "}}"
            ));
            searchFrom = start + token.length();
        }
        return replacements;
    }

    private TemplatePublicationException invalidMapping(TemplateMarker marker) {
        return new TemplatePublicationException(
                TemplatePublicationErrorCode.TEMPLATE_MARKER_MAPPING_INVALID,
                HttpStatus.BAD_REQUEST,
                "Every detected marker must have a confirmed mapping",
                Map.of("markerKind", marker.kind().name(), "markerValue", marker.value())
        );
    }
}
