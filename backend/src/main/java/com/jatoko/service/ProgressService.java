/*
 * JaToKo (Japanese-to-Korean Translator)
 * Copyright (C) 2025 The JaToKo Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jatoko.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ProgressService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String clientId) {
        // 타임아웃 30분 (대용량 파일 처리 고려)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitter.onCompletion(() -> {
            log.info("SSE emitter completed: {}", clientId);
            emitters.remove(clientId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE emitter timed out: {}", clientId);
            emitter.complete();
            emitters.remove(clientId);
        });

        emitter.onError((e) -> {
            log.error("SSE emitter error: {}", clientId, e);
            emitters.remove(clientId);
        });

        emitters.put(clientId, emitter);
        log.info("SSE emitter created for client: {}", clientId);
        return emitter;
    }

    public void sendProgress(String clientId, String message, int percentage) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(Map.of(
                                "message", message,
                                "percentage", percentage
                        )));
            } catch (IOException e) {
                log.error("Failed to send progress to client: {}", clientId, e);
                emitters.remove(clientId);
            }
        }
    }

    public void complete(String clientId) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("Translation finished"));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to send completion event: {}", clientId, e);
            } finally {
                emitters.remove(clientId);
            }
        }
    }
    
    public void sendError(String clientId, String errorMessage) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("message", errorMessage)));
                // 에러 후에는 연결 종료
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to send error event: {}", clientId, e);
            } finally {
                emitters.remove(clientId);
            }
        }
    }
}
