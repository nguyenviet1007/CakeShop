package bakery.controller;

import bakery.entity.User;
import bakery.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // Hiển thị trang Login/Register
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    // Xử lý Đăng nhập
    @PostMapping("/login")
    public String login(@RequestParam String phoneNumber,
                        @RequestParam String password,
                        HttpSession session, Model model) {

        Optional<User> user = userRepository.findByPhone(phoneNumber);

        if (user.isPresent() && user.get().getPassword().equals(password)) {
            session.setAttribute("user", user);
            return "redirect:/";
        }

        model.addAttribute("error", "Số điện thoại hoặc mật khẩu không đúng!");
        return "login";
    }
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Xóa session khi đăng xuất
        return "redirect:/login";
    }

}
