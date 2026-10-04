package in.arpit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.arpit.entity.Owner;
@Repository

public interface OwnerRepository  extends JpaRepository<Owner, Long>{
	 public  Optional<Owner> findByPhone(String phone);
     public boolean existsByPhone(String phone);

	 public Optional<Owner> findByUser_Id(Long userId);

}
