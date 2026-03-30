package com.edwin.edwin_ai_agent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.edwin.edwin_ai_agent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import cn.hutool.json.JSONUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存了工具调用信息的响应
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用内置的工具调用机制，自己维护上下文
    private final ChatOptions chatOptions;

    private String lastAssistantText = "";

    private String block(String title, String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        return title + "：\n" + content.trim();
    }

    private String buildStreamBubble(String kind, int step, String title, String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }

        return JSONUtil.toJsonStr(Map.of(
                "kind", kind,
                "step", step,
                "title", title,
                "content", content.trim()
        ));
    }

    private String mergeBlocks(String... blocks) {
        return Arrays.stream(blocks)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n\n"));
    }
    protected static final String STREAM_MESSAGE_SPLITTER = "\n<<__AGENT_STREAM_SPLITTER__>>\n";

    protected String joinStreamMessages(String... messages) {
        return Arrays.stream(messages)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(STREAM_MESSAGE_SPLITTER));
    }

    private void sendStreamMessages(SseEmitter emitter, String payload) throws IOException {
        if (StrUtil.isBlank(payload)) {
            return;
        }

        String[] items = payload.split(Pattern.quote(STREAM_MESSAGE_SPLITTER));
        for (String item : items) {
            if (StrUtil.isNotBlank(item)) {
                emitter.send(item);
            }
        }
    }

    @SafeVarargs
    private final String buildPayload(Map<String, Object>... bubbles) {
        List<Map<String, Object>> payload = Arrays.stream(bubbles)
                .filter(item -> item != null)
                .collect(Collectors.toList());

        if (payload.isEmpty()) {
            return "";
        }

        return JSONUtil.toJsonStr(payload);
    }

    private Map<String, Object> buildBubble(String kind, String title, String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        Map<String, Object> bubble = new LinkedHashMap<>();
        bubble.put("kind", kind);
        bubble.put("step", getCurrentStep());
        bubble.put("title", title);
        bubble.put("content", content.trim());
        return bubble;
    }

    @Override
    public String step() {
        try {
            boolean shouldAct = think();

            if (!shouldAct) {
                setState(AgentState.FINISHED);

                String processLog = getCurrentStep() > 1
                        ? "已完成工具调用，当前直接输出最终结果。"
                        : "本轮未调用任何工具，直接根据上下文生成回复。";

                String finalReply = StringUtils.hasText(lastAssistantText)
                        ? lastAssistantText
                        : "任务已完成，但未生成可展示的最终回复。";

                return buildPayload(
                        buildBubble("thought", "思考过程", processLog),
                        buildBubble("final", "最终回复", finalReply)
                );
            }

            return act();
        } catch (Exception e) {
            log.error("步骤执行失败", e);
            setState(AgentState.ERROR);
            return buildPayload(
                    buildBubble("error", "系统错误", e.getMessage())
            );
        }
    }


    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .internalToolExecutionEnabled(false)  //旧版本代码为 .withProxyToolCalls(true)
                .build();
    }

    @Override
    public boolean think() {
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }

        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);

        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();

            this.toolCallChatResponse = chatResponse;

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            String result = assistantMessage.getText();
            this.lastAssistantText = result;

            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();

            log.info(getName() + "的思考: " + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");

            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s",
                            toolCall.name(),
                            toolCall.arguments()))
                    .collect(Collectors.joining("\n"));

            if (StringUtils.hasText(toolCallInfo)) {
                log.info(toolCallInfo);
            }

            if (toolCallList.isEmpty()) {
                getMessageList().add(assistantMessage);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题: " + e.getMessage(), e);
            this.lastAssistantText = "处理时遇到错误: " + e.getMessage();
            getMessageList().add(new AssistantMessage(this.lastAssistantText));
            return false;
        }
    }



    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "";
        }

        AssistantMessage assistantMessage = toolCallChatResponse.getResult().getOutput();
        String assistantText = assistantMessage.getText();

        if (StringUtils.hasText(assistantText)) {
            this.lastAssistantText = assistantText;
        }

        String toolCallInfo = assistantMessage.getToolCalls().stream()
                .map(toolCall -> String.format("工具名称：%s\n参数：%s",
                        toolCall.name(),
                        toolCall.arguments()))
                .collect(Collectors.joining("\n\n"));

        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);

        setMessageList(toolExecutionResult.conversationHistory());

        ToolResponseMessage toolResponseMessage =
                (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());

        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));

        String toolResultLog = toolResponseMessage.getResponses().stream()
                .filter(response -> !"doTerminate".equals(response.name()))
                .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n\n"));

        if (StringUtils.hasText(toolResultLog)) {
            log.info(toolResultLog);
        }

        if (terminateToolCalled) {
            setState(AgentState.FINISHED);

            String finalReply = StringUtils.hasText(lastAssistantText)
                    ? lastAssistantText
                    : "任务已结束，但未生成可展示的最终回复。";

            String finalThought = mergeBlocks(
                    block("准备调用工具", toolCallInfo),
                    block("工具返回结果", toolResultLog),
                    block("执行结论", "工具流程已完成，开始输出最终回复。")
            );

            return buildPayload(
                    buildBubble("thought", "思考过程", finalThought),
                    buildBubble("final", "最终回复", finalReply)
            );
        }

        String thoughtContent = mergeBlocks(
                block("当前思考", assistantText),
                block("准备调用工具", toolCallInfo),
                block("工具返回结果", toolResultLog)
        );

        return buildPayload(
                buildBubble("thought", "思考过程", thoughtContent)
        );
    }
}


