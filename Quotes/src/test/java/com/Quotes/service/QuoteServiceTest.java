package com.Quotes.service;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.Quotes.exceptions.QuoteNotFoundException;
import com.Quotes.model.Quote;
import com.Quotes.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@ExtendWith(MockitoExtension.class)
public class QuoteServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private QuoteRepository quoteRepository;

    @InjectMocks
    private QuoteService quoteService;

    @BeforeEach
    void setUp() {

    }

    @Test
    void testFetchQuotesFromRemoteSource_success() {

        Quote.QuoteResponse mockResponse = new Quote.QuoteResponse();
        mockResponse.setQuotes(List.of(
                new Quote(1L, "Sample quote 1", "Author 1", 10),
                new Quote(2L, "Sample quote 2", "Author 2", 20)
        ));

        ResponseEntity<Quote.QuoteResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(Quote.QuoteResponse.class))).thenReturn(responseEntity);

        quoteService.fetchQuotesFromRemoteSource();

        verify(quoteRepository, times(1)).saveAll(mockResponse.getQuotes());
    }

    @Test
    void testFetchQuotesFromRemoteSource_failure() {

        when(restTemplate.getForEntity(anyString(), eq(Quote.QuoteResponse.class))).thenThrow(new RuntimeException("API error"));

        quoteService.fetchQuotesFromRemoteSource();

        verify(quoteRepository, never()).saveAll(any());

    }

    @Test
    void testLikeQuote_success() {
        Long quoteId = 1L;
        Quote existingQuote = new Quote(quoteId, "Sample quote", "Author", 10);
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(existingQuote));

        quoteService.likeQuote(quoteId);

        assertEquals(11, existingQuote.getLikeCount());
        verify(quoteRepository, times(1)).save(existingQuote);
    }

    @Test
    void testLikeQuote_quoteNotFound() {

        Long quoteId = 1L;
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(QuoteNotFoundException.class, () -> {
            quoteService.likeQuote(quoteId);
        });

        assertEquals("Quote not found with id: " + quoteId, exception.getMessage());
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void testGetRandomQuote_success() {

        List<Quote> quotes = Arrays.asList(
                new Quote(1L, "Sample quote 1", "Author 1", 5),
                new Quote(2L, "Sample quote 2", "Author 2", 10),
                new Quote(3L, "Sample quote 3", "Author 3", 15)
        );
        when(quoteRepository.findAll()).thenReturn(quotes);

        Quote randomQuote = quoteService.getRandomQuote();


        assertNotNull(randomQuote);
        assertTrue(quotes.contains(randomQuote));
    }

    @Test
    void testGetRandomQuote_noQuotesAvailable() {

        when(quoteRepository.findAll()).thenReturn(Collections.emptyList());

        Exception exception = assertThrows(QuoteNotFoundException.class, () -> {
            quoteService.getRandomQuote();
        });

        assertEquals("No quotes available", exception.getMessage());
    }

    @Test
    void testGetRandomQuote_allQuotesSeen() {

        List<Quote> quotes = Arrays.asList(
                new Quote(1L, "Sample quote 1", "Author 1", 5),
                new Quote(2L, "Sample quote 2", "Author 2", 10),
                new Quote(3L, "Sample quote 3", "Author 3", 15)
        );
        when(quoteRepository.findAll()).thenReturn(quotes);

        quoteService.seenQuoteIds.addAll(Arrays.asList(1L, 2L, 3L));

        Quote randomQuote = quoteService.getRandomQuote();

        assertNotNull(randomQuote);
        assertTrue(quotes.contains(randomQuote));
        assertEquals(1, quoteService.seenQuoteIds.size());
    }


    @Test
    void testGetAnotherQuoteFromSameAuthor_success() {
        Long previousQuoteId = 1L;
        String inputQuote = "Sample quote for testing";
        Quote previousQuote = new Quote(1L, "Sample quote 1", "Author 1", 5);
        when(quoteRepository.findById(previousQuoteId)).thenReturn(Optional.of(previousQuote));

        List<Quote> quotesByAuthor = Arrays.asList(
                new Quote(1L, "Sample quote 1", "Author 1", 5),
                new Quote(2L, "Sample quote 2", "Author 1", 10)
        );
        when(quoteRepository.findByAuthor("Author 1")).thenReturn(quotesByAuthor);

        Quote result = quoteService.getAnotherQuoteFromSameAuthor(inputQuote, previousQuoteId);

        assertNotNull(result);
        assertEquals(quotesByAuthor.get(0), result);
    }

    @Test
    void testGetAnotherQuoteFromSameAuthor_notFound() {

        Long previousQuoteId = 1L;
        String inputQuote = "Sample quote for testing";
        when(quoteRepository.findById(previousQuoteId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(QuoteNotFoundException.class, () -> {
            quoteService.getAnotherQuoteFromSameAuthor(inputQuote, previousQuoteId);
        });

        assertEquals("Quote not found with id: " + previousQuoteId, exception.getMessage());
    }

    @Test
    void testGetRandomUnseenQuote_positive() {
        List<Quote> quotes = Arrays.asList(
                new Quote(1L, "Sample quote 1", "Author 1", 5),
                new Quote(2L, "Sample quote 2", "Author 2", 10),
                new Quote(3L, "Sample quote 3", "Author 3", 15)
        );
        when(quoteRepository.findAll()).thenReturn(quotes);

        List<Long> seenQuoteIds = Arrays.asList(1L);
        quoteService.seenQuoteIds.addAll(seenQuoteIds);

        Quote randomQuote = quoteService.getRandomUnseenQuote();

        assertNotNull(randomQuote);
        assertTrue(quotes.contains(randomQuote));
    }


    @Test
    void testFilterSeenQuotes_someUnseenQuotes() {

        List<Quote> quotes = Arrays.asList(
                new Quote(1L, "Sample quote 1", "Author 1", 5),
                new Quote(2L, "Sample quote 2", "Author 2", 10),
                new Quote(3L, "Sample quote 3", "Author 3", 15)
        );
        quoteService.seenQuoteIds.add(1L);


        List<Quote> unseenQuotes = quoteService.filterSeenQuotes(quotes);


        assertEquals(2, unseenQuotes.size());
        assertTrue(unseenQuotes.contains(quotes.get(1)));
        assertTrue(unseenQuotes.contains(quotes.get(2)));
    }

    @Test
    void testFilterSeenQuotes_noUnseenQuotes() {
        List<Quote> quotes = Arrays.asList(
                new Quote(1L, "Sample quote 1", "Author 1", 5),
                new Quote(2L, "Sample quote 2", "Author 2", 10),
                new Quote(3L, "Sample quote 3", "Author 3", 15)
        );
        quoteService.seenQuoteIds.addAll(Arrays.asList(1L, 2L, 3L));

        List<Quote> unseenQuotes = quoteService.filterSeenQuotes(quotes);
        assertTrue(unseenQuotes.isEmpty());
    }


    @Test
    void testFindBestMatch_success() {
        String inputQuote = "Sample quote for testing";
        String[] inputWords = inputQuote.toLowerCase().split("\\W+");
        List<Quote> quotes = Arrays.asList(
                new Quote(1L, "Sample quote 1", "Author 1", 5),
                new Quote(2L, "Sample quote 2", "Author 2", 10),
                new Quote(3L, "Sample quote 3", "Author 3", 15)
        );


        Quote bestMatch = quoteService.findBestMatch(quotes, inputWords);

        assertNotNull(bestMatch);
        assertEquals(1L, bestMatch.getId());
    }

    @Test
    void testFindBestMatch_noMatchFound() {

        String inputQuote = "Go find yourself in its hidden depths";
        String[] inputWords = inputQuote.toLowerCase().split("\\W+");
        List<Quote> quotes = Arrays.asList(
                new Quote(1L, "Sample quote 1", "Author 1", 5),
                new Quote(2L, "Sample quote 2", "Author 2", 10),
                new Quote(3L, "Sample quote 3", "Author 3", 15)
        );

        Quote bestMatch = quoteService.findBestMatch(quotes, inputWords);

        assertNull(bestMatch);
    }


    @Test
    void testFindSimilarQuote_success() {

        Long previousQuoteId = 1L;
        String inputQuote = "Sample quote 1";
        String[] inputWords = inputQuote.toLowerCase().split("\\W+");

        List<Quote> quotes = Arrays.asList(
                new Quote(1L, "Sample quote 1", "Author 1", 5),
                new Quote(2L, "Sample quote 2", "Author 2", 10),
                new Quote(3L, "Sample quote 3", "Author 3", 15)
        );
        when(quoteRepository.findAll()).thenReturn(quotes);

        Quote expectedQuote = new Quote(2L, "Sample quote 2", "Author 2", 10);


        Quote result = quoteService.findSimilarQuote(inputQuote, previousQuoteId);


        assertEquals(expectedQuote.getId(), result.getId());
        assertEquals(expectedQuote.getQuote(), result.getQuote());
        assertEquals(expectedQuote.getAuthor(), result.getAuthor());
        assertEquals(expectedQuote.getLikeCount(), result.getLikeCount());
    }

}



