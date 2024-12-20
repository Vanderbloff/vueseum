package com.mvp.artplatform.custom.annotation;

import com.mvp.artplatform.entity.Artist;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BirthDeathYearValidator implements ConstraintValidator<ValidBirthDeathYears, Artist> {

    @Override
    public boolean isValid(Artist artist, ConstraintValidatorContext context) {
        String birthYearStr = artist.getBirthDate();
        String deathYearStr = artist.getDeathDate();

        // Allow null values for deathYear (e.g., still alive)
        if (birthYearStr == null || deathYearStr == null) {
            return true;
        }

        try {
            int birthYear = Integer.parseInt(birthYearStr);
            int deathYear = Integer.parseInt(deathYearStr);

            // Validate deathYear is greater than birthYear and within a reasonable range
            return deathYear > birthYear && (deathYear - birthYear) <= 120;
        } catch (NumberFormatException e) {
            return false; // Invalid number format
        }
    }
}
