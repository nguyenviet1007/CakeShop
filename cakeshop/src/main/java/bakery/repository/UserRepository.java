package bakery.repository;


import bakery.entity.Role;
import bakery.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    Optional<User> findByUsername(String username);
    User findByUsernameAndEmail(String username, String email);
    boolean existsByUsername(String username);
    List<User> findTop5ByOrderByIdDesc();
    @Query("SELECT u FROM User u LEFT JOIN u.roles r WHERE " +
            "(:roleName IS NULL OR r.name = :roleName) AND " +
            "(:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchAndFilterUsers(@Param("keyword") String keyword, @Param("roleName") String roleName);
    @Query("SELECT u FROM User u LEFT JOIN u.roles r WHERE " +
            "(:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:roleId IS NULL OR r.id = :roleId) AND " +
            "(:active IS NULL OR (u.active = true AND :active = 1) OR (u.active = false AND :active = 0))")
    Page<User> searchAndFilterUsers(
            @Param("keyword") String keyword,
            @Param("roleId") Long roleId, // Đổi Integer -> Long
            @Param("active") Integer active, // Giữ Integer để nhận 0/1 từ UI
            Pageable pageable);
    List<User> findByRolesContaining(Role role);
    @Query("""
    SELECT u FROM User u
    JOIN u.roles r
    WHERE r = :role
    AND (
        LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
    AND (:active IS NULL OR u.active = :active)
""")
    Page<User> searchShippers(Role role, String keyword, Boolean active, Pageable pageable);
    Page<User> findByRolesContaining(Role role, Pageable pageable);

}
