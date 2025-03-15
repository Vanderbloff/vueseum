package com.mvp.vueseum.repository;

import com.mvp.vueseum.entity.StandardizedTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StandardizedTermRepository extends JpaRepository<StandardizedTerm, Long> {

    Optional<StandardizedTerm> findByRawTermAndCategory(String rawTerm, String category);

    @Query("SELECT st FROM StandardizedTerm st WHERE st.category = :category " +
            "AND LOWER(st.standardizedTerm) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<StandardizedTerm> findByStandardizedTermStartingWithAndCategory(
            String prefix, String category);

    @Query("SELECT st FROM StandardizedTerm st WHERE st.category = :category " +
            "AND LOWER(st.rawTerm) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<StandardizedTerm> findByRawTermStartingWithAndCategory(
            String prefix, String category);
}