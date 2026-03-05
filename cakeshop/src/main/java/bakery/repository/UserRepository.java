package bakery.repository;


import bakery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Tìm bằng Số điện thoại
    Optional<User> findByPhone(String phone);

    // Tìm bằng Email
    Optional<User> findByEmail(String email);

    // Kiểm tra tồn tại để phục vụ Đăng ký
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}
