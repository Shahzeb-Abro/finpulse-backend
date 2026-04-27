package com.finpulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpulse.dto.request.MessageDto;
import com.finpulse.dto.response.AssistantResponse;
import com.finpulse.entity.ChatMessage;
import com.finpulse.entity.ChatSession;
import com.finpulse.entity.User;
import com.finpulse.enums.ChatRole;
import com.finpulse.repository.ChatMessageRepository;
import com.finpulse.repository.ChatSessionRepository;
import com.finpulse.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssistantService {
    private final ChatClient chatClient;
    private final AssistantPotTools assistantPotTools;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Value("${assistant.mock.enabled:false}")
    private boolean mockEnabled;

    private static final String SYSTEM_PROMPT = """
            You are FinPulse Assistant, a personal finance AI.
            You can read and manage the user's savings pots, budgets, and transactions.
            
            IMPORTANT RULES:
            1. Before taking ANY action (create, edit, delete, add money, withdraw), 
               you MUST ask the user to confirm first.
            2. Only proceed with an action after the user explicitly says "yes" or "confirm".
            3. Be concise and friendly.
            4. Use plain text, no markdown.
            5. When you don't have enough info to complete an action, ask for it.
            
            FORMATTING RULES:
                    - Use **bold** for important numbers and names only
                    - Use simple numbered lists (1. 2. 3.)
                    - Use plain dashes for bullet points (- item)
                    - NO headers (no ## or ###)
                    - NO horizontal rules (no ---)
                    - Keep responses short and conversational
                    - Add a blank line between paragraphs
            """;

    public void streamChat(String userMessage, List<MessageDto> history, Long sessionId, OutputStream outputStream) throws Exception {
        if (mockEnabled) {
            mockStream(userMessage, outputStream);
            return;
        }

        // Get or create chat session
        ChatSession session = getOrCreateSession(sessionId, userMessage);

        // Save the user message
        saveMessage(session, userMessage, ChatRole.USER);

        // Build message for LLM Model (Claude, Gemini etc)
        List<Message> messages = buildMessages(userMessage, history);

        // Call LLM Model
        ChatResponse response = chatClient.prompt()
                .messages(messages)
                .tools(assistantPotTools)
                .call()
                .chatResponse();

        AssistantMessage assistantMessage = response.getResult().getOutput();
        StringBuilder fullContent = new StringBuilder();

        if (assistantMessage.hasToolCalls()) {
            messages.add(assistantMessage);

            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                String toolResult = executeToolCall(toolCall);

                messages.add(new ToolResponseMessage(List.of(
                        new ToolResponseMessage.ToolResponse(
                                toolCall.id(),
                                toolCall.name(),
                                toolResult
                        )
                )));
            }

            chatClient.prompt()
                    .messages(messages)
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        try {
                            fullContent.append(chunk);

                            if (chunk.equals("\n") || chunk.contains("\n")) {
                                String escaped = chunk.replace("\n", "\\n");
                                outputStream.write(("data: " + escaped + "\n\n").getBytes());
                            } else {
                                outputStream.write(("data: " + chunk + "\n\n").getBytes());
                            }
                            outputStream.flush();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            outputStream.write(("session: " + session.getId() + "\n\n").getBytes());
                            outputStream.write("data: [DONE]\n\n".getBytes());
                            outputStream.flush();

                            // Save the fullContent AI Response Message
                            saveMessage(session, fullContent.toString(), ChatRole.ASSISTANT);
                        } catch (Exception ignored) {
                        }
                    })
                    .blockLast();
        } else {
            String content = assistantMessage.getText();
            saveMessage(session, content, ChatRole.ASSISTANT);

            // Stream line by line to preserve newlines
            String[] lines = content.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                // Stream each word in the line
                if (!line.isEmpty()) {
                    String[] words = line.split(" ");
                    for (String word : words) {
                        if (!word.isEmpty()) {
                            outputStream.write(("data: " + word + " \n\n").getBytes());
                            outputStream.flush();
                            Thread.sleep(20);
                        }
                    }
                }

                // Send newline between lines (except after last line)
                if (i < lines.length - 1) {
                    outputStream.write("data: \\n\n\n".getBytes());
                    outputStream.flush();
                }
            }

            outputStream.write(("session: " + session.getId() + "\n\n").getBytes());
            outputStream.write("data: [DONE]\n\n".getBytes());
            outputStream.flush();
        }
    }

    private ChatSession getOrCreateSession(Long sessionId, String firstMessage) {
        if (sessionId != null) {
            return chatSessionRepository.findById(sessionId)
                    .orElseGet(() -> createNewSession(firstMessage));
        }

        return createNewSession(firstMessage);
    }

    private ChatSession createNewSession(String firstMessage) {
        User loggedInUser = securityUtils.getCurrentUser();
        String title = firstMessage.length() > 47
                ? firstMessage.substring(0, 47) + "..."
                : firstMessage;

        ChatSession session = ChatSession.builder()
                .title(title)
                .user(loggedInUser)
                .activeFlag(Boolean.TRUE)
                .build();

        return chatSessionRepository.save(session);
    }

    private void saveMessage(ChatSession session, String content, ChatRole role) {
        ChatMessage message = ChatMessage.builder()
                .content(content)
                .role(role)
                .chatSession(session)
                .activeFlag(Boolean.TRUE)
                .build();

        chatMessageRepository.save(message);
    }

    private List<Message> buildMessages(String userMessage, List<MessageDto> history) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        if (history != null) {
            for (MessageDto msg : history) {
                if ("user".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else {
                    messages.add(new AssistantMessage(msg.getContent()));
                }
            }
        }

        messages.add(new UserMessage(userMessage));
        return messages;
    }

    private String executeToolCall(AssistantMessage.ToolCall toolCall) {
        try {
            JsonNode args = objectMapper.readTree(toolCall.arguments());

            return switch (toolCall.name()) {
                case "GetAllPots" -> assistantPotTools.getAllPots();
                case "GetAvailablePotThemes" -> assistantPotTools.getAvailablePotThemes();
                case "CreatePot" -> assistantPotTools.createPot(
                        args.get("name").asText(),
                        args.get("targetAmount").asDouble(),
                        args.get("themeId").asLong()
                );
                case "EditPot" -> assistantPotTools.editPot(
                        args.get("potId").asLong(),
                        args.get("name").asText(),
                        args.get("targetAmount").asDouble(),
                        args.get("themeId").asLong()
                );
                case "DeletePot" -> assistantPotTools.deletePot(
                        args.get("potId").asLong()
                );
                case "AddMoneyToPot" -> assistantPotTools.addMoneyToPot(
                        args.get("potId").asLong(),
                        args.get("amount").asDouble()
                );
                case "WithdrawMoneyFromPot" -> assistantPotTools.withdrawMoneyFromPot(
                        args.get("potId").asLong(),
                        args.get("amount").asDouble()
                );
                default -> "Unknown tool: " + toolCall.name();
            };
        } catch (Exception e) {
            return "Tool execution error: " + e.getMessage();
        }
    }

    private AssistantResponse mockResponse(String userMessage) {
        return new AssistantResponse(buildMockReply(userMessage), null);
    }

    private void mockStream(String userMessage, OutputStream outputStream) throws Exception {
        String reply = buildMockReply(userMessage);
        String[] words = reply.split(" ");
        for (String word : words) {
            outputStream.write(("data: " + word + "\n\n").getBytes());
            outputStream.flush();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
        outputStream.write("data: [DONE]\n\n".getBytes());
        outputStream.flush();
    }


    private String buildMockReply(String userMessage) {
        String lower = userMessage.toLowerCase();
        if (lower.contains("pot")) {
            return "You have 2 savings pots. Your Savings pot has 400.00 saved towards a 30000.00 target. Your Turkey Trip pot has 0.00 saved towards a 2500.00 target — consider adding some money to get started!";
        } else if (lower.contains("budget")) {
            return "I can see your budgets but need more data connected to give a full breakdown. Try asking about specific categories like food or transport.";
        } else if (lower.contains("spend") || lower.contains("transaction")) {
            return "Based on your recent transactions, your biggest spending categories appear to be food and transport. Would you like a detailed breakdown?";
        } else if (lower.contains("hello") || lower.contains("hi")) {
            return "Hello! I am your **FinPulse** assistant. I can help you understand your savings pots, budgets, and spending patterns. What would you like to know?";
        } else {
            return "I am here to help with your finances!\n\n You can ask me about your savings pots, budgets, spending patterns, or any financial questions you have.";
        }
    }

    public AssistantResponse chat(String userMessage) {
        if (mockEnabled) return mockResponse(userMessage);

        String reply = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .tools(assistantPotTools)
                .call()
                .content();

        return new AssistantResponse(reply, null);
    }

    public AssistantResponse chat(String userMessage, List<MessageDto> history) {
        if (mockEnabled) return mockResponse(userMessage);

        var promptSpec = chatClient.prompt()
                .system(SYSTEM_PROMPT);

        // add history so assistant remembers the conversation
        if (history != null) {
            for (MessageDto msg : history) {
                if ("user".equals(msg.getRole())) {
                    promptSpec = promptSpec.user(msg.getContent());
                } else {
                    promptSpec = promptSpec.system(msg.getContent());
                }
            }
        }

        String reply = promptSpec
                .user(userMessage)
                .tools(assistantPotTools)
                .call()
                .content();

        return new AssistantResponse(reply, null);
    }

}
