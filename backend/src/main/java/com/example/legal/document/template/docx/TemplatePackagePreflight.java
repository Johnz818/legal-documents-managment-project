package com.example.legal.document.template.docx;

import com.example.legal.document.template.inspection.TemplateInspectionErrorCode;
import com.example.legal.document.template.inspection.TemplateInspectionException;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

public final class TemplatePackagePreflight {

    private static final Set<String> REQUIRED_PARTS = Set.of(
            "[Content_Types].xml",
            "_rels/.rels",
            "word/document.xml"
    );
    private static final int BUFFER_SIZE = 8192;
    private static final Map<String, UnsupportedFeature> DENIED_RELATIONSHIP_TYPES = Map.ofEntries(
            Map.entry("http://schemas.microsoft.com/office/2006/relationships/vbaproject", UnsupportedFeature.MACRO),
            Map.entry("http://schemas.microsoft.com/office/2006/relationships/activexcontrol", UnsupportedFeature.ACTIVEX),
            Map.entry("http://schemas.microsoft.com/office/2006/relationships/activexcontrolbinary", UnsupportedFeature.ACTIVEX),
            Map.entry("http://schemas.openxmlformats.org/officedocument/2006/relationships/oleobject", UnsupportedFeature.EMBEDDED_OBJECT),
            Map.entry("http://schemas.openxmlformats.org/officedocument/2006/relationships/package", UnsupportedFeature.EMBEDDED_OBJECT),
            Map.entry("http://purl.oclc.org/ooxml/officedocument/relationships/oleobject", UnsupportedFeature.EMBEDDED_OBJECT),
            Map.entry("http://purl.oclc.org/ooxml/officedocument/relationships/package", UnsupportedFeature.EMBEDDED_OBJECT)
    );
    private static final Map<String, UnsupportedFeature> DENIED_CONTENT_TYPES = Map.ofEntries(
            Map.entry("application/vnd.ms-office.vbaproject", UnsupportedFeature.MACRO),
            Map.entry("application/vnd.ms-word.document.macroenabled.main+xml", UnsupportedFeature.MACRO),
            Map.entry("application/vnd.ms-word.template.macroenabledtemplate.main+xml", UnsupportedFeature.MACRO),
            Map.entry("application/vnd.ms-office.activex", UnsupportedFeature.ACTIVEX),
            Map.entry("application/vnd.ms-office.activex+xml", UnsupportedFeature.ACTIVEX),
            Map.entry("application/vnd.openxmlformats-officedocument.oleobject", UnsupportedFeature.EMBEDDED_OBJECT)
    );

    private final TemplatePackageLimits limits;

    public TemplatePackagePreflight(TemplatePackageLimits limits) {
        this.limits = limits;
    }

    public void validate(byte[] content) {
        if (content == null || content.length == 0) {
            throw failure(
                    TemplateInspectionErrorCode.TEMPLATE_FILE_EMPTY,
                    HttpStatus.BAD_REQUEST,
                    "Template file must not be empty"
            );
        }
        if (content.length > limits.maximumFileSize()) {
            throw failure(
                    TemplateInspectionErrorCode.TEMPLATE_FILE_TOO_LARGE,
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Template file exceeds the configured size limit"
            );
        }

        Set<String> names = new HashSet<>();
        Set<String> presentRequiredParts = new HashSet<>();
        long expandedSize = 0;
        int entryCount = 0;

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((entry = zip.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > limits.maximumEntryCount()) {
                    throw unsafe("Template package contains too many entries");
                }

                String name = validateEntryName(entry.getName(), names);
                rejectUnsupportedPackageContent(name);
                if (entry.isDirectory()) {
                    continue;
                }

                boolean relationshipMetadata = name.endsWith(".rels");
                boolean contentTypeMetadata = "[Content_Types].xml".equals(name);
                ByteArrayOutputStream metadataContent = relationshipMetadata || contentTypeMetadata
                        ? new ByteArrayOutputStream()
                        : null;
                long entrySize = 0;
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    entrySize += read;
                    expandedSize += read;
                    if (entrySize > limits.maximumEntrySize()) {
                        throw unsafe("Template package entry exceeds the configured size limit");
                    }
                    if (expandedSize > limits.maximumExpandedSize()) {
                        throw unsafe("Template package expands beyond the configured size limit");
                    }
                    if (metadataContent != null) {
                        metadataContent.write(buffer, 0, read);
                    }
                }

                if (relationshipMetadata) {
                    rejectUnsupportedRelationships(metadataContent.toByteArray());
                } else if (contentTypeMetadata) {
                    rejectUnsupportedContentTypes(metadataContent.toByteArray());
                }
                if (REQUIRED_PARTS.contains(name)) {
                    presentRequiredParts.add(name);
                }
            }
        } catch (TemplateInspectionException exception) {
            throw exception;
        } catch (ZipException exception) {
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_FILE_NOT_DOCX,
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Template file is not a valid DOCX package",
                    exception
            );
        } catch (IOException exception) {
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE,
                    HttpStatus.BAD_REQUEST,
                    "Template package could not be safely inspected",
                    exception
            );
        }

        if (!presentRequiredParts.containsAll(REQUIRED_PARTS)) {
            throw failure(
                    TemplateInspectionErrorCode.TEMPLATE_FILE_NOT_DOCX,
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Template file is not a valid DOCX package"
            );
        }
        if (expandedSize >= limits.compressionRatioMinimumExpandedSize()
                && expandedSize / (double) content.length > limits.maximumCompressionRatio()) {
            throw unsafe("Template package compression ratio exceeds the configured limit");
        }
    }

    private void rejectUnsupportedPackageContent(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("word/embeddings/")) {
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSUPPORTED,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Embedded files are not supported in document templates",
                    java.util.Map.of("feature", "EMBEDDED_OBJECT")
            );
        }
        if (normalized.startsWith("word/activex/")
                || normalized.equals("word/vbadata.xml")
                || normalized.endsWith("/vbaproject.bin")) {
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE,
                    HttpStatus.BAD_REQUEST,
                    "Active content is not permitted in document templates",
                    java.util.Map.of("feature", normalized.startsWith("word/activex/")
                            ? "ACTIVEX"
                            : "MACRO")
            );
        }
    }

    private String validateEntryName(String rawName, Set<String> names) {
        if (rawName == null || rawName.isBlank()) {
            throw unsafe("Template package contains an invalid entry name");
        }
        if (rawName.indexOf('\\') >= 0) {
            throw unsafe("Template package contains an unsafe entry name");
        }
        String name = rawName;
        if (name.startsWith("/") || name.contains("//")) {
            throw unsafe("Template package contains an unsafe entry name");
        }
        for (String segment : name.split("/")) {
            if (segment.equals("..") || segment.equals(".")) {
                throw unsafe("Template package contains an unsafe entry name");
            }
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        if (!names.add(normalized)) {
            throw unsafe("Template package contains duplicate entry names");
        }
        return name;
    }

    private void rejectUnsupportedRelationships(byte[] relationshipXml) {
        try {
            Document document = parseXml(relationshipXml);
            NodeList relationships = document.getElementsByTagNameNS("*", "Relationship");
            for (int index = 0; index < relationships.getLength(); index++) {
                Element relationship = (Element) relationships.item(index);
                if ("external".equalsIgnoreCase(relationship.getAttribute("TargetMode"))) {
                    throw new TemplateInspectionException(
                            TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE,
                            HttpStatus.BAD_REQUEST,
                            "External relationships are not supported in template packages",
                            java.util.Map.of("feature", "EXTERNAL_RELATIONSHIP")
                    );
                }
                rejectFeature(DENIED_RELATIONSHIP_TYPES.get(normalizeSemanticValue(
                        relationship.getAttribute("Type")
                )));
            }
        } catch (TemplateInspectionException exception) {
            throw exception;
        } catch (ParserConfigurationException | SAXException | IOException | IllegalArgumentException exception) {
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE,
                    HttpStatus.BAD_REQUEST,
                    "Template relationship metadata is malformed or unsafe",
                    exception
            );
        }
    }

    private void rejectUnsupportedContentTypes(byte[] contentTypeXml) {
        try {
            Document document = parseXml(contentTypeXml);
            NodeList declarations = document.getElementsByTagNameNS("*", "Default");
            rejectDeclaredContentTypes(declarations);
            rejectDeclaredContentTypes(document.getElementsByTagNameNS("*", "Override"));
        } catch (TemplateInspectionException exception) {
            throw exception;
        } catch (ParserConfigurationException | SAXException | IOException | IllegalArgumentException exception) {
            throw new TemplateInspectionException(
                    TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE,
                    HttpStatus.BAD_REQUEST,
                    "Template content-type metadata is malformed or unsafe",
                    exception
            );
        }
    }

    private void rejectDeclaredContentTypes(NodeList declarations) {
        for (int index = 0; index < declarations.getLength(); index++) {
            Element declaration = (Element) declarations.item(index);
            rejectFeature(DENIED_CONTENT_TYPES.get(normalizeSemanticValue(
                    declaration.getAttribute("ContentType")
            )));
        }
    }

    private Document parseXml(byte[] xml)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private String normalizeSemanticValue(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void rejectFeature(UnsupportedFeature feature) {
        if (feature != null) {
            throw feature.exception();
        }
    }

    private TemplateInspectionException unsafe(String message) {
        return failure(
                TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE,
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private TemplateInspectionException failure(
            TemplateInspectionErrorCode code,
            HttpStatus status,
            String message
    ) {
        return new TemplateInspectionException(code, status, message);
    }

    private enum UnsupportedFeature {
        MACRO(TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE, HttpStatus.BAD_REQUEST,
                "Active content is not permitted in document templates"),
        ACTIVEX(TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE, HttpStatus.BAD_REQUEST,
                "Active content is not permitted in document templates"),
        EMBEDDED_OBJECT(TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSUPPORTED,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Embedded files are not supported in document templates");

        private final TemplateInspectionErrorCode code;
        private final HttpStatus status;
        private final String message;

        UnsupportedFeature(TemplateInspectionErrorCode code, HttpStatus status, String message) {
            this.code = code;
            this.status = status;
            this.message = message;
        }

        private TemplateInspectionException exception() {
            return new TemplateInspectionException(
                    code, status, message, Map.of("feature", name())
            );
        }
    }
}
