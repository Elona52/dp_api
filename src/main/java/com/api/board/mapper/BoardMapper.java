package com.api.board.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.api.board.domain.FindBoard;
import com.api.board.domain.Reply;

@Mapper  
public interface BoardMapper {

	void insertBoard(FindBoard board);
	
	List<FindBoard> getBoardList(@Param("id") String id, 
	                             @Param("keyword") String keyword,
	                             @Param("category") String category);

	FindBoard getBoard(int no);

	void updateBoard(FindBoard board);
	
	void incrementBoardViews(int no);
	
	void deleteBoard(int no);
	
	void insertReply(Reply re);
	
	void updateReply(Reply re);
	
	void deleteReply(int no);
	
	List<Reply> getReplyList(int boardNo);
	
	int getAuctionCount(@Param("period") String period, @Param("keyword") String keyword);
}

