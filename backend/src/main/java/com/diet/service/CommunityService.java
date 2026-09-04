package com.diet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diet.dto.CommentCreateDTO;
import com.diet.dto.CommentVO;
import com.diet.dto.PostFeedVO;
import com.diet.dto.PostPublishDTO;
import com.diet.dto.RecipeRatingDTO;

import java.util.List;
import java.util.Map;

/**
 * 社区服务接口
 *
 * @author diet
 */
public interface CommunityService {

    /**
     * 发布动态(敏感词过滤)
     */
    Long publish(Long userId, PostPublishDTO dto);

    /**
     * 信息流: sort=hot(热门:点赞+评论加权)/new(最新)/follow(关注的人)
     */
    IPage<PostFeedVO> feed(Long userId, long current, long size, String sort);

    /**
     * 查询指定用户的动态(个人主页)
     */
    IPage<PostFeedVO> userPosts(Long visitorId, Long targetUserId, long current, long size);

    /**
     * 点赞/取消点赞(幂等切换)
     */
    boolean toggleLike(Long userId, Long postId);

    /**
     * 发表评论(敏感词过滤)
     */
    Long comment(Long userId, CommentCreateDTO dto);

    /**
     * 查询动态评论列表
     */
    List<CommentVO> comments(Long postId);

    /**
     * 关注/取消关注(幂等切换)
     */
    boolean toggleFollow(Long followerId, Long followeeId);

    /**
     * 个人主页数据(粉丝数/关注数/动态数)
     */
    Map<String, Object> profile(Long visitorId, Long targetUserId);

    /**
     * 收藏/取消收藏食谱(带分类文件夹)
     */
    boolean toggleCollectRecipe(Long userId, Long recipeId, String category);

    /**
     * 食谱收藏夹列表(按分类分组)
     */
    Map<String, List<Map<String, Object>>> collectFolder(Long userId);

    /**
     * 食谱评分与反馈(重复评分覆盖)
     */
    void rateRecipe(Long userId, RecipeRatingDTO dto);

    /**
     * 食谱平均评分
     */
    Map<String, Object> recipeScore(Long recipeId);

    /**
     * 举报内容
     */
    void report(Long reporterId, com.diet.dto.ReportCreateDTO dto);
}