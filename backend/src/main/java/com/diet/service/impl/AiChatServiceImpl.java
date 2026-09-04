package com.diet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.dto.ChatMessageVO;
import com.diet.dto.ChatSendDTO;
import com.diet.dto.DailySummaryVO;
import com.diet.dto.HealthCalcVO;
import com.diet.dto.RiskReportVO;
import com.diet.entity.AiChatMessage;
import com.diet.entity.SysUser;
import com.diet.mapper.AiChatMessageMapper;
import com.diet.service.AiChatService;
import com.diet.service.HealthCalcService;
import com.diet.service.UserService;
import com.diet.util.AiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI营养问答服务实现类
 * 回答必须关联当前用户的健康档案与近期摄入数据(个性化硬性约束)
 *
 * @author diet
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    /** 系统提示词 */
    private static final String SYSTEM_PROMPT =
            "你是一名专业注册营养师，基于中国居民膳食指南回答用户问题，结合用户自身健康数据给出个性化建议，"
                    + "禁止给出医疗诊断，回复末尾必须包含「仅供饮食参考，不构成医疗建议。」";

    /** Prompt模板缓存 */
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    private final AiChatMessageMapper chatMapper;
    private final AiClient aiClient;
    private final UserService userService;
    private final HealthCalcService healthCalcService;

    /** 是否本地模拟模式 */
    @Value("${ai.mock:true}")
    private boolean mock;

    public AiChatServiceImpl(AiChatMessageMapper chatMapper, AiClient aiClient,
                             UserService userService, HealthCalcService healthCalcService) {
        this.chatMapper = chatMapper;
        this.aiClient = aiClient;
        this.userService = userService;
        this.healthCalcService = healthCalcService;
    }

    /**
     * 发送问题: 组装用户健康数据 -> AI回答 -> 保存一问一答
     */
    @Override
    public ChatMessageVO send(Long userId, ChatSendDTO dto) {
        // 1. 组装用户健康数据上下文(个性化硬性约束)
        String healthContext = buildHealthContext(userId);
        // 2. 生成回答(真实AI或本地模拟)
        String answer;
        if (mock) {
            answer = mockAnswer(userId, dto.getQuestion(), healthContext);
        } else {
            String userPrompt = loadPrompt("chat_prompt.txt")
                    .replace("{{healthProfile}}", healthContext)
                    .replace("{{weeklyIntake}}", buildWeeklyIntake(userId))
                    .replace("{{question}}", dto.getQuestion());
            answer = aiClient.chat(SYSTEM_PROMPT, userPrompt);
        }
        // 3. 保存一问一答并返回AI消息
        saveMessage(userId, "USER", dto.getQuestion());
        return saveMessage(userId, "ASSISTANT", answer);
    }

    /**
     * 查询历史对话(按时间正序, 最近100条)
     */
    @Override
    public List<ChatMessageVO> history(Long userId) {
        List<AiChatMessage> messages = chatMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getUserId, userId)
                .orderByDesc(AiChatMessage::getCreateTime)
                .last("LIMIT 100"));
        // 转换并按时间正序返回
        return messages.stream()
                .map(this::toVO)
                .sorted((a, b) -> a.getCreateTime().compareTo(b.getCreateTime()))
                .toList();
    }

    /**
     * 保存消息并返回视图对象
     */
    private ChatMessageVO saveMessage(Long userId, String role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        chatMapper.insert(message);
        return toVO(message);
    }

    /**
     * 组装用户健康档案文本(个性化数据注入)
     */
    private String buildHealthContext(Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("性别:%s, 年龄:%s岁, 身高:%scm, 体重:%skg, 目标体重:%s, 活动量:%s, 健康目标:%s, 忌口:%s",
                user.getGender() != null && user.getGender() == 1 ? "男" : "女",
                user.getAge() != null ? user.getAge() : "未填",
                user.getHeight() != null ? user.getHeight() : "未填",
                user.getWeight() != null ? user.getWeight() : "未填",
                user.getTargetWeight() != null ? user.getTargetWeight() + "kg" : "未设置",
                activityText(user.getActivityLevel()),
                goalText(user.getHealthGoal()),
                user.getAllergy() == null || user.getAllergy().isBlank() ? "无" : user.getAllergy()));
        // 追加推荐摄入指标
        try {
            HealthCalcVO calc = healthCalcService.getProfile(userId);
            sb.append(String.format("; 每日推荐:%skcal, 蛋白目标%s g, 碳水%s g, 脂肪%s g",
                    calc.getDailyCalorie(), calc.getProteinGram(), calc.getCarbGram(), calc.getFatGram()));
        } catch (BusinessException ignored) {
            // 档案未完善时跳过推荐值
        }
        return sb.toString();
    }

    /**
     * 组装近7日摄入概况
     */
    private String buildWeeklyIntake(Long userId) {
        try {
            RiskReportVO report = healthCalcService.riskAnalysis(userId, 7);
            return String.format("日均摄入%skcal(推荐%s), 蛋白%s g, 钠%s mg, 纤维%s g, 主要风险:%s",
                    report.getAvgCalorie(), report.getAvgRecommendCalorie(), report.getAvgProtein(),
                    report.getAvgSodium(), report.getAvgFiber(),
                    report.getRisks().isEmpty() ? "无" : report.getRisks().stream()
                            .map(RiskReportVO.RiskItem::getName).limit(3).toList());
        } catch (BusinessException e) {
            return "近7日暂无完整饮食记录";
        }
    }

    /**
     * 本地模拟回答: 基于用户数据的规则应答(联调用)
     */
    private String mockAnswer(Long userId, String question, String healthContext) {
        String q = question.toLowerCase();
        StringBuilder sb = new StringBuilder();
        sb.append("根据您的健康档案（").append(healthContext).append("），为您解答：\n\n");
        if (q.contains("热量") || q.contains("卡") || q.contains(" kcal")) {
            sb.append("建议每日摄入以推荐值为基准上下浮动10%。若在减脂期，优先压缩精制碳水与油脂，");
            sb.append("保证蛋白质达标以维持肌肉量与饱腹感。\n");
        } else if (q.contains("蛋白")) {
            sb.append("蛋白质建议分布于三餐，每餐一掌心的瘦肉/鱼虾/豆制品。训练日可适当增加，");
            sb.append("植物蛋白(豆腐、豆浆)与动物蛋白搭配吸收更好。\n");
        } else if (q.contains("减") || q.contains("瘦")) {
            sb.append("减脂的核心是稳定的热量缺口(每日300-500kcal)，每周减重0.5-1kg最为安全，");
            sb.append("配合每周150分钟中等强度运动效果更佳。\n");
        } else if (q.contains("盐") || q.contains("钠")) {
            sb.append("每日食盐建议不超过5g(钠2000mg)，注意酱油、蚝油、酱料中的隐形钠，");
            sb.append("可用葱姜蒜、香辛料替代部分咸味。\n");
        } else if (q.contains("纤维") || q.contains("蔬菜") || q.contains("便秘")) {
            sb.append("每日蔬菜300-500g(深色占一半)，主食掺入燕麦、糙米、豆类，");
            sb.append("膳食纤维目标25-30g，同时保证足量饮水。\n");
        } else {
            sb.append("均衡饮食的基本框架: 每餐一份主食(粗细搭配)+一份蛋白质+两份蔬菜，");
            sb.append("控油25-30g/天、盐<5g/天、添加糖<25g/天。\n");
        }
        sb.append("\n如症状持续或涉及疾病，请及时就医。\n仅供饮食参考，不构成医疗建议。");
        return sb.toString();
    }

    /** 加载Prompt模板 */
    private String loadPrompt(String fileName) {
        return promptCache.computeIfAbsent(fileName, k -> {
            try {
                return new String(new ClassPathResource("prompts/" + k).getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new BusinessException("Prompt模板读取失败: " + k);
            }
        });
    }

    /** 实体转视图对象 */
    private ChatMessageVO toVO(AiChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    /** 活动量文本 */
    private String activityText(String level) {
        if (level == null) {
            return "未填";
        }
        return switch (level) {
            case "SEDENTARY" -> "久坐";
            case "MODERATE" -> "中度活动";
            case "HIGH" -> "高度活动";
            default -> "轻度活动";
        };
    }

    /** 健康目标文本 */
    private String goalText(String goal) {
        if (goal == null) {
            return "维持体重";
        }
        return switch (goal) {
            case "LOSE" -> "减脂";
            case "GAIN" -> "增重";
            default -> "维持体重";
        };
    }
}