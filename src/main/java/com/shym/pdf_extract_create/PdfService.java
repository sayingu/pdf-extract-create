package com.shym.pdf_extract_create;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PdfService {
    @Autowired
    private OpenAiChatModel chatModel;

    public List<Document> loadText(PdfUploadDto input) {
        TikaDocumentReader tikaDocumentReader =
                new TikaDocumentReader(input.getFile().getResource());
        return tikaDocumentReader.read();
    }

    public Map<String, Object> getInfoFromDoc(List<Document> documentList) {
        Map<String, Object> returnMap = new HashMap<String, Object>();

        final String EXTRACT_STRING_1 = "성명";
        final String EXTRACT_STRING_2 = "주민등록번호";

        Document doc = documentList.get(0);
        ChatResponse response = chatModel.call(new Prompt(
                new SystemMessage(
                        String.format("문서에서 특정 데이터를 추출하길 원해, 다음 위임장에서 위임자의 %s, %s를 |로 분리해서 반환해줘",
                                EXTRACT_STRING_1, EXTRACT_STRING_2)),
                new UserMessage(doc.getContent())));
        String textContent = response.getResult().getOutput().getContent();
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
}
