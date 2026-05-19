package com.team01.backend.domain.board.controller

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class BoardControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    @DisplayName("게시판 목록 조회 - 성공")
    fun t1() {
        // given

        // when
        val resultActions = mvc.perform(get("/boards")).andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(BoardController::class.java))
            .andExpect(handler().methodName("getAllBoards"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    @DisplayName("게시판 목록 조회 - 초기 데이터 반환")
    fun t2() {
        // given

        // when
        val resultActions = mvc.perform(get("/boards")).andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(BoardController::class.java))
            .andExpect(handler().methodName("getAllBoards"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(3))
    }

    @Test
    @DisplayName("게시판 목록 조회 - 게시판별 게시글 수 포함")
    fun t3() {
        // given

        // when
        val resultActions = mvc.perform(get("/boards")).andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(BoardController::class.java))
            .andExpect(handler().methodName("getAllBoards"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].postCount").exists())
            .andExpect(jsonPath("$.data[0].postCount").isNumber)
    }

    @Test
    @DisplayName("게시판별 카테고리 목록 조회 - 성공")
    fun t4() {
        // given

        // when
        val resultActions = mvc.perform(get("/boards/1/categories")).andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(BoardController::class.java))
            .andExpect(handler().methodName("getCategoriesByBoard"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
    }
}
