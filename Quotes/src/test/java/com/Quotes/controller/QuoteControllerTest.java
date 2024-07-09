package com.Quotes.controller;



import com.Quotes.model.Quote;
import com.Quotes.service.QuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class QuoteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuoteService quoteService;

    @InjectMocks
    private QuoteController quoteController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(quoteController).build();
    }

    @Test
    void testGetRandomQuote() {
        Quote expectedQuote = new Quote(1L, "Sample quote", "Author 1", 10);

        when(quoteService.getRandomQuote()).thenReturn(expectedQuote);

        ResponseEntity<Quote> response = quoteController.getRandomQuote();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedQuote, response.getBody());
    }


    @Test
    public void testLikeQuote() throws Exception {
        Long quoteId = 1L;
        doNothing().when(quoteService).likeQuote(eq(quoteId));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/quotes/{id}/like", quoteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Quote with ID 1 liked successfully."));
    }

    @Test
    public void testSearchQuotes() throws Exception {
        Long quoteId = 1L;
        Quote mockQuote = new Quote(quoteId, "Test quote", "Author", 0);
        when(quoteService.findSimilarQuote(anyString(), eq(quoteId))).thenReturn(mockQuote);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/quotes/similarquote")
                        .param("quote", "Test")
                        .param("id", String.valueOf(quoteId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.quote").value("Test quote"))
                .andExpect(jsonPath("$.author").value("Author"))
                .andExpect(jsonPath("$.likeCount").value(0));
    }
}


