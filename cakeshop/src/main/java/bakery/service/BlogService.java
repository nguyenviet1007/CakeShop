package bakery.service;

import bakery.dto.request.BlogRequestDto;
import bakery.entity.Blog;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Giao diện định nghĩa các dịch vụ quản lý bài viết (Blog) dành cho quản trị viên.
 * Cung cấp các phương thức để xử lý danh sách bài viết, tra cứu chi tiết
 * và thực hiện các thao tác CRUD (Thêm, Sửa, Xóa).
 *
 * @author YourName
 * @version 1.0
 */
public interface BlogService {

    /**
     * Truy xuất danh sách bài viết có hỗ trợ tìm kiếm theo từ khóa và phân trang.
     *
     * @param keyword Từ khóa tìm kiếm xuất hiện trong tiêu đề bài viết (có thể là null).
     * @param page    Số thứ tự trang cần lấy (thường bắt đầu từ 1 theo logic giao diện).
     * @param size    Số lượng bản ghi tối đa hiển thị trên mỗi trang.
     * @return Một đối tượng Page chứa danh sách bài viết và thông tin phân trang.
     */
    Page<Blog> getBlogsPaginated(String keyword, int page, int size);

    /**
     * Tìm kiếm và trả về thông tin chi tiết của một bài viết dựa trên mã định danh.
     *
     * @param id Mã định danh duy nhất của bài viết (ID).
     * @return Đối tượng Blog tương ứng nếu tìm thấy.
     * @throws IllegalArgumentException nếu không tìm thấy bài viết với mã cung cấp.
     */
    Blog getBlogById(Integer id);

    /**
     * Tạo mới một bài viết vào hệ thống.
     * Phương thức này xử lý việc lưu trữ thông tin văn bản và tệp tin hình ảnh đại diện.
     *
     * @param dto Đối tượng chứa dữ liệu yêu cầu tạo bài viết (Tiêu đề, tóm tắt, nội dung, ảnh).
     */
    void addBlog(BlogRequestDto dto);

    /**
     * Cập nhật thông tin cho một bài viết hiện có trong cơ sở dữ liệu.
     * Nếu bài viết có ảnh mới, hệ thống sẽ thực hiện thay thế ảnh cũ.
     *
     * @param id  Mã định danh của bài viết cần chỉnh sửa.
     * @param dto Đối tượng chứa dữ liệu cập nhật mới.
     */
    void editBlog(Integer id, BlogRequestDto dto);

    /**
     * Loại bỏ hoàn toàn một bài viết khỏi hệ thống dựa trên ID.
     *
     * @param id Mã định danh của bài viết cần xóa.
     */
    void deleteBlog(Integer id);
}