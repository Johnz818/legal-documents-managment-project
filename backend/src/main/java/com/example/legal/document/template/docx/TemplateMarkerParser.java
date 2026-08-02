package com.example.legal.document.template.docx;

import com.example.legal.document.template.inspection.TemplateInspectionErrorCode;
import com.example.legal.document.template.inspection.TemplateInspectionException;
import com.example.legal.document.template.inspection.TemplateMarker;
import com.example.legal.document.template.inspection.TemplateMarkerKind;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class TemplateMarkerParser {

    private static final Pattern CANONICAL_KEY = Pattern.compile("[a-z][a-z0-9_]{0,99}");

    List<TemplateMarker> parse(String text, String location) {
        List<TemplateMarker> markers = new ArrayList<>();
        int cursor = 0;
        while (cursor < text.length()) {
            int opening = text.indexOf("{{", cursor);
            int unmatchedClosing = text.indexOf("}}", cursor);
            if (unmatchedClosing >= 0 && (opening < 0 || unmatchedClosing < opening)) {
                throw invalid(location);
            }
            if (opening < 0) {
                return markers;
            }

            int closing = text.indexOf("}}", opening + 2);
            if (closing < 0) {
                throw invalid(location);
            }
            String value = text.substring(opening + 2, closing);
            if (value.contains("{{") || value.contains("}}") || value.isEmpty()) {
                throw invalid(location);
            }

            TemplateMarkerKind kind;
            if (isChineseMarker(value)) {
                kind = TemplateMarkerKind.CHINESE;
            } else if (CANONICAL_KEY.matcher(value).matches()) {
                kind = TemplateMarkerKind.CANONICAL;
            } else {
                throw invalid(location);
            }
            markers.add(new TemplateMarker(kind, value));
            cursor = closing + 2;
        }
        return markers;
    }

    private boolean isChineseMarker(String value) {
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount < 1 || codePointCount > 40) {
            return false;
        }
        return value.codePoints().allMatch(codePoint -> Character.UnicodeScript.of(codePoint)
                == Character.UnicodeScript.HAN);
    }

    private TemplateInspectionException invalid(String location) {
        return new TemplateInspectionException(
                TemplateInspectionErrorCode.TEMPLATE_MARKER_INVALID,
                HttpStatus.BAD_REQUEST,
                "Template contains invalid marker syntax",
                Map.of("location", location)
        );
    }
}
