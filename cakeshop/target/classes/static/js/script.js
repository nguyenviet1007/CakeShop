
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('searchInput');
    const suggestionsBox = document.getElementById('searchSuggestions');
    let debounceTimer;

    searchInput.addEventListener('input', function() {
        clearTimeout(debounceTimer);
        const query = this.value.trim();

        if (query.length === 0) {
            suggestionsBox.classList.add('d-none');
            return;
        }

        // Đợi 300ms sau khi người dùng ngừng gõ mới gọi API
        debounceTimer = setTimeout(() => {
            // Gọi API đến Backend để lấy danh sách sản phẩm
            fetch(` /search?keyword=${encodeURIComponent(query)}`)
                .then(response => {
                    if (!response.ok) throw new Error('Lỗi mạng');
                    return response.json();
                })
                .then(products => {
                    suggestionsBox.innerHTML = '';

                    if (products.length > 0) {
                        // Thay thế đoạn JS hiển thị cũ bằng đoạn này:
                        products.forEach(product => {
                            // Lấy URL ảnh trực tiếp từ JSON do Backend đã xử lý
                            const imgUrl = `/uploads/${product.imageUrl}`;

                            // Format giá tiền
                            const formattedPrice = new Intl.NumberFormat('vi-VN').format(product.price);

                            const item = document.createElement('div');
                            item.className = 'd-flex align-items-center p-2 border-bottom text-dark search-item';
                            item.style.cursor = 'pointer';

                            // Gắn sự kiện onclick gọi hàm loadProductDetail
                            item.onclick = function() {
                                loadProductDetail(product.productId);
                                document.getElementById('searchSuggestions').classList.add('d-none');
                                document.getElementById('searchInput').value = '';
                            };

                            // Render HTML (vẫn giữ onerror để phòng hờ)
                            item.innerHTML = `
        <img src="${imgUrl}" alt="${product.name}" 
             onerror="this.src='/img/chocolate-chip-cake0008choc-AAA.webp'"
             style="width: 50px; height: 50px; object-fit: cover; border-radius: 6px;" 
             class="me-3 shadow-sm">
        <div>
            <h6 class="mb-1 fw-bold" style="font-size: 14px;">${product.name}</h6>
            <span class="text-pink fw-bold" style="font-size: 13px;">${formattedPrice} VND</span>
        </div>
    `;
                            suggestionsBox.appendChild(item);
                        });
                        suggestionsBox.classList.remove('d-none');
                    } else {
                        suggestionsBox.innerHTML = '<div class="p-3 text-center text-muted small">Không tìm thấy sản phẩm nào</div>';
                        suggestionsBox.classList.remove('d-none');
                    }
                })
                .catch(error => {
                    console.error('Lỗi tìm kiếm:', error);
                });
        }, 300);
    });

    // Tự động ẩn menu gợi ý khi click ra ngoài thanh tìm kiếm
    document.addEventListener('click', function(e) {
        if (!searchInput.contains(e.target) && !suggestionsBox.contains(e.target)) {
            suggestionsBox.classList.add('d-none');
        }
    });
});

function addToCart(productId) {
    fetch(`/api/cart/add?productId=${productId}&quantity=1`, { method: 'POST' })
        .then(async res => {
            if (res.ok) {
                alert("Đã thêm bánh vào giỏ hàng! 🍰");
            } else {
                // Lấy nội dung text trả về từ ResponseEntity.body
                const errorMsg = await res.text();
                alert("Lỗi: " + errorMsg);
            }
        })
        .catch(err => {
            console.error(err);
            alert("Có lỗi hệ thống xảy ra!");
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
            alert("Thông báo: " + errorText);
            await loadCartData();
        }
    } catch (error) {
        console.error("Lỗi kết nối API:", error);
    }
}
let total=0;
let finalTotal = 0;
let appliedVoucherCode = null;
async function loadCartData() {

    const response = await fetch('/api/cart/items');
    const items = await response.json();
    console.log("Dữ liệu nhận được:", items);

    const list = document.getElementById('cartItemsList');
    const summary = document.getElementById('orderSummary');

    total = 0;

    list.innerHTML = '';
    summary.innerHTML = '';

    items.forEach(item => {

        const price = Number(item.price);
        const quantity = Number(item.quantity);
        const itemTotal = price * quantity;

        total += itemTotal;

        const imageUrl = item.product.mainImage?.imageUrl;

        // render cart list
        list.innerHTML += `
        <div class="d-flex align-items-center mb-3 p-2 border rounded-3 bg-white">
            <img src="/uploads/${imageUrl}" width="60" class="rounded me-3">
            <div class="flex-grow-1">
                <h6 class="mb-0">${item.product.name}</h6>
                <small class="text-pink">${price.toLocaleString('vi-VN')} VND</small>
                <div class="mt-1">
                    <button class="btn btn-sm" onclick="updateQty(${item.id}, ${quantity - 1})">-</button>
                    <span class="mx-2">${quantity}</span>
                    <button class="btn btn-sm" onclick="updateQty(${item.id}, ${quantity + 1})">+</button>
                </div>
            </div>
            <div class="mt-2">
                <button class="btn btn-sm text-danger p-0" onclick="removeFromCart(${item.id})">
                <small>Xóa</small>
                </button>
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

    document.getElementById('voucherMessage').innerText = '';
}
async function removeFromCart(itemId) {
    try {
        // 2. Gọi API update
        const response = await fetch(`/api/cart/delete?cartId=${itemId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            // 3. Cập nhật thành công thì nạp lại dữ liệu để UI thay đổi theo
            console.log("Xóa thành công");
            await loadCartData();
        } else {
            const errorText = await response.text();
            alert("Thông báo: " + errorText);
            await loadCartData();
        }
    } catch (error) {
        console.error("Lỗi kết nối API:", error);
    }
}
async function applyVoucher() {
    const code = document.getElementById('voucherCode').value.trim();
    const messageElement = document.getElementById('voucherMessage');
    const totalDisplay = document.getElementById('cartTotalPrice');

    if (!code) {
        messageElement.innerText = "Vui lòng nhập mã!";
        messageElement.className = "small mt-1 text-danger";
        return;
    }

    try {

        const response = await fetch(`/api/vouchers/check?code=${code}`);
        if (!response.ok) {
            const errorMsg = await response.text();
            throw new Error(errorMsg);
        }

        const voucher = await response.json();

        if (!response.ok || !voucher) {
            throw new Error("Mã giảm giá không hợp lệ hoặc đã hết hạn.");
        }

        // 1. Kiểm tra đơn hàng tối thiểu (Min Spend)
        if (total < voucher.minOrderValue) {
            throw new Error(`Đơn hàng tối thiểu ${voucher.minOrderValue.toLocaleString('vi-VN')}đ để sử dụng mã này.`);
        }

        // 2. Tính số tiền giảm theo %
        let discountAmount = (total * voucher.discountPercent) / 100;

        // 3. Kiểm tra mức giảm tối đa (Max Discount)
        if (discountAmount > voucher.maxDiscount) {
            discountAmount = voucher.maxDiscount;
        }

        finalTotal = total - discountAmount;

        // Hiển thị kết quả
        totalDisplay.innerHTML = `
            <del class="text-muted small">${total.toLocaleString('vi-VN')}đ</del> 
            <span class="text-pink">${finalTotal.toLocaleString('vi-VN')} VND</span>
        `;

        messageElement.innerText = `Đã áp dụng mã: Giảm ${discountAmount.toLocaleString('vi-VN')} VND`;
        messageElement.className = "small mt-1 text-success";
        appliedVoucherCode = code;

    } catch (error) {
        messageElement.innerText = error.message;
        messageElement.className = "small mt-1 text-danger";
        // Reset lại giá về ban đầu nếu áp dụng lỗi
        totalDisplay.innerText =total.toLocaleString('vi-VN') + ' VND';
    }
}

async function handleCheckout() {
    // Kiểm tra xem đã có phương thức thanh toán chưa
    const paymentRadio = document.querySelector('input[name="paymentMethod"]:checked');
    if (!paymentRadio) {
        alert("Vui lòng chọn phương thức thanh toán!");
        return;
    }
    const paymentMethod = paymentRadio.value;

    try {
        // 1. Gọi API tạo thông tin thanh toán (Gửi kèm mã voucher)
        const response = await fetch('/api/cart/checkout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            // Gửi voucherCode lên để Server tính lại giá khớp với DB
            body: JSON.stringify({ voucherCode: appliedVoucherCode })
        });

        if (response.status === 401) {
            alert("Vui lòng đăng nhập để thực hiện thanh toán!");
            window.location.href = "/login";
            return;
        }

        const data = await response.json();

        // Cập nhật giá hiển thị cuối cùng (Ưu tiên giá từ Server trả về cho chuẩn)
        const displayTotal = finalTotal || total;

        if (paymentMethod === "COD") {
            const confirmResponse = await fetch('/api/order/confirm', {
                method: "POST",
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    paymentMethod: "COD",
                    voucherCode: appliedVoucherCode
                })
            });

            if (confirmResponse.ok) {
                alert("Đặt hàng thành công!");
                window.location.href = "/";
            } else {
                const message = await confirmResponse.text();
                alert(message);
            }
            return;
        }

        // 2. Chèn nội dung QR vào Modal (Dùng displayTotal đã giảm giá)
        const qrContent = document.getElementById('qrContent');
        if (qrContent) {
            qrContent.innerHTML = `
                <div class="text-center p-3">
                    <h6 class="mb-3">Tổng thanh toán: <span class="text-danger fw-bold">${displayTotal.toLocaleString('vi-VN')} VND</span></h6>
                    ${appliedVoucherCode ? `<small class="text-success d-block mb-2">(Đã áp dụng mã: ${appliedVoucherCode})</small>` : ''}
                    <div class="bg-light p-2 rounded mb-3">
                        <img src="${data.qrUrl}" alt="Mã QR Thanh Toán" class="img-fluid shadow-sm" style="max-width: 250px;">
                    </div>
                    <p class="small text-muted mb-1">Nội dung chuyển khoản:</p>
                    <p class="fw-bold bg-warning-subtle d-inline-block px-2"></p>
                    <hr>
                    <button class="btn btn-success w-100 py-2" onclick="confirmPaymentSuccess()">Tôi đã chuyển khoản thành công</button>
                </div>
            `;
        }

        const checkoutModal = new bootstrap.Modal(document.getElementById('checkoutModal'));
        checkoutModal.show();

    } catch (error) {
        console.error("Lỗi khi xử lý thanh toán:", error);
        alert("Có lỗi xảy ra: " + error.message);
    }
}

// Hàm giả lập xác nhận sau khi chuyển khoản
async function confirmPaymentSuccess() {
    const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked').value;
    try {
        const response = await fetch('/api/order/confirm', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                paymentMethod: paymentMethod,
                voucherCode: appliedVoucherCode
            })
        });

        if (response.ok) {
            alert("Thanh toán thành công! Giỏ hàng đã được làm mới.");

            appliedVoucherCode = null;

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
async function loadFavProductDetail(productId) {
    sessionStorage.setItem("productId", productId);

    window.location.href = "/";
}
window.addEventListener("load", async () => {
    const productId = sessionStorage.getItem("productId");

    if (productId) {
        const response = await fetch(`/product/${productId}/fragment`);
        const html = await response.text();

        document.getElementById("productContent").innerHTML = html;

        sessionStorage.removeItem("productId");
    }
});
function changeImage(element) {

    const mainImage = document.getElementById("main-image");

    mainImage.src = element.src;

}

function toggleFavorite(productId, event) {
    event.stopPropagation();
    const heartBtn = event.currentTarget;
    const heartIcon = heartBtn.querySelector('i');

    // Gọi API đến Backend
    fetch(`/favorites/toggle/${productId}`, {
        method: 'POST'
    })
        .then(response => {
            if (response.status === 401) {
                alert("Bạn cần đăng nhập để thực hiện tính năng này!");
                return;
            }
            return response.json();
        })
        .then(isFavorited => {
            if (isFavorited !== undefined) {
                if (isFavorited) {
                    heartIcon.classList.remove('far', 'text-muted');
                    heartIcon.classList.add('fas', 'text-danger');
                } else {
                    heartIcon.classList.remove('fas', 'text-danger');
                    heartIcon.classList.add('far', 'text-muted');
                }
            }
            if (window.location.pathname.includes('/favorites')) {
                // Tìm thẻ card chứa sản phẩm và ẩn đi khi bỏ yêu thích
                heartBtn.closest('.col').remove();
            }
        })
        .catch(error => console.error('Lỗi:', error));
}
function confirmDeleteFavorite(productId) {
    if (confirm("Bạn có chắc chắn muốn xóa sản phẩm này khỏi danh sách yêu thích?")) {
        fetch(`/favorites/delete/${productId}`, {
            method: 'POST'
        })
            .then(response => {
                if (response.ok) {
                    return response.text();
                }
                throw new Error('Có lỗi xảy ra khi xóa');
            })
            .then(message => {
                // Hiển thị thông báo thành công
                alert(message);
                // Reload trang để cập nhật danh sách hoặc ẩn element bằng JS
                location.reload();
            })
            .catch(error => {
                alert(error.message);
            });
    }
}

function submitFeedback(event) {
    // 1. Ngăn chặn hành vi load lại trang mặc định của form
    event.preventDefault();

    // 2. Lấy dữ liệu từ form
    const form = event.target;
    const formData = new FormData(form);
    const productId = formData.get('productId');

    // 3. Gửi dữ liệu ngầm lên Backend bằng Fetch API
    fetch(form.action, {
        method: 'POST',
        body: formData
    })
        .then(response => {
            // Fetch API sẽ tự động bám theo lệnh redirect từ Backend
            if (response.ok) {
                // 4. Nếu thành công, gọi lại hàm load popup để cập nhật danh sách feedback mới
                alert("Cảm ơn bạn đã đánh giá sản phẩm!");

                // Hàm loadProductDetail này là hàm mở modal sản phẩm bạn đã có sẵn ở trang Home
                if (typeof loadProductDetail === 'function') {
                    loadProductDetail(productId);
                }
            } else {
                alert("Có lỗi xảy ra khi gửi đánh giá, vui lòng thử lại.");
            }
        })
        .catch(error => {
            console.error('Lỗi khi gửi feedback:', error);
            alert("Lỗi kết nối đến máy chủ.");
        });
}
document.querySelector(".profile-form").addEventListener("submit", function(e) {
    const phoneInput = document.getElementById("phone");
    const addressInput = document.getElementById("address");
    const nameInput = document.getElementById("name");
    const nameError = document.getElementById("nameError");


    const phoneValue = phoneInput.value.trim();
    const addressValue = addressInput.value.trim();
    const nameValue= nameInput.value.trim();

    const phonePattern = /^(0|\+84)[0-9]{9}$/;
    nameError.innerText = "";
    nameInput.classList.remove("is-invalid");


    if (phoneValue === "" || addressValue === ""|| nameValue==="") {
        e.preventDefault();
        alert("Vui lòng nhập đầy đủ thông tin!!");
        return;
    }


    if (!phonePattern.test(phoneValue)) {
        e.preventDefault();
        alert("Số điện thoại không hợp lệ! Vui lòng nhập định dạng 0xxxxxxxxx hoặc +84xxxxxxxxx.");
        phoneInput.focus();
        return;
    }

    if (addressValue.length <= 10) {
        e.preventDefault();
        alert("Địa chỉ phải dài hơn 10 ký tự!");
        addressInput.focus();
        return;
    }
    if (nameValue.length < 2) {
        e.preventDefault();
        nameError.innerText = "Tên hiển thị phải có ít nhất 2 ký tự";
        nameInput.classList.add("is-invalid");
        nameInput.focus();
        return;
    }

});