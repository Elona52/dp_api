package com.api.board.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.api.board.domain.FindBoard;
import com.api.board.domain.Reply;
import com.api.board.mapper.BoardMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class boardService {

	private final BoardMapper boardMapper;
	
	/**
	 * 카테고리 코드를 표시명으로 변환
	 * @param category 카테고리 코드 (real-estate, movable, site, other)
	 * @return 표시명 (부동산, 동산, 사이트, 기타)
	 */
	public String getCategoryDisplayName(String category) {
		if (category == null) {
			return "기타";
		}
		switch (category) {
			case "real-estate":
				return "부동산";
			case "movable":
				return "동산";
			case "site":
				return "사이트";
			default:
				return "기타";
		}
	}
	
	/**
	 * 게시글 리스트에 표시용 데이터 추가 (카테고리명, 번호 등)
	 * @param boardList 게시글 리스트
	 * @param totalCount 전체 개수
	 * @param pageNum 현재 페이지
	 * @param pageSize 페이지 크기
	 * @return 처리된 게시글 리스트 (각 게시글에 displayCategory, displayNumber 포함)
	 */
	public List<Map<String, Object>> processBoardListForDisplay(List<FindBoard> boardList, int totalCount, int pageNum, int pageSize) {
		List<Map<String, Object>> processedList = new ArrayList<>();
		
		if (boardList == null || boardList.isEmpty()) {
			return processedList;
		}
		
		for (int i = 0; i < boardList.size(); i++) {
			FindBoard board = boardList.get(i);
			Map<String, Object> boardMap = new HashMap<>();
			
			// 원본 게시글 데이터 (템플릿에서 board 객체로도 접근 가능하도록)
			boardMap.put("board", board);
			boardMap.put("no", board.getNo());
			boardMap.put("id", board.getId());
			boardMap.put("title", board.getTitle());
			boardMap.put("content", board.getContent());
			boardMap.put("category", board.getCategory());
			boardMap.put("views", board.getViews());
			boardMap.put("relatedLink", board.getRelatedLink());
			boardMap.put("regDate", board.getRegDate());
			
			// 비즈니스 로직: 카테고리명 변환
			boardMap.put("displayCategory", getCategoryDisplayName(board.getCategory()));
			
			// 비즈니스 로직: 게시글 번호 계산 (역순)
			int displayNumber = totalCount - (pageNum - 1) * pageSize - i;
			boardMap.put("displayNumber", displayNumber);
			
			processedList.add(boardMap);
		}
		
		return processedList;
	}

	public void insertBoard(FindBoard board) {
		boardMapper.insertBoard(board);
	}

	public List<FindBoard> getBoardList(String id, String keyword, String category) {
		return boardMapper.getBoardList(id, keyword, category);
	}

	public FindBoard getBoard(int no) {
		return boardMapper.getBoard(no);
	}

	public void updateBoard(FindBoard board) {
		boardMapper.updateBoard(board);
	}

	public void deleteBoard(int no) {
		boardMapper.deleteBoard(no);
	}

	public void insertReply(Reply re) {
		boardMapper.insertReply(re);
	}

	public void updateReply(Reply re) {
		boardMapper.updateReply(re);
	}

	public void deleteReply(int no) {
		boardMapper.deleteReply(no);
	}

	public List<Reply> getReplyList(int boardNo) {
		return boardMapper.getReplyList(boardNo);
	}
	
	/**
	 * 댓글 리스트에 표시용 데이터 추가 (날짜 포맷팅 등)
	 * @param replyList 댓글 리스트
	 * @param loginId 현재 로그인한 사용자 ID (null 가능)
	 * @return 처리된 댓글 리스트 (각 댓글에 displayRegDate, canEdit 포함)
	 */
	public List<Map<String, Object>> processReplyListForDisplay(List<Reply> replyList, String loginId) {
		List<Map<String, Object>> processedList = new ArrayList<>();
		
		if (replyList == null || replyList.isEmpty()) {
			return processedList;
		}
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
		
		for (Reply reply : replyList) {
			Map<String, Object> replyMap = new HashMap<>();
			
			// 원본 댓글 데이터
			replyMap.put("reply", reply);
			replyMap.put("no", reply.getNo());
			replyMap.put("id", reply.getId());
			replyMap.put("content", reply.getContent());
			replyMap.put("boardNo", reply.getBoardNo());
			replyMap.put("regDate", reply.getRegDate());
			
			// 비즈니스 로직: 날짜 포맷팅
			if (reply.getRegDate() != null) {
				replyMap.put("displayRegDate", dateFormat.format(reply.getRegDate()));
			} else {
				replyMap.put("displayRegDate", "");
			}
			
			// 비즈니스 로직: 수정/삭제 권한 확인 (본인 댓글만 수정/삭제 가능)
			replyMap.put("canEdit", loginId != null && loginId.equals(reply.getId()));
			
			processedList.add(replyMap);
		}
		
		return processedList;
	}

	public Map<String, Object> prepareBoardPageData(String id, String keyword, String category, Integer no, int pageNum, int pageSize) {
		Map<String, Object> data = new HashMap<>();
		
		if ("null".equals(id))
			id = null;
		if ("null".equals(keyword))
			keyword = null;
		if ("all".equals(category))
			category = null;

		// 전체 게시글 리스트 조회
		List<FindBoard> allBoardList;
		try {
			allBoardList = boardMapper.getBoardList(id, keyword, category);
		} catch (Exception e) {
			// 테이블이 없거나 오류 발생 시 빈 리스트 반환
			System.err.println("게시글 목록 조회 오류: " + e.getMessage());
			e.printStackTrace();
			allBoardList = new ArrayList<>();
		}
		int totalCount = allBoardList != null ? allBoardList.size() : 0;
		
		// 페이징 처리
		List<FindBoard> boardList = allBoardList;
		if (allBoardList != null && !allBoardList.isEmpty() && pageSize > 0) {
			int startIndex = (pageNum - 1) * pageSize;
			int endIndex = Math.min(startIndex + pageSize, allBoardList.size());
			
			if (startIndex < allBoardList.size()) {
				boardList = allBoardList.subList(startIndex, endIndex);
			} else {
				boardList = List.of();
			}
		}
		
		// 페이지네이션 정보 계산
		int pageCount = (totalCount + pageSize - 1) / pageSize; // 올림 계산
		if (pageCount == 0) pageCount = 1;
		
		// 시작 페이지와 끝 페이지 계산 (현재 페이지 기준 ±2)
		int startPage = Math.max(1, pageNum - 2);
		int endPage = Math.min(pageCount, pageNum + 2);
		
		// 비즈니스 로직: 게시글 리스트에 표시용 데이터 추가 (카테고리명, 번호 등)
		List<Map<String, Object>> processedBoardList = processBoardListForDisplay(boardList, totalCount, pageNum, pageSize);
		
		data.put("boardList", processedBoardList);
		data.put("totalCount", totalCount);
		data.put("pageNum", pageNum);
		data.put("pageSize", pageSize);
		data.put("pageCount", pageCount);
		data.put("startPage", startPage);
		data.put("endPage", endPage);
		data.put("id", id);
		data.put("keyword", keyword);
		data.put("category", category != null ? category : "all");
		
		// 상세 게시글 조회 (no가 있는 경우)
		if (no != null) {
			FindBoard board = boardMapper.getBoard(no);
			data.put("board", board);
			// 조회수 증가
			boardMapper.incrementBoardViews(no);
		}
		
		return data;
	}
}
