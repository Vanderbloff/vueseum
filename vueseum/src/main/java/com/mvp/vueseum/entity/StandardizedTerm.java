package com.mvp.vueseum.entity;

import com.mvp.vueseum.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "standardized_terms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StandardizedTerm extends BaseEntity {

    @Column(name = "raw_term", nullable = false, length = 1000)
    private String rawTerm;

    @Column(name = "standardized_term", nullable = false, length = 1000)
    private String standardizedTerm;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(name = "access_count")
    private Integer accessCount;

    @PrePersist
    protected void onCreate() {
        this.lastAccessed = LocalDateTime.now();
        this.accessCount = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastAccessed = LocalDateTime.now();
    }
}