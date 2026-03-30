package com.edwin.edwin_ai_agent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.edwin.edwin_ai_agent.constant.FileConstant;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.file.Paths;

public class PDFGenerationTool {

    private static final DeviceRgb PRIMARY = new DeviceRgb(34, 64, 120);
    private static final DeviceRgb TEXT_MAIN = new DeviceRgb(33, 37, 41);
    private static final DeviceRgb TEXT_MUTED = new DeviceRgb(108, 117, 125);
    private static final DeviceRgb BOX_BG = new DeviceRgb(245, 248, 252);
    private static final DeviceRgb BOX_BORDER = new DeviceRgb(210, 220, 235);
    private static final DeviceRgb TABLE_HEADER_BG = new DeviceRgb(232, 239, 249);
    private static final DeviceRgb TABLE_BORDER = new DeviceRgb(220, 227, 237);

    @Tool(description = """
            Generate a polished Chinese PDF report, proposal, plan, or guide.
            The PDF should be structured and visually professional.
            When calling this tool:
            1. provide title
            2. provide optional subtitle
            3. provide summary
            4. provide markdown-like body content
            5. provide optional tables as JSON array
            6. provide optional images as JSON array

            markdownContent supports:
            - '## ' for section heading
            - '### ' for subsection heading
            - '- ' or '* ' for bullet list
            - '> ' for highlighted note

            tableJson format:
            [
              {
                "title": "Table title",
                "columns": ["col1", "col2"],
                "rows": [["v1", "v2"], ["v3", "v4"]]
              }
            ]

            imageJson format:
            [
              {
                "title": "Image title",
                "path": "D:/example/a.png",
                "caption": "optional caption",
                "widthPercent": 75
              },
              {
                "title": "Remote image",
                "url": "https://example.com/demo.jpg",
                "caption": "optional caption",
                "widthPercent": 70
              }
            ]
            """)
    public String generatePDF(
            @ToolParam(description = "PDF file name, for example: ai_mvp_plan.pdf") String fileName,
            @ToolParam(description = "Document title") String title,
            @ToolParam(description = "Optional subtitle or audience description") String subtitle,
            @ToolParam(description = "Executive summary, 1 to 3 short paragraphs") String summary,
            @ToolParam(description = "Body content in markdown-like format with sections and bullet points") String markdownContent,
            @ToolParam(description = "Optional table JSON array string") String tableJson,
            @ToolParam(description = "Optional image JSON array string, supports local path and remote URL") String imageJson
    ) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String safeFileName = normalizeFileName(fileName);
        String filePath = fileDir + "/" + safeFileName;

        try {
            FileUtil.mkdir(fileDir);

            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf, PageSize.A4)) {

                document.setMargins(56, 48, 56, 48);

                FontSet fonts = createFontSet();
                document.setFont(fonts.normalFont);

                pdf.getDocumentInfo().setTitle(StrUtil.blankToDefault(title, "未命名文档"));
                pdf.getDocumentInfo().setAuthor("Edwin AI Agent");
                pdf.getDocumentInfo().setSubject("AI generated PDF");
                pdf.getDocumentInfo().setKeywords("AI, PDF, report, plan");

                addCover(document, fonts, title, subtitle, summary);
                renderMarkdown(document, fonts, markdownContent);
                renderTables(document, fonts, tableJson);
                renderImages(document, fonts, imageJson);

                document.add(new LineSeparator(new SolidLine()).setMarginTop(28).setMarginBottom(12));
                document.add(new Paragraph("Generated by AI PDF Tool")
                        .setFont(fonts.normalFont)
                        .setFontSize(10)
                        .setFontColor(TEXT_MUTED)
                        .setTextAlignment(TextAlignment.RIGHT));
            }

            return "PDF generated successfully to: " + filePath;
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    private String normalizeFileName(String fileName) {
        String name = StrUtil.blankToDefault(fileName, "document.pdf").trim();
        if (!name.toLowerCase().endsWith(".pdf")) {
            name = name + ".pdf";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private FontSet createFontSet() throws java.io.IOException {
        String regularFontPath = Paths.get("src/main/resources/static/fonts/SourceHanSansCN-Regular.otf")
                .toAbsolutePath()
                .toString();
        String boldFontPath = Paths.get("src/main/resources/static/fonts/SourceHanSansCN-Bold.otf")
                .toAbsolutePath()
                .toString();

        PdfFont normalFont;
        PdfFont boldFont;

        if (FileUtil.exist(regularFontPath)) {
            normalFont = PdfFontFactory.createFont(regularFontPath, PdfEncodings.IDENTITY_H);
        } else {
            normalFont = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
        }

        if (FileUtil.exist(boldFontPath)) {
            boldFont = PdfFontFactory.createFont(boldFontPath, PdfEncodings.IDENTITY_H);
        } else {
            boldFont = normalFont;
        }

        return new FontSet(normalFont, boldFont);
    }

    private void addCover(Document document, FontSet fonts, String title, String subtitle, String summary) {
        document.add(new Paragraph(StrUtil.blankToDefault(title, "未命名文档"))
                .setFont(fonts.boldFont)
                .setFontSize(24)
                .setFontColor(PRIMARY)
                .setMarginBottom(8));

        if (StrUtil.isNotBlank(subtitle)) {
            document.add(new Paragraph(subtitle)
                    .setFont(fonts.normalFont)
                    .setFontSize(12)
                    .setFontColor(TEXT_MUTED)
                    .setMarginBottom(18));
        }

        document.add(new LineSeparator(new SolidLine()).setMarginBottom(20));

        if (StrUtil.isNotBlank(summary)) {
            Div summaryBox = new Div()
                    .setBackgroundColor(BOX_BG)
                    .setBorder(new SolidBorder(BOX_BORDER, 1))
                    .setPadding(14)
                    .setMarginBottom(24);

            summaryBox.add(new Paragraph("摘要")
                    .setFont(fonts.boldFont)
                    .setFontSize(13)
                    .setFontColor(PRIMARY)
                    .setMarginBottom(8));

            summaryBox.add(new Paragraph(summary)
                    .setFont(fonts.normalFont)
                    .setFontSize(11)
                    .setFontColor(TEXT_MAIN)
                    .setMultipliedLeading(1.5f)
                    .setMargin(0));

            document.add(summaryBox);
        }
    }

    private void renderMarkdown(Document document, FontSet fonts, String markdownContent) {
        if (StrUtil.isBlank(markdownContent)) {
            return;
        }

        String[] lines = markdownContent.split("\\r?\\n");
        com.itextpdf.layout.element.List bulletList = null;

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();

            if (line.isEmpty()) {
                if (bulletList != null) {
                    document.add(bulletList.setMarginBottom(10));
                    bulletList = null;
                }
                continue;
            }

            if (line.startsWith("## ")) {
                if (bulletList != null) {
                    document.add(bulletList.setMarginBottom(10));
                    bulletList = null;
                }

                document.add(new Paragraph(line.substring(3).trim())
                        .setFont(fonts.boldFont)
                        .setFontSize(16)
                        .setFontColor(PRIMARY)
                        .setMarginTop(18)
                        .setMarginBottom(10));
                continue;
            }

            if (line.startsWith("### ")) {
                if (bulletList != null) {
                    document.add(bulletList.setMarginBottom(10));
                    bulletList = null;
                }

                document.add(new Paragraph(line.substring(4).trim())
                        .setFont(fonts.boldFont)
                        .setFontSize(13)
                        .setFontColor(TEXT_MAIN)
                        .setMarginTop(10)
                        .setMarginBottom(6));
                continue;
            }

            if (line.startsWith("- ") || line.startsWith("* ")) {
                if (bulletList == null) {
                    bulletList = new com.itextpdf.layout.element.List()
                            .setSymbolIndent(12)
                            .setMarginLeft(8)
                            .setMarginBottom(8);
                }

                ListItem item = new ListItem();
                item.add(new Paragraph(line.substring(2).trim())
                        .setFont(fonts.normalFont)
                        .setFontSize(11)
                        .setFontColor(TEXT_MAIN)
                        .setMultipliedLeading(1.45f));

                bulletList.add(item);

            }

            if (line.startsWith("> ")) {
                if (bulletList != null) {
                    document.add(bulletList.setMarginBottom(10));
                    bulletList = null;
                }

                Div noteBox = new Div()
                        .setBackgroundColor(new DeviceRgb(250, 251, 255))
                        .setBorderLeft(new SolidBorder(PRIMARY, 3))
                        .setPaddingLeft(12)
                        .setPaddingTop(8)
                        .setPaddingBottom(8)
                        .setMarginBottom(12);

                noteBox.add(new Paragraph(line.substring(2).trim())
                        .setFont(fonts.normalFont)
                        .setFontSize(10.5f)
                        .setFontColor(TEXT_MAIN)
                        .setMultipliedLeading(1.4f)
                        .setMargin(0));

                document.add(noteBox);
                continue;
            }

            if (bulletList != null) {
                document.add(bulletList.setMarginBottom(10));
                bulletList = null;
            }

            document.add(new Paragraph(line)
                    .setFont(fonts.normalFont)
                    .setFontSize(11)
                    .setFontColor(TEXT_MAIN)
                    .setMultipliedLeading(1.55f)
                    .setMarginBottom(10));
        }

        if (bulletList != null) {
            document.add(bulletList.setMarginBottom(10));
        }
    }

    private void renderTables(Document document, FontSet fonts, String tableJson) {
        JSONArray tables = safeParseArray(tableJson);
        if (tables == null || tables.isEmpty()) {
            return;
        }

        document.add(new Paragraph("表格信息")
                .setFont(fonts.boldFont)
                .setFontSize(16)
                .setFontColor(PRIMARY)
                .setMarginTop(18)
                .setMarginBottom(12));

        for (int i = 0; i < tables.size(); i++) {
            JSONObject tableObj = tables.getJSONObject(i);
            if (tableObj == null) {
                continue;
            }

            String tableTitle = tableObj.getStr("title");
            JSONArray columns = tableObj.getJSONArray("columns");
            JSONArray rows = tableObj.getJSONArray("rows");

            if (columns == null || columns.isEmpty()) {
                continue;
            }

            int colCount = columns.size();
            Table table = new Table(UnitValue.createPercentArray(colCount))
                    .useAllAvailableWidth()
                    .setMarginBottom(16);

            if (StrUtil.isNotBlank(tableTitle)) {
                document.add(new Paragraph(tableTitle)
                        .setFont(fonts.boldFont)
                        .setFontSize(12.5f)
                        .setFontColor(TEXT_MAIN)
                        .setMarginTop(8)
                        .setMarginBottom(8));
            }

            for (int c = 0; c < columns.size(); c++) {
                String col = String.valueOf(columns.get(c));
                table.addHeaderCell(createHeaderCell(fonts, col));
            }

            if (rows != null) {
                for (int r = 0; r < rows.size(); r++) {
                    JSONArray row = rows.getJSONArray(r);
                    if (row == null) {
                        continue;
                    }

                    for (int c = 0; c < colCount; c++) {
                        String cellText = c < row.size() ? String.valueOf(row.get(c)) : "";
                        table.addCell(createBodyCell(fonts, cellText));
                    }
                }
            }

            document.add(table);
        }
    }

    private Cell createHeaderCell(FontSet fonts, String text) {
        return new Cell()
                .add(new Paragraph(StrUtil.blankToDefault(text, "-"))
                        .setFont(fonts.boldFont)
                        .setFontSize(10.5f)
                        .setFontColor(PRIMARY)
                        .setMargin(0))
                .setBackgroundColor(TABLE_HEADER_BG)
                .setBorder(new SolidBorder(TABLE_BORDER, 1))
                .setPadding(8);
    }

    private Cell createBodyCell(FontSet fonts, String text) {
        return new Cell()
                .add(new Paragraph(StrUtil.blankToDefault(text, "-"))
                        .setFont(fonts.normalFont)
                        .setFontSize(10.5f)
                        .setFontColor(TEXT_MAIN)
                        .setMultipliedLeading(1.35f)
                        .setMargin(0))
                .setBorder(new SolidBorder(TABLE_BORDER, 1))
                .setPadding(8);
    }

    private void renderImages(Document document, FontSet fonts, String imageJson) {
        JSONArray images = safeParseArray(imageJson);
        if (images == null || images.isEmpty()) {
            return;
        }

        document.add(new Paragraph("图片资料")
                .setFont(fonts.boldFont)
                .setFontSize(16)
                .setFontColor(PRIMARY)
                .setMarginTop(18)
                .setMarginBottom(12));

        Rectangle effectiveArea = document.getPageEffectiveArea(PageSize.A4);
        float maxContentWidth = effectiveArea.getWidth();

        for (int i = 0; i < images.size(); i++) {
            JSONObject imageObj = images.getJSONObject(i);
            if (imageObj == null) {
                continue;
            }

            String imageTitle = imageObj.getStr("title");
            String caption = imageObj.getStr("caption");
            Float widthPercent = imageObj.getFloat("widthPercent");

            if (StrUtil.isNotBlank(imageTitle)) {
                document.add(new Paragraph(imageTitle)
                        .setFont(fonts.boldFont)
                        .setFontSize(12.5f)
                        .setFontColor(TEXT_MAIN)
                        .setMarginTop(8)
                        .setMarginBottom(8));
            }

            Image image = buildImageFromConfig(imageObj);
            if (image == null) {
                continue;
            }

            float finalWidthPercent = widthPercent == null ? 78f : Math.max(25f, Math.min(widthPercent, 100f));
            float targetWidth = maxContentWidth * finalWidthPercent / 100f;

            image.scaleToFit(targetWidth, 480f);
            image.setHorizontalAlignment(HorizontalAlignment.CENTER);
            image.setMarginBottom(6);

            document.add(image);

            if (StrUtil.isNotBlank(caption)) {
                document.add(new Paragraph(caption)
                        .setFont(fonts.normalFont)
                        .setFontSize(9.5f)
                        .setFontColor(TEXT_MUTED)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(14));
            } else {
                document.add(new Paragraph("").setMarginBottom(10));
            }
        }
    }

    private Image buildImageFromConfig(JSONObject imageObj) {
        String path = imageObj.getStr("path");
        String url = imageObj.getStr("url");

        try {
            ImageData imageData;

            if (StrUtil.isNotBlank(path)) {
                if (!FileUtil.exist(path)) {
                    throw new IllegalArgumentException("Image file not found: " + path);
                }
                imageData = ImageDataFactory.create(path);
            } else if (StrUtil.isNotBlank(url)) {
                byte[] imageBytes = HttpUtil.downloadBytes(url);
                imageData = ImageDataFactory.create(imageBytes);
            } else {
                throw new IllegalArgumentException("Each image item must provide either 'path' or 'url'");
            }

            return new Image(imageData);
        } catch (Exception e) {
            String source = StrUtil.isNotBlank(path) ? path : url;
            System.err.println("Failed to load image: " + source + ", reason: " + e.getMessage());
            return null;
        }
    }

    private JSONArray safeParseArray(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return JSONUtil.parseArray(json);
        } catch (Exception e) {
            System.err.println("Invalid JSON array: " + e.getMessage());
            return null;
        }
    }

    private static class FontSet {
        private final PdfFont normalFont;
        private final PdfFont boldFont;

        private FontSet(PdfFont normalFont, PdfFont boldFont) {
            this.normalFont = normalFont;
            this.boldFont = boldFont;
        }
    }
}
