package com.hoaxify.ws.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.hoaxify.ws.dto.DtoTag;

public interface ITagController {

	ResponseEntity<List<DtoTag>> getTags();
}
