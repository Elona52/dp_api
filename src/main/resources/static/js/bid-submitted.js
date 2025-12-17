/**
 * 입찰서 제출 완료 페이지 JavaScript
 */

function toggleCollapsible(header) {
    const content = header.nextElementSibling;
    header.classList.toggle('active');
    content.classList.toggle('active');
}

function goToPayment(paymentId) {
    if (!paymentId) {
        alert('결제 정보를 찾을 수 없습니다.');
        return;
    }

    // paymentId를 파라미터로 전달하여 결제 페이지에서 조회
    const url = '/payment/checkout?paymentId=' + paymentId;
    window.location.href = url;
}

