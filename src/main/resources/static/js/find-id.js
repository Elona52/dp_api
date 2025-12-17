/**
 * 아이디 찾기 페이지 JavaScript
 * - 서버의 MemberService와 연동하여 아이디 찾기 기능 제공
 */

$(document).ready(function() {
    // 아이디 찾기 폼 제출
    $('#findIdForm').on('submit', function(e) {
        e.preventDefault();
        
        const name = $('#name').val();
        const mobile1 = $('#mobile1').val();
        const mobile2 = $('#mobile2').val();
        
        // 입력값 검증 (서버에서도 검증하지만 클라이언트에서도 기본 검증)
        if(!name || !mobile1 || !mobile2) {
            alert('모든 항목을 입력해주세요.');
            return;
        }
        
        // 서버에 요청 (비즈니스 로직은 MemberService에서 처리)
        $.ajax({
            url: '/findId',
            type: 'POST',
            contentType: 'application/x-www-form-urlencoded',
            data: {
                name: name,
                mobile1: mobile1,
                mobile2: mobile2
            },
            success: function(result) {
                const resultBox = $('#resultBox');
                resultBox.show();
                
                // 서버에서 처리된 결과 표시
                if(result.success) {
                    resultBox.removeClass('error').addClass('success');
                    resultBox.html('<strong>아이디를 찾았습니다!</strong><br>아이디: <strong>' + result.data + '</strong>');
                } else {
                    resultBox.removeClass('success').addClass('error');
                    resultBox.html('<strong>오류</strong><br>' + result.message);
                }
            },
            error: function() {
                const resultBox = $('#resultBox');
                resultBox.show();
                resultBox.removeClass('success').addClass('error');
                resultBox.html('<strong>오류</strong><br>서버 오류가 발생했습니다.');
            }
        });
    });
    
    // 전화번호 입력 제한 (숫자만 입력)
    $('#mobile1, #mobile2').on('input', function() {
        this.value = this.value.replace(/[^0-9]/g, '');
    });
});



