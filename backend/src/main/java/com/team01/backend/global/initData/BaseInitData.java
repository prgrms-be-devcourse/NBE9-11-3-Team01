package com.team01.backend.global.initData;

import com.team01.backend.domain.board.entity.Board;
import com.team01.backend.domain.board.repository.BoardRepository;
import com.team01.backend.domain.board.service.BoardService;
import com.team01.backend.domain.category.entity.Category;
import com.team01.backend.domain.category.repository.CategoryRepository;
import com.team01.backend.domain.category.service.CategoryService;
import com.team01.backend.domain.comment.service.CommentService;
import com.team01.backend.domain.post.entity.Post;
import com.team01.backend.domain.post.entity.PostLike;
import com.team01.backend.domain.post.repository.PostLikeRepository;
import com.team01.backend.domain.post.repository.PostRepository;
import com.team01.backend.domain.post.service.PostService;
import com.team01.backend.domain.user.dto.SignUpRequest;
import com.team01.backend.domain.user.entity.User;
import com.team01.backend.domain.user.repository.UserRepository;
import com.team01.backend.domain.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {

    @Autowired
    @Lazy
    private BaseInitData self;
    @Autowired
    private BoardService boardService;
    @Autowired
    private PostService postService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private AuthService authService;
    @Autowired
    private PostLikeRepository postLikeRepository;

    @Bean
    public ApplicationRunner initData() {
        return args -> {
            self.setMember();
            self.setBoard();
            self.setCategory();
            self.setPost();
            self.setComment();
            self.setPostLike();
        };
    }

    // 유저 데이터 생성
    @Transactional
    public void setMember() {

        if (userRepository.count() > 0) return;
        authService.signUp(SignUpRequest.builder().email("user1@test.com").password("password1234").nickname("유저1").build());
        authService.signUp(SignUpRequest.builder().email("user2@test.com").password("password1234").nickname("유저2").build());
        authService.signUp(SignUpRequest.builder().email("admin@admin.com").password("passworda12345").nickname("admin").admin(true).adminToken("user_admin-2026").build());

        // 테스트용 유저 30명
        for (int i = 1; i <= 30; i++) {
            authService.signUp(SignUpRequest.builder()
                    .email("test" + i + "@test.com")
                    .password("password1234")
                    .nickname("테스터" + i)
                    .build());
        }
    }

    // 게시판 생성
    @Transactional
    public void setBoard(){
        if(boardService.count() > 0){
            return;
        }
        boardService.createBoard("자유게시판", "취업 준비의 고단함부터 소소한 일상까지, 우리들만의 솔직한 이야기를 나누는 공간입니다.");
        boardService.createBoard("취업 공고", "꿈을 향한 첫걸음, 최신 채용 공고를 확인하고 당신의 커리어를 시작하세요.");
        boardService.createBoard("자소서 피드백", "혼자 쓰면 막막한 자기소개서, 합격 선배와 동료들의 꼼꼼한 첨삭으로 완성도를 높여보세요.");

        //4번 게시판 삭제
        boardService.createBoard("name4", "description4");
        boardService.deleteBoard(4L);
    }

    // 게시글 생성
    @Transactional
    public void setPost(){
        if(postRepository.count() > 0) return;

        User author1 = userRepository.findByEmail("user1@test.com").orElseThrow();
        User author2 = userRepository.findByEmail("user2@test.com").orElseThrow();

        Board board = boardRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        Category category1 = categoryRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        Category category2 = categoryRepository.findById(2L)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        Category category3 = categoryRepository.findById(3L)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        for (int i = 1; i <= 15; i++) {
            Post post = new Post(author1, i + "번째 게시글입니다.", "내용 " + i, board, category1);
            postRepository.save(post);
        }
        Post post1 = new Post(author2, "2026 삼성전자 상반기 공채 일정 총정리",
                "삼성전자 상반기 공채 일정 공식 발표 기준으로 정리했습니다.\n" +
                        "틀린 부분 있으면 댓글로 알려주세요!\n" +
                        "\n" +
                        "\uD83D\uDCCC 전체 일정\n" +
                        "- 서류 접수: 3월 10일 ~ 3월 31일\n" +
                        "- GSAT: 4월 19일 (토)\n" +
                        "- 합격 발표: 5월 초 예정\n" +
                        "- 직무면접: 5월 중순 예정\n" +
                        "- 임원면접: 6월 초 예정\n" +
                        "- 최종 발표: 6월 말 예정\n" +
                        "\n" +
                        "\uD83D\uDCCC 지원 가능 부문\n" +
                        "- DX부문 (MX, VD/DA, 경영지원 등)\n" +
                        "- DS부문 (메모리, 파운드리, S.LSI 등)\n" +
                        "- 삼성디스플레이\n" +
                        "- 삼성SDI / 삼성전기 / 삼성SDS (별도 공채)\n" +
                        "\n" +
                        "\uD83D\uDCCC 주의사항\n" +
                        "- 계열사별로 일정 다를 수 있으니 반드시 공식 채용 홈페이지 확인\n" +
                        "- GSAT는 온라인이 아닌 오프라인 시험장에서 진행\n" +
                        "- 지원서는 한 번 제출하면 수정 불가 (초안 꼭 저장해두기)\n" +
                        "\n" +
                        "\uD83D\uDCCC 작년 대비 달라진 점\n" +
                        "- 서류 항목 일부 간소화\n" +
                        "- 직무 에세이 글자수 소폭 증가\n" +
                        "\n" +
                        "공채 준비하시는 분들 모두 화이팅입니다 \uD83D\uDD25", board, category3);
        postRepository.save(post1);

        for (int i = 16; i <= 30; i++) {
            Post post = new Post(author2, i + "번째 게시글입니다.", "내용 " + i, board, category2);
            postRepository.save(post);
        }

        Post post2 = new Post(author2, "취업 준비 6개월 차, 솔직한 현실 정리해봤어요",
                "안녕하세요, 올해 2월에 졸업하고 취준 6개월 차 접어드는 26살입니다.\n" +
                        "\n" +
                        "요즘 너무 지쳐서 그냥 털어놓고 싶어서 올려봐요.\n" +
                        "비슷한 상황이신 분들이랑 공감하고 싶기도 하고요.\n" +
                        "\n" +
                        "\uD83D\uDCCC 지금까지 한 것들\n" +
                        "- 자격증: 정보처리기사, SQLD 취득\n" +
                        "- 코테: 백준 골드 3 / 프로그래머스 Lv.3 일부\n" +
                        "- 토익: 875점 (더 올릴지 고민 중)\n" +
                        "- 지원한 곳: 대기업 4곳, 중견 3곳, 스타트업 2곳\n" +
                        "- 현재까지 결과: 서류 4곳 탈락, 코테 2곳 탈락, 면접 1곳 탈락, 대기 2곳\n" +
                        "\n" +
                        "\uD83D\uDCCC 솔직한 심정\n" +
                        "처음엔 \"6개월이면 붙겠지\" 했는데\n" +
                        "생각보다 훨씬 길고 외로운 싸움이더라고요.\n" +
                        "\n" +
                        "친구들은 하나둘 붙어서 회사 다니는데\n" +
                        "저는 아직도 카페에서 코테 풀고 있으니까요.\n" +
                        "\n" +
                        "근데 또 신기한 게, 포기하고 싶다가도\n" +
                        "합격 후기 하나 읽으면 다시 하게 되더라고요.\n" +
                        "\n" +
                        "\uD83D\uDCCC 그래서 드리고 싶은 말\n" +
                        "지금 저처럼 지쳐있는 분들,\n" +
                        "우리 그냥 조금만 더 버텨봐요.\n" +
                        "\n" +
                        "취준은 실력만큼 타이밍이랑 운도 있다고 생각해요.\n" +
                        "언젠간 우리 차례가 올 거라고 믿고 싶어요.\n" +
                        "\n" +
                        "혹시 비슷한 상황이신 분들 댓글로 이야기 나눠요 \uD83D\uDE42\n" +
                        "같이 힘내봐요.", board, category2);
        postRepository.save(post2);

        for (int i = 31; i <= 35; i++) {
            Post post = new Post(author2, i + "번째 게시글입니다.", "내용 " + i, board, category3);
            postRepository.save(post);
        }
    }

    @Transactional
    public void setComment() {
        if (commentService.count() > 0) return;

        User author1 = userRepository.findByEmail("user1@test.com").orElseThrow();
        User author2 = userRepository.findByEmail("user2@test.com").orElseThrow();

        // 일반 댓글 — parentId 자리에 null
        commentService.writeInitComment(1L, author1,"첫 번째 댓글입니다", null);
        commentService.writeInitComment(1L, author2,"두 번째 댓글입니다", null);
        commentService.writeInitComment(1L, author1,"세 번째 댓글입니다", null);

        // 대댓글 — parentId 자리에 id 값
        commentService.writeInitComment(1L, author2,"첫 번째 대댓글입니다", 1L);
        commentService.writeInitComment(1L, author1,"두 번째 대댓글입니다", 2L);

        commentService.writeInitComment(2L, author1,"첫 번째 댓글입니다", null);
        commentService.writeInitComment(2L, author2,"두 번째 댓글입니다", null);

        commentService.writeInitComment(2L, author2,"첫 번째 대댓글입니다", 6L);
    }
    @Transactional
    public void setCategory(){
        if(categoryService.count() > 0){
            return;
        }
        // 1번 게시판에 글 3개
        categoryService.create(1L, "가입인사");
        categoryService.create(1L, "취업준비");
        categoryService.create(1L, "정보공유");


        categoryService.create(2L, "Backend");
        categoryService.create(2L, "Frontend");
    }

    @Transactional
    public void setPostLike() {
        if (postLikeRepository.count() > 0) return;

        Post post1 = postRepository.findById(1L).orElseThrow();
        Post post2 = postRepository.findById(2L).orElseThrow();
        Post post3 = postRepository.findById(3L).orElseThrow();

        // post1 좋아요 12개
        for (long i = 3; i <= 14; i++) {
            User user = userRepository.findById(i).orElseThrow();
            postLikeRepository.save(new PostLike(user, post1));
            postRepository.increaseLikeCount(post1.getId());
        }

        // post2 좋아요 30개
        for (long i = 3; i <= 32; i++) {
            User user = userRepository.findById(i).orElseThrow();
            postLikeRepository.save(new PostLike(user, post2));
            postRepository.increaseLikeCount(post2.getId());
        }

        // post3 좋아요 20개
        for (long i = 3; i <= 22; i++) {
            User user = userRepository.findById(i).orElseThrow();
            postLikeRepository.save(new PostLike(user, post3));
            postRepository.increaseLikeCount(post3.getId());
        }
    }
}
