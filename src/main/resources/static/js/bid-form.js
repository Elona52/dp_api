/**
 * 입찰서 작성 페이지 JavaScript
 */

let selectedBidMethod = 'self';
let selectedPaymentMethod = 'cash';
const depositRate = 5; // 5%
let minBidAmount = 0;

// 페이지 로드 시 minBidAmount 설정
$(document).ready(function() {
    // Thymeleaf 변수를 JavaScript로 전달
    const minBidAmountElement = document.getElementById('minBidAmount');
    if (minBidAmountElement && minBidAmountElement.value) {
        minBidAmount = parseInt(minBidAmountElement.value.replace(/[^0-9]/g, '')) || 0;
    }
});

function selectBidMethod(method) {
    selectedBidMethod = method;
    document.querySelectorAll('.bid-method-btn').forEach(btn => {
        btn.classList.remove('selected');
    });
    document.querySelector(`[data-method="${method}"]`).classList.add('selected');
    checkSubmitButton();
}

function selectPaymentMethod(method) {
    selectedPaymentMethod = method;
    document.querySelectorAll('.payment-method-btn').forEach(btn => {
        btn.classList.remove('selected');
    });
    document.querySelector(`[data-method="${method}"]`).classList.add('selected');
    checkSubmitButton();
}

function formatNumber(input) {
    let value = input.value.replace(/[^0-9]/g, '');
    if (value) {
        value = parseInt(value).toLocaleString('ko-KR');
    }
    input.value = value;
}

function formatAccountNumber(input) {
    let value = input.value.replace(/[^0-9-]/g, '');
    input.value = value;
}

function calculateDeposit() {
    const bidAmountInput = document.getElementById('bidAmount');
    const depositAmountDiv = document.getElementById('depositAmount');
    
    let bidAmount = bidAmountInput.value.replace(/[^0-9]/g, '');
    if (!bidAmount) {
        depositAmountDiv.textContent = '0원';
        checkSubmitButton();
        return;
    }
    
    bidAmount = parseInt(bidAmount);
    const depositAmount = Math.floor(bidAmount * depositRate / 100);
    
    depositAmountDiv.textContent = depositAmount.toLocaleString('ko-KR') + '원';
    
    checkSubmitButton();
}

function checkSubmitButton() {
    const bidAmount = document.getElementById('bidAmount').value.replace(/[^0-9]/g, '');
    const refundBank = document.getElementById('refundBank').value;
    const refundAccountNumber = document.getElementById('refundAccountNumber').value;
    const refundAccountHolder = document.getElementById('refundAccountHolder').value;
    const submitBtn = document.getElementById('submitBtn');
    
    if (bidAmount && parseInt(bidAmount) >= minBidAmount && refundBank && refundAccountNumber && refundAccountHolder) {
        submitBtn.disabled = false;
    } else {
        submitBtn.disabled = true;
    }
}

function submitBidForm() {
    const bidAmount = document.getElementById('bidAmount').value.replace(/[^0-9]/g, '');
    const depositAmount = document.getElementById('depositAmount').textContent.replace(/[^0-9]/g, '');
    const refundBank = document.getElementById('refundBank').value;
    const refundAccountNumber = document.getElementById('refundAccountNumber').value;
    const refundAccountHolder = document.getElementById('refundAccountHolder').value;
    
    if (!bidAmount || parseInt(bidAmount) < minBidAmount) {
        alert('최저입찰가 이상의 금액을 입력해주세요.');
        return;
    }
    
    if (!refundBank || !refundAccountNumber || !refundAccountHolder) {
        alert('환불계좌 정보를 모두 입력해주세요.');
        return;
    }
    
    // 입찰서 제출 API 호출
    // Hidden input에서 값 가져오기
    const auctionNoInput = document.getElementById('hiddenAuctionNo');
    const itemIdInput = document.getElementById('hiddenItemId');
    const cltrNoInput = document.getElementById('hiddenCltrNo');
    const itemNameInput = document.getElementById('hiddenItemName');
    
    console.log('Hidden input 요소 확인:', {
        auctionNoInput: auctionNoInput ? auctionNoInput.value : 'null',
        itemIdInput: itemIdInput ? itemIdInput.value : 'null',
        cltrNoInput: cltrNoInput ? cltrNoInput.value : 'null',
        itemNameInput: itemNameInput ? itemNameInput.value : 'null'
    });
    
    let auctionNo = null;
    let itemId = null;
    let cltrNo = null;
    let itemName = '';
    
    if (auctionNoInput && auctionNoInput.value && auctionNoInput.value.trim() !== '') {
        auctionNo = parseInt(auctionNoInput.value);
    }
    if (itemIdInput && itemIdInput.value && itemIdInput.value.trim() !== '') {
        itemId = parseInt(itemIdInput.value);
    }
    if (cltrNoInput && cltrNoInput.value && cltrNoInput.value.trim() !== '') {
        cltrNo = cltrNoInput.value.trim();
    }
    if (itemNameInput && itemNameInput.value) {
        itemName = itemNameInput.value.trim();
    }
    
    // URL 파라미터에서도 확인 (fallback)
    const urlParams = new URLSearchParams(window.location.search);
    const urlAuctionNo = urlParams.get('auctionNo');
    const urlItemId = urlParams.get('itemId');
    const urlCltrNo = urlParams.get('cltrNo');
    
    if (!auctionNo && urlAuctionNo) {
        auctionNo = parseInt(urlAuctionNo);
    }
    if (!itemId && urlItemId) {
        itemId = parseInt(urlItemId);
    }
    if (!cltrNo && urlCltrNo) {
        cltrNo = urlCltrNo;
    }
    
    const finalAuctionNo = auctionNo;
    const finalItemId = itemId;
    const finalCltrNo = cltrNo;
    
    console.log('아이템 식별자 (최종):', {
        auctionNo: finalAuctionNo,
        itemId: finalItemId,
        cltrNo: finalCltrNo,
        itemName: itemName
    });
    
    // 값이 하나도 없으면 오류
    if (!finalAuctionNo && !finalItemId && !finalCltrNo) {
        console.error('아이템 식별자가 없습니다!');
        alert('물건 정보를 찾을 수 없습니다. 페이지를 새로고침하고 다시 시도해주세요.');
        return;
    }
    
    const requestData = {
        amount: parseInt(depositAmount),
        itemName: itemName,
        bidAmount: parseInt(bidAmount),
        depositAmount: parseInt(depositAmount),
        bidMethod: selectedBidMethod,
        paymentMethod: selectedPaymentMethod,
        refundBank: refundBank,
        refundAccountNumber: refundAccountNumber,
        refundAccountHolder: refundAccountHolder
    };
    
    if (finalAuctionNo) requestData.auctionNo = finalAuctionNo;
    if (finalItemId) requestData.itemId = finalItemId;
    if (finalCltrNo) requestData.cltrNo = finalCltrNo;
    
    // 입찰서 제출 API 호출
    console.log('입찰서 제출 요청 데이터:', requestData);
    
    $.ajax({
        url: '/payment/submit-bid',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(requestData),
        success: function(response) {
            console.log('입찰서 제출 응답:', response);
            
            if (response && response.success) {
                if (!response.paymentId) {
                    console.error('입찰서 제출 응답에 paymentId가 없습니다:', response);
                    alert('입찰서 제출에 실패했습니다: paymentId가 없습니다.');
                    return;
                }
                
                // 입찰서 제출 완료 페이지로 이동
                let successUrl = '/payment/bid-submitted?paymentId=' + response.paymentId;
                successUrl += '&bidAmount=' + bidAmount;
                successUrl += '&depositAmount=' + depositAmount;
                successUrl += '&bidMethod=' + encodeURIComponent(selectedBidMethod);
                successUrl += '&paymentMethod=' + encodeURIComponent(selectedPaymentMethod);
                successUrl += '&refundBank=' + encodeURIComponent(refundBank);
                successUrl += '&refundAccountNumber=' + encodeURIComponent(refundAccountNumber);
                successUrl += '&refundAccountHolder=' + encodeURIComponent(refundAccountHolder);
                
                console.log('입찰서 제출 완료 페이지로 이동:', successUrl);
                window.location.href = successUrl;
            } else {
                const errorMsg = response && response.message ? response.message : '알 수 없는 오류';
                console.error('입찰서 제출 실패:', errorMsg);
                
                // 이미 작성된 입찰서가 있는 경우
                if (errorMsg.includes('이미 입찰서가 작성되었습니다') && response.existingPaymentId) {
                    if (confirm('이미 입찰서가 작성되었습니다.\n기존 입찰서를 보시겠습니까?')) {
                        window.location.href = '/payment/detail/' + response.existingPaymentId;
                    }
                } else {
                    alert('입찰서 제출에 실패했습니다: ' + errorMsg);
                }
            }
        },
        error: function(xhr, status, error) {
            console.error('입찰서 제출 오류:', {
                status: status,
                error: error,
                responseText: xhr.responseText,
                responseJSON: xhr.responseJSON
            });
            
            let errorMessage = '입찰서 제출 중 오류가 발생했습니다.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMessage = xhr.responseJSON.message;
            } else if (xhr.responseText) {
                try {
                    const errorData = JSON.parse(xhr.responseText);
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                } catch (e) {
                    errorMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
                }
            }
            alert(errorMessage);
        }
    });
}

// 환불계좌 입력 시 제출 버튼 활성화 체크
$(document).ready(function() {
    $('#refundBank').on('change', checkSubmitButton);
    $('#refundAccountNumber').on('input', checkSubmitButton);
    $('#refundAccountHolder').on('input', checkSubmitButton);
});

