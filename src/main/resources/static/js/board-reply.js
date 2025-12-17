/**
 * 게시판 댓글 기능 JavaScript
 * - 댓글 등록, 수정, 삭제 기능
 * - 서버에서 포맷팅된 데이터를 사용하여 표시
 */

// 댓글 등록
function insertReply(boardNo, loginId) {
    const content = document.getElementById('replyContent').value.trim();
    if (!content) {
        alert('댓글 내용을 입력해주세요.');
        return;
    }
    
    $.ajax({
        url: '/insertReply.ajax',
        method: 'POST',
        data: {
            id: loginId,
            content: content,
            boardNo: boardNo
        },
        success: function(replyList) {
            // 댓글 리스트 업데이트
            updateReplyList(replyList, boardNo);
            document.getElementById('replyContent').value = '';
        },
        error: function(xhr) {
            alert('댓글 등록에 실패했습니다.');
            console.error('댓글 등록 오류:', xhr);
        }
    });
}

// 댓글 삭제
function deleteReply(replyNo, boardNo) {
    if (!confirm('댓글을 삭제하시겠습니까?')) {
        return;
    }
    
    $.ajax({
        url: '/deleteReply.ajax',
        method: 'POST',
        data: {
            no: replyNo,
            boardNo: boardNo
        },
        success: function(replyList) {
            // 댓글 리스트 업데이트 (서버에서 포맷팅된 데이터 사용)
            updateReplyList(replyList, boardNo);
        },
        error: function(xhr) {
            alert('댓글 삭제에 실패했습니다.');
            console.error('댓글 삭제 오류:', xhr);
        }
    });
}

// 댓글 수정
function editReply(button, replyNo, boardNo) {
    const replyItem = button.closest('.reply-item');
    const contentDiv = replyItem.querySelector('.reply-content');
    const currentContent = contentDiv.textContent.trim();
    
    const input = document.createElement('input');
    input.type = 'text';
    input.value = currentContent;
    input.style.cssText = 'width: 100%; padding: 4px 8px; border: 1px solid var(--border-gray); border-radius: 4px; font-size: 13px;';
    
    const saveBtn = document.createElement('button');
    saveBtn.textContent = '저장';
    saveBtn.style.cssText = 'padding: 4px 12px; margin-left: 5px; background: var(--court-blue); color: #fff; border: none; border-radius: 4px; font-size: 12px; cursor: pointer;';
    saveBtn.onclick = function() {
        const newContent = input.value.trim();
        if (!newContent) {
            alert('댓글 내용을 입력해주세요.');
            return;
        }
        
        $.ajax({
            url: '/updateReply.ajax',
            method: 'POST',
            data: {
                no: replyNo,
                id: replyItem.querySelector('.reply-meta span').textContent.trim(),
                content: newContent,
                boardNo: boardNo
            },
            success: function(replyList) {
                updateReplyList(replyList, boardNo);
            },
            error: function(xhr) {
                alert('댓글 수정에 실패했습니다.');
                console.error('댓글 수정 오류:', xhr);
            }
        });
    };
    
    const cancelBtn = document.createElement('button');
    cancelBtn.textContent = '취소';
    cancelBtn.style.cssText = 'padding: 4px 12px; margin-left: 5px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 4px; font-size: 12px; cursor: pointer;';
    cancelBtn.onclick = function() {
        contentDiv.textContent = currentContent;
    };
    
    contentDiv.innerHTML = '';
    contentDiv.appendChild(input);
    contentDiv.appendChild(saveBtn);
    contentDiv.appendChild(cancelBtn);
}

// 댓글 리스트 업데이트 (서버에서 포맷팅된 데이터를 그대로 표시)
function updateReplyList(replyList, boardNo) {
    const container = document.getElementById('replyListContainer');
    if (!container) return;
    
    if (replyList && replyList.length > 0) {
        let html = '';
        replyList.forEach(function(replyItem) {
            html += '<div class="reply-item" data-reply-no="' + replyItem.no + '">';
            html += '<div class="reply-meta">';
            html += '<span>' + (replyItem.id || '') + '</span>';
            if (replyItem.displayRegDate) {
                html += '<span> | ' + replyItem.displayRegDate + '</span>';
            }
            // 수정/삭제 버튼 (본인 댓글만 표시 - 서버에서 canEdit으로 판단)
            if (replyItem.canEdit) {
                html += '<span style="margin-left: 10px;">';
                html += '<button onclick="editReply(this, ' + replyItem.no + ', ' + boardNo + ')" style="padding: 2px 8px; font-size: 11px; background: #f5f5f5; border: 1px solid #ddd; cursor: pointer; border-radius: 2px;">수정</button>';
                html += '<button onclick="deleteReply(' + replyItem.no + ', ' + boardNo + ')" style="padding: 2px 8px; font-size: 11px; background: #f5f5f5; border: 1px solid #ddd; cursor: pointer; border-radius: 2px; margin-left: 3px;">삭제</button>';
                html += '</span>';
            }
            html += '</div>';
            html += '<div class="reply-content">' + (replyItem.content || '') + '</div>';
            html += '</div>';
        });
        container.innerHTML = html;
    } else {
        container.innerHTML = '<div style="padding: 20px; text-align: center; color: var(--text-gray); font-size: 13px;">등록된 댓글이 없습니다.</div>';
    }
}

