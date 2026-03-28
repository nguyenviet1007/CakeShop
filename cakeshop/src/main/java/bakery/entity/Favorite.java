package bakery.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "favorites")
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    public void setUser(User user) {
        this.user = user;
    }
    public void setProduct(Product product) {
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
    public User getUser() {
        return user;
    }
}
