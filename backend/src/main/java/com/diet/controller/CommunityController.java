package com.diet.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.dto.CommentCreateDTO;
import com.diet.dto.CommentVO;
import com.diet.dto.PostFeedVO;
import com.diet.dto.PostPublishDTO;
import com.diet.dto.RecipeRatingDTO;
import com.diet.dto.ReportCreateDTO;
import com.diet.service.CommunityService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 社区控制器
 * 信息流排序: hot-热门, new-最新, follow-关注
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    /**
     * 发布动态(文字+图片, 敏感词过滤)
     */
    @PostMapping("/publish")
    public Result<Long> publish(@Validated @RequestBody PostPublishDTO dto) {
        return Result.success(communityService.publish(UserContext.get().getUserId(), dto));
    }

    /**
     * 信息流(热门/最新/关注排序)
     */
    @GetMapping("/feed")
    public Result<IPage<PostFeedVO>> feed(@RequestParam(defaultValue = "new") String sort,
                                         @RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size) {
        return Result.success(communityService.feed(UserContext.get().getUserId(), current, size, sort));
    }

    /**
     * 指定用户动态列表(个人主页)
     */
    @GetMapping("/user/{userId}/posts")
    public Result<IPage<PostFeedVO>> userPosts(@PathVariable Long userId,
                                               @RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size) {
        return Result.success(communityService.userPosts(UserContext.get().getUserId(), userId, current, size));
    }

    /**
     * 个人主页信息(粉丝/关注/动态统计)
     */
    @GetMapping("/user/{userId}/profile")
    public Result<Map<String, Object>> profile(@PathVariable Long userId) {
        return Result.success(communityService.profile(UserContext.get().getUserId(), userId));
    }

    /**
     * 点赞/取消点赞(幂等切换)
     */
    @PostMapping("/{postId}/like")
    public Result<Boolean> like(@PathVariable Long postId) {
        return Result.success(communityService.toggleLike(UserContext.get().getUserId(), postId));
    }

    /**
     * 发表评论(敏感词过滤)
     */
    @PostMapping("/comment")
    public Result<Long> comment(@Validated @RequestBody CommentCreateDTO dto) {
        return Result.success(communityService.comment(UserContext.get().getUserId(), dto));
    }

    /**
     * 动态评论列表
     */
    @GetMapping("/{postId}/comments")
    public Result<List<CommentVO>> comments(@PathVariable Long postId) {
        return Result.success(communityService.comments(postId));
    }

    /**
     * 关注/取消关注
     */
    @PostMapping("/follow/{userId}")
    public Result<Boolean> follow(@PathVariable Long userId) {
        return Result.success(communityService.toggleFollow(UserContext.get().getUserId(), userId));
    }

    /**
     * 食谱收藏切换(带分类)
     */
    @PostMapping("/recipe/{recipeId}/collect")
    public Result<Boolean> collectRecipe(@PathVariable Long recipeId,
                                         @RequestParam(required = false) String category) {
        return Result.success(communityService.toggleCollectRecipe(
                UserContext.get().getUserId(), recipeId, category));
    }

    /**
     * 我的食谱收藏夹(按分类分组)
     */
    @GetMapping("/recipe/folder")
    public Result<Map<String, List<Map<String, Object>>>> folder() {
        return Result.success(communityService.collectFolder(UserContext.get().getUserId()));
    }

    /**
     * 食谱评分反馈(重复评分覆盖)
     */
    @PostMapping("/recipe/rating")
    public Result<Void> rate(@Validated @RequestBody RecipeRatingDTO dto) {
        communityService.rateRecipe(UserContext.get().getUserId(), dto);
        return Result.success();
    }

    /**
     * 食谱平均评分
     */
    @GetMapping("/recipe/{recipeId}/score")
    public Result<Map<String, Object>> score(@PathVariable Long recipeId) {
        return Result.success(communityService.recipeScore(recipeId));
    }

    /**
     * 举报动态/评论
     */
    @PostMapping("/report")
    public Result<Void> report(@Validated @RequestBody ReportCreateDTO dto) {
        communityService.report(UserContext.get().getUserId(), dto);
        return Result.success();
    }
}