package com.api.board.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.api.board.domain.FindBoard;
import com.api.board.domain.Reply;
import com.api.board.service.boardService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BoardController {

	private final boardService boardService;

	// 글 등록하기
	@RequestMapping(value = "/insertFindBoard", method = RequestMethod.POST)
	public String insertFindBoard(FindBoard board, HttpSession session) {

		Boolean isLogin = (Boolean) session.getAttribute("isLogin");
		String loginId = (String) session.getAttribute("loginId");

		// 로그인 안 되어 있으면 로그인 페이지로 리다이렉트
		if (isLogin == null || !isLogin || loginId == null) {
			return "redirect:/memberLogin";
		}

		// 세션의 로그인 아이디를 작성자로 설정
		board.setId(loginId);

		boardService.insertBoard(board);
		return "redirect:/boardFaq";
	}

	// 게시판 수정폼으로 이동
	@RequestMapping("/updateBoard")
	public String updateBoard(Model model,
			@RequestParam(name = "id", required = false, defaultValue = "null") String id,
			@RequestParam(name = "keyword", required = false, defaultValue = "null") String keyword,
			@RequestParam(name = "category", required = false, defaultValue = "all") String category,
			@RequestParam(name = "no") int no) {
		if ("null".equals(id))
			id = null;
		if ("null".equals(keyword))
			keyword = null;
		if ("all".equals(category))
			category = null;
		model.addAttribute("boardList", boardService.getBoardList(id, keyword, category));
		model.addAttribute("id", id);
		model.addAttribute("keyword", keyword);
		model.addAttribute("category", category != null ? category : "all");
		model.addAttribute("board", boardService.getBoard(no));
		// 수정 폼도 FAQ 레이아웃 템플릿 재사용
		return "board/board-faq-list";
	}

	// 게시글 수정하기
	@RequestMapping(value = "/updateFindBoard", method = RequestMethod.POST)
	public String updateFindBoard(Model model, FindBoard board) {
		boardService.updateBoard(board);
		model.addAttribute("board", boardService.getBoard(board.getNo()));
		return "redirect:/boardDetail?no=" + board.getNo();
	}

	// 게시글 삭제하기
	@RequestMapping("/deleteBoard")
	public String deleteFindBoard(@RequestParam(name = "no") int no) {
		boardService.deleteBoard(no);
		return "redirect:/boardList";
	}

	// 게시글 작성 폼 (로그인 사용자 전용)
	@RequestMapping(value = "/writeBoard", method = RequestMethod.GET)
	public String writeBoardForm(HttpSession session, Model model) {
		Boolean isLogin = (Boolean) session.getAttribute("isLogin");
		String loginId = (String) session.getAttribute("loginId");

		// 로그인 안 되어 있으면 로그인 페이지로
		if (isLogin == null || !isLogin || loginId == null) {
			return "redirect:/memberLogin";
		}

		// 세션 정보를 모델에 추가 (템플릿에서 사용)
		if (session != null) {
			model.addAttribute("session", session);
		}
		model.addAttribute("loginId", loginId);
		return "board/board-write";
	}

	// 댓글 달기
	@RequestMapping(value = "insertReply.ajax", method = RequestMethod.POST)
	@ResponseBody
	public List<Map<String, Object>> insertReply(@RequestParam(name = "id") String id, @RequestParam(name = "content") String content,
			@RequestParam(name = "boardNo") int boardNo, HttpSession session) {
		// 댓글 인서트 하기
		Reply re = new Reply();
		re.setId(id);
		re.setContent(content);
		re.setBoardNo(boardNo);
		boardService.insertReply(re);
		// 댓글 리스트 가져오기 및 표시용 데이터 처리
		String loginId = (String) session.getAttribute("loginId");
		List<Reply> replyList = boardService.getReplyList(boardNo);
		return boardService.processReplyListForDisplay(replyList, loginId);
	}

	// 댓글 수정하기
	@RequestMapping(value = "updateReply.ajax", method = RequestMethod.POST)
	@ResponseBody
	public List<Map<String, Object>> updateReply(@RequestParam(name = "no") int no, @RequestParam(name = "id") String id,
			@RequestParam(name = "content") String content, @RequestParam(name = "boardNo") int boardNo, HttpSession session) {
		// 댓글 업데이트하기
		Reply re = new Reply();
		re.setId(id);
		re.setContent(content);
		re.setBoardNo(boardNo);
		re.setNo(no);
		boardService.updateReply(re);
		// 댓글 리스트 가져오기 및 표시용 데이터 처리
		String loginId = (String) session.getAttribute("loginId");
		List<Reply> replyList = boardService.getReplyList(boardNo);
		return boardService.processReplyListForDisplay(replyList, loginId);
	}

	// 댓글 삭제하기
	@RequestMapping(value = "deleteReply.ajax", method = RequestMethod.POST)
	@ResponseBody
	public List<Map<String, Object>> deleteReply(@RequestParam(name = "no") int no, @RequestParam(name = "boardNo") int boardNo, HttpSession session) {
		// 댓글 삭제하기
		boardService.deleteReply(no);
		// 댓글 리스트 가져오기 및 표시용 데이터 처리
		String loginId = (String) session.getAttribute("loginId");
		List<Reply> replyList = boardService.getReplyList(boardNo);
		return boardService.processReplyListForDisplay(replyList, loginId);
	}

	// FAQ 게시판 - 물건상세검색 템플릿 레이아웃 사용
	@RequestMapping("/boardFaq")
	public String boardFaq(HttpSession session, Model model,
			@RequestParam(name = "id", required = false, defaultValue = "null") String id,
			@RequestParam(name = "keyword", required = false, defaultValue = "null") String keyword,
			@RequestParam(name = "category", required = false, defaultValue = "all") String category,
			@RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize) {

		if (session != null) {
			model.addAttribute("session", session);
		}

		Map<String, Object> data = boardService.prepareBoardPageData(id, keyword, category, null, pageNum, pageSize);
		model.addAllAttributes(data);

		return "board/board-faq-list";
	}

	// 게시판 목록 (boardFaq로 리다이렉트)
	@RequestMapping("/boardList")
	public String boardList() {
		return "redirect:/boardFaq";
	}

	// 게시글 상세 보기
	@RequestMapping("/boardDetail")
	public String boardDetail(HttpSession session, Model model,
			@RequestParam(name = "no") int no,
			@RequestParam(name = "id", required = false, defaultValue = "null") String id,
			@RequestParam(name = "keyword", required = false, defaultValue = "null") String keyword,
			@RequestParam(name = "category", required = false, defaultValue = "all") String category,
			@RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize) {

		if (session != null) {
			model.addAttribute("session", session);
		}

		// 상세 데이터 준비 (조회수 증가 포함)
		Map<String, Object> data = boardService.prepareBoardPageData(id, keyword, category, no, pageNum, pageSize);
		
		// 댓글 리스트 추가 (표시용 데이터 처리 포함)
		String loginId = (String) session.getAttribute("loginId");
		List<Reply> replyList = boardService.getReplyList(no);
		List<Map<String, Object>> processedReplyList = boardService.processReplyListForDisplay(replyList, loginId);
		data.put("replyList", processedReplyList);
		
		model.addAllAttributes(data);

		return "board/board-faq-list";
	}

}
