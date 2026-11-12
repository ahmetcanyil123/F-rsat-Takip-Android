package com.antigravity.firsattakip.export;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.antigravity.firsattakip.model.GroupedDealRow;
import com.antigravity.firsattakip.model.ReportSection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Raporları tarihiyle birlikte Word (.docx), PDF (.pdf) ve HTML (.html) olarak üretir.
 */
public class ReportExporter {

    public static class ExportResult {
        public final File docxFile;
        public final File pdfFile;
        public final File htmlFile;
        public final String htmlContent;

        public ExportResult(File docxFile, File pdfFile, File htmlFile, String htmlContent) {
            this.docxFile = docxFile;
            this.pdfFile = pdfFile;
            this.htmlFile = htmlFile;
            this.htmlContent = htmlContent;
        }
    }

    public static ExportResult exportAll(
            Context context,
            String shortCode,
            String reportTitle,
            List<ReportSection> sections
    ) throws IOException {
        File dir = new File(context.getFilesDir(), "reports");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Rapor klasörü oluşturulamadı.");
        }

        String fileKey = "ç".equalsIgnoreCase(shortCode) ? "cagri"
                : ("t".equalsIgnoreCase(shortCode) ? "tumu" : shortCode.toLowerCase(Locale.ROOT));
        String dateIso = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String dateHuman = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("tr", "TR")).format(new Date());

        String baseName = "Firsat_Raporu_" + fileKey + "_" + dateIso;
        File docxFile = new File(dir, baseName + ".docx");
        File pdfFile = new File(dir, baseName + ".pdf");
        File htmlFile = new File(dir, baseName + ".html");

        String html = buildHtml(reportTitle, dateHuman, sections);
        try (FileOutputStream fos = new FileOutputStream(htmlFile)) {
            fos.write(html.getBytes(StandardCharsets.UTF_8));
        }

        writeDocx(docxFile, reportTitle, dateHuman, sections);
        writePdf(pdfFile, reportTitle, dateHuman, sections);

        return new ExportResult(docxFile, pdfFile, htmlFile, html);
    }

    // =========================================================================
    // HTML OLUŞTURUCU
    // =========================================================================

    private static String buildHtml(String title, String dateHuman, List<ReportSection> sections) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='tr'><head><meta charset='utf-8'>")
                .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
                .append("<style>")
                .append("body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;background:#0f172a;color:#e2e8f0;margin:0;padding:12px;line-height:1.5;}")
                .append(".meta{background:#1e293b;border-left:4px solid #3b82f6;padding:10px 12px;border-radius:8px;margin-bottom:16px;font-size:13px;}")
                .append("h1{font-size:18px;color:#f8fafc;margin:0 0 6px 0;}")
                .append("h2{font-size:16px;color:#93c5fd;margin:20px 0 6px 0;border-bottom:1px solid #334155;padding-bottom:4px;}")
                .append(".sub{font-size:12px;color:#94a3b8;margin-bottom:10px;}")
                .append("ul{padding-left:18px;margin:8px 0 16px 0;} li{margin-bottom:6px;font-size:13px;}")
                .append(".tbl-wrap{overflow-x:auto;-webkit-overflow-scrolling:touch;border-radius:8px;border:1px solid #334155;margin-bottom:18px;}")
                .append("table{width:100%;border-collapse:collapse;font-size:12.5px;min-width:560px;background:#1e293b;}")
                .append("th{background:#1e3a8a;color:#fff;text-align:left;padding:8px;border:1px solid #334155;}")
                .append("td{padding:7px 8px;border:1px solid #334155;vertical-align:top;color:#f1f5f9;}")
                .append("tr:nth-child(even) td{background:#162032;}")
                .append(".rate{color:#4ade80;font-weight:700;white-space:nowrap;}")
                .append("a{color:#60a5fa;}")
                .append("</style></head><body>");

        sb.append("<div class='meta'><h1>").append(esc(title)).append("</h1>")
                .append("<div><strong>Tarih:</strong> ").append(esc(dateHuman)).append("</div>")
                .append("<div><strong>Kriterler:</strong> İndirim &ge; %40 veya (&gt; 1000 TL &amp; İndirim &ge; %25)</div>")
                .append("</div>");

        for (ReportSection sec : sections) {
            sb.append("<h2>").append(esc(sec.title)).append("</h2>");
            sb.append("<div class='sub'>").append(esc(sec.subtitle))
                    .append(" — <a href='").append(esc(sec.sourceUrl)).append("'>Kaynak</a></div>");

            if (!sec.bulletItems.isEmpty()) {
                sb.append("<ul>");
                for (String b : sec.bulletItems) {
                    sb.append("<li>").append(esc(b)).append("</li>");
                }
                sb.append("</ul>");
            }

            if (!sec.tableRows.isEmpty()) {
                sb.append("<div class='tbl-wrap'><table><thead><tr>")
                        .append("<th>Marka</th><th>Ürün Adı</th><th>Normal Fiyat</th>")
                        .append("<th>İndirimli Fiyat</th><th>İndirim Oranı</th>")
                        .append("</tr></thead><tbody>");
                for (GroupedDealRow row : sec.tableRows) {
                    sb.append("<tr>")
                            .append("<td><strong>").append(esc(row.brandDisplay)).append("</strong></td>")
                            .append("<td>").append(esc(row.productsDisplay)).append("</td>")
                            .append("<td>").append(esc(row.origPriceDisplay)).append("</td>")
                            .append("<td>").append(esc(row.discPriceDisplay)).append("</td>")
                            .append("<td class='rate'>").append(esc(row.discountRateDisplay)).append("</td>")
                            .append("</tr>");
                }
                sb.append("</tbody></table></div>");
            } else if (sec.bulletItems.isEmpty()) {
                sb.append("<div class='sub'><em>Kriterlere uyan ürün bulunamadı.</em></div>");
            }
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    // =========================================================================
    // WORD (.DOCX) OLUŞTURUCU (SAF JAVA OOXML ZIP)
    // =========================================================================

    private static void writeDocx(
            File outFile,
            String title,
            String dateHuman,
            List<ReportSection> sections
    ) throws IOException {
        String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                + "</Types>";

        String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                + "</Relationships>";

        StringBuilder doc = new StringBuilder();
        doc.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">")
                .append("<w:body>");

        // Başlık ve tarih
        doc.append(docxParagraph(title, true, 32, "0F172A"));
        doc.append(docxParagraph("Rapor Tarihi: " + dateHuman, false, 20, "334155"));
        doc.append(docxParagraph("Filtre Kriterleri: İndirim >= %40 veya (Normal Fiyat > 1000 TL & İndirim >= %25)",
                false, 18, "475569"));

        for (ReportSection sec : sections) {
            doc.append(docxParagraph(sec.title, true, 26, "1E3A8A"));
            doc.append(docxParagraph(sec.subtitle + " | " + sec.sourceUrl, false, 18, "64748B"));

            for (String b : sec.bulletItems) {
                doc.append(docxParagraph("• " + b, false, 20, "1E293B"));
            }

            if (!sec.tableRows.isEmpty()) {
                doc.append("<w:tbl>")
                        .append("<w:tblPr>")
                        .append("<w:tblW w:w=\"9800\" w:type=\"dxa\"/>")
                        .append("<w:tblBorders>")
                        .append("<w:top w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CBD5E1\"/>")
                        .append("<w:left w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CBD5E1\"/>")
                        .append("<w:bottom w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CBD5E1\"/>")
                        .append("<w:right w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CBD5E1\"/>")
                        .append("<w:insideH w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CBD5E1\"/>")
                        .append("<w:insideV w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CBD5E1\"/>")
                        .append("</w:tblBorders>")
                        .append("</w:tblPr>");

                // Başlık satırı
                doc.append(docxRow(
                        new String[]{"Marka", "Ürün Adı", "Normal Fiyat", "İndirimli Fiyat", "İndirim Oranı"},
                        new int[]{1400, 4400, 1300, 1300, 1400},
                        true,
                        "1E3A8A",
                        "FFFFFF"
                ));

                for (int i = 0; i < sec.tableRows.size(); i++) {
                    GroupedDealRow r = sec.tableRows.get(i);
                    String fill = (i % 2 == 1) ? "F8FAFC" : "FFFFFF";
                    doc.append(docxRow(
                            new String[]{r.brandDisplay, r.productsDisplay, r.origPriceDisplay,
                                    r.discPriceDisplay, r.discountRateDisplay},
                            new int[]{1400, 4400, 1300, 1300, 1400},
                            false,
                            fill,
                            "0F172A"
                    ));
                }
                doc.append("</w:tbl>");
                doc.append(docxParagraph("", false, 16, "000000"));
            }
        }

        doc.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>")
                .append("<w:pgMar w:top=\"720\" w:right=\"720\" w:bottom=\"720\" w:left=\"720\"/></w:sectPr>")
                .append("</w:body></w:document>");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile))) {
            putZipEntry(zos, "[Content_Types].xml", contentTypes);
            putZipEntry(zos, "_rels/.rels", rels);
            putZipEntry(zos, "word/document.xml", doc.toString());
        }
    }

    private static void putZipEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static String docxParagraph(String text, boolean bold, int halfPt, String colorHex) {
        return "<w:p><w:pPr><w:spacing w:after=\"100\"/></w:pPr><w:r><w:rPr>"
                + (bold ? "<w:b/>" : "")
                + "<w:color w:val=\"" + colorHex + "\"/>"
                + "<w:sz w:val=\"" + halfPt + "\"/><w:szCs w:val=\"" + halfPt + "\"/>"
                + "</w:rPr><w:t xml:space=\"preserve\">" + escXml(text) + "</w:t></w:r></w:p>";
    }

    private static String docxRow(String[] cols, int[] widths, boolean bold, String fillHex, String textColorHex) {
        StringBuilder sb = new StringBuilder("<w:tr>");
        for (int i = 0; i < cols.length; i++) {
            sb.append("<w:tc><w:tcPr>")
                    .append("<w:tcW w:w=\"").append(widths[i]).append("\" w:type=\"dxa\"/>")
                    .append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"").append(fillHex).append("\"/>")
                    .append("</w:tcPr>")
                    .append("<w:p><w:pPr><w:spacing w:before=\"40\" w:after=\"40\"/></w:pPr>")
                    .append("<w:r><w:rPr>")
                    .append(bold ? "<w:b/>" : "")
                    .append("<w:color w:val=\"").append(textColorHex).append("\"/>")
                    .append("<w:sz w:val=\"17\"/><w:szCs w:val=\"17\"/>")
                    .append("</w:rPr><w:t xml:space=\"preserve\">").append(escXml(cols[i])).append("</w:t>")
                    .append("</w:r></w:p></w:tc>");
        }
        sb.append("</w:tr>");
        return sb.toString();
    }

    // =========================================================================
    // PDF (.PDF) OLUŞTURUCU (YERLEŞİK ANDROID PdfDocument - A4 ÇOKLU SAYFA)
    // =========================================================================

    private static void writePdf(
            File outFile,
            String title,
            String dateHuman,
            List<ReportSection> sections
    ) throws IOException {
        final int PAGE_W = 595; // A4 pt
        final int PAGE_H = 842;
        final int MARGIN = 28;
        final int CONTENT_W = PAGE_W - 2 * MARGIN;

        PdfDocument pdf = new PdfDocument();
        int pageNum = 1;
        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create());
        Canvas canvas = page.getCanvas();
        float y = MARGIN;

        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#0F172A"));
        titlePaint.setTextSize(15f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint subPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(Color.parseColor("#475569"));
        subPaint.setTextSize(9.5f);

        TextPaint secPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        secPaint.setColor(Color.parseColor("#1E3A8A"));
        secPaint.setTextSize(12f);
        secPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint cellPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setColor(Color.parseColor("#0F172A"));
        cellPaint.setTextSize(8.2f);

        TextPaint cellBoldPaint = new TextPaint(cellPaint);
        cellBoldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint headerTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        headerTextPaint.setColor(Color.WHITE);
        headerTextPaint.setTextSize(8.5f);
        headerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint bgHeader = new Paint();
        bgHeader.setColor(Color.parseColor("#1E3A8A"));
        Paint bgAlt = new Paint();
        bgAlt.setColor(Color.parseColor("#F8FAFC"));
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#CBD5E1"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(0.6f);

        // Üst başlık
        y = drawWrapped(canvas, title, titlePaint, MARGIN, y, CONTENT_W) + 4;
        y = drawWrapped(canvas, "Tarih: " + dateHuman + "  |  Kriter: >= %40 indirim veya (>1000 TL & >= %25)",
                subPaint, MARGIN, y, CONTENT_W) + 10;

        int[] colWidths = {78, 229, 76, 76, 80}; // toplam 539

        for (ReportSection sec : sections) {
            if (y > PAGE_H - 90) {
                pdf.finishPage(page);
                page = pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNum).create());
                canvas = page.getCanvas();
                y = MARGIN;
            }
            y = drawWrapped(canvas, sec.title, secPaint, MARGIN, y, CONTENT_W) + 2;
            y = drawWrapped(canvas, sec.subtitle, subPaint, MARGIN, y, CONTENT_W) + 6;

            for (String b : sec.bulletItems) {
                StaticLayout sl = buildLayout("• " + b, cellPaint, CONTENT_W - 8);
                if (y + sl.getHeight() + 4 > PAGE_H - MARGIN) {
                    pdf.finishPage(page);
                    page = pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNum).create());
                    canvas = page.getCanvas();
                    y = MARGIN;
                }
                canvas.save();
                canvas.translate(MARGIN + 4, y);
                sl.draw(canvas);
                canvas.restore();
                y += sl.getHeight() + 3;
            }

            if (!sec.tableRows.isEmpty()) {
                y += 4;
                y = drawTableHeader(canvas, MARGIN, y, colWidths, headerTextPaint, bgHeader, borderPaint);

                for (int i = 0; i < sec.tableRows.size(); i++) {
                    GroupedDealRow r = sec.tableRows.get(i);
                    String[] cols = {r.brandDisplay, r.productsDisplay, r.origPriceDisplay,
                            r.discPriceDisplay, r.discountRateDisplay};

                    StaticLayout[] layouts = new StaticLayout[5];
                    int maxH = 14;
                    for (int c = 0; c < 5; c++) {
                        layouts[c] = buildLayout(cols[c], c == 0 ? cellBoldPaint : cellPaint, colWidths[c] - 8);
                        if (layouts[c].getHeight() + 6 > maxH) maxH = layouts[c].getHeight() + 6;
                    }

                    if (y + maxH > PAGE_H - MARGIN) {
                        pdf.finishPage(page);
                        page = pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNum).create());
                        canvas = page.getCanvas();
                        y = MARGIN;
                        y = drawTableHeader(canvas, MARGIN, y, colWidths, headerTextPaint, bgHeader, borderPaint);
                    }

                    if (i % 2 == 1) {
                        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_W, y + maxH, bgAlt);
                    }

                    float x = MARGIN;
                    for (int c = 0; c < 5; c++) {
                        canvas.drawRect(x, y, x + colWidths[c], y + maxH, borderPaint);
                        canvas.save();
                        canvas.translate(x + 4, y + 3);
                        layouts[c].draw(canvas);
                        canvas.restore();
                        x += colWidths[c];
                    }
                    y += maxH;
                }
                y += 12;
            }
        }

        pdf.finishPage(page);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            pdf.writeTo(fos);
        } finally {
            pdf.close();
        }
    }

    private static float drawTableHeader(
            Canvas canvas, float startX, float y, int[] colWidths,
            TextPaint textPaint, Paint bg, Paint border
    ) {
        String[] headers = {"Marka", "Ürün Adı", "Normal", "İndirimli", "Oran"};
        float totalW = 0;
        for (int w : colWidths) totalW += w;
        float rowH = 18f;
        canvas.drawRect(startX, y, startX + totalW, y + rowH, bg);
        float x = startX;
        for (int c = 0; c < headers.length; c++) {
            canvas.drawRect(x, y, x + colWidths[c], y + rowH, border);
            canvas.drawText(headers[c], x + 4, y + 12, textPaint);
            x += colWidths[c];
        }
        return y + rowH;
    }

    private static StaticLayout buildLayout(String text, TextPaint paint, int width) {
        return StaticLayout.Builder
                .obtain(text == null ? "" : text, 0, text == null ? 0 : text.length(), paint, Math.max(10, width))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.08f)
                .setIncludePad(false)
                .build();
    }

    private static float drawWrapped(Canvas canvas, String text, TextPaint paint, float x, float y, int width) {
        StaticLayout sl = buildLayout(text, paint, width);
        canvas.save();
        canvas.translate(x, y);
        sl.draw(canvas);
        canvas.restore();
        return y + sl.getHeight();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String escXml(String s) {
        if (s == null) return "";
        String cleaned = s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        return esc(cleaned).replace("'", "&apos;");
    }
}
