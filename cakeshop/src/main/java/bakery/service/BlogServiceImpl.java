package bakery.service;

import bakery.dto.request.BlogRequestDto;
import bakery.entity.Blog;
import bakery.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Lớp triển khai các logic nghiệp vụ liên quan đến quản lý bài viết (Blog).
 * Chịu trách nhiệm xử lý các thao tác CRUD, tìm kiếm phân trang và quản lý
 * tệp tin hình ảnh tải lên cho bài viết.
 *
 * @author YourName
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    /**
     * Kho lưu trữ dữ liệu bài viết.
     */
    private final BlogRepository blogRepository;

    /**
     * Truy xuất danh sách bài viết có phân trang và sắp xếp.
     * Mặc định sắp xếp theo mã ID giảm dần để hiển thị bài viết mới nhất lên đầu.
     *
     * @param keyword Từ khóa tìm kiếm trong tiêu đề bài viết.
     * @param page    Số thứ tự trang từ giao diện (bắt đầu từ 1).
     * @param size    Số lượng bản ghi trên mỗi trang.
     * @return Một trang dữ liệu bài viết (Page<Blog>).
     */
    @Override
    public Page<Blog> getBlogsPaginated(String keyword, int page, int size) {
        // Chuyển đổi từ 1-based page (UI) sang 0-based page (Spring Data JPA)
        return blogRepository.searchBlogs(
                keyword,
                PageRequest.of(page - 1, size, Sort.by("id").descending())
        );
    }

    /**
     * Tìm kiếm một bài viết dựa trên ID.
     *
     * @param id Mã định danh của bài viết.
     * @return Đối tượng Blog nếu tồn tại.
     * @throws IllegalArgumentException nếu không tìm thấy ID tương ứng trong DB.
     */
    @Override
    public Blog getBlogById(Integer id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết với ID: " + id));
    }

    /**
     * Thêm mới một bài viết vào hệ thống.
     * Thực hiện lưu trữ thông tin văn bản và xử lý tệp tin ảnh đính kèm.
     *
     * @param dto Đối tượng chứa dữ liệu bài viết từ yêu cầu của người dùng.
     */
    @Override
    @Transactional
    public void addBlog(BlogRequestDto dto) {
        Blog blog = new Blog();
        blog.setTitle(dto.getTitle());
        blog.setSummary(dto.getSummary());
        blog.setContent(dto.getContent());

        // Xử lý lưu trữ hình ảnh vật lý và nhận lại tên file để lưu vào DB
        blog.setImage(saveImage(dto.getImageFile()));

        blogRepository.save(blog);
    }

    /**
     * Cập nhật thông tin bài viết hiện có.
     * Chỉ thay đổi hình ảnh nếu người dùng có tải lên tệp tin mới.
     *
     * @param id  ID của bài viết cần chỉnh sửa.
     * @param dto Dữ liệu cập nhật mới.
     */
    @Override
    @Transactional
    public void editBlog(Integer id, BlogRequestDto dto) {
        Blog existingBlog = getBlogById(id);
        existingBlog.setTitle(dto.getTitle());
        existingBlog.setSummary(dto.getSummary());
        existingBlog.setContent(dto.getContent());

        // Nếu có tệp tin ảnh mới được tải lên thì mới thực hiện cập nhật trường image
        String newImage = saveImage(dto.getImageFile());
        if (newImage != null) {
            existingBlog.setImage(newImage);
        }

        blogRepository.save(existingBlog);
    }

    /**
     * Xóa bài viết khỏi hệ thống.
     * Kiểm tra sự tồn tại trước khi thực hiện xóa để tránh lỗi xung đột dữ liệu.
     *
     * @param id ID bài viết cần xóa.
     */
    @Override
    @Transactional
    public void deleteBlog(Integer id) {
        if (!blogRepository.existsById(id)) {
            throw new IllegalArgumentException("Không thể xóa bài viết không tồn tại ID: " + id);
        }
        blogRepository.deleteById(id);
    }

    /**
     * Phương thức hỗ trợ lưu trữ tệp tin hình ảnh vào thư mục vật lý của máy chủ.
     * Quy trình: Kiểm tra thư mục -> Đổi tên file bằng UUID -> Ghi file vào ổ đĩa.
     *
     * @param file Tệp tin nhận được từ MultipartRequest.
     * @return Tên tệp tin duy nhất đã lưu, trả về null nếu file trống hoặc lỗi.
     * @throws RuntimeException nếu xảy ra lỗi trong quá trình ghi tệp tin.
     */
    private String saveImage(MultipartFile file) {
        // Trả về null ngay nếu không có file được chọn hoặc file rỗng
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            // 1. Khởi tạo đường dẫn thư mục lưu trữ 'uploads'
            Path uploadPath = Paths.get("uploads");

            // 2. Tạo thư mục nếu chưa tồn tại trên hệ thống tệp tin
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 3. Sử dụng UUID để tạo tên file duy nhất, tránh ghi đè nếu trùng tên gốc
            String fileName = file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            // 4. Thực hiện sao chép luồng dữ liệu vào đích đến vật lý
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (Exception e) {
            // Bọc lỗi IO vào RuntimeException để kích hoạt cơ chế Rollback của Spring nếu cần
            throw new RuntimeException("Lỗi nghiêm trọng trong quá trình lưu trữ hình ảnh: " + e.getMessage());
        }
    }
}