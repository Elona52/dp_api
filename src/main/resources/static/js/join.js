/**
 * 회원가입 및 회원정보 수정 페이지 JavaScript
 * - 회원가입 폼 검증 및 제출 처리
 * - 아이디 중복체크
 * - 회원정보 수정 기능
 */

$(document).ready(function() {
    // 탭 전환 기능
    const tabs = document.querySelectorAll('.nav-link');
    const contents = document.querySelectorAll('.tab-content > div');

    tabs.forEach(tab => {
        tab.addEventListener('click', function(e){
            e.preventDefault();
            tabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');

            const target = this.getAttribute('data-tab');
            contents.forEach(c => c.classList.remove('active'));
            document.getElementById(target).classList.add('active');
        });
    });

    // 회원가입 - 주소찾기
    window.joinDaumPostcode = function() {
        new daum.Postcode({
            oncomplete: function(data) {
                document.getElementById("joinZipcode").value = data.zonecode;
                document.getElementById("joinAddress1").value = data.roadAddress;
                document.getElementById("joinAddress2").focus();
            }
        }).open();
    };

    // 회원가입 폼 필수값 검증
    const memberJoinForm = document.getElementById('memberJoinForm');
    if (memberJoinForm) {
        memberJoinForm.addEventListener('submit', function(e) {
            // 우편번호 검증
            const zipcode = document.getElementById('joinZipcode').value;
            if (!zipcode || zipcode.trim() === '') {
                alert('우편번호는 필수 입력입니다.\n\n"주소찾기" 버튼을 눌러 주소를 선택한 후 가입을 진행해주세요.');
                e.preventDefault();
                return false;
            }
            
            // 아이디 중복체크 검증
            const idCheck = document.getElementById('idCheck').value;
            if (!idCheck || idCheck !== 'checked') {
                alert('아이디 중복체크를 해주세요.');
                e.preventDefault();
                return false;
            }
            
            // 비밀번호 일치 확인
            const pass1 = document.getElementById('joinPass1').value;
            const pass2 = document.getElementById('joinPass2').value;
            if (pass1 !== pass2) {
                alert('비밀번호가 일치하지 않습니다.');
                e.preventDefault();
                return false;
            }
        });
    }

    // 아이디 중복체크 버튼 클릭 이벤트
    $('#idCheckBtn').click(function() {
        const id = $('#joinId').val();
        
        if (!id || id.trim() === '') {
            alert('아이디를 입력해주세요.');
            $('#joinId').focus();
            return;
        }
        
        $.ajax({
            url: '/idCheck',
            type: 'POST',
            data: { id: id },
            success: function(response) {
                // ServiceResult<Boolean> 구조: { success, message, data }
                if (response && response.success && response.data === true) {
                    alert(response.message || '사용 가능한 아이디입니다.');
                    $('#idCheck').val('checked');
                    $('#idCheckBtn').prop('disabled', true).css('background-color', '#28a745');
                } else {
                    alert(response && response.message ? response.message : '이미 사용 중인 아이디입니다.');
                    $('#idCheck').val('');
                    $('#joinId').focus();
                }
            },
            error: function(xhr, status, error) {
                console.error('중복체크 오류:', error);
                alert('중복체크 중 오류가 발생했습니다.');
            }
        });
    });
    
    // 아이디 입력 시 중복체크 상태 초기화
    $('#joinId').on('input', function() {
        $('#idCheck').val('');
        $('#idCheckBtn').prop('disabled', false).css('background-color', '');
    });

    // 회원정보수정 - 비밀번호 확인
    $('#modifyBtn').click(function() {
        const id = $('#updateId').val();
        const pass = $('#updatePass').val();
        
        if (!pass || pass.trim() === '') {
            alert('비밀번호를 입력해주세요.');
            return;
        }
        
        $.ajax({
            url: '/isPass',
            type: 'POST',
            data: { id: id, pass: pass },
            success: function(response) {
                // ServiceResult<Boolean> 구조: { success, message, data }
                if (response && response.success && response.data === true) {
                    $('#modifyPassCheck').hide();
                    $('#modifyInfo').show();
                    
                    // 회원 정보 불러오기
                    $.ajax({
                        url: '/getMemberInfo',
                        type: 'POST',
                        data: { id: id },
                        success: function(data) {
                            // ServiceResult<Member> 구조: { success, message, data }
                            if (data && data.success && data.data) {
                                const member = data.data;
                                $('#updateName').val(member.name || '');
                                if(member.phone && member.phone.length >= 10) {
                                    $('#updateMobile1').val(member.phone.substring(0, 3));
                                    $('#updateMobile2').val(member.phone.substring(3));
                                }
                            }
                        },
                        error: function(xhr, status, error) {
                            console.error('회원 정보 조회 오류:', error);
                        }
                    });
                } else {
                    $('#modifyPassCheck').show();
                    alert(response && response.message ? response.message : '비밀번호가 일치하지 않습니다.');
                }
            },
            error: function(xhr, status, error) {
                console.error('비밀번호 확인 오류:', error);
                alert('비밀번호 확인 중 오류가 발생했습니다.');
            }
        });
    });
    
    // 새 비밀번호 일치 확인
    $('#updatePass2').on('input', function() {
        const pass1 = $('#updatePass1').val();
        const pass2 = $('#updatePass2').val();
        
        if (pass2 && pass1 === pass2) {
            $('#passPossible').show();
        } else {
            $('#passPossible').hide();
        }
    });
});

