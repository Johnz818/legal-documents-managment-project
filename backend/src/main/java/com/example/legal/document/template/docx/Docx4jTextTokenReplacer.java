package com.example.legal.document.template.docx;

import org.docx4j.TraversalUtil;
import org.docx4j.XmlUtils;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class Docx4jTextTokenReplacer {

    void replace(WordprocessingMLPackage wordPackage, ReplacementResolver resolver) {
        replaceBlocks(wordPackage.getMainDocumentPart().getContent(), resolver);
    }

    private void replaceBlocks(List<Object> blocks, ReplacementResolver resolver) {
        for (Object wrapped : blocks) {
            Object block = XmlUtils.unwrap(wrapped);
            if (block instanceof P paragraph) {
                replaceParagraph(paragraph, resolver);
            } else if (block instanceof Tbl table) {
                replaceTableContent(table.getContent(), resolver);
            }
        }
    }

    private void replaceTableContent(List<Object> content, ReplacementResolver resolver) {
        for (Object wrapped : content) {
            Object value = XmlUtils.unwrap(wrapped);
            if (value instanceof Tc cell) {
                replaceBlocks(cell.getContent(), resolver);
            } else if (value instanceof Tr row) {
                replaceTableContent(row.getContent(), resolver);
            } else if (value instanceof ContentAccessor accessor && !(value instanceof SdtElement)) {
                replaceTableContent(accessor.getContent(), resolver);
            }
        }
    }

    private void replaceParagraph(P paragraph, ReplacementResolver resolver) {
        List<Text> nodes = new ArrayList<>();
        collectTextNodes(paragraph, nodes, Collections.newSetFromMap(new IdentityHashMap<>()));
        StringBuilder combined = new StringBuilder();
        for (Text node : nodes) {
            combined.append(value(node));
        }
        List<TextReplacement> replacements = resolver.resolve(combined.toString());
        for (int index = replacements.size() - 1; index >= 0; index--) {
            replace(nodes, replacements.get(index));
        }
    }

    private void replace(List<Text> nodes, TextReplacement replacement) {
        int offset = 0;
        int firstIndex = -1;
        int lastIndex = -1;
        int firstOffset = 0;
        int lastOffset = 0;
        for (int index = 0; index < nodes.size(); index++) {
            String value = value(nodes.get(index));
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
            throw new IllegalStateException("Replacement offsets do not match paragraph text");
        }
        String first = value(nodes.get(firstIndex));
        String last = value(nodes.get(lastIndex));
        String suffix = last.substring(lastOffset);
        setValue(nodes.get(firstIndex), first.substring(0, firstOffset) + replacement.value()
                + (firstIndex == lastIndex ? suffix : ""));
        for (int index = firstIndex + 1; index < lastIndex; index++) {
            setValue(nodes.get(index), "");
        }
        if (lastIndex > firstIndex) {
            setValue(nodes.get(lastIndex), suffix);
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

    private String value(Text text) {
        return text.getValue() == null ? "" : text.getValue();
    }

    private void setValue(Text text, String value) {
        text.setValue(value);
        text.setSpace(hasBoundaryWhitespace(value) ? "preserve" : null);
    }

    private boolean hasBoundaryWhitespace(String value) {
        return !value.isEmpty()
                && (Character.isWhitespace(value.charAt(0))
                || Character.isWhitespace(value.charAt(value.length() - 1)));
    }

    @FunctionalInterface
    interface ReplacementResolver {
        List<TextReplacement> resolve(String paragraphText);
    }

    record TextReplacement(int start, int end, String value) {
    }
}
