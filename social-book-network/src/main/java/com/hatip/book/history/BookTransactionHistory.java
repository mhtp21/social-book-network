package com.hatip.book.history;

import com.hatip.book.book.Book;
import com.hatip.book.common.BaseEntity;
import com.hatip.book.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class BookTransactionHistory extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // User ile ilişki kuruluyor
    private User user; // UUID yerine User nesnesi
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false) // Book ile ilişki kuruluyor
    private Book book;
    private boolean returned;
    private boolean returnApproved;
}

