package com.team01.backend.domain.comment.controller

import com.jayway.jsonpath.JsonPath
import com.team01.backend.config.TestMailConfig
import com.team01.backend.config.TestRedisConfig
import com.team01.backend.domain.comment.dto.CommentDeleteResponseDto
import com.team01.backend.domain.comment.repository.CommentRepository
import com.team01.backend.domain.comment.service.CommentService
import com.team01.backend.domain.post.entity.Post
import com.team01.backend.domain.post.repository.PostRepository
import com.team01.backend.domain.user.entity.Role
import com.team01.backend.domain.user.entity.User
import com.team01.backend.domain.user.repository.UserRepository
import com.team01.backend.global.security.JwtTokenProvider
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestMailConfig::class, TestRedisConfig::class)
@Transactional
class CommentControllerTest {

    @Autowired private lateinit var mvc: MockMvc
    @Autowired private lateinit var postRepository: PostRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var commentRepository: CommentRepository
    @Autowired private lateinit var commentService: CommentService
    @Autowired private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var testUser: User
    private lateinit var testPost: Post
    private lateinit var testPost2: Post
    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        testUser = userRepository.findByEmail("user1@test.com").orElseThrow()
        accessToken = jwtTokenProvider.createAccessToken(testUser.email, testUser.role.key)
        testPost = postRepository.findById(1L).orElseThrow()
        testPost2 = postRepository.findById(2L).orElseThrow()
    }

    private fun authCookie(token: String = accessToken) = Cookie("accessToken", token)

    @Test
    @DisplayName("댓글 생성 - 1번 글에 생성")
    fun writeComment_success() {
        val content = "새로운 댓글"
        mvc.perform(
            post("/posts/${testPost.id}/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(authCookie())
                .content("""{"content": "$content"}"""),
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").value(content))
            .andExpect(jsonPath("$.data.author").value("유저1"))
    }

    @Test
    @DisplayName("댓글 생성 - 내용이 없을 시 예외")
    fun writeComment_emptyContent_fails() {
        mvc.perform(
            post("/posts/${testPost.id}/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(authCookie())
                .content("""{"content": ""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
    }

    @Test
    @DisplayName("댓글 작성 실패 - 없는 게시글")
    fun writeComment_postNotFound() {
        mvc.perform(
            post("/posts/999/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(authCookie())
                .content("""{"content": "댓글"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    @DisplayName("댓글 작성 실패 - 삭제된 게시글")
    fun writeComment_deletedPost_fails() {
        ReflectionTestUtils.setField(testPost, "deleted", true)
        postRepository.saveAndFlush(testPost)
        mvc.perform(
            post("/posts/${testPost.id}/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(authCookie())
                .content("""{"content": "댓글"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
    }

    @Test
    @DisplayName("댓글 작성 실패 - 300자 초과")
    fun writeComment_contentTooLong_fails() {
        mvc.perform(
            post("/posts/${testPost.id}/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(authCookie())
                .content("""{"content": "${"a".repeat(301)}"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
    }

    @Test
    @DisplayName("대댓글 생성 - 부모 댓글에 대댓글 작성")
    fun writeReply_success() {
        val parentRes = mvc.perform(
            post("/posts/${testPost.id}/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(authCookie())
                .content("""{"content": "부모"}"""),
        ).andReturn().response.contentAsString
        val parentId = JsonPath.read<Int>(parentRes, "$.data.id")
        mvc.perform(
            post("/posts/${testPost.id}/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(authCookie())
                .content("""{"content": "대댓글", "parentId": $parentId}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").value("대댓글"))
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 대댓글에 답글 불가")
    fun writeReply_toReply_fails() {
        val p1 = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"부모"}"""))
            .andReturn().response.contentAsString
        val parentId = JsonPath.read<Int>(p1, "$.data.id")
        val p2 = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"자식","parentId":$parentId}"""))
            .andReturn().response.contentAsString
        val childId = JsonPath.read<Int>(p2, "$.data.id")
        mvc.perform(
            post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
                .cookie(authCookie()).content("""{"content":"불가","parentId":$childId}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("답글에는 답글을 달 수 없습니다."))
    }

    @Test
    @DisplayName("댓글 작성 실패 - 다른 게시글의 댓글에 대댓글")
    fun writeReply_wrongPost_fails() {
        val res = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"원글댓글"}"""))
            .andReturn().response.contentAsString
        val parentId = JsonPath.read<Int>(res, "$.data.id")
        mvc.perform(
            post("/posts/${testPost2.id}/comments").contentType(MediaType.APPLICATION_JSON)
                .cookie(authCookie()).content("""{"content":"잘못","parentId":$parentId}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("잘못된 게시글의 댓글입니다."))
    }

    @Test
    @DisplayName("댓글 수정 - 본인 댓글 수정")
    fun updateComment_success() {
        val res = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"원본"}"""))
            .andReturn().response.contentAsString
        val id = JsonPath.read<Int>(res, "$.data.id")
        mvc.perform(
            put("/comments/$id").contentType(MediaType.APPLICATION_JSON).cookie(authCookie())
                .content("""{"content":"수정됨"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").value("수정됨"))
    }

    @Test
    @DisplayName("댓글 조회 - 삭제된 루트는 플레이스홀더")
    fun getComments_deletedRoot_showsPlaceholder() {
        val res = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"원댓글"}"""))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val rootId = JsonPath.read<Number>(res, "$.data.id").toLong()
        commentRepository.findById(rootId).orElseThrow().softDelete()
        commentRepository.flush()
        val list = mvc.perform(get("/posts/${testPost.id}/comments").cookie(authCookie()))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val ids: List<Int> = JsonPath.read(list, "$.data[*].id")
        val idx = ids.indexOf(rootId.toInt())
        assertTrue(idx >= 0)
        assertEquals(
            CommentDeleteResponseDto.DELETED_CONTENT_PLACEHOLDER,
            JsonPath.read(list, "$.data[$idx].content"),
        )
    }

    @Test
    @DisplayName("댓글 조회 - 삭제된 답글은 replies에 플레이스홀더")
    fun getComments_deletedReply_showsPlaceholder() {
        val rootRes = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"루트"}"""))
            .andReturn().response.contentAsString
        val rootId = JsonPath.read<Number>(rootRes, "$.data.id").toLong()
        val replyRes = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"답글","parentId":$rootId}"""))
            .andReturn().response.contentAsString
        val replyId = JsonPath.read<Number>(replyRes, "$.data.id").toLong()
        commentRepository.findById(replyId).orElseThrow().softDelete()
        commentRepository.flush()
        val list = mvc.perform(get("/posts/${testPost.id}/comments").cookie(authCookie()))
            .andReturn().response.contentAsString
        val rootIdx = (JsonPath.read<List<Int>>(list, "$.data[*].id")).indexOf(rootId.toInt())
        val replyIdx = (JsonPath.read<List<Int>>(list, "$.data[$rootIdx].replies[*].id")).indexOf(replyId.toInt())
        assertEquals(
            CommentDeleteResponseDto.DELETED_CONTENT_PLACEHOLDER,
            JsonPath.read(list, "$.data[$rootIdx].replies[$replyIdx].content"),
        )
    }

    @Test
    @DisplayName("댓글 작성 실패 - 삭제된 부모에 답글")
    fun writeComment_replyToDeletedParent_fails() {
        val res = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"부모"}"""))
            .andReturn().response.contentAsString
        val rootId = JsonPath.read<Number>(res, "$.data.id").toLong()
        commentRepository.findById(rootId).orElseThrow().softDelete()
        commentRepository.flush()
        mvc.perform(
            post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
                .cookie(authCookie()).content("""{"content":"불가","parentId":$rootId}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("삭제된 댓글에는 답글을 달 수 없습니다."))
    }

    @Test
    @DisplayName("댓글 삭제 - 소프트 삭제 후 조회")
    fun deleteComment_softDelete_thenGetShowsPlaceholder() {
        val res = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"삭제대상"}"""))
            .andReturn().response.contentAsString
        val id = JsonPath.read<Number>(res, "$.data.id").toLong()
        mvc.perform(delete("/comments/$id").cookie(authCookie())).andExpect(status().isOk)
        val list = mvc.perform(get("/posts/${testPost.id}/comments").cookie(authCookie()))
            .andReturn().response.contentAsString
        val idx = (JsonPath.read<List<Int>>(list, "$.data[*].id")).indexOf(id.toInt())
        assertEquals(CommentDeleteResponseDto.DELETED_CONTENT_PLACEHOLDER, JsonPath.read(list, "$.data[$idx].content"))
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 본인이 아님")
    fun deleteComment_notOwner_throws() {
        val res = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"내꺼"}"""))
            .andReturn().response.contentAsString
        val id = JsonPath.read<Number>(res, "$.data.id").toLong()
        val other = userRepository.saveAndFlush(User("other@test.com", "pw", "다른사람", null, Role.USER, null))
        assertThrows(AccessDeniedException::class.java) {
            commentService.deleteComment(id, other.email)
        }
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 이미 삭제됨")
    fun deleteComment_alreadyDeleted() {
        val res = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"두번"}"""))
            .andReturn().response.contentAsString
        val id = JsonPath.read<Number>(res, "$.data.id").toLong()
        mvc.perform(delete("/comments/$id").cookie(authCookie())).andExpect(status().isOk)
        mvc.perform(delete("/comments/$id").cookie(authCookie()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("이미 삭제된 댓글입니다."))
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 삭제된 게시글")
    fun deleteComment_postDeleted_notFound() {
        val res = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"글삭제"}"""))
            .andReturn().response.contentAsString
        val id = JsonPath.read<Number>(res, "$.data.id").toLong()
        ReflectionTestUtils.setField(testPost, "deleted", true)
        postRepository.saveAndFlush(testPost)
        mvc.perform(delete("/comments/$id").cookie(authCookie()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    @DisplayName("댓글 좋아요 토글 - 등록 후 취소")
    fun toggleCommentLike_likeAndUnlike() {
        val res = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"좋아요"}"""))
            .andReturn().response.contentAsString
        val id = JsonPath.read<Number>(res, "$.data.id").toLong()
        mvc.perform(post("/comments/$id/likes").cookie(authCookie()))
            .andExpect(jsonPath("$.data.liked").value(true))
            .andExpect(jsonPath("$.data.likeCount").value(1))
        mvc.perform(post("/comments/$id/likes").cookie(authCookie()))
            .andExpect(jsonPath("$.data.liked").value(false))
            .andExpect(jsonPath("$.data.likeCount").value(0))
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 삭제된 댓글")
    fun toggleCommentLike_deletedComment_fails() {
        val res = mvc.perform(post("/posts/${testPost.id}/comments").contentType(MediaType.APPLICATION_JSON)
            .cookie(authCookie()).content("""{"content":"삭제"}"""))
            .andReturn().response.contentAsString
        val id = JsonPath.read<Number>(res, "$.data.id").toLong()
        commentService.deleteComment(id, testUser.email)
        mvc.perform(post("/comments/$id/likes").cookie(authCookie()))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("댓글 조회 실패 - 비로그인")
    fun getComments_withoutLogin_fails() {
        mvc.perform(get("/posts/${testPost.id}/comments")).andExpect(status().isUnauthorized)
    }
}
