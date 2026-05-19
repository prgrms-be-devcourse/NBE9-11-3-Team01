package com.team01.backend.domain.board.controller

import com.team01.backend.domain.board.repository.BoardRepository
import com.team01.backend.global.security.JwtTokenProvider
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
internal class AdminBoardControllerTest @Autowired constructor(
    private val mvc: MockMvc,
    private val jwtTokenProvider: JwtTokenProvider,
    private val boardRepository: BoardRepository
) {
    private lateinit var adminCookie: Cookie
    private lateinit var user1Cookie: Cookie

    @BeforeEach
    fun setToken() {
        val adminAccessToken = jwtTokenProvider.createAccessToken("admin@admin.com", "ROLE_ADMIN")
        adminCookie = Cookie("accessToken", adminAccessToken)

        val user1AccessToken = jwtTokenProvider.createAccessToken("user1@test.com", "ROLE_USER")
        user1Cookie = Cookie("accessToken", user1AccessToken)
    }

    @Test
    @DisplayName("게시판 생성 테스트")
    fun t1() {
        val resultActions = mvc.perform(
            post("/admin/boards")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "게시판 이름 1",
                        "description": "게시판 설명 1"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(AdminBoardController::class.java))
            .andExpect(handler().methodName("createBoard"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        resultActions
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.name").value("게시판 이름 1"))
            .andExpect(jsonPath("$.data.description").value("게시판 설명 1"))
            .andExpect(jsonPath("$.data.createdAt").exists())
    }

    @Test
    @DisplayName("게시판 생성 테스트 - null")
    fun t2() {
        val resultActions = mvc.perform(
            post("/admin/boards")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "description": "게시판 설명 1"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(AdminBoardController::class.java))
            .andExpect(handler().methodName("createBoard"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_JSON"))
            .andExpect(jsonPath("$.message").value("잘못된 형식의 JSON 데이터입니다."))
    }

    @Test
    @DisplayName("게시판 생성 테스트 - 최소 길이 충족 X")
    fun t3() {
        val resultActions = mvc.perform(
            post("/admin/boards")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "1",
                        "description": "설명1"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(AdminBoardController::class.java))
            .andExpect(handler().methodName("createBoard"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message", startsWith("입력값이 올바르지 않습니다.")))
            .andExpect(jsonPath("$.message", containsString("name: size")))
            .andExpect(jsonPath("$.message", containsString("description: size")))
    }

    @Test
    @DisplayName("게시판 생성 테스트 - 중복된 이름")
    fun t4() {
        val name = boardRepository.findAll()[0].name
        val resultActions = mvc.perform(
            post("/admin/boards")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "$name",
                        "description": "description 1"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(AdminBoardController::class.java))
            .andExpect(handler().methodName("createBoard"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message", startsWith("중복된 이름입니다")))
    }

    @Test
    @DisplayName("게시판 생성 테스트 - 일반 유저 로그인")
    fun t5() {
        val resultActions = mvc.perform(
            post("/admin/boards")
                .cookie(user1Cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "게시판 이름 1",
                        "description": "게시판 설명 1"
                    }
                """)
        ).andDo(print())

        resultActions.andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("게시판 생성 테스트 - 로그인 안된 상태")
    fun t6() {
        val resultActions = mvc.perform(
            post("/admin/boards")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "게시판 이름 1",
                        "description": "게시판 설명 1"
                    }
                """)
        ).andDo(print())

        resultActions.andExpect(status().isUnauthorized)
    }

    @Test
    @Transactional
    @DisplayName("게시판 수정 테스트")
    fun u1() {
        val targetId = 1
        val resultActions = mvc.perform(
            put("/admin/boards/$targetId")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "modified 1",
                        "description": "description1"
                    }
                """)
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(AdminBoardController::class.java))
            .andExpect(handler().methodName("updateBoard"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.name").value("modified 1"))
            .andExpect(jsonPath("$.data.description").value("description1"))
            .andExpect(jsonPath("$.data.modifiedAt").exists())
    }

    @Test
    @DisplayName("게시판 수정 테스트 - null")
    fun u2() {
        val resultActions = mvc.perform(
            put("/admin/boards/2")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": null,
                        "description": "게시판 설명 1"
                    }
                """)
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(AdminBoardController::class.java))
            .andExpect(handler().methodName("updateBoard"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_JSON"))
            .andExpect(jsonPath("$.message", startsWith("잘못된 형식의 JSON 데이터입니다.")))
    }

    @Test
    @DisplayName("게시판 수정 테스트 - 없는 id")
    fun u3() {
        val resultActions = mvc.perform(
            put("/admin/boards/6")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "name6",
                        "description": "description 6"
                    }
                """)
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(AdminBoardController::class.java))
            .andExpect(handler().methodName("updateBoard"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message", startsWith("요청하신 데이터를 찾을 수 없습니다.")))
    }

    @Test
    @DisplayName("게시판 삭제 테스트")
    fun d1() {
        val resultActions = mvc.perform(
            delete("/admin/boards/3")
                .cookie(adminCookie)
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(AdminBoardController::class.java))
            .andExpect(handler().methodName("deleteBoard"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @DisplayName("게시판 삭제 테스트 - 없는 id")
    fun d2() {
        val resultActions = mvc.perform(
            delete("/admin/boards/6")
                .cookie(adminCookie)
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(AdminBoardController::class.java))
            .andExpect(handler().methodName("deleteBoard"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message", startsWith("요청하신 데이터를 찾을 수 없습니다.")))
    }

    @Test
    @DisplayName("게시판 삭제 테스트 - 삭제된 게시판")
    fun d3() {
        val resultActions = mvc.perform(
            delete("/admin/boards/4")
                .cookie(adminCookie)
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(AdminBoardController::class.java))
            .andExpect(handler().methodName("deleteBoard"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message", startsWith("요청하신 데이터를 찾을 수 없습니다.")))
    }

    @Test
    @DisplayName("게시판 다건 조회 테스트")
    fun v1() {
        val resultActions = mvc.perform(
            get("/admin/boards")
                .cookie(adminCookie)
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(AdminBoardController::class.java))
            .andExpect(handler().methodName("getBoards"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.exist[?(@.deleted == true)]").doesNotExist())
            .andExpect(jsonPath("$.data.deleted[?(@.deleted == true)]").exists())
    }
}