package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.config.SecurityUtils;
import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import io.github.caioeduardopereirafelix.financeapi.service.StatementImportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/transaction/import")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StatementImportController {

    static final long MAX_BYTES = 2L * 1024 * 1024;

    private final StatementImportService importService;
    private final SecurityUtils securityUtils;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StatementImportService.Result importStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean invertSign) {
        if (file.getSize() > MAX_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_BYTES);
        }
        try {
            return importService.importFile(securityUtils.getAuthenticatedUser(), file.getBytes(), invertSign);
        } catch (IOException e) {
            throw new InvalidFieldException("file", "Nao foi possivel ler o arquivo enviado");
        }
    }
}
