package com.team01.backend.domain.post.controller

import com.team01.backend.domain.board.entity.Board
import com.team01.backend.domain.board.repository.BoardRepository
import com.team01.backend.domain.category.entity.Category
import com.team01.backend.domain.category.repository.CategoryRepository
import com.team01.backend.domain.post.repository.PostRepository
import com.team01.backend.domain.post.service.PostService
import com.team01.backend.global.security.JwtTokenProvider
import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostControllerTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var postService: PostService

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var boardRepository: BoardRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var user1Cookie: Cookie

    @BeforeEach
    fun setToken() {

        val user1AccessToken = jwtTokenProvider.createAccessToken("user1@test.com", "ROLE_USER")

        user1Cookie = Cookie("accessToken", user1AccessToken)
    }

    @Test
    @DisplayName("게시판별 글 목록 조회 - 성공")
    fun t1() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/1/posts?page=1&size=20"))
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("getPostsByBoardId"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.posts").isArray())
            .andExpect(jsonPath("$.data.currentPage").value(1))
            .andExpect(jsonPath("$.data.totalPages").exists())
            .andExpect(jsonPath("$.data.totalElements").exists())
            .andExpect(jsonPath("$.data.hasNext").exists())
            .andExpect(jsonPath("$.data.posts[0].author").exists())
            .andExpect(jsonPath("$.data.posts[0].categoryId").exists())
            .andExpect(jsonPath("$.data.posts[0].categoryName").exists())
    }

    @Test
    @DisplayName("게시판별 글 목록 조회 - 존재하지 않는 게시판")
    fun t2() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/999/posts"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    @DisplayName("글 작성")
    fun t3() {
        val title = "제목입니다."
        val content = "내용입니다."
        val boardId = 1L
        val categoryId = 1L

        val resultActions = mvc
            .perform(
                post("/posts")
                    .cookie(user1Cookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "title": "$title",
                            "content": "$content",
                            "boardId": $boardId,
                            "categoryId": $categoryId
                        }
                        """.trimIndent()
                    )
            )
            .andDo(print())

        // 검증
        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("write"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.title").value(title))
            .andExpect(jsonPath("$.data.content").value(content))
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.modifiedAt").exists())
    }

    @Test
    @DisplayName("글 작성 실패 - 제목이 입력되지 않은 경우")
    fun t4() {
        val title = ""
        val content = "내용입니다."
        val boardId = 1L
        val categoryId = 1L

        val resultActions = mvc
            .perform(
                post("/posts")
                    .cookie(user1Cookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "title": "$title",
                            "content": "$content",
                            "boardId": $boardId,
                            "categoryId": $categoryId
                        }
                        """.trimIndent()
                    )
            )
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("write"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    @DisplayName("글 작성, 내용이 입력되지 않은 경우")
    fun t5() {
        val title = "제목입니다."
        val content = ""
        val boardId = 1L
        val categoryId = 1L

        val resultActions = mvc
            .perform(
                post("/posts")
                    .cookie(user1Cookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "title": "$title",
                            "content": "$content",
                            "boardId": $boardId,
                            "categoryId": $categoryId
                        }
                        """.trimIndent()
                    )
            )
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("write"))
            .andExpect(status().isBadRequest()) // 💡 입력값 검증 실패에 따른 상태코드(400) 검증을 명확히 추가했습니다.
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    @DisplayName("글 작성 실패 - JSON 양식이 잘못된 경우")
    fun t6() {
        val title = "제목입니다."
        val content = "내용입니다."

        val resultActions = mvc
            .perform(
                post("/posts")
                    .cookie(user1Cookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "title": "$title"   // 의도적인 콤마(,) 누락으로 INVALID_JSON 유도
                            "content": "$content"
                        }
                        """.trimIndent()
                    )
            )
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("write"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_JSON"))
            .andExpect(jsonPath("$.message").value("잘못된 형식의 JSON 데이터입니다."))
    }


    @Test
    @DisplayName("글 수정 성공 - 제목, 내용, 올바른 카테고리 변경")
    fun t7_1() {
        // 기존 게시글 정보 조회 (연관된 게시판 ID를 얻기 위해서)

        val targetId = 1L
        val targetPost = postRepository.findById(targetId).orElse(null)
            ?: throw EntityNotFoundException("대상 게시글 없음")

        // 해당 게시판에 속한 다른 카테고리를 새롭게 준비 (검증 로직 통과를 위함)
        val targetBoardId = targetPost.board.id
        val newCategory = categoryRepository.save(
            Category(
                boardId = targetBoardId,
                name = "수정된 카테고리"
            )
        )
        val newCategoryId = newCategory.id

        val title = "제목 수정"
        val content = "내용 수정"

        val resultActions = mvc
            .perform(
                put("/posts/$targetId") // 접두사 제거 완료
                    .cookie(user1Cookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "title": "$title",
                            "content": "$content",
                            "categoryId": $newCategoryId
                        }
                        """.trimIndent()
                    )
            )
            .andDo(print())


        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("modify"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value(title))
            .andExpect(jsonPath("$.data.content").value(content))
            .andExpect(jsonPath("$.data.categoryId").value(newCategoryId))
            .andExpect(jsonPath("$.data.categoryName").value("수정된 카테고리"))

        val post = postRepository.findById(targetId).orElse(null)
            ?: throw EntityNotFoundException()

        AssertionsForClassTypes.assertThat(post.title).isEqualTo(title)
        AssertionsForClassTypes.assertThat(post.content).isEqualTo(content)
        AssertionsForClassTypes.assertThat(post.category.id).isEqualTo(newCategoryId)
    }

    @Test
    @DisplayName("글 수정 실패 - 다른 게시판의 카테고리 ID를 전달한 경우")
    fun t7_2() {
        // 기존 게시글 준비

        val targetId = 1L
        val targetPost = postRepository.findById(targetId).orElse(null)
            ?: throw EntityNotFoundException("대상 게시글 없음")

        //        Long originalBoardId = targetPost.getBoard().getId();

        // 다른 게시판, 그 게시판의 카테고리 생성 (예: 공지사항 게시판)
        val anotherBoard = boardRepository.save(
            Board(
                name = "공지사항",
                description = "공지사항 게시판"
            )
        )
        val invalidCategory = categoryRepository.save(
            Category(
                boardId = anotherBoard.id,
                name = "공지용 카테고리"
            )
        )
        val invalidCategoryId = invalidCategory.id

        val resultActions = mvc
            .perform(
                put("/posts/$targetId")
                    .cookie(user1Cookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "title": "수정 시도",
                            "content": "내용 수정 시도",
                            "categoryId": $invalidCategoryId
                        }
                        """.trimIndent()
                    )
            )
            .andDo(print())


        resultActions
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").value("해당 게시판에서 사용할 수 없는 카테고리입니다."))

        // DB 데이터가 변경되지 않았는지 확인 (Safety Check)
        val post = postRepository.findById(targetId).orElse(null)
            ?: throw EntityNotFoundException()

        AssertionsForClassTypes.assertThat(post.category.id).isNotEqualTo(invalidCategoryId)
    }

    @Test
    @DisplayName("글 수정 실패 - 작성자가 아닌 경우 (인가 실패)")
    fun t7_3() {
        // 로그인 (user1이 작성한 글을 user2로 로그인해서 수정 시도)
        val user2AccessToken = jwtTokenProvider.createAccessToken("user2@test.com", "ROLE_USER")
        val user2Cookie = Cookie("accessToken", user2AccessToken)

        val targetId = 1L // 1번 게시글의 작성자는 user1

        val resultActions = mvc
            .perform(
                put("/posts/$targetId")
                    .cookie(user2Cookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "title": "수정 시도",
                            "content": "내용 수정 시도",
                            "categoryId": 1
                        }
                        """.trimIndent()
                    )
            )
            .andDo(print())

        resultActions
            .andExpect(status().isForbidden()) // 403 Forbidden 기대
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("작성자만 수정할 수 있습니다."))
    }

    @Test
    @DisplayName("게시글 상세 조회 - 성공")
    fun t8() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(
                get("/posts/1")
                    .cookie(user1Cookie)
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("getPostById"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.title").exists())
            .andExpect(jsonPath("$.data.content").exists())
            .andExpect(jsonPath("$.data.author").exists())
            .andExpect(jsonPath("$.data.comments").isArray())
            .andExpect(jsonPath("$.data.likeCount").exists())
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.modifiedAt").exists())
    }

    @Test
    @DisplayName("게시글 상세 조회 - 존재하지 않는 게시글")
    fun t9() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(
                get("/posts/999")
                    .cookie(user1Cookie)
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    @DisplayName("글 삭제 성공")
    fun t10_1() {
        val post = postService.write("user1@test.com", "테스트 제목", "테스트 내용", 1L, 1L)
        val targetId = post.id

        val resultActions = mvc
            .perform(
                delete("/posts/$targetId") // 정적 임포트 및 접두사 제거 완료
                    .cookie(user1Cookie)
            )
            .andDo(print())

        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))

        val deletedPost = postRepository.findById(targetId).orElse(null)
            ?: throw EntityNotFoundException("대상 게시글 없음")
        assertThat(deletedPost.deleted).isTrue()
    }

    @Test
    @DisplayName("글 삭제 실패 - 작성자가 아닌 경우")
    fun t10_2() {
        // author : user1

        val post = postService.write("user1@test.com", "테스트 제목", "테스트 내용", 1L, 1L)
        val targetId = post.id

        // actor : user2
        val user2AccessToken = jwtTokenProvider.createAccessToken("user2@test.com", "ROLE_USER")
        val user2Cookie = Cookie("accessToken", user2AccessToken)

        // 다른 유저가 삭제 요청
        val resultActions = mvc
            .perform(
                delete("/posts/$targetId") // 정적 임포트 및 접두사 제거 완료
                    .cookie(user2Cookie)
            )
            .andDo(print())

        // 403 Forbidden 검증
        resultActions
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("작성자만 삭제할 수 있습니다."))

        // 삭제되지 않았는지 확인
        val notDeletedPost = postRepository.findById(targetId).orElse(null)
            ?: throw EntityNotFoundException("대상 게시글 없음")
        assertThat(notDeletedPost.deleted).isFalse()
    }

    @Test
    @DisplayName("게시글 상세 조회 - 삭제된 게시글")
    fun t11() {
        // given
        val post = postService.write("user1@test.com", "테스트 제목", "테스트 내용", 1L, 1L)
        postService.delete(post.id, "user1@test.com")
        assertThat(post.deleted).isTrue()

        // when
        val resultActions: ResultActions = mvc
            .perform(
                get("/posts/${post.id}")
                    .cookie(user1Cookie)
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    @DisplayName("게시판별 글 목록 조회 - 삭제된 게시판")
    fun t12() {
        // given: BaseInitData에서 4번 게시판이 삭제된 상태

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/4/posts"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    @DisplayName("게시판별-카테고리별 글 목록 조회 성공")
    fun t13() {
        // given
        val boardId = 1L
        val categoryId = 1L

        // when
        val resultActions: ResultActions = mvc
            .perform(
                get("/boards/$boardId/categories/$categoryId/posts")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("getPostsByCategory"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.posts").isArray())
            .andExpect(jsonPath("$.data.currentPage").value(1))
            .andExpect(jsonPath("$.data.totalPages").exists())
            .andExpect(jsonPath("$.data.totalElements").exists())
            .andExpect(jsonPath("$.data.hasNext").exists())
            .andExpect(jsonPath("$.data.posts[0].categoryId").value(categoryId))
    }

    @Test
    @DisplayName("게시판별-카테고리별 글 목록 조회 실패 - 타 게시판의 카테고리 선택")
    fun t14() {
        // given: 테스트 시점 기준 1번 게시판에는 3번 카테고리까지 존재
        val boardId = 1L
        val invalidCategoryId = 4L

        // when
        val resultActions: ResultActions = mvc
            .perform(
                get("/boards/$boardId/categories/$invalidCategoryId/posts")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").value("해당 게시판에서 사용할 수 없는 카테고리입니다."))
    }

    @Test
    @DisplayName("게시판별 글 목록 조회 - 잘못된 페이지 번호")
    fun t15() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/1/posts?page=0"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
    }

    @Test
    @DisplayName("게시글 키워드 검색 - 결과 있음")
    fun t16() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/1/posts?page=1&keyword=2026 삼성전자"))
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("getPostsByBoardId"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.posts").isArray())
            .andExpect(jsonPath("$.data.posts[0].title").value("2026 삼성전자 상반기 공채 일정 총정리"))
            .andExpect(jsonPath("$.data.totalElements").value(1))
    }

    @Test
    @DisplayName("게시글 키워드 검색 - 결과 없음")
    fun t17() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/1/posts?page=1&keyword=존재하지않는키워드"))
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("getPostsByBoardId"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.posts").isEmpty())
            .andExpect(jsonPath("$.data.totalElements").value(0))
    }

    @Test
    @DisplayName("게시글 키워드 검색 - 검색어 50자 초과")
    fun t18() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/1/posts?page=1&keyword=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
    }

    @Test
    @DisplayName("게시글 카테고리 필터링 - 성공")
    fun t19() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/1/posts?page=1&categoryId=1"))
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("getPostsByBoardId"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.posts").isArray())
            .andExpect(jsonPath("$.data.posts[0].categoryId").value(1))
            .andExpect(jsonPath("$.data.totalElements").value(15))
    }

    @Test
    @DisplayName("게시글 키워드 + 카테고리 필터링 - 성공")
    fun t20() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/1/posts?page=1&keyword=취업 준비 6개월&categoryId=2"))
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("getPostsByBoardId"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.posts").isArray())
            .andExpect(jsonPath("$.data.posts[0].title").value("취업 준비 6개월 차, 솔직한 현실 정리해봤어요"))
            .andExpect(jsonPath("$.data.totalElements").value(1))
    }

    @Test
    @DisplayName("게시글 카테고리 필터링 - 존재하지 않는 카테고리")
    fun t21() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/1/posts?page=1&categoryId=999"))
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("getPostsByBoardId"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.posts").isEmpty())
            .andExpect(jsonPath("$.data.totalElements").value(0))
    }

    @Test
    @DisplayName("게시판별-카테고리별 글 목록 조회 실패 - 잘못된 페이지 번호")
    fun t22() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/1/categories/1/posts?page=0"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
    }

    @Test
    @DisplayName("게시판별-카테고리별 글 목록 조회 - 키워드 검색 성공")
    fun t23() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/1/categories/1/posts?page=1&keyword=15번째"))
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("getPostsByCategory"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.posts").isArray())
            .andExpect(jsonPath("$.data.posts[0].title").value("15번째 게시글입니다."))
            .andExpect(jsonPath("$.data.totalElements").value(1))
    }

    @Test
    @DisplayName("게시판별-카테고리별 글 목록 조회 - 키워드 검색 결과 없음")
    fun t24() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/boards/1/categories/1/posts?page=1&keyword=존재하지않는키워드"))
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(PostController::class.java))
            .andExpect(handler().methodName("getPostsByCategory"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.posts").isEmpty())
            .andExpect(jsonPath("$.data.totalElements").value(0))
    }

    @Test
    @DisplayName("게시글 상세 조회 실패 - 비로그인 사용자")
    fun t25() {
        // given

        // when
        val resultActions: ResultActions = mvc
            .perform(get("/posts/1"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }
}
