package com.diet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.BusinessException;
import com.diet.dto.CommentCreateDTO;
import com.diet.dto.CommentVO;
import com.diet.dto.PostFeedVO;
import com.diet.dto.PostPublishDTO;
import com.diet.dto.RecipeRatingDTO;
import com.diet.dto.ReportCreateDTO;
import com.diet.entity.CommunityPost;
import com.diet.entity.PostComment;
import com.diet.entity.PostLike;
import com.diet.entity.Recipe;
import com.diet.entity.RecipeCollect;
import com.diet.entity.RecipeRating;
import com.diet.entity.ContentReport;
import com.diet.entity.SysUser;
import com.diet.entity.UserFollow;
import com.diet.mapper.CommunityPostMapper;
import com.diet.mapper.PostCommentMapper;
import com.diet.mapper.PostLikeMapper;
import com.diet.mapper.RecipeCollectMapper;
import com.diet.mapper.RecipeMapper;
import com.diet.mapper.RecipeRatingMapper;
import com.diet.mapper.ContentReportMapper;
import com.diet.mapper.UserFollowMapper;
import com.diet.service.CommunityService;
import com.diet.service.UserService;
import com.diet.util.SensitiveWordFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 社区服务实现类
 * 信息流排序: hot(热度=点赞+2*评论+发布时间衰减的简化版) / new(发布时间) / follow(仅关注者)
 *
 * @author diet
 */
@Service
public class CommunityServiceImpl implements CommunityService {

    private final CommunityPostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final PostLikeMapper likeMapper;
    private final UserFollowMapper followMapper;
    private final RecipeMapper recipeMapper;
    private final RecipeCollectMapper recipeCollectMapper;
    private final RecipeRatingMapper ratingMapper;
    private final ContentReportMapper reportMapper;
    private final UserService userService;
    private final SensitiveWordFilter sensitiveFilter;
    private final ObjectMapper objectMapper;

    public CommunityServiceImpl(CommunityPostMapper postMapper, PostCommentMapper commentMapper,
                                PostLikeMapper likeMapper, UserFollowMapper followMapper,
                                RecipeMapper recipeMapper, RecipeCollectMapper recipeCollectMapper,
                                RecipeRatingMapper ratingMapper, ContentReportMapper reportMapper,
                                UserService userService, SensitiveWordFilter sensitiveFilter,
                                ObjectMapper objectMapper) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.followMapper = followMapper;
        this.recipeMapper = recipeMapper;
        this.recipeCollectMapper = recipeCollectMapper;
        this.ratingMapper = ratingMapper;
        this.reportMapper = reportMapper;
        this.userService = userService;
        this.sensitiveFilter = sensitiveFilter;
        this.objectMapper = objectMapper;
    }

    /**
     * 发布动态: 标题与正文过敏感词过滤后落库
     */
    @Override
    public Long publish(Long userId, PostPublishDTO dto) {
        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setTitle(sensitiveFilter.filter(dto.getTitle()));
        post.setContent(sensitiveFilter.filter(dto.getContent()));
        // 图片列表序列化为JSON
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            try {
                post.setImages(objectMapper.writeValueAsString(dto.getImages()));
            } catch (Exception e) {
                throw new BusinessException("图片数据格式错误");
            }
        }
        post.setRecipeId(dto.getRecipeId());
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setIsTop(0);
        postMapper.insert(post);
        return post.getId();
    }

    /**
     * 信息流
     */
    @Override
    public IPage<PostFeedVO> feed(Long userId, long current, long size, String sort) {
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        String order = sort == null ? "new" : sort;
        switch (order) {
            case "hot" -> wrapper.orderByDesc(CommunityPost::getLikeCount)
                    .orderByDesc(CommunityPost::getCommentCount);
            case "follow" -> {
                // 仅关注者的动态(未关注任何人则返回空)
                List<Long> followees = followMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                                .eq(UserFollow::getFollowerId, userId))
                        .stream().map(UserFollow::getFolloweeId).toList();
                if (followees.isEmpty()) {
                    return new Page<PostFeedVO>(current, size);
                }
                wrapper.in(CommunityPost::getUserId, followees);
                wrapper.orderByDesc(CommunityPost::getPublishTime);
            }
            default -> wrapper.orderByDesc(CommunityPost::getPublishTime);
        }
        // 置顶优先
        wrapper.orderByDesc(CommunityPost::getIsTop);
        IPage<CommunityPost> page = postMapper.selectPage(new Page<>(current, size), wrapper);
        return convertPage(userId, page);
    }

    /**
     * 指定用户动态(个人主页)
     */
    @Override
    public IPage<PostFeedVO> userPosts(Long visitorId, Long targetUserId, long current, long size) {
        IPage<CommunityPost> page = postMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getUserId, targetUserId)
                        .orderByDesc(CommunityPost::getPublishTime));
        return convertPage(visitorId, page);
    }

    /**
     * 点赞切换: 已赞则取消(删除记录并-1), 未赞则点赞(+1)
     */
    @Override
    public boolean toggleLike(Long userId, Long postId) {
        PostLike exist = likeMapper.selectOne(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId));
        if (exist != null) {
            likeMapper.deleteById(exist.getId());
            CommunityPost post = postMapper.selectById(postId);
            if (post != null) {
                post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                postMapper.updateById(post);
            }
            return false;
        }
        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        likeMapper.insert(like);
        CommunityPost post = postMapper.selectById(postId);
        if (post != null) {
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
        }
        return true;
    }

    /**
     * 评论: 敏感词过滤后落库, 动态评论数+1
     */
    @Override
    public Long comment(Long userId, CommentCreateDTO dto) {
        CommunityPost post = postMapper.selectById(dto.getPostId());
        if (post == null) {
            throw new BusinessException("动态不存在");
        }
        PostComment comment = new PostComment();
        comment.setPostId(dto.getPostId());
        comment.setUserId(userId);
        comment.setContent(sensitiveFilter.filter(dto.getContent()));
        comment.setStatus(0);
        commentMapper.insert(comment);
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);
        return comment.getId();
    }

    /**
     * 动态评论列表(屏蔽的不返回)
     */
    @Override
    public List<CommentVO> comments(Long postId) {
        List<PostComment> comments = commentMapper.selectList(new LambdaQueryWrapper<PostComment>()
                .eq(PostComment::getPostId, postId)
                .eq(PostComment::getStatus, 0)
                .orderByAsc(PostComment::getCreateTime));
        if (comments.isEmpty()) {
            return List.of();
        }
        Map<Long, SysUser> userMap = userService.listByIds(
                comments.stream().map(PostComment::getUserId).toList())
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return comments.stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setPostId(c.getPostId());
            vo.setUserId(c.getUserId());
            SysUser u = userMap.get(c.getUserId());
            vo.setNickname(u != null ? u.getNickname() : "未知用户");
            vo.setContent(c.getContent());
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).toList();
    }

    /**
     * 关注切换: 已关注则取关, 未关注则关注
     */
    @Override
    public boolean toggleFollow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BusinessException("不能关注自己");
        }
        UserFollow exist = followMapper.selectOne(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId));
        if (exist != null) {
            followMapper.deleteById(exist.getId());
            return false;
        }
        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        followMapper.insert(follow);
        return true;
    }

    /**
     * 个人主页: 用户信息+粉丝/关注/动态统计+访客是否已关注
     */
    @Override
    public Map<String, Object> profile(Long visitorId, Long targetUserId) {
        SysUser user = userService.getById(targetUserId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        result.put("username", user.getUsername());
        result.put("postCount", postMapper.selectCount(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getUserId, targetUserId)));
        result.put("followerCount", followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFolloweeId, targetUserId)));
        result.put("followingCount", followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, targetUserId)));
        if (!visitorId.equals(targetUserId)) {
            result.put("followed", followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, visitorId)
                    .eq(UserFollow::getFolloweeId, targetUserId)) > 0);
        } else {
            result.put("followed", null);
        }
        return result;
    }

    /**
     * 食谱收藏切换(带分类文件夹)
     */
    @Override
    public boolean toggleCollectRecipe(Long userId, Long recipeId, String category) {
        RecipeCollect exist = recipeCollectMapper.selectOne(new LambdaQueryWrapper<RecipeCollect>()
                .eq(RecipeCollect::getUserId, userId).eq(RecipeCollect::getRecipeId, recipeId));
        if (exist != null) {
            recipeCollectMapper.deleteById(exist.getId());
            return false;
        }
        RecipeCollect collect = new RecipeCollect();
        collect.setUserId(userId);
        collect.setRecipeId(recipeId);
        collect.setCategory(StringUtils.hasText(category) ? category : "默认");
        recipeCollectMapper.insert(collect);
        return true;
    }

    /**
     * 收藏夹: 按自定义分类分组返回食谱
     */
    @Override
    public Map<String, List<Map<String, Object>>> collectFolder(Long userId) {
        List<RecipeCollect> collects = recipeCollectMapper.selectList(
                new LambdaQueryWrapper<RecipeCollect>()
                        .eq(RecipeCollect::getUserId, userId)
                        .orderByDesc(RecipeCollect::getCreateTime));
        Map<String, List<Map<String, Object>>> folder = new LinkedHashMap<>();
        if (collects.isEmpty()) {
            return folder;
        }
        Map<Long, Recipe> recipeMap = recipeMapper.selectBatchIds(
                collects.stream().map(RecipeCollect::getRecipeId).toList())
                .stream().collect(Collectors.toMap(Recipe::getId, Function.identity()));
        for (RecipeCollect c : collects) {
            String cat = c.getCategory() == null ? "默认" : c.getCategory();
            List<Map<String, Object>> list = folder.computeIfAbsent(cat, k -> new ArrayList<>());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("collectId", c.getId());
            item.put("recipeId", c.getRecipeId());
            item.put("category", cat);
            item.put("createTime", c.getCreateTime());
            Recipe r = recipeMap.get(c.getRecipeId());
            if (r != null) {
                item.put("recipeName", r.getRecipeName());
                item.put("caloriePerServing", r.getCaloriePerServing());
                item.put("difficulty", r.getDifficulty());
                item.put("cookingTime", r.getCookingTime());
                item.put("isOfficial", r.getIsOfficial());
            }
            list.add(item);
        }
        return folder;
    }

    /**
     * 食谱评分(同用户重复评分覆盖)
     */
    @Override
    public void rateRecipe(Long userId, RecipeRatingDTO dto) {
        if (recipeMapper.selectById(dto.getRecipeId()) == null) {
            throw new BusinessException("食谱不存在");
        }
        RecipeRating exist = ratingMapper.selectOne(new LambdaQueryWrapper<RecipeRating>()
                .eq(RecipeRating::getUserId, userId).eq(RecipeRating::getRecipeId, dto.getRecipeId()));
        if (exist != null) {
            exist.setScore(dto.getScore());
            exist.setFeedback(dto.getFeedback());
            ratingMapper.updateById(exist);
        } else {
            RecipeRating rating = new RecipeRating();
            rating.setUserId(userId);
            rating.setRecipeId(dto.getRecipeId());
            rating.setScore(dto.getScore());
            rating.setFeedback(dto.getFeedback());
            ratingMapper.insert(rating);
        }
    }

    /**
     * 食谱平均评分与评分数
     */
    @Override
    public Map<String, Object> recipeScore(Long recipeId) {
        List<RecipeRating> ratings = ratingMapper.selectList(new LambdaQueryWrapper<RecipeRating>()
                .eq(RecipeRating::getRecipeId, recipeId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", ratings.size());
        result.put("avgScore", ratings.isEmpty() ? 0.0
                : ratings.stream().mapToInt(RecipeRating::getScore).average().orElse(0.0));
        return result;
    }

    /**
     * 举报内容(进入后台待处理队列)
     */
    @Override
    public void report(Long reporterId, ReportCreateDTO dto) {
        if (!"POST".equals(dto.getTargetType()) && !"COMMENT".equals(dto.getTargetType())) {
            throw new BusinessException("目标类型不合法");
        }
        ContentReport report = new ContentReport();
        report.setReporterId(reporterId);
        report.setTargetType(dto.getTargetType());
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        report.setStatus(0);
        reportMapper.insert(report);
    }

    /**
     * 动态页转信息流VO(批量组装作者/点赞/关注标记/图片/关联食谱)
     */
    private IPage<PostFeedVO> convertPage(Long userId, IPage<CommunityPost> page) {
        List<CommunityPost> posts = page.getRecords();
        Page<PostFeedVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (posts.isEmpty()) {
            voPage.setRecords(List.of());
            return voPage;
        }
        // 批量查作者
        Map<Long, SysUser> userMap = userService.listByIds(
                posts.stream().map(CommunityPost::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        // 批量查当前用户点赞标记
        Set<Long> likedPostIds = likeMapper.selectList(new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId)
                        .in(PostLike::getPostId, posts.stream().map(CommunityPost::getId).toList()))
                .stream().map(PostLike::getPostId).collect(Collectors.toSet());
        // 批量查当前用户关注标记
        Set<Long> followedIds = followMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId))
                .stream().map(UserFollow::getFolloweeId).collect(Collectors.toSet());
        // 批量查关联食谱
        List<Long> recipeIds = posts.stream().map(CommunityPost::getRecipeId)
                .filter(r -> r != null).distinct().toList();
        Map<Long, Recipe> recipeMap = recipeIds.isEmpty() ? Map.of()
                : recipeMapper.selectBatchIds(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, Function.identity()));

        voPage.setRecords(posts.stream().map(post -> {
            PostFeedVO vo = new PostFeedVO();
            vo.setId(post.getId());
            vo.setAuthorId(post.getUserId());
            SysUser author = userMap.get(post.getUserId());
            vo.setAuthorName(author != null ? author.getUsername() : "未知");
            vo.setAuthorNickname(author != null && StringUtils.hasText(author.getNickname())
                    ? author.getNickname() : vo.getAuthorName());
            vo.setTitle(post.getTitle());
            vo.setContent(post.getContent());
            vo.setLikeCount(post.getLikeCount());
            vo.setCommentCount(post.getCommentCount());
            vo.setIsTop(post.getIsTop());
            vo.setPublishTime(post.getPublishTime());
            vo.setLiked(likedPostIds.contains(post.getId()));
            if (!post.getUserId().equals(userId)) {
                vo.setFollowed(followedIds.contains(post.getUserId()));
            }
            // 图片JSON反序列化
            if (StringUtils.hasText(post.getImages())) {
                try {
                    vo.setImages(objectMapper.readValue(post.getImages(), new TypeReference<>() {
                    }));
                } catch (Exception ignored) {
                    // 图片数据异常时忽略
                }
            }
            if (post.getRecipeId() != null) {
                vo.setRecipeId(post.getRecipeId());
                Recipe r = recipeMap.get(post.getRecipeId());
                if (r != null) {
                    vo.setRecipeName(r.getRecipeName());
                }
            }
            return vo;
        }).toList());
        return voPage;
    }
}