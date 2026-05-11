package com.example.smart_cinema_booking_system.repository;

import com.example.smart_cinema_booking_system.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Integer> {
}