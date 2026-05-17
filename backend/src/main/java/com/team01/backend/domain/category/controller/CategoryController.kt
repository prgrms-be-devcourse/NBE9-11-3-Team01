package com.team01.backend.domain.category.controller

import com.team01.backend.domain.category.dto.CategoryCreateResponseDto
import com.team01.backend.domain.category.dto.CategoryResponseDto
import com.team01.backend.domain.category.service.CategoryService
import com.team01.backend.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@Tag(name = "관리자 카테고리 관리", description = "관리자 카테고리 관리 관련 API")
@RestController
@RequestMapping("/admin/categories")
class CategoryController(
    private val categoryService: CategoryService
) {
    data class CategoryCreateReq(
            @field:PositiveOrZero(message = "게시판이 없습니다")
            val boardId: Long,

            @field:NotBlank(message = "이름이 없습니다")
            @field:Size(min=2, message = "카테고리 이름은 2자 이상이어야 합니다")
            val name: String
    )
    @Operation(summary = "카테고리 생성", description = "카테고리를 생성합니다. 관리자 권한에서만 가능합니다.")
    @PostMapping
    fun createCategory(
            @RequestBody @Valid req: CategoryCreateReq
    ): ResponseEntity<ApiResponse<CategoryCreateResponseDto>>{
        val categoryCreateResponseDto = categoryService.create(req.boardId, req.name)

        return ResponseEntity.ok(ApiResponse.ofSuccess(categoryCreateResponseDto))
    }

    data class CategoryUpdateReq(
            @field:NotBlank(message = "이름이 없습니다")
            @field:Size(min=2, message = "카테고리 이름은 2자 이상이어야 합니다")
            val name: String
    )
    @Operation(summary = "카테고리 수정", description = "카테고리를 수정합니다. 관리자 권한에서만 가능합니다.")
    @PutMapping("/{categoryId}")
    fun updateCategory(
            @PathVariable categoryId: Long,
            @RequestBody @Valid req: CategoryUpdateReq
    ): ResponseEntity<ApiResponse<CategoryCreateResponseDto>>{
        // 카테고리가 있는 게시판은 변경하지 않고, 이름만 수정
        val categoryResponseDto = categoryService.update(categoryId, req.name)

        return ResponseEntity.ok(ApiResponse.ofSuccess(categoryResponseDto))
    }

    @Operation(summary = "카테고리 조회", description = "전체 카테고리를 조회합니다. 관리자 권한에서만 가능합니다.")
    @GetMapping
    fun viewCategory(): ResponseEntity<ApiResponse<List<CategoryResponseDto>>>{
        val categories = categoryService.list()
        return ResponseEntity.ok(ApiResponse.ofSuccess(categories))
    }

}
