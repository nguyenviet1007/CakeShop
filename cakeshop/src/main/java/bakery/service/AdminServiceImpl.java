package bakery.service;

import bakery.dto.request.DashboardStatsDto;
import bakery.dto.request.UserRequestDto;
import bakery.entity.Role;
import bakery.entity.User;
import bakery.repository.OrderRepository;
import bakery.repository.ProductRepository;
import bakery.repository.RoleRepository;
import bakery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;

/**
 * Lớp triển khai các dịch vụ nghiệp vụ dành cho Quản trị viên.
 * Xử lý các logic phức tạp về thống kê, quản lý người dùng và bảo mật hệ thống.
 *
 * @author YourName
 * @version 1.1
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Truy vấn các số liệu thống kê tổng quát cho trang Dashboard.
     *
     * @param timeRange Khoảng thời gian lọc (hiện tại hỗ trợ "all").
     * @return Đối tượng DTO chứa tổng số user, sản phẩm, đơn hàng và doanh thu.
     */
    @Override
    public DashboardStatsDto getDashboardStats(String timeRange) {
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();

        // SỬA LỖI: Kiểm tra null an toàn tránh NullPointerException khi unboxing
        Double revenue = orderRepository.calculateTotalRevenue();
        double totalRevenue = (revenue != null) ? revenue : 0.0;

        return new DashboardStatsDto(totalUsers, totalProducts, totalOrders, totalRevenue);
    }

    /**
     * Tìm kiếm và lọc danh sách người dùng (trả về danh sách không phân trang).
     */
    @Override
    public List<User> getUsers(String keyword, String roleName) {
        return userRepository.searchAndFilterUsers(keyword, roleName);
    }

    /**
     * Đảo ngược trạng thái hoạt động của người dùng (Kích hoạt <-> Khóa).
     *
     * @param userId ID của người dùng cần thay đổi.
     * @return Đối tượng User sau khi cập nhật.
     * @throws IllegalArgumentException nếu không tìm thấy người dùng.
     */
    @Override
    @Transactional
    public User toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        // SỬA LỖI: Entity dùng Boolean active, không dùng int 0/1
        user.setActive(user.getActive() == null || !user.getActive());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User addAccount(UserRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException("Email này đã được sử dụng!");
        }

        User newUser = new User();
        newUser.setName(requestDto.getName());
        newUser.setEmail(requestDto.getEmail());
        newUser.setPhone(requestDto.getPhone());
        newUser.setAddress(requestDto.getAddress());
        newUser.setUsername(requestDto.getUsername() != null && !requestDto.getUsername().isEmpty()
                ? requestDto.getUsername() : requestDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(requestDto.getPassword()));

        // SỬA LỖI: Entity dùng Boolean
        newUser.setActive(true);

        if (requestDto.getRoleIds() != null && !requestDto.getRoleIds().isEmpty()) {
            // JpaRepository.findAllById nhận Iterable<Long>, requestDto.getRoleIds() đã là Set<Long> nên ok
            List<Role> roles = roleRepository.findAllById(requestDto.getRoleIds());
            newUser.setRoles(new HashSet<>(roles));
        }

        return userRepository.save(newUser);
    }

    @Override
    @Transactional
    public User editAccount(Long userId, UserRequestDto requestDto) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        existingUser.setName(requestDto.getName());
        existingUser.setPhone(requestDto.getPhone());
        existingUser.setAddress(requestDto.getAddress());

        if (requestDto.getRoleIds() != null) {
            List<Role> roles = roleRepository.findAllById(requestDto.getRoleIds());
            existingUser.setRoles(new HashSet<>(roles));
        }

        return userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        // SỬA LỖI: Kiểm tra null cho Authentication
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("Cần đăng nhập để thực hiện thao tác này");

        String currentUserEmail = auth.getName();

        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng!"));

        if (userToDelete.getEmail().equals(currentUserEmail)) {
            throw new IllegalArgumentException("Bạn không thể tự xóa chính mình!");
        }

        userRepository.deleteById(userId);
    }

    // SỬA LỖI: Tham số phải khớp hoàn toàn với AdminService (Long roleId, Integer active)
    @Override
    public Page<User> getUsersPaginated(String keyword, Long roleId, Integer active, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        return userRepository.searchAndFilterUsers(kw, roleId, active, pageable);
    }

    /**
     * Truy xuất danh sách 5 người dùng mới nhất phục vụ Dashboard.
     */
    @Override
    public List<User> getLatestUsers() {
        return userRepository.findTop5ByOrderByIdDesc();
    }

    /**
     * Lấy toàn bộ danh sách vai trò để đổ vào các Dropdown/Checkbox trên giao diện.
     */
    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Tìm kiếm nâng cao và phân trang người dùng.
     *
     * @param keyword Từ khóa tìm kiếm (Tên/Email).
     * @param roleId  ID vai trò cần lọc.
     * @param status  Trạng thái cần lọc.
     * @param page    Số trang (1-based).
     * @param size    Kích thước trang.
     * @return Đối tượng Page chứa kết quả đã lọc.
     */

}