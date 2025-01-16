package com.shym.pdf_extract_create;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PdfController {
    @Autowired
    private PdfService pdfService;

    @GetMapping("/pdf/upload")
    public String pdfUploadPage() {
        return "pdf/upload";
    }

    @PostMapping("/pdf/upload")
    @ResponseBody
    public Map<String, Object> pdfUpload(PdfUploadDto input) {
        return pdfService.getInfoFromDoc(pdfService.loadText(input));
    }

    @PostMapping("/pdf/download")
    @ResponseBody
    public ResponseEntity<InputStreamResource> pdfDownload(@RequestBody PdfDownloadDto input) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfService.writePdf(input)));
    }
}
