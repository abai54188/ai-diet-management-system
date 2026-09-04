package com.diet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.CommunityPost;
import com.diet.mapper.CommunityPostMapper;
import com.diet.service.CommunityPostService;
import org.springframework.stereotype.Service;

/**
 * 社区动态 服务实现类
 *
 * @author diet
 */
@Service
public class CommunityPostServiceImpl extends ServiceImpl<CommunityPostMapper, CommunityPost> implements CommunityPostService {
}