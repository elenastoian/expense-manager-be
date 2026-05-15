package com.elenastoian.expense.manager.document.model;

import com.elenastoian.expense.manager.identity.domain.model.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    String originalName;
    String contentType;
    String size;
    LocalDateTime uploadDate;
    String status;
    @ManyToOne
    @JoinColumn(name = "owner_id")
    User owner;
}
