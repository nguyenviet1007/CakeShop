package bakery.controller;

import bakery.entity.*;
import bakery.repository.*;
import bakery.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class ForgotPasswordController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ===== FORM =====
    @GetMapping("/forgot-password")
    public String showForm() {
        return "forgot-password";
    }

    // ===== GỬI MAIL =====
    @PostMapping("/forgot-password")
    public String process(@RequestParam String username,
                          @RequestParam String email) {

        User user = userRepository.findByUsernameAndEmail(username, email);

        if (user == null) {
            return "redirect:/forgot-password?error";
        }

        String token = UUID.randomUUID().toString();

        PasswordResetToken t = new PasswordResetToken();
        t.setToken(token);
        t.setUser(user);
        t.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        tokenRepository.save(t);

        String link = "http://localhost:8080/reset-password?token=" + token;

        mailService.send(email, "Reset Password", "Click: " + link);

        return "redirect:/forgot-password?success";
    }

    // ===== CLICK LINK =====
    @GetMapping("/reset-password")
    public String showReset(@RequestParam String token, Model model) {

        PasswordResetToken t = tokenRepository.findByToken(token);

        if (t == null || t.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "redirect:/login?invalid";
        }

        model.addAttribute("token", token);

        return "reset-password";
    }

    // ===== RESET PASSWORD =====
    @PostMapping("/reset-password")
    public String reset(@RequestParam String token,
                        @RequestParam String password,
                        @RequestParam String confirmPassword,
                        Model model) {

        PasswordResetToken t = tokenRepository.findByToken(token);

        if (t == null) {
            return "redirect:/login?error";
        }

        // ✅ check confirm password
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "❌ Mật khẩu không trùng");
            model.addAttribute("token", token);
            return "reset-password";
        }

        // ✅ check độ dài
        if (password.length() < 6) {
            model.addAttribute("error", "❌ Mật khẩu phải >= 6 ký tự");
            model.addAttribute("token", token);
            return "reset-password";
        }

        User user = t.getUser();

        // ✅ encode password
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);

        // ✅ xoá token sau khi dùng
        tokenRepository.delete(t);

        return "redirect:/login?success";
    }
}