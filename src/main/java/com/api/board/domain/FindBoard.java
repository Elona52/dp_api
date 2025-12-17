package com.api.board.domain;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data  
@NoArgsConstructor 
@AllArgsConstructor 
public class FindBoard {

	private int no;
	private String id;
	private String title;
	private String content;
	private String category; 
	private int views;
	private String relatedLink;
	private Timestamp regDate;
}

