package in.arpit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.arpit.entity.Driver;
@Repository

public interface DriverRepository extends JpaRepository<Driver, Long> {
	 public boolean existsByVehicleNumber(String vehicleNumber);

	 public  boolean existsByPhone(String phone);

	public  Optional<Driver> findByVehicleNumber(String vehicleNumber);

	public Optional<Driver> findByUser_Id(Long userId);

}
