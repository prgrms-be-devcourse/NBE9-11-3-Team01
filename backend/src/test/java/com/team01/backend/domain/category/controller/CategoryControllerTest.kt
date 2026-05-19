package com.team01.backend.domain.category.controller

import com.team01.backend.domain.category.repository.CategoryRepository
import com.team01.backend.global.security.JwtTokenProvider
import jakarta.servlet.http.Cookie
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
@Transactional
@AutoConfigureMockMvc
internal class CategoryControllerTest @Autowired constructor(
    private val mvc: MockMvc,
    private val jwtTokenProvider: JwtTokenProvider,
    private val categoryRepository: CategoryRepository
) {
    private lateinit var adminCookie: Cookie
    private lateinit var user1Cookie: Cookie

    @BeforeEach
    fun setUp() {
        // 토큰 발급 및 쿠키 생성
        val adminAccessToken = jwtTokenProvider.createAccessToken("admin@admin.com", "ROLE_ADMIN")
        adminCookie = Cookie("accessToken", adminAccessToken)

        val user1AccessToken = jwtTokenProvider.createAccessToken("user1@test.com", "ROLE_USER")
        user1Cookie = Cookie("accessToken", user1AccessToken)
    }

    @Test
    @DisplayName("카테고리 생성 테스트")
    fun c1() {
        val resultActions = mvc.perform(
            post("/admin/categories")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "boardId": 1,
                        "name": "카테고리 4"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CategoryController::class.java))
            .andExpect(handler().methodName("createCategory"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").isNumber) // 하드코딩 값(6) 보다는 유연하게 숫자 타입 검증 권장
            .andExpect(jsonPath("$.data.boardId").value(1))
            .andExpect(jsonPath("$.data.name").value("카테고리 4"))
            .andExpect(jsonPath("$.data.createdAt").exists())
    }

    @Test
    @DisplayName("카테고리 생성 테스트 - null")
    fun c2() {
        val resultActions = mvc.perform(
            post("/admin/categories")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "카테고리 2"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CategoryController::class.java))
            .andExpect(handler().methodName("createCategory"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_JSON"))
            .andExpect(jsonPath("$.message", startsWith("잘못된 형식의 JSON 데이터입니다.")))
    }

    @Test
    @DisplayName("카테고리 생성 테스트 - 이름 최소 글자수")
    fun c3() {
        val resultActions = mvc.perform(
            post("/admin/categories")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "boardId": 1,
                        "name": "6"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CategoryController::class.java))
            .andExpect(handler().methodName("createCategory"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message", startsWith("입력값이 올바르지 않습니다.")))
    }

    @Test
    @DisplayName("카테고리 생성 테스트 - 없는 게시판에 등록")
    fun c4() {
        val resultActions = mvc.perform(
            post("/admin/categories")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "boardId": 11,
                        "name": "category 2"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CategoryController::class.java))
            .andExpect(handler().methodName("createCategory"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message", startsWith("요청하신 데이터를 찾을 수 없습니다")))
    }

    @Test
    @DisplayName("카테고리 생성 테스트 - 게시판 별 이름 중복")
    fun c5() {
        val name = categoryRepository.findAll()[0].name
        val resultActions = mvc.perform(
            post("/admin/categories")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "boardId": 1,
                        "name": "$name"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CategoryController::class.java))
            .andExpect(handler().methodName("createCategory"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").value("중복된 이름입니다"))
    }

    @Test
    @DisplayName("카테고리 생성 - 다른게시판에만 존재하는 이름 - 성공")
    fun c6() {
        val resultActions = mvc.perform(
            post("/admin/categories")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "boardId": 3,
                        "name": "카테고리 1"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CategoryController::class.java))
            .andExpect(handler().methodName("createCategory"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").isNumber)
            .andExpect(jsonPath("$.data.boardId").value(3))
            .andExpect(jsonPath("$.data.name").value("카테고리 1"))
            .andExpect(jsonPath("$.data.createdAt").exists())
    }

    @Test
    @DisplayName("카테고리 생성 테스트 - 권한 없음 ")
    fun c7() {
        val resultActions = mvc.perform(
            post("/admin/categories")
                .cookie(user1Cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "boardId": 1,
                        "name": "카테고리 6"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message", startsWith("권한이 없습니다.")))
    }

    @Test
    @DisplayName("카테고리 수정 테스트")
    fun u1() {
        val resultActions = mvc.perform(
            put("/admin/categories/1")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "카테고리 1 - 수정"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CategoryController::class.java))
            .andExpect(handler().methodName("updateCategory"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.boardId").value(1))
            .andExpect(jsonPath("$.data.name").value("카테고리 1 - 수정"))
            .andExpect(jsonPath("$.data.modifiedAt").exists())
    }

    @Test
    @DisplayName("카테고리 수정 테스트 - null")
    fun u2() {
        val resultActions = mvc.perform(
            put("/admin/categories/2")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CategoryController::class.java))
            .andExpect(handler().methodName("updateCategory"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_JSON"))
            .andExpect(jsonPath("$.message", startsWith("잘못된 형식의 JSON 데이터입니다.")))
    }

    @Test
    @DisplayName("카테고리 수정 테스트 - 없는 id")
    fun u3() {
        val resultActions = mvc.perform(
            put("/admin/categories/21")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "카테고리 21"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CategoryController::class.java))
            .andExpect(handler().methodName("updateCategory"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message", startsWith("요청하신 데이터를 찾을 수 없습니다")))
    }

    @Test
    @DisplayName("카테고리 수정 테스트 - 이름 중복 체크")
    fun u4() {
        val name = categoryRepository.findAll()[0].name
        val resultActions = mvc.perform(
            put("/admin/categories/2")
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "$name"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CategoryController::class.java))
            .andExpect(handler().methodName("updateCategory"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message", startsWith("중복된 이름입니다")))
    }

    @Test
    @DisplayName("카테고리 수정 테스트 - 권한 없음 ")
    fun u5() {
        val resultActions = mvc.perform(
            put("/admin/categories/1")
                .cookie(user1Cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "카테고리 1 - 수정"
                    }
                """)
        ).andDo(print())

        resultActions
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message", startsWith("권한이 없습니다.")))
    }

    @Test
    @DisplayName("카테고리 관리자 조회 테스트")
    fun v1() {
        val resultActions = mvc.perform(
            get("/admin/categories")
                .cookie(adminCookie)
        ).andDo(print())

        resultActions
            .andExpect(handler().handlerType(CategoryController::class.java))
            .andExpect(handler().methodName("viewCategory"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[4]").exists())
    }

    @Test
    @DisplayName("카테고리 관리자 조회 테스트 - 권한 없음 ")
    fun v2() {
        val resultActions = mvc.perform(
            get("/admin/categories")
        ).andDo(print())

        resultActions
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message", startsWith("인증이 필요합니다.")))
    }
}