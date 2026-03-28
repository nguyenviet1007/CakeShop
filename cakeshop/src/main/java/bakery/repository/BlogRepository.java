package bakery.repository;

import bakery.entity.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Giao diện kho lưu trữ cho thực thể Blog.
 * Cung cấp các phương thức truy vấn dữ liệu bài viết từ cơ sở dữ liệu.
 * Mở rộng JpaRepository để sử dụng các tính năng CRUD cơ bản và phân trang.
 *
 * @author YourName
 * @version 1.0
 */
@Repository
public interface BlogRepository extends JpaRepository<Blog, Integer> {

    /**
     * Tìm kiếm các bài viết Blog theo từ khóa xuất hiện trong tiêu đề.
     * Hỗ trợ tìm kiếm không phân biệt chữ hoa chữ thường và phân trang dữ liệu.
     *
     * @param keyword  Từ khóa dùng để tìm kiếm tiêu đề bài viết (nếu null sẽ lấy tất cả).
     * @param pageable Đối tượng chứa thông tin phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách các bài viết phù hợp với từ khóa.
     */
    @Query("SELECT b FROM Blog b WHERE " +
            "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Blog> searchBlogs(String keyword, Pageable pageable);
}