package in.arpit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.arpit.dto.DriverRequestDTO;
import in.arpit.dto.DriverResponseDTO;
import in.arpit.entity.Booking;
import in.arpit.entity.Driver;
import in.arpit.exception.DriverNotFoundException;
import in.arpit.exception.OwnerNotFoundException;
import in.arpit.repository.BookingRepository;
import in.arpit.repository.DriverRepository;
import jakarta.transaction.Transactional;

@Service
public class DriverService {

    private DriverRepository driverrepo;
    private BookingRepository bookrepo;

    @Autowired
    public DriverService(DriverRepository driverrepo, BookingRepository bookrepo) {
        super();
        this.driverrepo = driverrepo;
        this.bookrepo = bookrepo;
    }

    // ---------- Helper mapping methods ----------

    private Driver toEntity(DriverRequestDTO dto) {
        Driver driver = new Driver();
        driver.setName(dto.getName());
        driver.setVehicleNumber(dto.getVehicleNumber());
        driver.setPhone(dto.getPhone());
        return driver;
    }

    private DriverResponseDTO toResponseDTO(Driver driver) {
        DriverResponseDTO response = new DriverResponseDTO();
        response.setId(driver.getId());
        response.setName(driver.getName());
        response.setVehicleNumber(driver.getVehicleNumber());
        response.setPhone(driver.getPhone());
        return response;
    }

    // ---------- Business methods ----------

    @Transactional
    public DriverResponseDTO registerDriver(DriverRequestDTO dto) {

        Driver driver = toEntity(dto);

        if (driver.getVehicleNumber() == null || driver.getVehicleNumber().isBlank()) {
            throw new RuntimeException("Vehicle number required hai");
        }

        if (driver.getPhone() == null || driver.getPhone().isBlank()) {
            throw new RuntimeException("Phone number must not be blank");
        }

        if (driverrepo.existsByVehicleNumber(driver.getVehicleNumber())) {
            throw new RuntimeException("Vehicle number already registered : "
                    + driver.getVehicleNumber());
        }

        if (driverrepo.existsByPhone(driver.getPhone())) {
            throw new RuntimeException("Phone number already registered : "
                    + driver.getPhone());
        }

        Driver saved = driverrepo.save(driver);
        return toResponseDTO(saved);
    }

    public Driver loginDriver(String vehicleNumber) {

        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new RuntimeException("Vehicle number is required");
        }

        Driver driver = driverrepo.findByVehicleNumber(vehicleNumber).orElse(null);

        if (driver == null) {
            throw new DriverNotFoundException("Driver not registered. Please register first.");
        }

        return driver;
    }

    public DriverResponseDTO getDriverById(Long id) {
        Driver d = driverrepo.findById(id).orElse(null);
        if (d == null) {
            throw new DriverNotFoundException("Driver not found with id : " + id);
        }
        return toResponseDTO(d);
    }

    public DriverResponseDTO getDriverByVehicle(String vehicleNumber) {
        Driver d = driverrepo.findByVehicleNumber(vehicleNumber).orElse(null);

        if (d == null) {
            throw new DriverNotFoundException("Vehicle nahi mila : " + vehicleNumber);
        }

        return toResponseDTO(d);
    }

    @Transactional
    public DriverResponseDTO updateDriver(Long id, DriverRequestDTO dto) {

        Driver d = driverrepo.findById(id).orElse(null);
        if (d == null) {
            throw new DriverNotFoundException("Driver not found with id : " + id);
        }

        if (dto.getName() != null) {
            d.setName(dto.getName());
        }

        if (dto.getVehicleNumber() != null
                && !dto.getVehicleNumber().equals(d.getVehicleNumber())) {

            if (driverrepo.existsByVehicleNumber(dto.getVehicleNumber())) {
                throw new RuntimeException(
                        "Vehicle number already exists: " + dto.getVehicleNumber());
            }
            d.setVehicleNumber(dto.getVehicleNumber());
        }

        if (dto.getPhone() != null
                && !dto.getPhone().equals(d.getPhone())) {

            if (driverrepo.existsByPhone(dto.getPhone())) {
                throw new RuntimeException(
                        "Phone number already exists: " + dto.getPhone());
            }
            d.setPhone(dto.getPhone());
        }

        Driver updated = driverrepo.save(d);
        return toResponseDTO(updated);
    }

    public String deleteDriver(Long id) {

        Driver d = driverrepo.findById(id).orElse(null);
        if (d == null) {
            throw new OwnerNotFoundException("Given Owner Id not found : " + id);
        }
        driverrepo.delete(d);
        return "Driver deleted successfully";
    }
}