function addToCart(productId) {
    fetch(`/api/cart/add?productId=${productId}&quantity=1`, { method: 'POST' })
        .then(res => {
            if(res.ok) alert("Đã thêm bánh vào giỏ hàng! 🍰");
        });
}

function showCartModal() {
    const myModal = new bootstrap.Modal(document.getElementById('cartModal'));
    loadCartData();
    myModal.show();
}

async function updateQty(cartId, newQuantity) {
    // 1. Nếu số lượng giảm xuống 0, có thể hỏi người dùng có muốn xóa không
    if (newQuantity < 1) {
        if (confirm("Bạn có muốn xóa sản phẩm này khỏi giỏ hàng?")) {

        } else {
            return; // Dừng lại nếu không đồng ý
        }
    }

    try {
        // 2. Gọi API update    
        const response = await fetch(`/api/cart/update?cartId=${cartId}&quantity=${newQuantity}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            // 3. Cập nhật thành công thì nạp lại dữ liệu để UI thay đổi theo
            console.log("Cập nhật số lượng thành công");
            await loadCartData();
        } else {
            const errorText = await response.text();
            alert("Lỗi cập nhật: " + errorText);
        }
    } catch (error) {
        console.error("Lỗi kết nối API:", error);
    }
}

async function loadCartData() {

    const response = await fetch('/api/cart/items');
    const items = await response.json();

    const list = document.getElementById('cartItemsList');
    const summary = document.getElementById('orderSummary');

    let total = 0;

    list.innerHTML = '';
    summary.innerHTML = '';

    items.forEach(item => {

        const price = Number(item.price);
        const quantity = Number(item.quantity);
        const itemTotal = price * quantity;

        total += itemTotal;

        const imageUrl = item.product.images?.[0]?.imageUrl;

        // render cart list
        list.innerHTML += `
        <div class="d-flex align-items-center mb-3 p-2 border rounded-3 bg-white">
            <img src="/img/${imageUrl}" width="60" class="rounded me-3">
            <div class="flex-grow-1">
                <h6 class="mb-0">${item.product.name}</h6>
                <small class="text-pink">${price.toLocaleString('vi-VN')} VND</small>
                <div class="mt-1">
                    <button class="btn btn-sm" onclick="updateQty(${item.id}, ${quantity - 1})">-</button>
                    <span class="mx-2">${quantity}</span>
                    <button class="btn btn-sm" onclick="updateQty(${item.id}, ${quantity + 1})">+</button>
                </div>
            </div>
        </div>`;

        // render order summary
        summary.innerHTML += `
        <div class="d-flex justify-content-between small mb-1">
            <span>${item.product.name} x${quantity}</span>
            <span>${itemTotal.toLocaleString('vi-VN')} VND</span>
        </div>`;
    });

    document.getElementById('cartTotalPrice').innerText =
        total.toLocaleString('vi-VN') + ' VND';
}
async function handleCheckout() {
    const paymentMethod =
        document.querySelector('input[name="paymentMethod"]:checked').value;
    try {
        // 1. Gọi API tạo thông tin thanh toán
        const response = await fetch('/api/cart/checkout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        if (response.status === 401) {
            alert("Vui lòng đăng nhập để thực hiện thanh toán!");
            window.location.href = "/login";
            return;
        }

        const data = await response.json();

        if (paymentMethod === "COD") {

            const confirmResponse = await fetch('/api/order/confirm', {
                method: "POST",
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ paymentMethod: "COD" })
            });

            const message = await confirmResponse.text();

            if (confirmResponse.ok) {
                alert("Đặt hàng thành công!");
                await loadCartData();
                window.location.href = "/";
            } else {
                alert(message);
            }

            return;
        }

        // 2. Chèn nội dung QR vào Modal
        const qrContent = document.getElementById('qrContent');
        if (qrContent) {
            qrContent.innerHTML = `
                <div class="text-center p-3">
                    <h6 class="mb-3">Tổng tiền: <span class="text-danger fw-bold">${data.total.toLocaleString()} VND</span></h6>
                    <div class="bg-light p-2 rounded mb-3">
                        <img src="${data.qrUrl}" alt="Mã QR Thanh Toán" class="img-fluid shadow-sm" style="max-width: 250px;">
                    </div>
                    <p class="small text-muted mb-1">Nội dung chuyển khoản:</p>
                    <p class="fw-bold bg-warning-subtle d-inline-block px-2">Ali Cake Shop</p>
                    <hr>
                    <button class="btn btn-success w-100 py-2" onclick="confirmPaymentSuccess()">Tôi đã chuyển khoản thành công</button>
                </div>
            `;
        }

        // 3. Kích hoạt Modal hiển thị
        const checkoutModal = new bootstrap.Modal(document.getElementById('checkoutModal'));
        checkoutModal.show();

    } catch (error) {
        console.error("Lỗi khi xử lý thanh toán:", error);
        alert(error.message);
    }
}

// Hàm giả lập xác nhận sau khi chuyển khoản
async function confirmPaymentSuccess() {
    const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked').value;
    try {
        const response = await fetch('/api/order/confirm', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({paymentMethod: paymentMethod})
        });

        if (response.ok) {
            alert("Thanh toán thành công! Giỏ hàng đã được làm mới.");

            // 1. Đóng Modal thanh toán
            const checkoutModal = bootstrap.Modal.getInstance(document.getElementById('checkoutModal'));
            checkoutModal.hide();

            // 2. Cập nhật lại giao diện giỏ hàng (sẽ hiện trống)
            await loadCartData();

            // 3. Điều hướng người dùng về trang chủ hoặc trang lịch sử đơn hàng
            window.location.href = "/";
        } else {
            alert("Có lỗi xảy ra khi xác nhận đơn hàng.");
        }
    } catch (error) {
        console.error("Lỗi xác nhận:", error);
    }
}

async function loadProductDetail(productId) {

    const response = await fetch(`/product/${productId}/fragment`);

    const html = await response.text();

    document.getElementById("productContent").innerHTML = html;

}
function changeImage(element) {

    const mainImage = document.getElementById("main-image");

    mainImage.src = element.src;

}