package com.maisprati.destinify.backend.repositories;

import com.maisprati.destinify.backend.domain.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    Page<Hotel> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Hotel> findByCityContainingIgnoreCase(String city, Pageable pageable);

    Page<Hotel> findByNameContainingIgnoreCaseAndCityContainingIgnoreCase(String name, String city, Pageable pageable);

    @Query(value = """
            SELECT * FROM tb_hotels h
            WHERE (CAST(:name AS text) IS NULL OR
                translate(lower(h.name), 'áàâãéêíóôõúüç', 'aaaaeeiooouuc') LIKE
                '%' || translate(lower(CAST(:name AS text)), 'áàâãéêíóôõúüç', 'aaaaeeiooouuc') || '%')
              AND (CAST(:city AS text) IS NULL OR
                translate(lower(h.city), 'áàâãéêíóôõúüç', 'aaaaeeiooouuc') LIKE
                '%' || translate(lower(CAST(:city AS text)), 'áàâãéêíóôõúüç', 'aaaaeeiooouuc') || '%')
            """,
            countQuery = """
            SELECT count(*) FROM tb_hotels h
            WHERE (CAST(:name AS text) IS NULL OR
                translate(lower(h.name), 'áàâãéêíóôõúüç', 'aaaaeeiooouuc') LIKE
                '%' || translate(lower(CAST(:name AS text)), 'áàâãéêíóôõúüç', 'aaaaeeiooouuc') || '%')
              AND (CAST(:city AS text) IS NULL OR
                translate(lower(h.city), 'áàâãéêíóôõúüç', 'aaaaeeiooouuc') LIKE
                '%' || translate(lower(CAST(:city AS text)), 'áàâãéêíóôõúüç', 'aaaaeeiooouuc') || '%')
            """, nativeQuery = true)
    Page<Hotel> searchIgnoringAccents(@Param("name") String name, @Param("city") String city, Pageable pageable);

}
