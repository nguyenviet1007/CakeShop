package bakery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "Users")
@Data // Tự động tạo Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // Constructor không đối số (thay thế public User(){})
@AllArgsConstructor // Constructor đầy đủ đối số
@Builder // Hỗ trợ tạo object theo pattern Builder (rất hữu ích khi tạo User từ OAuth2)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Sử dụng Integer theo file thứ 2 (hoặc Long tùy sở thích của bạn)

    @Column(columnDefinition = "nvarchar(255)") // Giữ nvarchar để hỗ trợ tiếng Việt có dấu
    private String name;

    @Column(unique = true) // Email nên là duy nhất
    private String email;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không đúng định dạng (10 số, bắt đầu bằng 0 hoặc +84)")
    private String phone;
    @Size(min = 11, message = "Địa chỉ phải cụ thể hơn (độ dài trên 10 ký tự)")
    private String address;

    @Column(unique = true) // Username cũng nên duy nhất
    private String username;

    private String password;

    // 🔥 Tính năng Soft Delete / Trạng thái tài khoản từ file thứ 2
    @Column(name = "active")
    private Boolean active = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "User_Role", // Tên bảng trung gian
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    // GETTER
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Set<Role> getRoles() { return roles; }
    public Boolean getActive() { return active; } // 🔥 thêm

    // SETTER
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public void setActive(Boolean active) { this.active = active; } //
}