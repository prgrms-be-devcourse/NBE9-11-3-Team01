package com.team01.backend.domain.post.service;

import com.team01.backend.domain.post.dto.PostLikeResponseDto;
import com.team01.backend.domain.post.entity.Post;
import com.team01.backend.domain.post.entity.PostLike;
import com.team01.backend.domain.post.repository.PostLikeRepository;
import com.team01.backend.domain.post.repository.PostRepository;
import com.team01.backend.domain.user.entity.User;
import com.team01.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;


    // ─────────────────────────────────────────────────────────────
    //  좋아요 토글 (메인 API)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public PostLikeResponseDto toggleLike(Long postId, String email) {

        User user = findUser(email);
        Post post = findPost(postId); // 검증과 동시에 Post 객체 확보

        boolean liked;

        int inserted = postLikeRepository.tryInsert(user.getId(), postId);
        if (inserted > 0) {
            postRepository.increaseLikeCount(postId);
            liked = true;
        } else {
            int deleted = postLikeRepository.deleteByUserIdAndPostId(user.getId(), postId);
            if (deleted > 0) {
                postRepository.decreaseLikeCount(postId);
                liked = false;
            }
            else{
                liked = true;
            }
        }
        evictTop5Cache(post.getBoard().getId());

        int likeCount = postLikeRepository.countByPostId(postId);
        return new PostLikeResponseDto(liked, likeCount);
    }

    // ─────────────────────────────────────────────────────────────
    // 좋아요 수 조회 - DB
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public int getLikeCount(Long postId) {
        findPost(postId); // 존재 검증
        return postLikeRepository.countByPostId(postId);
    }

    // ─────────────────────────────────────────────────────────────
    //  좋아요 목록 조회 (DB 기준 이력)
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PostLike> getLikes(Long postId) {
        return postLikeRepository.findByPost_Id(postId);
    }

    // ─────────────────────────────────────────────────────────────
    //  내부 헬퍼
    // ─────────────────────────────────────────────────────────────

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없어요"));
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));
    }

    // 캐시 무효화 로직
    private void evictTop5Cache(Long boardId) {
        String cacheKey = "top5:board:" + boardId;
        redisTemplate.delete(cacheKey);
        log.info("좋아요 변경으로 인한 캐시 무효화 완료: {}", cacheKey);
    }
}

