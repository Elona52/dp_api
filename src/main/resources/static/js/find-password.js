/**
 * 비밀번호 찾기 페이지 JavaScript
 * - 서버의 MemberService와 연동하여 비밀번호 찾기 및 재설정 기능 제공
 */

$(document).ready(function() {
    let verifiedId = null;
    
    // 회원 정보 확인 (비밀번호 찾기)
    $('#findPasswordForm').on('submit', function(e) {
        e.preventDefault();
        
        const id = $('#id').val();
        const name = $('#name').val();
        const mobile1 = $('#mobile1').val();
        const mobile2 = $('#mobile2').val();
        
        // 입력값 검증 (서버에서도 검증하지만 클라이언트에서도 기본 검증)
        if(!id || !name || !mobile1 || !mobile2) {
            alert('모든 항목을 입력해주세요.');
            return;
        }
        
        // 서버에 요청 (비즈니스 로직은 MemberService에서 처리)
        $.ajax({
            url: '/findPassword',
            type: 'POST',
            contentType: 'application/x-www-form-urlencoded',
            data: {
                id: id,
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
                    resultBox.html('<strong>회원 정보가 확인되었습니다.</strong><br>새 비밀번호를 설정해주세요.');
                    verifiedId = id;
                    $('#resetId').val(id);
                    $('#resetPasswordSection').show();
                } else {
                    resultBox.removeClass('success').addClass('error');
                    resultBox.html('<strong>오류</strong><br>' + result.message);
                    $('#resetPasswordSection').hide();
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
    
    // 비밀번호 재설정
    $('#resetPasswordForm').on('submit', function(e) {
        e.preventDefault();
        
        const id = $('#resetId').val();
        const newPassword = $('#newPassword').val();
        const confirmPassword = $('#confirmPassword').val();
        
        // 입력값 검증
        if(!id || !verifiedId || id !== verifiedId) {
            alert('회원 정보 확인이 필요합니다.');
            return;
        }
        
        if(!newPassword || !confirmPassword) {
            alert('비밀번호를 입력해주세요.');
            return;
        }
        
        if(newPassword !== confirmPassword) {
            alert('비밀번호가 일치하지 않습니다.');
            return;
        }
        
        // 비밀번호 길이 검증 (서버에서도 검증하지만 클라이언트에서도 기본 검증)
        if(newPassword.length < 4) {
            alert('비밀번호는 4자 이상이어야 합니다.');
            return;
        }
        
        // 서버에 요청 (비즈니스 로직은 MemberService에서 처리)
        $.ajax({
            url: '/resetPassword',
            type: 'POST',
            contentType: 'application/x-www-form-urlencoded',
            data: {
                id: id,
                newPassword: newPassword
            },
            success: function(result) {
                // 서버에서 처리된 결과 표시
                if(result.success) {
                    alert('비밀번호가 성공적으로 변경되었습니다.\n로그인 페이지로 이동합니다.');
                    window.location.href = '/memberLogin';
                } else {
                    alert(result.message);
                }
            },
            error: function() {
                alert('서버 오류가 발생했습니다.');
            }
        });
    });
    
    // 전화번호 입력 제한 (숫자만 입력)
    $('#mobile1, #mobile2').on('input', function() {
        this.value = this.value.replace(/[^0-9]/g, '');
    });
});



