package com.example.legal.document.template.docx;

import com.example.legal.document.template.inspection.TemplateMarker;
import com.example.legal.document.template.publication.TemplateDocumentNormalizer;
import com.example.legal.document.template.publication.TemplatePublicationErrorCode;
import com.example.legal.document.template.publication.TemplatePublicationException;
import org.docx4j.TraversalUtil;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.P;
import org.docx4j.wml.Pict;
import org.docx4j.wml.SdtElement;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Docx4jTemplateDocumentNormalizer implements TemplateDocumentNormalizer {

    private final TemplatePackagePreflight preflight;
    private final TemplateMarkerParser markerParser = new TemplateMarkerParser();

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
            normalizeBlocks(wordPackage.getMainDocumentPart().getContent(), markerMappings);
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

    private void normalizeBlocks(List<Object> blocks, Map<TemplateMarker, String> mappings) {
        for (Object wrapped : blocks) {
            Object block = XmlUtils.unwrap(wrapped);
            if (block instanceof P paragraph) {
                normalizeParagraph(paragraph, mappings);
            } else if (block instanceof Tbl table) {
                normalizeTableContent(table.getContent(), mappings);
            }
        }
    }

    private void normalizeTableContent(List<Object> content, Map<TemplateMarker, String> mappings) {
        for (Object wrapped : content) {
            Object value = XmlUtils.unwrap(wrapped);
            if (value instanceof Tc cell) {
                normalizeBlocks(cell.getContent(), mappings);
            } else if (value instanceof Tr row) {
                normalizeTableContent(row.getContent(), mappings);
            } else if (value instanceof ContentAccessor accessor && !(value instanceof SdtElement)) {
                normalizeTableContent(accessor.getContent(), mappings);
            }
        }
    }

    private void normalizeParagraph(P paragraph, Map<TemplateMarker, String> mappings) {
        List<Text> nodes = new ArrayList<>();
        collectTextNodes(paragraph, nodes, Collections.newSetFromMap(new IdentityHashMap<>()));
        StringBuilder combined = new StringBuilder();
        for (Text node : nodes) {
            combined.append(node.getValue() == null ? "" : node.getValue());
        }
        List<TemplateMarker> markers = markerParser.parse(combined.toString(), "MAIN_BODY");
        int searchFrom = 0;
        List<Replacement> replacements = new ArrayList<>();
        for (TemplateMarker marker : markers) {
            String token = "{{" + marker.value() + "}}";
            int start = combined.indexOf(token, searchFrom);
            String canonicalKey = mappings.get(marker);
            if (start < 0 || canonicalKey == null) {
                throw invalidMapping(marker);
            }
            replacements.add(new Replacement(start, start + token.length(), "{{" + canonicalKey + "}}"));
            searchFrom = start + token.length();
        }
        for (int index = replacements.size() - 1; index >= 0; index--) {
            replace(nodes, replacements.get(index));
        }
    }

    private void replace(List<Text> nodes, Replacement replacement) {
        int offset = 0;
        int firstIndex = -1;
        int lastIndex = -1;
        int firstOffset = 0;
        int lastOffset = 0;
        for (int index = 0; index < nodes.size(); index++) {
            String value = nodes.get(index).getValue() == null ? "" : nodes.get(index).getValue();
            int end = offset + value.length();
            if (firstIndex < 0 && replacement.start() < end) {
                firstIndex = index;
                firstOffset = replacement.start() - offset;
            }
            if (replacement.end() <= end) {
                lastIndex = index;
                lastOffset = replacement.end() - offset;
                break;
            }
            offset = end;
        }
        if (firstIndex < 0 || lastIndex < 0) {
            throw new IllegalStateException("Marker offsets do not match paragraph text");
        }
        String first = nodes.get(firstIndex).getValue() == null ? "" : nodes.get(firstIndex).getValue();
        String last = nodes.get(lastIndex).getValue() == null ? "" : nodes.get(lastIndex).getValue();
        String suffix = last.substring(lastOffset);
        nodes.get(firstIndex).setValue(first.substring(0, firstOffset) + replacement.value()
                + (firstIndex == lastIndex ? suffix : ""));
        for (int index = firstIndex + 1; index < lastIndex; index++) {
            nodes.get(index).setValue("");
        }
        if (lastIndex > firstIndex) {
            nodes.get(lastIndex).setValue(suffix);
        }
    }

    private void collectTextNodes(Object wrapped, List<Text> result, Set<Object> visited) {
        Object value = XmlUtils.unwrap(wrapped);
        if (value == null || !visited.add(value)) {
            return;
        }
        if (value instanceof Text text) {
            result.add(text);
            return;
        }
        if (value instanceof SdtElement || value instanceof Drawing || value instanceof Pict) {
            return;
        }
        List<Object> children = TraversalUtil.getChildrenImpl(value);
        if (children != null) {
            for (Object child : children) {
                collectTextNodes(child, result, visited);
            }
        }
    }

    private TemplatePublicationException invalidMapping(TemplateMarker marker) {
        return new TemplatePublicationException(
                TemplatePublicationErrorCode.TEMPLATE_MARKER_MAPPING_INVALID,
                HttpStatus.BAD_REQUEST,
                "Every detected marker must have a confirmed mapping",
                Map.of("markerKind", marker.kind().name(), "markerValue", marker.value())
        );
    }

    private record Replacement(int start, int end, String value) {
    }
}
