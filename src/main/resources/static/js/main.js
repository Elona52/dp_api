/**
 * 메인 페이지 JavaScript
 * 히어로 섹션 슬라이드, 공지사항 슬라이드, 경매일정 탭 전환 등의 기능을 담당
 */

console.log('메인 페이지 로드 완료');

// =============================================================================
// 위성 지도 프리뷰 렌더링
// =============================================================================
document.addEventListener('DOMContentLoaded', function() {
    const mapContainers = document.querySelectorAll('.interest-map');
    mapContainers.forEach(container => {
        const address = container.getAttribute('data-address');
        if (!address || address.trim() === '') {
            const placeholder = document.createElement('div');
            placeholder.className = 'map-placeholder';
            placeholder.textContent = '주소 정보가 없습니다.';
            container.appendChild(placeholder);
            return;
        }

        const iframe = document.createElement('iframe');
        iframe.setAttribute('loading', 'lazy');
        iframe.setAttribute('referrerpolicy', 'no-referrer-when-downgrade');
        iframe.src = `https://maps.google.com/maps?q=${encodeURIComponent(address)}&output=embed&t=k&z=19`;
        container.appendChild(iframe);
    });
});

// =============================================================================
// 히어로 섹션 카테고리 슬라이드
// =============================================================================
(function() {
    // DOM이 완전히 로드된 후 실행
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initHeroSlider);
    } else {
        initHeroSlider();
    }
    
    function initHeroSlider() {
        const slides = document.querySelectorAll('.hero-slider .slide[data-category]');
        const categoryButtons = document.querySelectorAll('.category-btn');
        const categories = ['주거용건물', '자동차', '상가용건물', '토지', '산업용건물', '임야'];
        let currentIndex = 3; // 토지부터 시작 (active 클래스가 있는 버튼)
        let autoSlideInterval;
        
        console.log('히어로 슬라이드 초기화 - 슬라이드 개수: ' + slides.length + ', 버튼 개수: ' + categoryButtons.length);
        
        // 이미지가 이미 HTML에 설정되어 있으므로 추가 로드 불필요
        // 다만 이미지 로드 확인을 위해 체크
        slides.forEach((slide, index) => {
            const imagePath = slide.getAttribute('data-image');
            const bgImage = slide.style.backgroundImage;
            console.log('슬라이드 이미지 확인:', slide.getAttribute('data-category'), '->', bgImage);
            
            // 이미지가 로드되지 않은 경우에만 재시도
            if (!bgImage || bgImage === 'none' || bgImage === '') {
                if (imagePath) {
                    slide.style.backgroundImage = 'url(' + imagePath + ')';
                    console.log('이미지 경로 재설정:', imagePath);
                }
            }
        });
        
        // 카테고리명을 영문 코드로 매핑
        const categoryCodeMap = {
            '주거용건물': 'residential',
            '자동차': 'vehicle',
            '상가용건물': 'commercial',
            '토지': 'land',
            '산업용건물': 'industrial',
            '임야': 'forest'
        };
        
        // 카테고리별 슬라이드 표시 함수
        window.showCategorySlide = function(categoryName) {
            console.log('슬라이드 전환:', categoryName);
            
            // 모든 슬라이드 숨기기
            slides.forEach(slide => {
                slide.style.opacity = '0';
            });
            
            // 해당 카테고리 슬라이드 표시
            const targetSlide = document.querySelector(`.hero-slider .slide[data-category="${categoryName}"]`);
            if (targetSlide) {
                targetSlide.style.opacity = '1';
                console.log('슬라이드 표시 성공:', categoryName);
            } else {
                console.error('슬라이드를 찾을 수 없음:', categoryName);
            }
            
            // 버튼 활성화 상태 업데이트
            categoryButtons.forEach(btn => {
                if (btn.getAttribute('data-slide') === categoryName) {
                    btn.classList.add('active');
                    btn.style.background = '#0066cc';
                } else {
                    btn.classList.remove('active');
                    btn.style.background = 'rgba(0,0,0,0.8)';
                }
            });
            
            // 현재 인덱스 업데이트
            currentIndex = categories.indexOf(categoryName);
            if (currentIndex === -1) currentIndex = 0;
            
            // 자동 슬라이드 재시작
            restartAutoSlide();
        };
        
        // 자동 슬라이드 함수
        function nextSlide() {
            currentIndex = (currentIndex + 1) % categories.length;
            showCategorySlide(categories[currentIndex]);
        }
        
        // 자동 슬라이드 시작
        function startAutoSlide() {
            if (autoSlideInterval) {
                clearInterval(autoSlideInterval);
            }
            autoSlideInterval = setInterval(nextSlide, 4000); // 4초마다 전환
        }
        
        // 자동 슬라이드 재시작
        function restartAutoSlide() {
            if (autoSlideInterval) {
                clearInterval(autoSlideInterval);
            }
            startAutoSlide();
        }
        
        // 버튼 호버 시 자동 슬라이드 일시 정지
        categoryButtons.forEach(btn => {
            btn.addEventListener('mouseenter', function() {
                if (autoSlideInterval) {
                    clearInterval(autoSlideInterval);
                }
            });
            
            btn.addEventListener('mouseleave', function() {
                startAutoSlide();
            });
        });
        
        // 초기 자동 슬라이드 시작
        startAutoSlide();
        
        console.log('히어로 카테고리 슬라이드 시작 완료');
    }
})();

// =============================================================================
// 공지사항 슬라이드
// =============================================================================
let noticeCurrentIndex = 0;
let noticeItems = null;

// DOM 로드 후 공지사항 아이템 초기화
document.addEventListener('DOMContentLoaded', function() {
    noticeItems = document.querySelectorAll('#noticeSlider .notice-item');
    if (noticeItems.length > 0) {
        console.log('공지사항 슬라이드 시작: ' + noticeItems.length + '개');
    }
});

function showNotice(index) {
    if (!noticeItems || noticeItems.length === 0) return;
    noticeItems.forEach((item, i) => {
        item.style.opacity = i === index ? '1' : '0';
    });
}

function nextNotice() {
    if (!noticeItems || noticeItems.length === 0) return;
    noticeCurrentIndex = (noticeCurrentIndex + 1) % noticeItems.length;
    showNotice(noticeCurrentIndex);
}

function prevNotice() {
    if (!noticeItems || noticeItems.length === 0) return;
    noticeCurrentIndex = (noticeCurrentIndex - 1 + noticeItems.length) % noticeItems.length;
    showNotice(noticeCurrentIndex);
}

// =============================================================================
// 경매일정 탭 전환
// =============================================================================
function switchTab(button, tabIndex) {
    // 모든 탭 버튼 스타일 초기화
    const tabs = document.querySelectorAll('.schedule-tab');
    tabs.forEach(tab => {
        tab.style.background = '#fff';
        tab.style.fontWeight = 'normal';
    });
    
    // 선택된 탭 활성화
    button.style.background = '#f5f5f5';
    button.style.fontWeight = '600';
    
    // 탭에 따라 메시지 표시 (실제로는 다른 데이터를 로드할 수 있음)
    if (tabIndex === 1 || tabIndex === 2) {
        const scheduleList = document.querySelector('.schedule-list');
        if (scheduleList && !scheduleList.querySelector('.no-data-tab')) {
            const originalContent = scheduleList.innerHTML;
            scheduleList.setAttribute('data-original', originalContent);
            scheduleList.innerHTML = '<div class="no-data-tab" style="text-align: center; color: #999; padding: 40px 20px; font-size: 13px;">해당 정보가 없습니다.</div>';
        }
    } else if (tabIndex === 0) {
        const scheduleList = document.querySelector('.schedule-list');
        const originalContent = scheduleList.getAttribute('data-original');
        if (originalContent) {
            scheduleList.innerHTML = originalContent;
        }
    }
}

