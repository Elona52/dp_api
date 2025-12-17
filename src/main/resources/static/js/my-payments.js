/**
 * 입찰내역 관리 페이지 JavaScript
 */

/**
 * 입찰 상세 페이지로 이동
 */
function goToBidDetail(paymentId) {
    if (paymentId) {
        window.location.href = '/payment/detail/' + paymentId;
    }
}

/**
 * 결제 버튼 클릭 핸들러
 * 이벤트 전파를 막고 결제 페이지로 이동
 */
function handlePaymentClick(event, button) {
    // 이벤트 전파 중지 (행 클릭 이벤트 방지)
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    
    console.log('결제 버튼 클릭됨');
    
    // 버튼의 data 속성에서 값 가져오기
    const paymentId = button.getAttribute('data-payment-id');
    const auctionNo = button.getAttribute('data-auction-no');
    const itemId = button.getAttribute('data-item-id');
    const cltrNo = button.getAttribute('data-cltr-no');
    
    console.log('결제 정보:', {
        paymentId: paymentId,
        auctionNo: auctionNo,
        itemId: itemId,
        cltrNo: cltrNo
    });
    
    // paymentId를 우선적으로 사용 (가장 안정적)
    if (paymentId && paymentId !== '' && paymentId !== 'null') {
        const paymentUrl = '/payment/checkout?paymentId=' + paymentId;
        console.log('결제 페이지로 이동 (paymentId 사용):', paymentUrl);
        window.location.href = paymentUrl;
        return;
    }
    
    // paymentId가 없으면 다른 식별자 사용
    // 결제 페이지 URL 구성
    let paymentUrl = '/payment/checkout?';
    let hasParam = false;
    
    if (auctionNo && auctionNo !== '' && auctionNo !== 'null') {
        paymentUrl += 'auctionNo=' + auctionNo;
        hasParam = true;
    } else if (itemId && itemId !== '' && itemId !== 'null') {
        paymentUrl += 'itemId=' + itemId;
        hasParam = true;
    } else if (cltrNo && cltrNo !== '' && cltrNo !== 'null') {
        paymentUrl += 'cltrNo=' + encodeURIComponent(cltrNo);
        hasParam = true;
    }
    
    if (!hasParam) {
        alert('물건 정보를 찾을 수 없습니다. 관리자에게 문의해주세요.');
        console.error('물건 식별자 없음:', { paymentId, auctionNo, itemId, cltrNo });
        return;
    }
    
    console.log('결제 페이지로 이동:', paymentUrl);
    window.location.href = paymentUrl;
}

/**
 * 입찰내역 삭제
 */
function deletePayment(paymentId) {
    console.log('=== 삭제 함수 호출 ===');
    console.log('paymentId:', paymentId, '타입:', typeof paymentId);
    
    if (!paymentId) {
        alert('입찰내역 ID가 없습니다.');
        console.error('paymentId가 없습니다!');
        return;
    }
    
    if (!confirm('입찰내역을 삭제하시겠습니까?\n삭제된 내역은 복구할 수 없습니다.')) {
        return;
    }
    
    const deleteUrl = '/payment/delete/' + paymentId;
    console.log('삭제 URL:', deleteUrl);
    
    // 먼저 POST로 시도 (더 안정적)
    $.ajax({
        url: deleteUrl,
        method: 'POST',
        dataType: 'json',
        beforeSend: function() {
            console.log('POST 삭제 요청 전송 중...');
        },
        success: function(response) {
            console.log('삭제 응답 받음:', response);
            if (response && response.success) {
                alert('입찰내역이 삭제되었습니다.');
                // 페이지 새로고침
                location.reload();
            } else {
                const errorMsg = response ? (response.message || '알 수 없는 오류') : '응답이 없습니다.';
                alert('삭제 중 오류가 발생했습니다: ' + errorMsg);
                console.error('삭제 실패:', response);
            }
        },
        error: function(xhr, status, error) {
            console.error('POST 삭제 오류:', { 
                status: status, 
                error: error, 
                statusCode: xhr.status,
                responseText: xhr.responseText,
                url: deleteUrl
            });
            
            // POST가 실패하면 DELETE로 재시도
            if (xhr.status === 404 || xhr.status === 405) {
                console.log('DELETE 메서드로 재시도...');
                $.ajax({
                    url: deleteUrl,
                    method: 'DELETE',
                    dataType: 'json',
                    success: function(response) {
                        console.log('DELETE 삭제 응답 받음:', response);
                        if (response && response.success) {
                            alert('입찰내역이 삭제되었습니다.');
                            location.reload();
                        } else {
                            const errorMsg = response ? (response.message || '알 수 없는 오류') : '응답이 없습니다.';
                            alert('삭제 중 오류가 발생했습니다: ' + errorMsg);
                        }
                    },
                    error: function(xhr2, status2, error2) {
                        console.error('DELETE도 실패:', { 
                            status: status2, 
                            error: error2, 
                            statusCode: xhr2.status,
                            responseText: xhr2.responseText 
                        });
                        let errorMsg = '삭제 중 오류가 발생했습니다.';
                        
                        if (xhr2.status === 401 || xhr2.status === 403) {
                            errorMsg = '로그인이 필요합니다.';
                        } else if (xhr2.status === 404) {
                            errorMsg = '입찰내역을 찾을 수 없습니다. (경로: ' + deleteUrl + ')';
                        } else if (xhr2.status === 500) {
                            errorMsg = '서버 오류가 발생했습니다.';
                        }
                        
                        alert(errorMsg + '\n상태 코드: ' + xhr2.status);
                    }
                });
                return;
            }
            
            let errorMsg = '삭제 중 오류가 발생했습니다.';
            
            if (xhr.status === 401 || xhr.status === 403) {
                errorMsg = '로그인이 필요합니다.';
            } else if (xhr.status === 404) {
                errorMsg = '입찰내역을 찾을 수 없습니다. (경로: ' + deleteUrl + ')';
            } else if (xhr.status === 500) {
                errorMsg = '서버 오류가 발생했습니다.';
            }
            
            alert(errorMsg + '\n상태 코드: ' + xhr.status);
        }
    });
}

/**
 * 기존 함수 (하위 호환성 유지)
 */
function goToPayment(paymentId, auctionNo, itemId, cltrNo) {
    console.log('goToPayment 호출:', { paymentId, auctionNo, itemId, cltrNo });
    
    let paymentUrl = '/payment/checkout?';
    let hasParam = false;
    
    if (auctionNo && auctionNo !== null && auctionNo !== 'null' && auctionNo !== '') {
        paymentUrl += 'auctionNo=' + auctionNo;
        hasParam = true;
    } else if (itemId && itemId !== null && itemId !== 'null' && itemId !== '') {
        paymentUrl += 'itemId=' + itemId;
        hasParam = true;
    } else if (cltrNo && cltrNo !== null && cltrNo !== 'null' && cltrNo !== '') {
        paymentUrl += 'cltrNo=' + encodeURIComponent(cltrNo);
        hasParam = true;
    }
    
    if (!hasParam) {
        alert('물건 정보를 찾을 수 없습니다.');
        return;
    }
    
    window.location.href = paymentUrl;
}

