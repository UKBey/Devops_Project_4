package com.example.patent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {

    private final CertificationRepository repo;
    private final AuthorRepository authorRepo;
    private final PatentRepository patentRepo;

    public CertificationController(CertificationRepository repo,
                                   AuthorRepository authorRepo,
                                   PatentRepository patentRepo) {
        this.repo = repo;
        this.authorRepo = authorRepo;
        this.patentRepo = patentRepo;
    }

    @GetMapping
    public List<Certification> all() {
        return repo.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            Long authorId = Long.valueOf(body.get("authorId").toString());
            Long patentId = Long.valueOf(body.get("patentId").toString());
            LocalDate issueDate = LocalDate.parse(body.get("issueDate").toString());
            Integer duration = Integer.valueOf(body.get("durationYears").toString());

            Author author = authorRepo.findById(authorId).orElse(null);
            Patent patent = patentRepo.findById(patentId).orElse(null);
            if (author == null) return ResponseEntity.badRequest().body(Map.of("error", "Author not found: " + authorId));
            if (patent == null) return ResponseEntity.badRequest().body(Map.of("error", "Patent not found: " + patentId));

            return ResponseEntity.ok(repo.save(new Certification(author, patent, issueDate, duration)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
