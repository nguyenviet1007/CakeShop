package bakery.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Đối tượng vận chuyển dữ liệu (DTO) cho yêu cầu liên quan đến bài viết (Blog).
 * Lớp này được sử dụng để hứng dữ liệu từ các biểu mẫu (form) thêm mới hoặc cập nhật bài viết,
 * bao gồm cả tệp tin hình ảnh tải lên.
 *
 * @author YourName
 * @version 1.0
 */
@Data
public class BlogRequestDto {

    /**
     * Tiêu đề của bài viết Blog.
     */
    private String title;

    /**
     * Đoạn tóm tắt ngắn gọn nội dung bài viết để hiển thị trên danh sách tin tức.
     */
    private String summary;

    /**
     * Nội dung chi tiết của bài viết (có thể chứa mã HTML từ các trình soạn thảo).
     */
    private String content;

    /**
     * Tệp tin hình ảnh đại diện cho bài viết được tải lên từ phía máy khách (Client).
     */
    private MultipartFile imageFile;
}