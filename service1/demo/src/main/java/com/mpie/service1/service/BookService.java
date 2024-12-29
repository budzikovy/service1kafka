package com.mpie.service1.service;

import com.mpie.service1.mapper.BookMapper;
import com.mpie.service1.model.Book;
import com.mpie.service1.model.BookDto;
import com.mpie.service1.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final KafkaSender kafkaSender;

    public List<BookDto> getBooks() {
        List<Book> books = bookRepository.findAll();
        return bookMapper.toDtos(books);
    }

    @Transactional
    public BookDto createBook(BookDto bookDto) {
        Book entity = bookMapper.toEntity(bookDto);
        return bookMapper.toDto(bookRepository.save(entity));
    }

    @Transactional
    public BookDto rentBook(String clientName, String isbn) {
        Book book = bookRepository.findById(isbn)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book with given isbn does not exist"));
        if (nonNull(book.getBorrower())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book with the given isbn number is currently unavailable.");
        }
        book.setBorrower(clientName);
        bookRepository.save(book);
        kafkaSender.sendBookRented(book);
        return bookMapper.toDto(book);
    }

    @Transactional
    public void returnBook(String isbn) {
        Book book = bookRepository.findById(isbn)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book with given isbn does not exist"));
        book.setBorrower(null);
        bookRepository.save(book);
        kafkaSender.sendBookReturned(book);
    }


}
