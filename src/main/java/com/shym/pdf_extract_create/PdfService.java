package com.shym.pdf_extract_create;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PdfService {
    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Autowired
    private OllamaChatModel ollamaChatModel;

    public List<Document> loadText(PdfUploadDto input) {
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(input.getFile().getResource());
        return tikaDocumentReader.read();
    }

    public Map<String, Object> getInfoFromDoc(List<Document> documentList) {
        Map<String, Object> returnMap = new HashMap<String, Object>();

        final String EXTRACT_STRING_1 = "성명";
        final String EXTRACT_STRING_2 = "주민등록번호";

        final String SYSTEM_PROMPT = String.format(
                "한글로 작성된 문서에서 특정 데이터를 추출하길 원해, 다음 위임장에서 위임자의 %s, %s를 |로 분리해서 반환해줘. 만약, 추출할수 없을경우에는 빈 문자열로 |를 포함해서 반환해줘.",
                EXTRACT_STRING_1, EXTRACT_STRING_2);
        System.out.println(SYSTEM_PROMPT);

        Document doc = documentList.get(0);
        System.out.println(doc.getContent());

        ChatResponse response = openAiChatModel.call(
                new Prompt(new SystemMessage(SYSTEM_PROMPT), new UserMessage(doc.getContent())));
        String textContent = response.getResult().getOutput().getContent();
        System.out.println(textContent);
        String[] textContentArr = textContent.split("\\|");

        returnMap.put(EXTRACT_STRING_1, textContentArr[0]);
        returnMap.put(EXTRACT_STRING_2, textContentArr[1]);

        return returnMap;
    }

    public ByteArrayInputStream writePdf(PdfDownloadDto input) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PDDocument document = null;
        try {
            document = Loader.loadPDF(new ClassPathResource("/static/samples/일반위임장.pdf").getFile());
            PDPage page = document.getPage(0);
            PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true);

            PDType0Font font = PDType0Font.load(document,
                    new ClassPathResource("/static/samples/malgun.ttf").getFile());
            contentStream.setFont(font, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(220, 625);
            contentStream.showText(input.getName());
            contentStream.endText();
            contentStream.beginText();
            contentStream.newLineAtOffset(220, 605);
            contentStream.showText(input.getJumin());
            contentStream.endText();

            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document,
                    createStampImage(input.getName()), null);
            contentStream.drawImage(pdImage, 430, 40, 80, 80);

            contentStream.close();

            document.save(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (document != null)
                try {
                    document.close();
                } catch (IOException e) {
                }
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private byte[] createStampImage(String name) {
        int imageSize = 200;
        int fontSize = 84;
        int strokeSize = Math.round(fontSize / 10);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, imageSize, imageSize);
            g2d.setComposite(AlphaComposite.Src);

            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(strokeSize));
            g2d.drawRect(10, 10, imageSize - 20, imageSize - 20);

            g2d.setFont(new Font("Batang", Font.BOLD, 80));
            FontMetrics metrics = g2d.getFontMetrics();
            String formattedName = name + "인";

            int lineHeight = metrics.getAscent() + metrics.getDescent();
            int totalTextHeight = lineHeight * 2;
            int startY = (imageSize - totalTextHeight) / 3 + lineHeight;

            for (int i = 0; i < formattedName.length(); i += 2) {
                String line = formattedName.substring(i, Math.min(i + 2, formattedName.length()));
                int textWidth = metrics.stringWidth(line);
                int x = (imageSize - textWidth) / 2 + (strokeSize / 2);
                int y = startY + (i / 2) * lineHeight;
                g2d.drawString(line, x, y);
            }
            g2d.dispose();
            ImageIO.write(image, "png", baos);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }
}
