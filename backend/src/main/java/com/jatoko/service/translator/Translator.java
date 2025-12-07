package com.jatoko.service.translator;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.TextTranslationOptions;

@Service
public class Translator {
    private static final Logger logger = LoggerFactory.getLogger(Translator.class);
    private static final int MAX_RETRIES = 3;
    private static final String SOURCE_LANG = "ja";
    private static final String TARGET_LANG = "ko";

    private final DeepLClient client;
    private final String glossaryId;

    public Translator(
            @Value("${deepl.auth-key}") String authKey,
            @Value("${deepl.glossary-id:#{null}}") String glossaryId) {
        this.client = new DeepLClient(authKey);
        this.glossaryId = glossaryId;

        if (glossaryId == null || glossaryId.trim().isEmpty()) {
            logger.info("DeepL 용어집 ID가 설정되지 않았습니다. 용어집 없이 번역합니다.");
        } else {
            logger.info("DeepL 용어집 ID: {}", glossaryId);
        }
    }

    public List<String> translate(List<String> texts) throws DeepLException, InterruptedException {
        TextTranslationOptions options = createTranslationOptions();
        List<TextResult> translated = executeWithRetry(texts, options);
        return extractTranslatedTexts(translated);
    }

    private TextTranslationOptions createTranslationOptions() {
        TextTranslationOptions options = new TextTranslationOptions();
        if (glossaryId != null && !glossaryId.trim().isEmpty()) {
            options.setGlossaryId(glossaryId);
            logger.debug("용어집 사용: {}", glossaryId);
        } else {
            logger.debug("용어집 없이 번역");
        }
        return options;
    }

    private List<TextResult> executeWithRetry(List<String> texts, TextTranslationOptions options)
            throws DeepLException, InterruptedException {
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {
                return client.translateText(texts, SOURCE_LANG, TARGET_LANG, options);
            } catch (DeepLException e) {
                if (shouldRetry(e, retry)) {
                    waitBeforeRetry(retry);
                } else {
                    throw e;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }
        throw new DeepLException("Translation failed after " + MAX_RETRIES + " attempts");
    }

    private boolean shouldRetry(DeepLException e, int currentRetry) {
        if (currentRetry >= MAX_RETRIES - 1) {
            return false;
        }
        String errorMessage = e.getMessage();
        return errorMessage != null && (
            errorMessage.contains("Too many requests") ||
            errorMessage.contains("high load") ||
            errorMessage.contains("429")
        );
    }

    private void waitBeforeRetry(int retry) throws InterruptedException {
        int waitSeconds = 2 * (retry + 1);
        logger.info("DeepL API 부하 감지. {}초 대기 후 재시도... ({}/{})",
            waitSeconds, retry + 1, MAX_RETRIES);
        TimeUnit.SECONDS.sleep(waitSeconds);
    }

    private List<String> extractTranslatedTexts(List<TextResult> translated) {
        return translated.stream()
            .map(result -> {
                String text = result.getText();
                logger.debug("번역 결과: {}", text);
                return text;
            })
            .collect(Collectors.toList());
    }
}