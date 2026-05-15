package com.team01.backend.domain.board.service

import com.team01.backend.domain.board.dto.*
import com.team01.backend.domain.board.entity.Board
import com.team01.backend.domain.board.repository.BoardRepository
import com.team01.backend.domain.post.repository.PostRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service


@Service
@RequiredArgsConstructor
class BoardService(
    private val boardRepository: BoardRepository,
    private val postRepository: PostRepository
) {

    // 게시판 생성, dto 형식으로 반환
    @Transactional
    fun createBoard(name: String,description: String): BoardCreateResponseDto{
        // 삭제되지 않은 게시판 중 중복이 있는지 확인
        if(boardRepository.existsByNameAndDeletedFalse(name)){
            throw  IllegalArgumentException("중복된 이름입니다")
        }
        val board = Board(name, description)
        boardRepository.save(board)
        return BoardCreateResponseDto(board)
    }
    // 게시판 목록 조회
    fun getAllBoards(): List<BoardResponse> {
        val boards: List<Board> = boardRepository.findAllByDeletedFalse()

        // 게시판별 게시글 수 한 번에 조회 (N+1 방지)
        val postCountMap = postRepository.countByBoardGrouped()
            .associate { row -> (row[0] as Long) to (row[1] as Long) }

        return boards
            .map {
                    board: Board ->
                BoardResponse.from(
                    board,
                    postCountMap[board.id] ?:0L
                )
            }
    }

    // 관리자 게시판 목록 조회 (삭제된것까지 포함해서)
    fun getAllBoardsByAdmin(): AdminBoardListResponseDto{
        val exist: List<AdminBoardResponseDto> = boardRepository.findAllByDeletedFalse()
            .map(::AdminBoardResponseDto)
        val deleted: List<AdminBoardResponseDto> = boardRepository.findAllByDeletedTrue()
            .map(::AdminBoardResponseDto)
        return  AdminBoardListResponseDto(exist, deleted)
    }

    // 게시판 수정, dto 형식으로 반환
    @Transactional
    fun updateBoard(id: Long, name: String , description: String): BoardUpdateResponseDto {
        val board: Board? = boardRepository.findByIdAndDeletedFalse(id)
        if(boardRepository.existsByNameAndDeletedFalse(name)){
            throw IllegalArgumentException("중복된 이름입니다")
        }
        board?.let{
            it.update(name, description)
            boardRepository.save(board)
        }?:throw EntityNotFoundException("")

        return BoardUpdateResponseDto(board)
    }

    // board 수 반환
    fun count(): Long {
        return boardRepository.count()
    }

    // 게시판 삭제
    @Transactional
    fun deleteBoard(id: Long){
        val board: Board? = boardRepository.findByIdAndDeletedFalse(id)
        board?.let{  // soft delete
            it.delete()
            boardRepository.save(board)
        }?:throw EntityNotFoundException("") // 없는 id, 삭제된 게시판 예외 처리
    }

    fun existsById(id: Long):Boolean {
        return boardRepository.existsById(id)
    }
}
