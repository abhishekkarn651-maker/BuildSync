package com.buildsync.media;

import com.buildsync.media.application.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MediaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StorageService storageService;

    @Test
    @DisplayName("GET /api/v1/media/avatars/{filename} streams avatar publicly with cache headers")
    void getAvatar_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "integration-avatar.png", "image/png", new byte[]{1, 2, 3, 4});
        String avatarUrl = storageService.storeAvatar(file);
        String filename = avatarUrl.replace("/api/v1/media/avatars/", "");

        mockMvc.perform(get("/api/v1/media/avatars/" + filename))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().exists("Cache-Control"));
    }

    @Test
    @DisplayName("GET /api/v1/media/avatars/{filename} returns 404 for missing file")
    void getAvatar_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/media/avatars/non-existent-image-file.jpg"))
                .andExpect(status().isNotFound());
    }
}

