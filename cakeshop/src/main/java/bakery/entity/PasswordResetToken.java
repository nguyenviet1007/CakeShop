package bakery.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private LocalDateTime expiryDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public PasswordResetToken(){}

    public Long getId() { return id; }
    public String getToken() { return token; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public User getUser() { return user; }

    public void setId(Long id) { this.id = id; }
    public void setToken(String token) { this.token = token; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public void setUser(User user) { this.user = user; }
}