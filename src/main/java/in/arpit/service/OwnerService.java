package in.arpit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.arpit.dto.OwnerResponseDTO;
import in.arpit.entity.Owner;
import in.arpit.exception.OwnerNotFoundException;
import in.arpit.repository.OwnerRepository;
import in.arpit.repository.ParkingRepository;
import jakarta.transaction.Transactional;

@Service
public class OwnerService {

    private OwnerRepository ownerrepo;
    private ParkingRepository parkingrepo;

    @Autowired
    public OwnerService(OwnerRepository ownerrepo, ParkingRepository parkingrepo) {
        this.ownerrepo = ownerrepo;
        this.parkingrepo = parkingrepo;
    }

  
    private OwnerResponseDTO toResponse(Owner owner) {

        OwnerResponseDTO response = new OwnerResponseDTO();

        response.setOwnerId(owner.getOwnerId());
        response.setOwnerName(owner.getOwnerName());
        response.setPhone(owner.getPhone());

        return response;
    }

    public OwnerResponseDTO registerOwner(Owner o) {

        if (ownerrepo.existsByPhone(o.getPhone())) {
            throw new RuntimeException("Is phone number se owner already registered hai: " + o.getPhone());
        }

        Owner saved = ownerrepo.save(o);

        return toResponse(saved);
    }

    public Owner loginOwner(String phone) {

        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Phone number is required");
        }

        Owner o = ownerrepo.findByPhone(phone).orElse(null);

        if (o == null) {
            throw new OwnerNotFoundException("Owner not registered. Please register first.");
        }

        return o;
    }

    @Transactional
    public OwnerResponseDTO getOwnerById(Long id) {

        Owner o = ownerrepo.findById(id).orElse(null);

        if (o == null) {
            throw new OwnerNotFoundException("Owner not found with id : " + id);
        }

        return toResponse(o);
    }

    public OwnerResponseDTO getOwnerByPhone(String phone) {

        Owner o = ownerrepo.findByPhone(phone).orElse(null);

        if (o == null) {
            throw new OwnerNotFoundException("Given phone number owner not found : " + phone);
        }

        return toResponse(o);
    }

    @Transactional
    public OwnerResponseDTO updateOwner(Long ownerId, Owner o) {

        Owner o1 = ownerrepo.findById(ownerId).orElse(null);

        if (o1 == null) {
            throw new OwnerNotFoundException("Given Owner Id not found : " + ownerId);
        }

        o1.setOwnerName(o.getOwnerName());
        o1.setPhone(o.getPhone());

        Owner updated = ownerrepo.save(o1);

        return toResponse(updated);
    }

    public String deleteOwner(Long id) {

        Owner o1 = ownerrepo.findById(id).orElse(null);

        if (o1 == null) {
            throw new OwnerNotFoundException("Given Owner Id not found : " + id);
        }

        ownerrepo.delete(o1);

        return "Owner deleted successfully";
    }
}



