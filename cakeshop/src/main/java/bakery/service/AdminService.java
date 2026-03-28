package bakery.service;

import bakery.dto.request.DashboardStatsDto;
import bakery.dto.request.UserRequestDto;
import bakery.entity.Role;
import bakery.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Giao diện định nghĩa các dịch vụ quản trị hệ thống Bakery.
 * Cung cấp các phương thức để xử lý số liệu thống kê, quản lý người dùng
 * và phân quyền trong hệ thống.
 *
 * @author YourName
 * @version 1.0
 */
public interface AdminService {

    /**
     * Lấy số liệu thống kê tổng quan của hệ thống dựa trên khoảng thời gian lọc.
     * Các số liệu bao gồm: Tổng người dùng, tổng sản phẩm, tổng đơn hàng và doanh thu.
     *
     * @param timeRange Khoảng thời gian cần thống kê (ví dụ: "all", "today", "month").
     * @return Đối tượng DashboardStatsDto chứa các chỉ số thống kê.
     */
    DashboardStatsDto getDashboardStats(String timeRange);

    /**
     * Tìm kiếm và lọc danh sách tài khoản người dùng theo từ khóa và vai trò.
     *
     * @param keyword  Từ khóa tìm kiếm (theo tên hoặc email).
     * @param roleName Tên vai trò cần lọc (ví dụ: "ADMIN", "CUSTOMER").
     * @return Danh sách các đối tượng User thỏa mãn điều kiện.
     */
    List<User> getUsers(String keyword, String roleName);

    /**
     * Chuyển đổi trạng thái hoạt động của tài khoản người dùng.
     * Nếu tài khoản đang hoạt động (Status = 1) sẽ bị khóa (Status = 0) và ngược lại.
     *
     * @param userId Mã định danh duy nhất của người dùng.
     * @return Đối tượng User sau khi đã được cập nhật trạng thái.
     */
    User toggleUserStatus(Long userId);

    /**
     * Thực hiện thêm mới một tài khoản người dùng vào hệ thống.
     * Bao gồm các bước kiểm tra trùng lặp email và mã hóa mật khẩu.
     *
     * @param requestDto Đối tượng chứa thông tin yêu cầu tạo tài khoản.
     * @return Đối tượng User vừa được tạo thành công.
     */
    User addAccount(UserRequestDto requestDto);

    /**
     * Cập nhật thông tin chi tiết cho một tài khoản người dùng hiện có.
     *
     * @param userId     Mã định danh của tài khoản cần chỉnh sửa.
     * @param requestDto Đối tượng chứa thông tin cập nhật mới.
     * @return Đối tượng User sau khi đã lưu thông tin thay đổi.
     */
    User editAccount(Long userId, UserRequestDto requestDto);

    /**
     * Xóa vĩnh viễn một tài khoản khỏi hệ thống dựa trên ID.
     * Lưu ý: Cần kiểm tra các ràng buộc dữ liệu liên quan trước khi xóa.
     *
     * @param userId Mã định danh của người dùng cần xóa.
     */
    void deleteAccount(Long userId);

    /**
     * Lấy danh sách các người dùng mới được đăng ký gần đây nhất.
     * Thường được dùng để hiển thị tại mục "Recent Users" trên Dashboard.
     *
     * @return Danh sách 5 người dùng mới nhất.
     */
    List<User> getLatestUsers();

    /**
     * Lấy toàn bộ danh sách các vai trò (Roles) hiện có trong hệ thống.
     *
     * @return Danh sách các đối tượng Role.
     */
    List<Role> getAllRoles();

    /**
     * Tìm kiếm, lọc và phân trang danh sách người dùng.
     * Đây là phương thức nâng cao phục vụ cho giao diện danh sách người dùng lớn.
     *
     * @param keyword  Từ khóa tìm kiếm (Tên hoặc Email).
     * @param roleId   Mã ID của vai trò cần lọc.
     * @param active  Mã trạng thái tài khoản (1: Hoạt động, 0: Khóa).
     * @param page     Số thứ tự trang cần lấy (bắt đầu từ 1).
     * @param size     Số lượng bản ghi hiển thị trên mỗi trang.
     * @return Một đối tượng Page chứa danh sách người dùng và thông tin phân trang.
     */
    Page<User> getUsersPaginated(String keyword, Long roleId, Integer active, int page, int size);
}