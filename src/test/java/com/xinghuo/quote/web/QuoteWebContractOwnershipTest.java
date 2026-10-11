package com.xinghuo.quote.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class QuoteWebContractOwnershipTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void quoteSpecificHttpContractsLiveWithQuoteWebBoundary() throws IOException {
        Path main = ROOT.resolve("src/main/java/com/xinghuo");
        assertTrue(Files.isRegularFile(main.resolve("quote/web/PublicQuoteRequest.java")));
        assertTrue(Files.isRegularFile(main.resolve("quote/web/QuoteLeadDtos.java")));
        assertFalse(Files.exists(main.resolve("dto/PublicQuoteRequest.java")));
        assertFalse(Files.exists(main.resolve("dto/QuoteLeadDtos.java")));

        String controller = Files.readString(main.resolve("quote/web/PublicQuoteController.java"));
        String leadController = Files.readString(main.resolve("quote/web/AdminLeadController.java"));
        String mapper = Files.readString(main.resolve("quote/web/QuoteWebMapper.java"));

        assertFalse(controller.contains("import com.xinghuo.dto.PublicQuoteRequest"));
        assertFalse(leadController.contains("import com.xinghuo.dto.QuoteLeadDtos"));
        assertFalse(mapper.contains("import com.xinghuo.dto.PublicQuoteRequest"));
        assertFalse(mapper.contains("import com.xinghuo.dto.QuoteLeadDtos"));

        String request = Files.readString(main.resolve("quote/web/PublicQuoteRequest.java"));
        assertTrue(request.contains("@NotBlank @Size(max = 100) String game"));
        assertTrue(request.contains("@NotNull @AssertTrue(message = \"请先阅读并同意服务规则与隐私说明\") Boolean privacyConsent"));

        String leads = Files.readString(main.resolve("quote/web/QuoteLeadDtos.java"));
        assertTrue(leads.contains("record QuoteCrmUpdateRequest"));
        assertTrue(leads.contains("record QuoteLeadPageResponse"));
        assertTrue(leads.contains("record QuoteLeadResponse"));
    }
}
