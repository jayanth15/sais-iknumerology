package com.namesuggestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:friendlyTest;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class FriendlyEnHighlightTests {
    @Autowired MockMvc mvc;

    @Test void mappingsMatchUserTableIncludingBlankNine() {
        assertEquals("4", FriendlyEn.forVithi(1));
        assertEquals("7", FriendlyEn.forVithi(2));
        assertEquals("9", FriendlyEn.forVithi(3));
        assertEquals("1", FriendlyEn.forVithi(4));
        assertEquals("9", FriendlyEn.forVithi(5));
        assertEquals("9", FriendlyEn.forVithi(6));
        assertEquals("2", FriendlyEn.forVithi(7));
        assertEquals("5, 6", FriendlyEn.forVithi(8));
        assertEquals("", FriendlyEn.forVithi(9));
    }

    @Test void july1992ProducesHighlightedFourAndFriendlyOne() {
        var result = new RaasiCalculatorService().calculate(LocalDate.of(1992,7,3), LocalTime.of(13,49), "Kanchipuram");
        assertEquals(4, result.vithiEn());
        assertEquals("1", result.friendlyEn());
    }

    @Test @WithMockUser void selectedRowAppearsInRenderedPageExactlyOnce() throws Exception {
        String html = mvc.perform(post("/raasi").with(csrf())
            .param("dateOfBirth","1992-07-03")
            .param("timeOfBirth","13:49")
            .param("placeOfBirth","Kanchipuram"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("Your friendly number"));
        assertTrue(html.contains("Friendly En <strong>1</strong>"));
        var currentRows = Pattern.compile("<tr[^>]*class=\"is-current\"[^>]*>").matcher(html);
        assertEquals(1,currentRows.results().count(),"Only selected Vithi En row can be highlighted");
        assertTrue(Pattern.compile("<tr[^>]*class=\"is-current\"[^>]*>\\s*<th[^>]*>4<span").matcher(html).find());
        assertTrue(html.contains("✓ Your Vithi En"));
    }
}
