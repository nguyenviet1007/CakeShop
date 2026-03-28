package bakery.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Đối tượng vận chuyển dữ liệu (DTO) phục vụ cho tính năng thống kê trên Dashboard.
 * Lớp này tổng hợp các số liệu quan trọng về người dùng, sản phẩm, đơn hàng và doanh thu
 * để hiển thị lên giao diện quản trị viên.
 *
 * @author YourName
 * @version 1.0
 */
@Getter
@Setter
@AllArgsConstructor
public class DashboardStatsDto {

    /**
     * Tổng số lượng người dùng (khách hàng và nhân viên) hiện có trong hệ thống.
     */
    private long totalUsers;

    /**
     * Tổng số lượng sản phẩm bánh và đồ uống đang được kinh doanh.
     */
    private long totalProducts;

    /**
     * Tổng số lượng đơn hàng đã được tạo (bao gồm tất cả các trạng thái).
     */
    private long totalOrders;

    /**
     * Tổng doanh thu thu được từ các đơn hàng đã hoàn thành (đơn vị: VNĐ).
     */
    private double totalRevenue;
}