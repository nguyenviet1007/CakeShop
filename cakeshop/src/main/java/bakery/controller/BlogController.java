package bakery.controller;

import bakery.dto.request.BlogRequestDto;
import bakery.entity.Blog;
import bakery.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller xử lý các yêu cầu liên quan đến quản lý bài viết (Blog) trong khu vực quản trị.
 * Cung cấp các chức năng xem danh sách, tìm kiếm, thêm, sửa và xóa bài viết.
 *
 * @author YourName
 * @version 1.0
 */
@Controller
@RequestMapping("/admin/blogs")
@RequiredArgsConstructor
public class BlogController {

    /**
     * Dịch vụ xử lý logic nghiệp vụ cho bài viết.
     */
    private final BlogService blogService;

    /**
     * Hiển thị trang quản lý danh sách bài viết có hỗ trợ tìm kiếm và phân trang.
     *
     * @param keyword Từ khóa tìm kiếm bài viết (có thể để trống).
     * @param page    Số thứ tự trang hiện tại (mặc định là trang 1).
     * @param model   Đối tượng để truyền dữ liệu sang giao diện Thymeleaf.
     * @return Tên tệp view "admin/admin-blogs.html".
     */
    @GetMapping
    public String getBlogsPage(@RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "1") int page,
                               Model model) {
        // Truy xuất dữ liệu phân trang từ Service (mặc định mỗi trang 10 bản ghi)
        model.addAttribute("blogPage", blogService.getBlogsPaginated(keyword, page, 10));

        // Gửi lại keyword và trang hiện tại để hiển thị trên giao diện
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);

        return "admin/admin-blogs";
    }

    /**
     * Tiếp nhận dữ liệu từ form và thực hiện tạo mới một bài viết.
     *
     * @param dto Dữ liệu bài viết bao gồm tiêu đề, nội dung và tệp tin hình ảnh.
     * @param ra  Đối tượng để truyền thông báo thành công sau khi redirect.
     * @return Đường dẫn chuyển hướng về trang danh sách bài viết.
     */
    @PostMapping("/add")
    public String addBlog(@ModelAttribute BlogRequestDto dto, RedirectAttributes ra) {
        blogService.addBlog(dto);

        // Thông báo xuất hiện 1 lần duy nhất sau khi tải lại trang
        ra.addFlashAttribute("successMsg", "Đăng bài viết mới thành công!");

        return "redirect:/admin/blogs";
    }

    /**
     * Cập nhật thông tin của bài viết hiện có dựa trên mã ID.
     *
     * @param id  Mã định danh duy nhất của bài viết cần chỉnh sửa.
     * @param dto Dữ liệu cập nhật mới.
     * @param ra  Đối tượng truyền thông báo.
     * @return Đường dẫn chuyển hướng về trang danh sách bài viết.
     */
    @PostMapping("/edit/{id}")
    public String editBlog(@PathVariable Integer id, @ModelAttribute BlogRequestDto dto, RedirectAttributes ra) {
        blogService.editBlog(id, dto);

        ra.addFlashAttribute("successMsg", "Cập nhật bài viết thành công!");

        return "redirect:/admin/blogs";
    }

    /**
     * Xóa bài viết khỏi hệ thống dựa trên ID được cung cấp.
     *
     * @param id Mã định danh của bài viết cần xóa.
     * @param ra Đối tượng truyền thông báo.
     * @return Đường dẫn chuyển hướng về trang danh sách bài viết.
     */
    @PostMapping("/delete/{id}")
    public String deleteBlog(@PathVariable Integer id, RedirectAttributes ra) {
        blogService.deleteBlog(id);

        ra.addFlashAttribute("successMsg", "Đã xóa bài viết thành công!");

        return "redirect:/admin/blogs";
    }
}