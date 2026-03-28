package bakery.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.Set;

/**
 * Đối tượng vận chuyển dữ liệu (DTO) cho các yêu cầu liên quan đến tài khoản người dùng.
 * Lớp này được sử dụng để tiếp nhận thông tin từ giao diện người dùng (form) cho các
 * nghiệp vụ như tạo tài khoản mới, cập nhật hồ sơ cá nhân hoặc quản lý vai trò.
 *
 * @author YourName
 * @version 1.0
 */
@Getter
@Setter
public class UserRequestDto {

    /**
     * Họ và tên đầy đủ của người dùng.
     */
    private String name;

    /**
     * Địa chỉ email cá nhân (thường được dùng để liên hệ và khôi phục mật khẩu).
     */
    private String email;

    /**
     * Số điện thoại liên lạc của người dùng.
     */
    private String phone;

    /**
     * Địa chỉ thường trú hoặc địa chỉ giao hàng mặc định.
     */
    private String address;

    /**
     * Tên đăng nhập duy nhất dùng để truy cập vào hệ thống.
     */
    private String username;

    /**
     * Mật khẩu đăng nhập (dạng văn bản thô, cần được mã hóa trước khi lưu xuống cơ sở dữ liệu).
     */
    private String password;

    /**
     * Tập hợp các mã định danh (ID) của các vai trò được gán cho tài khoản này.
     * Ví dụ: 1 cho ADMIN, 2 cho STAFF, 3 cho CUSTOMER.
     */
    private Set<Long> roleIds;
}