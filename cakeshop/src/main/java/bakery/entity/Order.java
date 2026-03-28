package bakery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Orders")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "shipper_id")
    private User shipper;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "order_date")
    private LocalDateTime orderDate = LocalDateTime.now();

    private String status;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    private String payment;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "delivery_image")
    private String deliveryImage;


    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // LƯU TIỀN TẠI THỜI ĐIỂM ĐÓ
    @Column(name = "delivered_fee")
    private Double deliveredFee;

    @Column(name = "failed_fee")
    private Double failedFee;

    // --- GETTER ---
    public Long getId() { return id; }
    public User getUser() { return user; }
    public User getShipper() { return shipper; }
    public List<OrderDetail> getOrderDetails() { return orderDetails; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public String getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getPayment() { return payment; }
    public String getFailureReason() { return failureReason; }
    public String getDeliveryImage() { return deliveryImage; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; } // 🔥 thêm

    // --- SETTER ---
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setShipper(User shipper) { this.shipper = shipper; }
    public void setOrderDetails(List<OrderDetail> orderDetails) { this.orderDetails = orderDetails; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public void setStatus(String status) { this.status = status; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setPayment(String payment) { this.payment = payment; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public void setDeliveryImage(String deliveryImage) { this.deliveryImage = deliveryImage; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; } //

    public Double getDeliveredFee() { return deliveredFee; }
    public Double getFailedFee() { return failedFee; }

    public void setDeliveredFee(Double deliveredFee) { this.deliveredFee = deliveredFee; }
    public void setFailedFee(Double failedFee) { this.failedFee = failedFee; }
}