package com.edwin.edwin_ai_agent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PDFGenerationToolTest {
    @TempDir
    Path tempDir;
    private String extractFilePath(String result) {
        String prefix = "PDF generated successfully to:";
        if (result == null || !result.startsWith(prefix)) {
            return null;
        }
        return result.substring(prefix.length()).trim();
    }
    private File createTestImage() throws Exception {
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(245, 248, 252));
            g.fillRect(0, 0, 640, 360);

            g.setColor(new Color(34, 64, 120));
            g.fillRoundRect(40, 40, 560, 120, 20, 20);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 28));
            g.drawString("AI MVP Prototype", 170, 110);

            g.setColor(new Color(80, 80, 80));
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g.drawString("Local test image for PDF tool", 150, 210);
        } finally {
            g.dispose();
        }

        File imageFile = tempDir.resolve("test-image.png").toFile();
        ImageIO.write(image, "png", imageFile);
        return imageFile;
    }


    @Test
    void testGenerateBasicPDF() {
        PDFGenerationTool tool = new PDFGenerationTool();

        String result = tool.generatePDF(
                "ai_mvp_plan.pdf",
                "AI 产品 MVP 开发计划",
                "面向创业团队的 0 到 1 执行方案",
                "本文档总结了 AI 产品 MVP 的目标、范围、阶段拆解与落地建议，适合用于项目启动和团队对齐。",
                """
                ## 项目背景
                目标是在有限时间内完成一个可验证需求的 AI MVP。

                ## 核心目标
                - 明确目标用户
                - 验证核心场景
                - 快速完成首版上线

                ### 执行建议
                > 建议优先聚焦单一核心场景，不要一开始做过多功能。

                ## 里程碑
                - 第 1 周：需求确认
                - 第 2 周：原型设计
                - 第 3 周：开发联调
                - 第 4 周：上线验证
                """,
                null,
                null
        );

        assertNotNull(result);
        assertTrue(result.startsWith("PDF generated successfully to:"), result);

        String filePath = extractFilePath(result);
        assertNotNull(filePath);
        assertTrue(new File(filePath).exists(), "生成的 PDF 文件不存在: " + filePath);
    }
}
