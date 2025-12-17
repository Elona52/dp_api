/**
 * 즐겨찾기 페이지 JavaScript
 * - 즐겨찾기 목록 로드 및 표시
 * - 즐겨찾기 삭제 기능
 * - 서버에서 처리된 데이터를 사용하여 표시
 */

// jQuery 로드 확인 및 초기화
function initFavorites() {
    console.log('즐겨찾기 페이지 초기화');
    if (typeof jQuery !== 'undefined') {
        loadFavorites();
    } else {
        console.error('jQuery가 로드되지 않았습니다.');
        setTimeout(function() {
            if (typeof jQuery !== 'undefined') {
                loadFavorites();
            } else {
                const container = document.getElementById('favoritesContainer');
                if (container) {
                    container.innerHTML = '<div class="no-favorites"><p>페이지 로드 중 오류가 발생했습니다. 새로고침해주세요.</p></div>';
                }
            }
        }, 1000);
    }
}

// DOM 로드 완료 후 초기화
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initFavorites);
} else {
    if (typeof jQuery !== 'undefined') {
        $(document).ready(initFavorites);
    } else {
        initFavorites();
    }
}

// 즐겨찾기 목록 로드
function loadFavorites() {
    console.log('=== 즐겨찾기 목록 로드 시작 ===');
    
    if (typeof jQuery === 'undefined') {
        console.error('❌ jQuery가 없습니다.');
        showNoFavorites('jQuery를 로드할 수 없습니다.');
        return;
    }
    
    const container = $('#favoritesContainer');
    if (!container || container.length === 0) {
        console.error('❌ favoritesContainer를 찾을 수 없습니다.');
        return;
    }
    
    console.log('✅ 컨테이너 찾음, AJAX 요청 시작...');
    
    $.ajax({
        url: '/api/favorites',
        method: 'GET',
        timeout: 15000,
        dataType: 'json',
        beforeSend: function() {
            console.log('📤 AJAX 요청 전송 중...');
        },
        success: function(response) {
            console.log('========================================');
            console.log('✅ AJAX 성공 응답 받음');
            console.log('========================================');
            console.log('전체 응답:', JSON.stringify(response, null, 2));
            console.log('응답 타입:', typeof response);
            console.log('response.success:', response ? response.success : 'undefined');
            console.log('response.favorites:', response ? response.favorites : 'undefined');
            console.log('response.favorites 타입:', response && response.favorites ? typeof response.favorites : 'undefined');
            console.log('response.favorites 배열 여부:', response && response.favorites ? Array.isArray(response.favorites) : 'undefined');
            
            try {
                if (!response) {
                    console.error('❌ 응답이 null입니다.');
                    showNoFavorites('서버 응답이 없습니다.');
                    return;
                }
                
                // 응답 구조 확인
                console.log('응답의 모든 키:', Object.keys(response));
                
                if (response.success === true) {
                    // favorites가 배열인지 확인
                    let favoritesArray = [];
                    if (response.favorites !== undefined) {
                        if (Array.isArray(response.favorites)) {
                            favoritesArray = response.favorites;
                        } else if (response.favorites && typeof response.favorites === 'object') {
                            // 단일 객체인 경우 배열로 변환
                            favoritesArray = [response.favorites];
                        }
                    }
                    
                    console.log('✅ 즐겨찾기 목록 발견:', favoritesArray.length, '개');
                    console.log('즐겨찾기 배열:', favoritesArray);
                    
                    if (favoritesArray.length === 0) {
                        console.log('ℹ️ 즐겨찾기가 없습니다.');
                        showNoFavorites();
                        $('#favoritesCount').text('0건');
                    } else {
                        // 각 즐겨찾기 항목의 구조 확인
                        favoritesArray.forEach((fav, idx) => {
                            console.log(`즐겨찾기 [${idx}]:`, fav);
                            console.log(`  - favoriteId:`, fav.favoriteId || fav.id);
                            console.log(`  - itemId:`, fav.itemId);
                            console.log(`  - item:`, fav.item);
                            console.log(`  - item 존재 여부:`, fav.item ? '있음' : '없음');
                            if (fav.item) {
                                console.log(`  - item.cltrNo:`, fav.item.cltrNo);
                                console.log(`  - item.cltrNm:`, fav.item.cltrNm);
                            }
                        });
                        
                        displayFavorites(favoritesArray);
                        $('#favoritesCount').text(favoritesArray.length + '건');
                    }
                } 
                else if (response.success === false) {
                    console.warn('⚠️ 즐겨찾기 조회 실패:', response.message || '알 수 없는 오류');
                    showNoFavorites(response.message || '즐겨찾기를 불러올 수 없습니다.');
                    $('#favoritesCount').text('0건');
                }
                else {
                    console.error('❌ 예상치 못한 응답 구조:', response);
                    console.error('응답 키들:', Object.keys(response));
                    showNoFavorites('응답 형식이 올바르지 않습니다.');
                }
            } catch (e) {
                console.error('❌ 응답 처리 중 예외 발생:', e);
                console.error('스택:', e.stack);
                showNoFavorites('데이터 처리 중 오류가 발생했습니다.');
            }
        },
        error: function(xhr, status, error) {
            console.error('❌ AJAX 오류 발생');
            console.error('상태:', status);
            console.error('오류:', error);
            console.error('상태 코드:', xhr.status);
            console.error('응답 텍스트:', xhr.responseText);
            
            const container = $('#favoritesContainer');
            if (xhr.status === 401 || xhr.status === 403) {
                container.html('<div class="no-favorites"><h3>로그인이 필요합니다</h3><p><a href="/memberLogin">로그인하러 가기</a></p></div>');
            } else if (xhr.status === 0) {
                showNoFavorites('서버에 연결할 수 없습니다. 네트워크를 확인해주세요.');
            } else if (xhr.status === 500) {
                showNoFavorites('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
            } else {
                showNoFavorites('즐겨찾기를 불러오는 중 오류가 발생했습니다.');
            }
        },
        complete: function() {
            console.log('✅ AJAX 요청 완료');
        }
    });
}

// 즐겨찾기 표시 (테이블 형태 - 서버에서 처리된 데이터 사용)
function displayFavorites(favorites) {
    console.log('=== displayFavorites 호출 ===');
    console.log('즐겨찾기 개수:', favorites ? favorites.length : 0);
    
    if (typeof jQuery === 'undefined') {
        console.error('❌ jQuery가 없습니다.');
        return;
    }
    
    const container = $('#favoritesContainer');
    if (!container || container.length === 0) {
        console.error('❌ favoritesContainer를 찾을 수 없습니다.');
        return;
    }
    
    if (!favorites || !Array.isArray(favorites) || favorites.length === 0) {
        console.log('ℹ️ 즐겨찾기가 없습니다.');
        showNoFavorites();
        return;
    }
    
    console.log('✅ 즐겨찾기 목록 표시 시작:', favorites.length, '개');
    let html = '<table class="auction-table">';
    html += '<thead>';
    html += '<tr>';
    html += '<th style="width: 5%;">물건<br>번호 ▲</th>';
    html += '<th style="width: 45%;">소재지 및 내역 ▲</th>';
    html += '<th style="width: 15%;">비고</th>';
    html += '<th style="width: 20%;">감정평가액 ▲<br>최저매각가격</th>';
    html += '<th style="width: 10%;">담당계<br>매각기일 ▲</th>';
    html += '<th style="width: 5%;">삭제</th>';
    html += '</tr>';
    html += '</thead>';
    html += '<tbody>';
    
    favorites.forEach(function(fav, index) {
        try {
            // 서버에서 처리된 데이터 사용
            const favoriteId = fav.favoriteId || fav.id || 0;
            const cltrNo = escapeHtml(fav.cltrNo || '');
            const address = escapeHtml(fav.address || fav.itemName || '');
            const goodsNm = escapeHtml(fav.goodsNm || '');
            const formattedDate = fav.formattedDate || '-';
            const appraisalPriceFormatted = fav.appraisalPriceFormatted || '-';
            const minPriceFormatted = fav.minPriceFormatted || '-';
            const pricePercent = fav.pricePercent || '';
            const uscbCnt = fav.uscbCnt || 0;
            
            html += '<tr onclick="goToDetail(\'' + cltrNo + '\')" style="cursor: pointer;">';
            
            // 물건번호
            html += '<td class="item-number">' + (index + 1) + '</td>';
            
            // 소재지 및 내역
            html += '<td>';
            html += '<div class="location-details">';
            html += '<div style="display: flex; gap: 8px; align-items: center; margin-bottom: 5px;">';
            html += '<span class="usage-badge">온비드</span>';
            html += '<span class="case-number">물건번호: ' + cltrNo + '</span>';
            html += '</div>';
            html += '<div class="address">';
            html += address;
            html += '</div>';
            if (goodsNm) {
                html += '<div class="building-info">' + goodsNm + '</div>';
            }
            html += '</div>';
            html += '</td>';
            
            // 비고
            html += '<td class="remarks">';
            html += '<div>• 공공경매</div>';
            if (uscbCnt > 0) {
                html += '<div>• 유찰 ' + uscbCnt + '회</div>';
            }
            html += '</td>';
            
            // 감정평가액 / 최저매각가격
            html += '<td class="price-info">';
            html += '<div class="price-label">감정평가액</div>';
            html += '<div class="price-value">' + appraisalPriceFormatted + '</div>';
            html += '<div class="price-label">최저매각가격</div>';
            html += '<div class="price-value min-price">';
            html += '<span>' + minPriceFormatted + '</span>';
            if (pricePercent) {
                html += '<span class="price-percent">' + pricePercent + '</span>';
            }
            html += '</div>';
            html += '</td>';
            
            // 담당계 / 매각기일
            html += '<td class="dept-info">';
            html += '<div class="dept-name">캠코</div>';
            html += '<div class="auction-date">' + formattedDate + '</div>';
            html += '<div>';
            if (uscbCnt === 0) {
                html += '<span class="status-badge-table new">신건</span>';
            } else if (uscbCnt <= 2) {
                html += '<span class="status-badge-table failed">유찰 ' + uscbCnt + '회</span>';
            } else {
                html += '<span class="status-badge-table">유찰 ' + uscbCnt + '회</span>';
            }
            html += '</div>';
            html += '</td>';
            
            // 삭제 버튼 (클릭 시 이벤트 전파 중지)
            html += '<td style="text-align: center;" onclick="event.stopPropagation();">';
            html += '<button class="btn-remove-favorite" onclick="event.stopPropagation(); removeFavorite(' + favoriteId + ')" title="즐겨찾기 삭제">';
            html += '<i class="fas fa-trash-alt"></i>';
            html += '</button>';
            html += '</td>';
            
            html += '</tr>';
        } catch (e) {
            console.error(`[${index}] ❌ 즐겨찾기 항목 처리 중 오류:`, e);
            console.error('오류 발생한 즐겨찾기 데이터:', fav);
        }
    });
    
    html += '</tbody>';
    html += '</table>';
    container.html(html);
    
    console.log(`✅ 즐겨찾기 표시 완료: ${favorites.length}개 항목 처리됨`);
}

// HTML 이스케이프 (서버에서 처리되지 않은 텍스트용)
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 상세페이지로 이동
function goToDetail(cltrNo) {
    if (!cltrNo) {
        alert('물건 정보를 찾을 수 없습니다.');
        return;
    }
    window.location.href = '/api-item-detail?cltrNo=' + encodeURIComponent(cltrNo);
}

// 즐겨찾기 없음 표시
function showNoFavorites(message) {
    const container = $('#favoritesContainer');
    if (!container || container.length === 0) {
        console.error('favoritesContainer를 찾을 수 없습니다.');
        return;
    }
    
    const displayMessage = message || '즐겨찾기한 상품이 없습니다';
    const html = '<div class="no-favorites">' +
        '<div class="no-favorites-icon">⭐</div>' +
        '<h3>' + (message ? message : '즐겨찾기한 상품이 없습니다') + '</h3>' +
        (message ? '' : '<p>관심 있는 상품을 즐겨찾기에 추가해보세요!</p>') +
        (message ? '' : '<a href="/auctionList" class="btn" style="padding: 12px 24px; background: var(--court-blue); color: #fff; text-decoration: none; border-radius: 0; font-size: 16px; font-weight: 700; display: inline-block;">경매 둘러보기</a>') +
        '</div>';
    container.html(html);
}

// 즐겨찾기 삭제
function removeFavorite(favoriteId) {
    console.log('=== 즐겨찾기 삭제 요청 ===');
    console.log('favoriteId:', favoriteId);
    
    if (!favoriteId) {
        alert('즐겨찾기 ID가 없습니다.');
        console.error('❌ favoriteId가 null 또는 undefined입니다.');
        return;
    }
    
    if (!confirm('즐겨찾기에서 삭제하시겠습니까?')) {
        return;
    }
    
    $.ajax({
        url: '/api/favorites/' + favoriteId,
        method: 'DELETE',
        dataType: 'json',
        beforeSend: function() {
            console.log('📤 삭제 요청 전송 중...');
        },
        success: function(response) {
            console.log('✅ 삭제 응답 받음:', response);
            
            if (response && response.success) {
                alert('즐겨찾기에서 삭제되었습니다.');
                console.log('✅ 즐겨찾기 삭제 성공');
                // 목록 다시 로드
                loadFavorites();
            } else {
                const errorMsg = response ? (response.message || '알 수 없는 오류') : '응답이 없습니다.';
                alert('삭제 중 오류가 발생했습니다: ' + errorMsg);
                console.error('❌ 삭제 실패:', errorMsg);
            }
        },
        error: function(xhr, status, error) {
            console.error('❌ 즐겨찾기 삭제 오류');
            console.error('상태 코드:', xhr.status);
            console.error('상태:', status);
            console.error('오류:', error);
            console.error('응답 텍스트:', xhr.responseText);
            
            let errorMsg = '삭제 중 오류가 발생했습니다.';
            
            if (xhr.status === 401 || xhr.status === 403) {
                errorMsg = '로그인이 필요합니다.';
            } else if (xhr.status === 404) {
                errorMsg = '즐겨찾기를 찾을 수 없습니다.';
            } else if (xhr.status === 500) {
                errorMsg = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
            } else if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMsg = xhr.responseJSON.message;
            }
            
            alert(errorMsg);
        },
        complete: function() {
            console.log('✅ 삭제 요청 완료');
        }
    });
}

