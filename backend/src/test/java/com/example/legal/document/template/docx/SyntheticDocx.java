package com.example.legal.document.template.docx;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;

import java.io.ByteArrayOutputStream;

final class SyntheticDocx {

    private SyntheticDocx() {
    }

    static byte[] paragraphs(String... values) {
        return create(wordPackage -> {
            for (String value : values) {
                wordPackage.getMainDocumentPart().addParagraphOfText(value);
            }
        });
    }

    static byte[] create(PackageCustomizer customizer) {
        try {
            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
            customizer.customize(wordPackage);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            wordPackage.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create synthetic DOCX", exception);
        }
    }

    static P paragraph(String... runs) {
        P paragraph = new P();
        for (String value : runs) {
            R run = new R();
            Text text = new Text();
            text.setValue(value);
            run.getContent().add(text);
            paragraph.getContent().add(run);
        }
        return paragraph;
    }

    @FunctionalInterface
    interface PackageCustomizer {
        void customize(WordprocessingMLPackage wordPackage) throws Exception;
    }
}
