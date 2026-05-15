package com.team01.backend.domain.category.service

import com.team01.backend.domain.board.service.BoardService
import com.team01.backend.domain.category.dto.CategoryCreateResponseDto
import com.team01.backend.domain.category.dto.CategoryResponseDto
import com.team01.backend.domain.category.entity.Category
import com.team01.backend.domain.category.repository.CategoryRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val boardService: BoardService
){
    @Transactional
    fun create(boardId: Long, name: String): CategoryCreateResponseDto{

        if(!boardService.existsById(boardId)){ //boardId에 해당하는 게시판이 없다면 예외처리
            throw EntityNotFoundException()
        }
        if(categoryRepository.existsByBoardIdAndName(boardId, name)){
            throw IllegalArgumentException("중복된 이름입니다")
        }
        val category = Category(boardId, name)
        categoryRepository.save(category)
        return CategoryCreateResponseDto.from(category)
    }

    fun count() =
        categoryRepository.count()

    @Transactional
    fun update(categoryId: Long, name: String): CategoryCreateResponseDto{
        val category = categoryRepository.findByIdOrNull(categoryId)
            ?:throw EntityNotFoundException("존재하지 않는 카테고리입니다")

        if(category.name!=name && categoryRepository.existsByBoardIdAndName(category.boardId, name)){ //게시판 별 이름 중복 체크 - 내 이름 제외
            throw  IllegalArgumentException("중복된 이름입니다")
        }

        category.update(name)
//        categoryRepository.save(category)
        return  CategoryCreateResponseDto.from(category)
    }

    fun list(): List<CategoryResponseDto>{
        return categoryRepository.findAll()
            .map(CategoryResponseDto::from)
    }

    // 게시판별 카테고리 목록 조회 (글쓰기 페이지 카테고리 드롭다운용)
    fun listByBoardId(boardId: Long): List<CategoryResponseDto>{
        return categoryRepository.findByBoardId(boardId)
            .map(CategoryResponseDto::from)
    }
}
