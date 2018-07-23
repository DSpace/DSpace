package ua.edu.sumdu.essuir.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ua.edu.sumdu.essuir.entity.Publication;
import ua.edu.sumdu.essuir.service.ExportDocumentProcessorService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping(value = "/export")
public class ExportController {

    @Resource
    private ExportDocumentProcessorService documentProcessor;

    @ResponseBody
    @RequestMapping(value = "/user", method = RequestMethod.POST, produces = {"application/vnd.openxmlformats-officedocument.wordprocessingml.document"})
    public void export(@RequestParam("publications") String publicationList, @RequestParam("author") String author, HttpServletResponse response) throws IOException {
        List<Publication> publications = new ObjectMapper()
                .readValue(publicationList, new TypeReference<List<Publication>>() {
                });

        XWPFDocument document = documentProcessor.createDocument(author, publications);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        document.write(byteArrayOutputStream);

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=publications.docx");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.getOutputStream().write(byteArrayOutputStream.toByteArray());
        response.getOutputStream().flush();
    }
}
