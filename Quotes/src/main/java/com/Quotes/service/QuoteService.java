package com.Quotes.service;

import com.Quotes.exceptions.QuoteNotFoundException;
import com.Quotes.model.Quote;
import com.Quotes.repository.QuoteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuoteService {

    @Autowired
    private QuoteRepository quoteRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    private final String QUOTES_API_URL = "https://dummyjson.com/quotes";
    Set<Long> seenQuoteIds = new HashSet<>();


    //Load intial quotes from remote to h2 db
    @PostConstruct
    @Scheduled(fixedRate = 3600000) // Fetch quotes every hour
    public void fetchQuotesFromRemoteSource() {
        try {
            ResponseEntity<Quote.QuoteResponse> response = restTemplate.getForEntity(QUOTES_API_URL, Quote.QuoteResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Quote> quotes = response.getBody().getQuotes();
                quoteRepository.saveAll(quotes);
            }
        } catch (Exception e) {
            System.err.println("Error fetching quotes: " + e.getMessage());
        }
    }

    //Return highly rated quotes and random quotes to the user
    public Quote getRandomQuote() {
        List<Quote> quotes = quoteRepository.findAll();
        if (quotes.isEmpty()) {
            throw new QuoteNotFoundException("No quotes available");
        }

        List<Quote> unseenQuotes = filterSeenQuotes(quotes);


        if (unseenQuotes.isEmpty()) {
            seenQuoteIds.clear();
            unseenQuotes.addAll(quotes);
        }


        unseenQuotes.sort(Comparator.comparingInt(Quote::getLikeCount).reversed());


        int topPercentage = Math.max(1, unseenQuotes.size() / 10);
        List<Quote> topQuotes = unseenQuotes.subList(0, topPercentage);
        Quote randomQuote = topQuotes.get(new Random().nextInt(topQuotes.size()));


        seenQuoteIds.add(randomQuote.getId());

        return randomQuote;
    }

    //Allow quotes to be liked
    public void likeQuote(Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new QuoteNotFoundException("Quote not found with id: " + id));
        quote.setLikeCount(quote.getLikeCount() + 1);
        quoteRepository.save(quote);
    }

    // Return similar quotes || same author || random quotes to the user
    public Quote findSimilarQuote(String inputQuote, Long previousQuoteId) {
        List<Quote> allQuotes = quoteRepository.findAll();
        String[] inputWords = inputQuote.toLowerCase().split("\\W+");

        List<Quote> quotes = allQuotes.stream()
                .filter(quote -> !quote.getId().equals(previousQuoteId))
                .collect(Collectors.toList());

        List<Quote> unseenQuotes = filterSeenQuotes(quotes);

        if (unseenQuotes.isEmpty()) {
            seenQuoteIds.clear();
            unseenQuotes.addAll(quotes);
        }

        Quote similarQuote = findBestMatch(unseenQuotes, inputWords);


        if (similarQuote != null) {
            return Optional.of(similarQuote).get();
        }


        Quote anotherQuoteFromSameAuthor = getAnotherQuoteFromSameAuthor(inputQuote, previousQuoteId);
        if (anotherQuoteFromSameAuthor != null) {
            return Optional.of(anotherQuoteFromSameAuthor).get();
        }


        return Optional.ofNullable(getRandomUnseenQuote()).get();
    }


    // Logic for return quotes same author
    public Quote getAnotherQuoteFromSameAuthor(String inputQuote, Long previousQuoteId) {

        Quote previousQuote = quoteRepository.findById(previousQuoteId)
                .orElseThrow(() -> new QuoteNotFoundException("Quote not found with id: " + previousQuoteId));
        String author = previousQuote.getAuthor();

        List<Quote> quotesByAuthor = quoteRepository.findByAuthor(author);

        List<Quote> unseenQuotes = filterSeenQuotes(quotesByAuthor);


        if (!unseenQuotes.isEmpty()) {
           seenQuoteIds.add(unseenQuotes.get(0).getId());
           return unseenQuotes.get(0);
        }


        return null;
    }

    // Common logic for return random unseen quotes
    public Quote getRandomUnseenQuote() {
        List<Quote> quotes = quoteRepository.findAll();
        List<Quote> unseenQuotes = filterSeenQuotes(quotes);

        if (unseenQuotes.isEmpty()) {
            seenQuoteIds.clear();
            unseenQuotes.addAll(quotes);
        }

        Quote randomQuote = unseenQuotes.get(new Random().nextInt(unseenQuotes.size()));
        seenQuoteIds.add(randomQuote.getId());

        return randomQuote;
    }

    // common logic for find similar quotes based on same words containing string
    public Quote findBestMatch(List<Quote> quotes, String[] inputWords) {
        if (inputWords.length == 0) {
            return null;
        }

        Optional<Quote> bestMatch = Optional.empty();
        int maxMatchCount = 0;

        for (Quote quote : quotes) {
            Set<String> inputWordSet = new HashSet<>(Arrays.asList(inputWords));
            Set<String> quoteWordSet = new HashSet<>(Arrays.asList(quote.getQuote().split("\\W+")));

            inputWordSet.removeIf(word -> word.length() <= 3);
            quoteWordSet.removeIf(word -> word.length() <= 3);


            int matchCount = 0;
            for (String word : inputWordSet) {
                if (quoteWordSet.contains(word)) {
                    matchCount++;
                }
            }

            if (matchCount > maxMatchCount) {
                maxMatchCount = matchCount;
                bestMatch = Optional.of(quote);
            }
        }

        bestMatch.ifPresent(quote -> seenQuoteIds.add(quote.getId()));
        return bestMatch.orElse(null);
    }





    // Common logic for filter seen quotes
    public List<Quote> filterSeenQuotes(List<Quote> quotes) {
        List<Quote> unseenQuotes = new ArrayList<>();
        for (Quote quote : quotes) {
            if (!seenQuoteIds.contains(quote.getId())) {
                unseenQuotes.add(quote);
            }
        }
        return unseenQuotes;
    }


}
