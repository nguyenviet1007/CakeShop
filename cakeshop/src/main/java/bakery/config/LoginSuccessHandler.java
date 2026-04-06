package bakery.config;

import bakery.entity.User;
import bakery.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        // 1. Lấy username từ Spring Security
        String username = authentication.getName();

        // 2. Tìm user trong DB và lưu vào Session
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            HttpSession session = request.getSession();
            session.setAttribute("user", userOpt.get());
        }

        // 3. Kiểm tra Role để điều hướng
        Collection<? extends GrantedAuthority> authorities =
                authentication.getAuthorities();

        System.out.println("===== LOGIN SUCCESS =====");

        for (GrantedAuthority auth : authorities) {
            String role = auth.getAuthority();
            System.out.println("ROLE: " + role);

            // Nếu là Admin -> vào trang quản trị
            if (role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER")) {
                response.sendRedirect("/admin/dashboard");
                return;
            }

            // Nếu là Shipper -> vào trang shipper
            if (role.equals("ROLE_SHIPPER")) {
                response.sendRedirect("/shipper");
                return;
            }
        }

        // 4. MẶC ĐỊNH: Điều hướng về "/" vì HomeController của bạn dùng @GetMapping("/")
        response.sendRedirect("/");
    }
}