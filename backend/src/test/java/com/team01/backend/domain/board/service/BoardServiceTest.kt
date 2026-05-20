package com.team01.backend.domain.board.service

import com.team01.backend.domain.board.entity.Board
import com.team01.backend.domain.board.repository.BoardRepository
import com.team01.backend.domain.post.repository.PostRepository
import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class BoardServiceTest {
    @Mock
    lateinit var boardRepository: BoardRepository

    @Mock
    lateinit var postRepository: PostRepository

    @InjectMocks
    lateinit var boardService: BoardService

    @Test
    @DisplayName("게시판 생성 - 성공")
    fun t1() {
        // given
        whenever(boardRepository.existsByNameAndDeletedFalse("자유게시판")).thenReturn(false)
        whenever(boardRepository.save(any())).thenAnswer { it.getArgument<Board>(0).apply { setBaseFields(id = 1L) } }

        // when
        val result = boardService.createBoard("자유게시판", "설명")

        // then
        assertEquals(1L, result.id)
        assertEquals("자유게시판", result.name)
        verify(boardRepository).save(any())
    }

    @Test
    @DisplayName("게시판 목록 조회 - 게시글 수 매핑")
    fun t2() {
        // given
        val board1 = board(id = 1L, name = "자유")
        val board2 = board(id = 2L, name = "질문")
        whenever(boardRepository.findAllByDeletedFalse()).thenReturn(listOf(board1, board2))
        whenever(postRepository.countByBoardGrouped()).thenReturn(listOf(arrayOf(1L, 5L)))

        // when
        val result = boardService.getAllBoards()

        // then
        assertEquals(2, result.size)
        assertEquals(5L, result[0].postCount)
        assertEquals(0L, result[1].postCount)
    }

    @Test
    @DisplayName("게시판 삭제 - 없는 게시판이면 예외")
    fun t3() {
        // given
        whenever(boardRepository.findByIdAndDeletedFalse(999L)).thenReturn(null)

        // when // then
        assertThrows(EntityNotFoundException::class.java) {
            boardService.deleteBoard(999L)
        }
    }

    private fun board(id: Long? = null, name: String = "게시판", description: String = "설명"): Board {
        val board = Board(name, description)
        id?.let { board.setBaseFields(id = it) }
        return board
    }

    private fun BaseEntity.setBaseFields(id: Long) {
        val now = java.time.LocalDateTime.of(2026, 5, 18, 12, 0)
        setField("id", id)
        setField("createdAt", now)
        setField("modifiedAt", now)
    }

    private fun BaseEntity.setField(name: String, value: Any) {
        val field = BaseEntity::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }
}
