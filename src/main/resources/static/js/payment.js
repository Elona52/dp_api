/**
 * 결제 페이지 JavaScript
 */
$(document).ready(function() {
    if (typeof window.IMP === 'undefined') {
        console.error('아임포트 스크립트가 로드되지 않았습니다.');
        alert('결제 시스템을 초기화할 수 없습니다. 페이지를 새로고침해주세요.');
        return;
    }
    
    const IMP = window.IMP;
    const impCode = window.iamportImpCode || 'imp00000000';
    console.log('아임포트 초기화:', impCode);
    IMP.init(impCode);
    
    $('#paymentBtn').click(function() {
        // 데이터 속성에서 값 가져오기
        const $data = $('#paymentData');
        const auctionNo = $data.data('auction-no') ? parseInt($data.data('auction-no')) : null;
        const itemId = $data.data('item-id') ? parseInt($data.data('item-id')) : null;
        const cltrNo = $data.data('cltr-no') || null;
        
        // 입찰서에서 전달된 보증금액이 있으면 사용, 없으면 기본 금액 사용
        const urlParams = new URLSearchParams(window.location.search);
        const depositAmount = urlParams.get('depositAmount');
        const bidAmount = urlParams.get('bidAmount');
        const amount = depositAmount ? parseInt(depositAmount) : (parseInt($data.data('amount')) || 0);
        
        const itemName = $data.data('item-name') || '';
        const buyerName = $data.data('buyer-name') || '';
        const buyerEmail = $data.data('buyer-email') || '';
        const buyerPhone = $data.data('buyer-phone') || '';
        
        // 결제 준비
        const requestData = {
            amount: amount,
            itemName: itemName
        };
        if (auctionNo) requestData.auctionNo = auctionNo;
        if (itemId) requestData.itemId = itemId;
        if (cltrNo) requestData.cltrNo = cltrNo;
        
        $.ajax({
            url: '/payment/prepare',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function(response) {
                if (response.success) {
                    requestPay(response.merchantUid, amount, itemName, buyerName, buyerEmail, buyerPhone);
                } else {
                    alert(response.message || '결제 준비에 실패했습니다.');
                }
            },
            error: function(xhr, status, error) {
                console.error('결제 준비 오류:', { status, error, responseText: xhr.responseText });
                let errorMessage = '결제 준비 중 오류가 발생했습니다.';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMessage = xhr.responseJSON.message;
                } else if (xhr.status === 401 || xhr.status === 403) {
                    errorMessage = '로그인이 필요합니다.';
                }
                alert(errorMessage);
            }
        });
    });
    
    function requestPay(merchantUid, amount, itemName, buyerName, buyerEmail, buyerPhone) {
        // 입찰서 정보 가져오기
        const urlParams = new URLSearchParams(window.location.search);
        const bidAmount = urlParams.get('bidAmount');
        const depositAmount = urlParams.get('depositAmount');
        const bidMethod = urlParams.get('bidMethod') || 'self';
        const selectedBank = urlParams.get('selectedBank') || '';
        
        IMP.request_pay({
            pg: 'html5_inicis',
            pay_method: 'card',
            merchant_uid: merchantUid,
            name: itemName,
            amount: amount,
            buyer_name: buyerName,
            buyer_email: buyerEmail,
            buyer_tel: buyerPhone
        }, function(rsp) {
            if (rsp.success) {
                $.ajax({
                    url: '/payment/complete',
                    method: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify({
                        imp_uid: rsp.imp_uid,
                        merchant_uid: rsp.merchant_uid
                    }),
                    success: function(result) {
                        if (result.success) {
                            // 입찰서 정보와 함께 success 페이지로 이동
                            let successUrl = '/payment/success?merchantUid=' + merchantUid;
                            if (bidAmount) successUrl += '&bidAmount=' + bidAmount;
                            if (depositAmount) successUrl += '&depositAmount=' + depositAmount;
                            if (bidMethod) successUrl += '&bidMethod=' + encodeURIComponent(bidMethod);
                            if (selectedBank) successUrl += '&selectedBank=' + encodeURIComponent(selectedBank);
                            location.href = successUrl;
                        } else {
                            alert('결제 검증에 실패했습니다: ' + (result.message || '알 수 없는 오류'));
                            location.href = '/payment/fail?message=' + encodeURIComponent(result.message || '결제 검증 실패');
                        }
                    },
                    error: function(xhr, status, error) {
                        console.error('결제 완료 처리 오류:', { status, error, responseText: xhr.responseText });
                        let errorMessage = '결제 완료 처리 중 오류가 발생했습니다.';
                        if (xhr.responseJSON && xhr.responseJSON.message) {
                            errorMessage = xhr.responseJSON.message;
                        }
                        alert(errorMessage);
                        location.href = '/payment/fail?message=' + encodeURIComponent(errorMessage);
                    }
                });
            } else {
                alert('결제에 실패했습니다: ' + rsp.error_msg);
                location.href = '/payment/fail?message=' + encodeURIComponent(rsp.error_msg);
            }
        });
    }
});

