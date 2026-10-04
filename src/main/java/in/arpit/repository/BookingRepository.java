package in.arpit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.arpit.entity.Booking;
@Repository

public interface BookingRepository extends JpaRepository<Booking, Long> {
	public List<Booking> findRecentBookingsByDriver(Long Id);

   public List<Booking> findByDriverIdAndStatus(Long Id,String status);
   List<Booking> findByParking_Owner_OwnerId(Long ownerId);
   List<Booking> findByDriver_Id(Long driverId);

}
