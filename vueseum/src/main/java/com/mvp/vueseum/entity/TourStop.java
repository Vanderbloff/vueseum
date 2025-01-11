package com.mvp.vueseum.entity;

import com.mvp.vueseum.entity.base.baseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tour_stops",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_tour_sequence",
                        columnNames = {"tour_id", "sequence_number"}
                )
        })
@Getter
@Setter
public class TourStop extends baseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    // Position in the tour sequence
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(columnDefinition = "TEXT")
    private String standardDescription;

    @Column(columnDefinition = "TEXT")
    private String tourContextDescription;


    // Whether this is a required stop (e.g., for themed tours)
    private boolean isRequired = false;

    @PrePersist
    @PreUpdate
    public void validateSequence() {
        if (sequenceNumber == null || sequenceNumber < 0) {
            throw new IllegalStateException("Sequence number must be non-negative");
        }
    }
}