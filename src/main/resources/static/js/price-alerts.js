/**
 * 가격 알림 페이지 JavaScript
 */

function loadAlerts() {
    console.log('=== 가격 알림 목록 로드 시작 ===');
    
    if (typeof jQuery === 'undefined') {
        console.error('❌ jQuery가 없습니다.');
        showNoAlerts('jQuery를 로드할 수 없습니다.');
        return;
    }
    
    const container = $('#alertsContainer');
    if (!container || container.length === 0) {
        console.error('❌ alertsContainer를 찾을 수 없습니다.');
        return;
    }
    
    $.ajax({
        url: '/api/favorites/alerts/api',
        method: 'GET',
        timeout: 15000,
        dataType: 'json',
        success: function(response) {
            console.log('✅ AJAX 성공 응답 받음:', response);
            
            try {
                if (!response) {
                    console.error('❌ 응답이 null입니다.');
                    showNoAlerts('서버 응답이 없습니다.');
                    return;
                }
                
                if (response.success === true && response.alerts) {
                    console.log('✅ 가격 알림 목록 발견:', response.alerts.length, '개');
                    displayAlerts(response.alerts);
                    $('#alertsCount').text(response.alerts.length + '건');
                } 
                else if (response.success === false) {
                    console.warn('⚠️ 가격 알림 조회 실패:', response.message || '알 수 없는 오류');
                    showNoAlerts(response.message || '가격 알림을 불러올 수 없습니다.');
                }
                else if (response.alerts && Array.isArray(response.alerts) && response.alerts.length === 0) {
                    console.log('ℹ️ 가격 알림이 없습니다.');
                    showNoAlerts();
                }
                else {
                    console.error('❌ 예상치 못한 응답 구조:', response);
                    showNoAlerts('응답 형식이 올바르지 않습니다.');
                }
            } catch (e) {
                console.error('❌ 응답 처리 중 예외 발생:', e);
                showNoAlerts('데이터 처리 중 오류가 발생했습니다.');
            }
        },
        error: function(xhr, status, error) {
            console.error('❌ AJAX 오류 발생:', status, error);
            const container = $('#alertsContainer');
            if (xhr.status === 401 || xhr.status === 403) {
                container.html('<div class="no-alerts"><h3>로그인이 필요합니다</h3><p><a href="/memberLogin">로그인하러 가기</a></p></div>');
            } else {
                showNoAlerts('가격 알림을 불러오는 중 오류가 발생했습니다.');
            }
        }
    });
}

function displayAlerts(alerts) {
    console.log('=== displayAlerts 호출 ===');
    console.log('알림 개수:', alerts ? alerts.length : 0);
    
    if (typeof jQuery === 'undefined') {
        console.error('❌ jQuery가 없습니다.');
        return;
    }
    
    const container = $('#alertsContainer');
    if (!container || container.length === 0) {
        console.error('❌ alertsContainer를 찾을 수 없습니다.');
        return;
    }
    
    if (!alerts || !Array.isArray(alerts) || alerts.length === 0) {
        console.log('ℹ️ 가격 알림이 없습니다.');
        showNoAlerts();
        return;
    }
    
    let html = '<table class="alert-table">';
    html += '<thead>';
    html += '<tr>';
    html += '<th style="width: 8%;">번호</th>';
    html += '<th style="width: 30%;">물건명</th>';
    html += '<th style="width: 20%;">이전 가격</th>';
    html += '<th style="width: 20%;">변동 가격</th>';
    html += '<th style="width: 12%;">알림 상태</th>';
    html += '<th style="width: 10%;">알림 일시</th>';
    html += '</tr>';
    html += '</thead>';
    html += '<tbody>';
    
    alerts.forEach(function(alert, index) {
        const previousPrice = alert.previousPrice || 0;
        const newPrice = alert.newPrice || 0;
        const priceDiff = newPrice - previousPrice;
        const priceChangeClass = priceDiff < 0 ? 'price-down' : (priceDiff > 0 ? 'price-up' : 'price-same');
        const priceChangeIcon = priceDiff < 0 ? '↓' : (priceDiff > 0 ? '↑' : '→');
        const priceChangeText = priceDiff < 0 ? '하락' : (priceDiff > 0 ? '상승' : '동일');
        
        const sentDate = alert.sentDate ? new Date(alert.sentDate).toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        }) : '-';
        
        html += '<tr>';
        html += '<td class="alert-number">' + (alerts.length - index) + '</td>';
        html += '<td class="alert-item-name">' + escapeHtml(alert.itemPlnmNo || '알 수 없음') + '</td>';
        html += '<td class="alert-price">' + formatPrice(previousPrice) + '원</td>';
        html += '<td class="alert-price">';
        html += '<div class="price-change ' + priceChangeClass + '">';
        html += '<span>' + priceChangeIcon + '</span>';
        html += '<span>' + formatPrice(newPrice) + '원</span>';
        html += '<span style="font-size: 11px;">(' + priceChangeText + ')</span>';
        html += '</div>';
        html += '</td>';
        html += '<td class="alert-status">';
        html += '<span class="' + (alert.alertSent ? 'status-sent' : 'status-pending') + '">';
        html += (alert.alertSent ? '전송완료' : '대기중');
        html += '</span>';
        html += '</td>';
        html += '<td class="alert-date">' + sentDate + '</td>';
        html += '</tr>';
    });
    
    html += '</tbody>';
    html += '</table>';
    
    container.html(html);
    console.log('✅ 가격 알림 표시 완료:', alerts.length, '개');
}

function formatPrice(price) {
    if (!price || price === 0) return '0';
    return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showNoAlerts(message) {
    const container = $('#alertsContainer');
    if (!container || container.length === 0) {
        console.error('alertsContainer를 찾을 수 없습니다.');
        return;
    }
    
    const displayMessage = message || '가격 알림 내역이 없습니다';
    const html = '<div class="no-alerts">' +
        '<div class="no-alerts-icon">🔔</div>' +
        '<h3>' + displayMessage + '</h3>' +
        (message ? '' : '<p>즐겨찾기한 물건의 가격이 변동되면 알림을 받을 수 있습니다.</p>') +
        '</div>';
    container.html(html);
}

// DOM 로드 완료 후 초기화
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadAlerts);
} else {
    if (typeof jQuery !== 'undefined') {
        $(document).ready(loadAlerts);
    } else {
        setTimeout(loadAlerts, 500);
    }
}

