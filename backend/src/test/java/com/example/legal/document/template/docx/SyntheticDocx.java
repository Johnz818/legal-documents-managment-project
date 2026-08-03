package com.example.legal.document.template.docx;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

final class SyntheticDocx {

    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );

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

    static void addImageParagraph(WordprocessingMLPackage wordPackage) throws Exception {
        BinaryPartAbstractImage image = BinaryPartAbstractImage.createImagePart(wordPackage, ONE_PIXEL_PNG);
        Drawing drawing = new Drawing();
        drawing.getAnchorOrInline().add(image.createImageInline("logo", "Law firm logo", 1, 1, false));
        R run = new R();
        run.getContent().add(drawing);
        P paragraph = new P();
        paragraph.getContent().add(run);
        wordPackage.getMainDocumentPart().addObject(paragraph);
    }

    @FunctionalInterface
    interface PackageCustomizer {
        void customize(WordprocessingMLPackage wordPackage) throws Exception;
    }
}
