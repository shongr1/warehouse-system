package com.warehouse.system.pdf;

import com.lowagie.text.pdf.BaseFont;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PdfFonts {

    private PdfFonts() {}

    public static BaseFont hebrewBaseFont() {
        try (InputStream in = PdfFonts.class.getResourceAsStream("/fonts/NotoSansHebrew-Regular.ttf")) {
            if (in == null) throw new RuntimeException("Font not found in resources: /fonts/NotoSansHebrew-Regular.ttf");

            Path tmp = Files.createTempFile("NotoSansHebrew-", ".ttf");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();

            return BaseFont.createFont(tmp.toAbsolutePath().toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Hebrew font", e);
        }
    }
}
