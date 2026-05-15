package com.example.patent;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seed(AuthorRepository authorRepo,
                           PatentRepository patentRepo,
                           CertificationRepository certRepo) {
        return args -> {
            if (authorRepo.count() > 0) return;

            Author tesla  = authorRepo.save(new Author("Tesla", "Smiljan, Croatia"));
            Author edison = authorRepo.save(new Author("Edison", "Milan, Ohio"));
            Author curie  = authorRepo.save(new Author("Curie", "Warsaw, Poland"));

            Patent acMotor   = patentRepo.save(new Patent("AC Induction Motor", "Two-phase motor with rotating magnetic field"));
            Patent phonograph = patentRepo.save(new Patent("Phonograph", "Mechanical sound recording and reproduction"));
            Patent radium    = patentRepo.save(new Patent("Radium Isolation", "Method for isolating pure radium chloride"));
            Patent teslaCoil = patentRepo.save(new Patent("Tesla Coil", "Resonant transformer for high-voltage AC"));

            certRepo.save(new Certification(tesla,  acMotor,    LocalDate.of(1888, 5,  1), 17));
            certRepo.save(new Certification(tesla,  teslaCoil,  LocalDate.of(1891, 4, 25), 17));
            certRepo.save(new Certification(edison, phonograph, LocalDate.of(1878, 2, 19), 14));
            certRepo.save(new Certification(curie,  radium,     LocalDate.of(1903, 7,  1), 20));
        };
    }
}
