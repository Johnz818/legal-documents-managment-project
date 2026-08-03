package com.example.legal.document.template.docx;

import com.example.legal.document.template.inspection.DetectedTemplateMarker;
import com.example.legal.document.template.inspection.TemplateDocumentInspector;
import com.example.legal.document.template.inspection.TemplateInspection;
import com.example.legal.document.template.inspection.TemplateInspectionErrorCode;
import com.example.legal.document.template.inspection.TemplateInspectionException;
import com.example.legal.document.template.inspection.TemplateMarker;
import org.docx4j.TraversalUtil;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.JaxbXmlPart;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.WordprocessingML.CommentsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.EndnotesPart;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.FootnotesPart;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.wml.CTTxbxContent;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Docx4jTemplateDocumentInspector implements TemplateDocumentInspector {

    private static final String MAIN_BODY = "MAIN_BODY";

    private final TemplatePackagePreflight preflight;
    private final TemplateMarkerParser markerParser = new TemplateMarkerParser();

    public Docx4jTemplateDocumentInspector(TemplatePackagePreflight preflight) {
        this.preflight = preflight;
    }

    @Override
    public TemplateInspection inspect(byte[] docxContent) {
        preflight.validate(docxContent);
        try {
            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.load(
                    new ByteArrayInputStream(docxContent)
            );
            LinkedHashMap<TemplateMarker, Integer> occurrences = new LinkedHashMap<>();
            scanBlocks(wordPackage.getMainDocumentPart().getContent(), occurrences);
            guardContentControls(wordPackage.getMainDocumentPart().getJaxbElement());
            guardTextBoxes(wordPackage.getMainDocumentPart().getJaxbElement());
            guardUnsupportedParts(wordPackage);
            return new TemplateInspection(occurrences.entrySet().stream()
                    .map(entry -> new DetectedTemplateMarker(
                            entry.getKey().kind(),
                            entry.getKey().value(),
                            entry.getValue()
                    ))
                    .toList());
        } catch (TemplateInspectionException exception) {
            throw exception;
        } catch (Docx4JException | RuntimeException exception) {
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSUPPORTED,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Template package uses malformed or unsupported DOCX structures",
                    exception
            );
        }
    }

    private void scanBlocks(
            List<Object> blocks,
            LinkedHashMap<TemplateMarker, Integer> occurrences
    ) {
        for (Object wrapped : blocks) {
            Object block = XmlUtils.unwrap(wrapped);
            if (block instanceof P paragraph) {
                addMarkers(paragraphText(paragraph), MAIN_BODY, occurrences);
            } else if (block instanceof Tbl table) {
                scanTableContent(table.getContent(), occurrences);
            } else if (block instanceof SdtElement) {
                // Content controls are deliberately outside the G2 marker contract.
            }
        }
    }

    private void scanTableContent(
            List<Object> content,
            LinkedHashMap<TemplateMarker, Integer> occurrences
    ) {
        for (Object wrapped : content) {
            Object value = XmlUtils.unwrap(wrapped);
            if (value instanceof Tc cell) {
                scanBlocks(cell.getContent(), occurrences);
            } else if (value instanceof Tr row) {
                scanTableContent(row.getContent(), occurrences);
            } else if (value instanceof ContentAccessor accessor) {
                scanTableContent(accessor.getContent(), occurrences);
            }
        }
    }

    private void addMarkers(
            String text,
            String location,
            LinkedHashMap<TemplateMarker, Integer> occurrences
    ) {
        for (TemplateMarker marker : markerParser.parse(text, location)) {
            occurrences.merge(marker, 1, Integer::sum);
        }
    }

    private String paragraphText(P paragraph) {
        List<Text> textNodes = new ArrayList<>();
        collectTextNodes(paragraph, textNodes, identitySet());
        StringBuilder text = new StringBuilder();
        for (Text textNode : textNodes) {
            if (textNode.getValue() != null) {
                text.append(textNode.getValue());
            }
        }
        return text.toString();
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
        if (value instanceof SdtElement
                || value instanceof Drawing
                || value instanceof Pict
                || value instanceof CTTxbxContent) {
            return;
        }
        List<Object> children = TraversalUtil.getChildrenImpl(value);
        if (children == null) {
            return;
        }
        for (Object child : children) {
            collectTextNodes(child, result, visited);
        }
    }

    private void guardUnsupportedParts(WordprocessingMLPackage wordPackage) {
        for (Part part : wordPackage.getParts().getParts().values()) {
            String location = unsupportedLocation(part);
            if (part instanceof JaxbXmlPart<?> xmlPart) {
                guardContentControls(xmlPart.getJaxbElement());
                if (location != null) {
                    guardParagraphs(xmlPart.getJaxbElement(), location);
                }
            }
        }
    }

    private String unsupportedLocation(Part part) {
        if (part instanceof HeaderPart) {
            return "HEADER";
        }
        if (part instanceof FooterPart) {
            return "FOOTER";
        }
        if (part instanceof FootnotesPart) {
            return "FOOTNOTE";
        }
        if (part instanceof EndnotesPart) {
            return "ENDNOTE";
        }
        if (part instanceof CommentsPart) {
            return "COMMENT";
        }
        return null;
    }

    private void guardTextBoxes(Object root) {
        visit(root, identitySet(), value -> {
            if (value instanceof CTTxbxContent textBox) {
                guardParagraphs(textBox, "TEXT_BOX");
                return false;
            }
            return !(value instanceof SdtElement);
        });
    }

    private void guardContentControls(Object root) {
        visit(root, identitySet(), value -> {
            if (value instanceof SdtElement contentControl) {
                rejectMarkers(contentControlText(contentControl), "CONTENT_CONTROL");
                return false;
            }
            return true;
        });
    }

    private String contentControlText(Object root) {
        List<String> textNodes = new ArrayList<>();
        collectContentControlTextNodes(root, textNodes, identitySet());
        return String.join("", textNodes);
    }

    private void collectContentControlTextNodes(Object wrapped, List<String> result, Set<Object> visited) {
        Object value = XmlUtils.unwrap(wrapped);
        if (value == null || !visited.add(value)) {
            return;
        }
        if (value instanceof Text text) {
            result.add(text.getValue());
            return;
        }
        if (value instanceof Drawing || value instanceof Pict || value instanceof CTTxbxContent) {
            return;
        }
        List<Object> children = TraversalUtil.getChildrenImpl(value);
        if (children != null) {
            for (Object child : children) {
                collectContentControlTextNodes(child, result, visited);
            }
        }
    }

    private void guardParagraphs(Object root, String location) {
        visit(root, identitySet(), value -> {
            if (value instanceof P paragraph) {
                rejectMarkers(paragraph, location);
                return false;
            }
            return !(value instanceof SdtElement);
        });
    }

    private void rejectMarkers(P paragraph, String location) {
        rejectMarkers(paragraphText(paragraph), location);
    }

    private void rejectMarkers(String text, String location) {
        List<TemplateMarker> markers = markerParser.parse(text, location);
        if (!markers.isEmpty()) {
            TemplateMarker marker = markers.getFirst();
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_MARKER_UNSUPPORTED_LOCATION,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Template markers are not supported in "
                            + location.toLowerCase().replace('_', ' '),
                    Map.of(
                            "location", location,
                            "markerKind", marker.kind().name(),
                            "markerValue", marker.value()
                    )
            );
        }
    }

    private void visit(Object wrapped, Set<Object> visited, NodeVisitor visitor) {
        Object value = XmlUtils.unwrap(wrapped);
        if (value == null || !visited.add(value) || !visitor.visit(value)) {
            return;
        }
        List<Object> children = TraversalUtil.getChildrenImpl(value);
        if (children == null) {
            return;
        }
        for (Object child : children) {
            visit(child, visited, visitor);
        }
    }

    private Set<Object> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    @FunctionalInterface
    private interface NodeVisitor {
        boolean visit(Object value);
    }
}
