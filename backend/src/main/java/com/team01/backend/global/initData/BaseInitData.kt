package com.team01.backend.global.initData

import com.team01.backend.domain.board.repository.BoardRepository
import com.team01.backend.domain.board.service.BoardService
import com.team01.backend.domain.category.repository.CategoryRepository
import com.team01.backend.domain.category.service.CategoryService
import com.team01.backend.domain.comment.service.CommentService
import com.team01.backend.domain.post.entity.Post
import com.team01.backend.domain.post.entity.PostLike
import com.team01.backend.domain.post.repository.PostLikeRepository
import com.team01.backend.domain.post.repository.PostRepository
import com.team01.backend.domain.post.service.PostService
import com.team01.backend.domain.user.dto.SignUpRequest
import com.team01.backend.domain.user.entity.User
import com.team01.backend.domain.user.repository.UserRepository
import com.team01.backend.domain.user.service.AuthService
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.transaction.annotation.Transactional

@Configuration
class BaseInitData(
    @Lazy private val self: BaseInitData,
    private val boardService: BoardService,
    private val postService: PostService,
    private val commentService: CommentService,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val categoryRepository: CategoryRepository,
    private val boardRepository: BoardRepository,
    private val categoryService: CategoryService,
    private val authService: AuthService,
    private val postLikeRepository: PostLikeRepository,
) {

    @Bean
    fun initData(): ApplicationRunner = ApplicationRunner {
        self.setMember()
        self.setBoard()
        self.setCategory()
        self.setPost()
        self.setComment()
        self.setPostLike()
    }

    @Transactional
    fun setMember() {
        if (userRepository.count() > 0) return

        authService.signUp(
            SignUpRequest(
                email = "user1@test.com",
                password = "password1234",
                nickname = "유저1",
            ),
        )
        authService.signUp(
            SignUpRequest(
                email = "user2@test.com",
                password = "password1234",
                nickname = "유저2",
            ),
        )

        for (i in 3..40) {
            authService.signUp(
                SignUpRequest(
                    email = "user$i@test.com",
                    password = "password1234",
                    nickname = "테스트유저$i",
                ),
            )
        }
    }

    @Transactional
    fun setBoard() {
        if (boardService.count() > 0) return

        boardService.createBoard(
            "자유게시판",
            "취업 준비의 고단함부터 소소한 일상까지, 우리들만의 솔직한 이야기를 나누는 공간입니다.",
        )
        boardService.createBoard(
            "취업 공고",
            "꿈을 향한 첫걸음, 최신 채용 공고를 확인하고 당신의 커리어를 시작하세요.",
        )
        boardService.createBoard(
            "자소서 피드백",
            "혼자 쓰면 막막한 자기소개서, 합격 선배와 동료들의 꼼꼼한 첨삭으로 완성도를 높여보세요.",
        )

        boardService.createBoard("name4", "description4")
        boardService.deleteBoard(4L)
    }

    @Transactional
    fun setPost() {
        if (postRepository.count() > 0) return

        val author1 = userRepository.findByEmail("user1@test.com").orElseThrow()
        val author2 = userRepository.findByEmail("user2@test.com").orElseThrow()

        val board = boardRepository.findById(1L)
            .orElseThrow { RuntimeException("Board not found") }
        val category1 = categoryRepository.findById(1L)
            .orElseThrow { RuntimeException("Category not found") }
        val category2 = categoryRepository.findById(2L)
            .orElseThrow { RuntimeException("Category not found") }
        val category3 = categoryRepository.findById(3L)
            .orElseThrow { RuntimeException("Category not found") }

        for (i in 1..15) {
            val post = Post(author1, "${i}번째 게시글입니다.", "내용 $i", board, category1)
            postRepository.save(post)
        }

        val post1 = Post(
            author2,
            "2026 삼성전자 상반기 공채 일정 총정리",
            """
            삼성전자 상반기 공채 일정 공식 발표 기준으로 정리했습니다.
            틀린 부분 있으면 댓글로 알려주세요!

            📌 전체 일정
            - 서류 접수: 3월 10일 ~ 3월 31일
            - GSAT: 4월 19일 (토)
            - 합격 발표: 5월 초 예정
            - 직무면접: 5월 중순 예정
            - 임원면접: 6월 초 예정
            - 최종 발표: 6월 말 예정

            📌 지원 가능 부문
            - DX부문 (MX, VD/DA, 경영지원 등)
            - DS부문 (메모리, 파운드리, S.LSI 등)
            - 삼성디스플레이
            - 삼성SDI / 삼성전기 / 삼성SDS (별도 공채)

            📌 주의사항
            - 계열사별로 일정 다를 수 있으니 반드시 공식 채용 홈페이지 확인
            - GSAT는 온라인이 아닌 오프라인 시험장에서 진행
            - 지원서는 한 번 제출하면 수정 불가 (초안 꼭 저장해두기)

            📌 작년 대비 달라진 점
            - 서류 항목 일부 간소화
            - 직무 에세이 글자수 소폭 증가

            공채 준비하시는 분들 모두 화이팅입니다 🔥
            """.trimIndent(),
            board,
            category3,
        )
        postRepository.save(post1)

        for (i in 16..30) {
            val post = Post(author2, "${i}번째 게시글입니다.", "내용 $i", board, category2)
            postRepository.save(post)
        }

        val post2 = Post(
            author2,
            "취업 준비 6개월 차, 솔직한 현실 정리해봤어요",
            """
            안녕하세요, 올해 2월에 졸업하고 취준 6개월 차 접어드는 26살입니다.

            요즘 너무 지쳐서 그냥 털어놓고 싶어서 올려봐요.
            비슷한 상황이신 분들이랑 공감하고 싶기도 하고요.

            📌 지금까지 한 것들
            - 자격증: 정보처리기사, SQLD 취득
            - 코테: 백준 골드 3 / 프로그래머스 Lv.3 일부
            - 토익: 875점 (더 올릴지 고민 중)
            - 지원한 곳: 대기업 4곳, 중견 3곳, 스타트업 2곳
            - 현재까지 결과: 서류 4곳 탈락, 코테 2곳 탈락, 면접 1곳 탈락, 대기 2곳

            📌 솔직한 심정
            처음엔 "6개월이면 붙겠지" 했는데
            생각보다 훨씬 길고 외로운 싸움이더라고요.

            친구들은 하나둘 붙어서 회사 다니는데
            저는 아직도 카페에서 코테 풀고 있으니까요.

            근데 또 신기한 게, 포기하고 싶다가도
            합격 후기 하나 읽으면 다시 하게 되더라고요.

            📌 그래서 드리고 싶은 말
            지금 저처럼 지쳐있는 분들,
            우리 그냥 조금만 더 버텨봐요.

            취준은 실력만큼 타이밍이랑 운도 있다고 생각해요.
            언젠간 우리 차례가 올 거라고 믿고 싶어요.

            혹시 비슷한 상황이신 분들 댓글로 이야기 나눠요 🙂
            같이 힘내봐요.
            """.trimIndent(),
            board,
            category2,
        )
        postRepository.save(post2)

        for (i in 31..35) {
            val post = Post(author2, "${i}번째 게시글입니다.", "내용 $i", board, category3)
            postRepository.save(post)
        }
    }

    @Transactional
    fun setComment() {
        if (commentService.count() > 0) return

        val author1 = userRepository.findByEmail("user1@test.com").orElseThrow()
        val author2 = userRepository.findByEmail("user2@test.com").orElseThrow()

        commentService.writeInitComment(1L, author1, "첫 번째 댓글입니다", null)
        commentService.writeInitComment(1L, author2, "두 번째 댓글입니다", null)
        commentService.writeInitComment(1L, author1, "세 번째 댓글입니다", null)

        commentService.writeInitComment(1L, author2, "첫 번째 대댓글입니다", 1L)
        commentService.writeInitComment(1L, author1, "두 번째 대댓글입니다", 2L)

        commentService.writeInitComment(2L, author1, "첫 번째 댓글입니다", null)
        commentService.writeInitComment(2L, author2, "두 번째 댓글입니다", null)

        commentService.writeInitComment(2L, author2, "첫 번째 대댓글입니다", 6L)
    }

    @Transactional
    fun setCategory() {
        if (categoryService.count() > 0) return

        categoryService.create(1L, "가입인사")
        categoryService.create(1L, "취업준비")
        categoryService.create(1L, "정보공유")

        categoryService.create(2L, "Backend")
        categoryService.create(2L, "Frontend")
    }

    @Transactional
    fun setPostLike() {
        if (postLikeRepository.count() > 0) return

        val post1 = postRepository.findById(1L).orElseThrow()
        val post2 = postRepository.findById(2L).orElseThrow()
        val post3 = postRepository.findById(3L).orElseThrow()

        for (i in 3L..14L) {
            val user = userRepository.findById(i).orElseThrow()
            postLikeRepository.save(PostLike(user, post1))
            postRepository.increaseLikeCount(post1.id)
        }

        for (i in 3L..32L) {
            val user = userRepository.findById(i).orElseThrow()
            postLikeRepository.save(PostLike(user, post2))
            postRepository.increaseLikeCount(post2.id!!)
        }

        for (i in 3L..22L) {
            val user = userRepository.findById(i).orElseThrow()
            postLikeRepository.save(PostLike(user, post3))
            postRepository.increaseLikeCount(post3.id)
        }
    }
}
