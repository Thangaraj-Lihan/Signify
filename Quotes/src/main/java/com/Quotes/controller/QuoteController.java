package com.Quotes.controller;


import com.Quotes.model.Quote;
import com.Quotes.service.QuoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    @Autowired
    private QuoteService quoteService;

    @GetMapping("/quote")
    public ResponseEntity<Quote> getRandomQuote() {
        Quote quote = quoteService.getRandomQuote();
        return ResponseEntity.ok(quote);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<String> likeQuote(@PathVariable Long id) {
        quoteService.likeQuote(id);
        return ResponseEntity.ok("Quote with ID " + id + " liked successfully.");
    }

    @GetMapping("/similarquote")
    public ResponseEntity<Quote> searchQuotes(@RequestParam String quote, @RequestParam Long id) {
        Quote foundQuote = quoteService.findSimilarQuote(quote,id);
        return ResponseEntity.ok(foundQuote);
    }

}
