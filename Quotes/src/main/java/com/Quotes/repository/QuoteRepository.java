package com.Quotes.repository;

import com.Quotes.model.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByAuthor(String author);
}
