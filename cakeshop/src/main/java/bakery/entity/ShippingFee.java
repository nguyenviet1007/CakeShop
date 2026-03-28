package bakery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shipping_fee")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(value = 0, message = "Phí phải >= 0")
    @Column(nullable = false)
    private Double deliveredFee = 0.0;

    @Min(value = 0, message = "Phí phải >= 0")
    @Column(nullable = false)
    private Double failedFee = 0.0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getDeliveredFee() {
        return deliveredFee;
    }

    public void setDeliveredFee(Double deliveredFee) {
        this.deliveredFee = deliveredFee;
    }

    public Double getFailedFee() {
        return failedFee;
    }

    public void setFailedFee(Double failedFee) {
        this.failedFee = failedFee;
    }
}