package com.finance_tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.dto.BackupExportRequestDTO;
import com.finance_tracker.dto.BackupMetadataDTO;
import com.finance_tracker.service.BackupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.finance_tracker.utils.security.JwtService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BackupController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})
class BackupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private BackupService backupService;

    private void setAuth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("42", null, Collections.emptyList()));
    }

    @Test
    void exportBackup_validRequest_returnsOctetStream() throws Exception {
        setAuth();
        when(backupService.exportBackup(eq(42L), anyString())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(post("/api/backup/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BackupExportRequestDTO("password123"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("finora-backup-")));
    }

    @Test
    void importBackup_emptyFile_returnsBadRequest() throws Exception {
        setAuth();
        MockMultipartFile emptyFile = new MockMultipartFile("file", "backup.enc", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/api/backup/import")
                        .file(emptyFile)
                        .param("password", "password123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void importBackup_shortPassword_returnsBadRequest() throws Exception {
        setAuth();
        MockMultipartFile file = new MockMultipartFile("file", "backup.enc", "application/octet-stream", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/backup/import")
                        .file(file)
                        .param("password", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void importBackup_validRequest_returnsMetadata() throws Exception {
        setAuth();
        MockMultipartFile file = new MockMultipartFile("file", "backup.enc", "application/octet-stream", new byte[]{1, 2, 3});
        BackupMetadataDTO meta = BackupMetadataDTO.builder().userId(42L).expenseCount(10).build();
        when(backupService.importBackup(eq(42L), any(), anyString())).thenReturn(meta);

        mockMvc.perform(multipart("/api/backup/import")
                        .file(file)
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(42));
    }
}
