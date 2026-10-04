package in.arpit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.arpit.entity.Parking;
@Repository
public interface ParkingRepository extends JpaRepository<Parking, Long> {
	 
	List<Parking> findByAvailableSlotsGreaterThan(int slots);
	List<Parking> findByLocationContainingIgnoreCase(String location);
	List<Parking> findByOwner_OwnerId(Long ownerId);

	/** Bounding-box pre-filter for nearby search. Rows without coordinates are excluded. */
	@Query("select p from Parking p where p.latitude is not null and p.longitude is not null "
			+ "and p.latitude between :minLat and :maxLat "
			+ "and p.longitude between :minLon and :maxLon")
	List<Parking> findWithinBox(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
			@Param("minLon") double minLon, @Param("maxLon") double maxLon);
}
