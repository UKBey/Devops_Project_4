package com.example.patent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "certifications")
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patent_id", nullable = false)
    private Patent patent;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "duration_years")
    private Integer durationYears;

    public Certification() {}

    public Certification(Author author, Patent patent, LocalDate issueDate, Integer durationYears) {
        this.author = author;
        this.patent = patent;
        this.issueDate = issueDate;
        this.durationYears = durationYears;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
    public Patent getPatent() { return patent; }
    public void setPatent(Patent patent) { this.patent = patent; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public Integer getDurationYears() { return durationYears; }
    public void setDurationYears(Integer durationYears) { this.durationYears = durationYears; }
}
