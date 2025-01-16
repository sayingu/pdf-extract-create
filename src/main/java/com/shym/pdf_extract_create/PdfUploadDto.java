package com.shym.pdf_extract_create;

import org.springframework.web.multipart.MultipartFile;
import lombok.Data;

@Data
public class PdfUploadDto {
    private MultipartFile file;
}
